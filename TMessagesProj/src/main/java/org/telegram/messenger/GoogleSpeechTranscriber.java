package org.telegram.messenger;

import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * LuminaGram — cloud speech-to-text via Google Cloud Speech-to-Text v1 (bring-your-own
 * API key). Base64-encodes the voice file and POSTs it as JSON to {@code speech:recognize},
 * then joins the returned transcript alternatives.
 *
 * Runs on {@link Utilities#globalQueue} and delivers {@link Callback} on that background
 * thread, as the {@link LuminaTranscriber} contract requires. The shared
 * {@link LuminaTranslatorUtil#http} helper isn't reused because it targets the translator
 * callback type and re-marshals results onto the UI thread; only its {@code readAll}
 * utility is reused for the response body.
 *
 * Telegram voice notes are 48 kHz Opus in an OGG container, so the request is pinned to
 * {@code OGG_OPUS} / 48000 Hz. The API key is supplied in the query string because that is
 * Google's documented API-key auth for this endpoint; it is never logged, and errors
 * surfaced to the callback carry the HTTP status only.
 */
final class GoogleSpeechTranscriber implements LuminaTranscriber {

    // Google synchronous recognize accepts ~1 minute / 10 MB of inline audio.
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final int TIMEOUT = 60000;

    @Override
    public String id() {
        return "google";
    }

    @Override
    public String displayName() {
        return "Google Speech";
    }

    @Override
    public boolean isOffline() {
        return false;
    }

    @Override
    public boolean needsKey() {
        return true;
    }

    @Override
    public boolean needsBaseUrl() {
        return false;
    }

    @Override
    public boolean needsModel() {
        return false;
    }

    @Override
    public void transcribe(File audio, String langHint, Callback cb) {
        // Never touch the network on the caller thread; hop onto the shared background queue.
        Utilities.globalQueue.postRunnable(() -> run(audio, langHint, cb));
    }

    private void run(File audio, String langHint, Callback cb) {
        final String key = LuminaConfig.getString("sttKey_" + id(), "").trim();
        if (TextUtils.isEmpty(key)) {
            cb.onError("Missing API key");
            return;
        }
        if (audio == null || !audio.exists() || !audio.isFile()) {
            cb.onError("Audio file not found");
            return;
        }
        final long fileLen = audio.length();
        if (fileLen <= 0) {
            cb.onError("Audio file is empty");
            return;
        }
        if (fileLen > MAX_BYTES) {
            cb.onError("Audio file too large (max 10 MB)");
            return;
        }

        HttpURLConnection c = null;
        try {
            cb.onProgress(0f);
            final byte[] bytes = readFile(audio);
            final String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            final String body = new JSONObject()
                    .put("config", new JSONObject()
                            .put("encoding", "OGG_OPUS")
                            .put("sampleRateHertz", 48000)
                            .put("languageCode", bcp47(langHint)))
                    .put("audio", new JSONObject().put("content", b64))
                    .toString();

            final String url = "https://speech.googleapis.com/v1/speech:recognize?key=" + key;
            c = (HttpURLConnection) new URI(url).toURL().openConnection();
            c.setConnectTimeout(TIMEOUT);
            c.setReadTimeout(TIMEOUT);
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setDoOutput(true);
            final OutputStream os = c.getOutputStream();
            try {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            } finally {
                os.close();
            }

            final int code = c.getResponseCode();
            final InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
            final String raw = LuminaTranslatorUtil.readAll(in);
            if (code >= 400) {
                cb.onError("HTTP " + code);
                return;
            }

            final JSONObject json = new JSONObject(raw);
            final JSONArray results = json.optJSONArray("results");
            final StringBuilder sb = new StringBuilder();
            if (results != null) {
                for (int i = 0; i < results.length(); i++) {
                    final JSONObject r = results.optJSONObject(i);
                    if (r == null) {
                        continue;
                    }
                    final JSONArray alts = r.optJSONArray("alternatives");
                    if (alts == null || alts.length() == 0) {
                        continue;
                    }
                    final JSONObject a0 = alts.optJSONObject(0);
                    if (a0 == null) {
                        continue;
                    }
                    final String t = a0.optString("transcript", "");
                    if (!TextUtils.isEmpty(t)) {
                        if (sb.length() > 0) {
                            sb.append(' ');
                        }
                        sb.append(t.trim());
                    }
                }
            }
            cb.onProgress(1f);
            final String text = sb.toString().trim();
            if (TextUtils.isEmpty(text)) {
                cb.onError("Empty transcription");
            } else {
                cb.onResult(text);
            }
        } catch (Exception e) {
            cb.onError(e.getMessage());
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ignore) {
                }
            }
        }
    }

    private static byte[] readFile(File f) throws Exception {
        final InputStream in = new BufferedInputStream(new FileInputStream(f));
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            final byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
        } finally {
            in.close();
        }
        return bos.toByteArray();
    }

    /**
     * Best-effort ISO language hint -> BCP-47 code Google Speech expects. Region-qualified
     * hints (e.g. "pt-BR") pass through with an upper-cased region; bare codes map to a
     * common regional default; unknown/empty falls back to "en-US".
     */
    private static String bcp47(String hint) {
        if (hint == null) {
            return "en-US";
        }
        final String s = hint.trim().toLowerCase().replace('_', '-');
        if (s.isEmpty()) {
            return "en-US";
        }
        final int dash = s.indexOf('-');
        if (dash > 0) {
            return s.substring(0, dash) + "-" + s.substring(dash + 1).toUpperCase();
        }
        switch (s) {
            case "en": return "en-US";
            case "zh": return "zh-CN";
            case "es": return "es-ES";
            case "pt": return "pt-BR";
            case "fr": return "fr-FR";
            case "de": return "de-DE";
            case "it": return "it-IT";
            case "ru": return "ru-RU";
            case "ja": return "ja-JP";
            case "ko": return "ko-KR";
            case "ar": return "ar-SA";
            case "hi": return "hi-IN";
            case "tr": return "tr-TR";
            case "nl": return "nl-NL";
            case "pl": return "pl-PL";
            case "uk": return "uk-UA";
            case "vi": return "vi-VN";
            case "id": return "id-ID";
            case "th": return "th-TH";
            default: return s;
        }
    }
}
