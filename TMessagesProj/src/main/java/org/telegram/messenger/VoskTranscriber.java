package org.telegram.messenger;

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.util.Locale;

/**
 * Offline voice-to-text engine backed by <a href="https://alphacephei.com/vosk/">Vosk</a>
 * (id {@code "vosk"}). Fully on-device: no network call, no Telegram premium. The per-language
 * acoustic model is downloaded once by {@link LuminaVoskModelManager} into app-private storage.
 *
 * <p>Vosk 0.3.47 API used here: {@code new org.vosk.Model(String path)},
 * {@code new org.vosk.Recognizer(Model, float sampleRate)},
 * {@code boolean acceptWaveForm(short[] buf, int len)}, {@code String getResult()} /
 * {@code String getFinalResult()} (both return JSON {@code {"text":"..."}}), and both
 * {@code Model} and {@code Recognizer} are {@code Closeable}.
 */
final class VoskTranscriber implements LuminaTranscriber {

    /** Samples per acceptWaveForm() call (~0.5 s at 16 kHz). */
    private static final int CHUNK = 8000;

    @Override
    public String id() {
        return "vosk";
    }

    @Override
    public String displayName() {
        return "Vosk (offline)";
    }

    @Override
    public boolean isOffline() {
        return true;
    }

    @Override
    public boolean needsKey() {
        return false;
    }

    @Override
    public boolean needsBaseUrl() {
        return false;
    }

    @Override
    public boolean needsModel() {
        return true;
    }

    @Override
    public void transcribe(File audio, String langHint, Callback cb) {
        final String lang = resolveLang(langHint);
        if (!LuminaVoskModelManager.isModelReady(lang)) {
            cb.onError("Vosk model not downloaded for '" + lang + "'");
            return;
        }
        Utilities.globalQueue.postRunnable(() -> runVosk(audio, lang, cb));
    }

    private void runVosk(File audio, String lang, Callback cb) {
        Model model = null;
        Recognizer recognizer = null;
        try {
            cb.onProgress(0f);
            short[] pcm = LuminaAudioDecoder.decodeToPcm16kMono(audio);
            if (pcm == null || pcm.length == 0) {
                cb.onError("empty audio");
                return;
            }

            model = new Model(LuminaVoskModelManager.modelDir(lang).getAbsolutePath());
            recognizer = new Recognizer(model, 16000f);

            StringBuilder collected = new StringBuilder();
            int offset = 0;
            final int total = pcm.length;
            while (offset < total) {
                int len = Math.min(CHUNK, total - offset);
                short[] buf;
                if (offset == 0 && len == total) {
                    buf = pcm;
                } else {
                    buf = new short[len];
                    System.arraycopy(pcm, offset, buf, 0, len);
                }
                if (recognizer.acceptWaveForm(buf, len)) {
                    appendText(collected, recognizer.getResult());
                }
                offset += len;
                cb.onProgress((float) offset / total);
            }
            appendText(collected, recognizer.getFinalResult());

            cb.onResult(collected.toString().trim());
        } catch (Throwable e) {
            FileLog.e(e);
            cb.onError(e.getMessage() != null ? e.getMessage() : e.toString());
        } finally {
            try {
                if (recognizer != null) {
                    recognizer.close();
                }
            } catch (Throwable ignore) {
            }
            try {
                if (model != null) {
                    model.close();
                }
            } catch (Throwable ignore) {
            }
        }
    }

    /** Pull the "text" field out of a Vosk result JSON and append it. */
    private static void appendText(StringBuilder sb, String json) {
        if (json == null) {
            return;
        }
        try {
            String text = new JSONObject(json).optString("text", "").trim();
            if (text.length() > 0) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(text);
            }
        } catch (Throwable ignore) {
        }
    }

    private static String resolveLang(String langHint) {
        String lang = langHint;
        if (lang == null || lang.trim().isEmpty()) {
            lang = LuminaConfig.getString("voskModelLang", appLang());
        }
        if (lang == null || lang.trim().isEmpty()) {
            lang = "en";
        }
        return LuminaVoskModelManager.normalizeLang(lang);
    }

    private static String appLang() {
        try {
            Locale locale = LocaleController.getInstance().getCurrentLocale();
            if (locale != null) {
                String code = locale.getLanguage();
                if (code != null && !code.isEmpty()) {
                    return code;
                }
            }
        } catch (Throwable ignore) {
        }
        return "en";
    }
}
