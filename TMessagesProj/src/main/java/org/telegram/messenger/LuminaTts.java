package org.telegram.messenger;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * LuminaGram: "reverse voice" — type text, (optionally) translate it into the recipient's
 * language, speak it with the on-device system TTS engine, and send the result as a REAL
 * Telegram voice message (OGG/Opus, {@code voice=true} + waveform + duration) so the other
 * side receives it in the ordinary voice bubble on the official client.
 *
 * <h3>Pipeline</h3>
 * <ol>
 *   <li>{@link android.speech.tts.TextToSpeech#synthesizeToFile} → a local RIFF/WAV file
 *       (offline, free, no cloud). The engine is Android's system TTS; NO voice cloning /
 *       no deep-fake — it is a generic synthetic voice.</li>
 *   <li>{@link #readWavAsMono48k} parses that WAV (16-bit PCM), down-mixes to mono and
 *       linearly resamples to 48&nbsp;kHz — the sample rate the built-in opus recorder wants.</li>
 *   <li>{@link MediaController#luminaEncodePcmToOgg} REUSES the exact native opus pipeline
 *       the microphone recorder uses ({@code startRecord/writeFrame/stopRecord}) to produce a
 *       Telegram-compatible voice {@code .ogg}.</li>
 *   <li>We build a {@code TL_document} with a voice {@code TL_documentAttributeAudio}
 *       (waveform via {@link MediaController#getWaveform}, duration) and hand it to the SAME
 *       {@link SendMessagesHelper.SendMessageParams#of} voice-send path the recorder uses.</li>
 * </ol>
 *
 * <p>Everything is local: system TTS runs on-device and the translation goes through the fork's
 * existing {@link LuminaTranslators} engine. Gated behind {@link LuminaConfig#reverseVoice}
 * (advanced, default OFF).</p>
 *
 * <p><b>Threading.</b> A fresh {@link TextToSpeech} instance is created per request and shut
 * down when the utterance finishes/fails — this keeps us clear of the engine's shared state and
 * needs no init queue. Synthesis + encoding run on the TTS callback thread; the final
 * {@code sendMessage} is marshalled back onto the main thread.</p>
 */
public final class LuminaTts {

    private LuminaTts() {}

    /** Result of a reverse-voice attempt. Callbacks may arrive on any thread. */
    public interface Callback {
        /** The voice message was handed to the send pipeline. */
        void onSent();
        /** Something failed; {@code reason} is a short non-localized tag for logs. */
        void onError(String reason);
    }

    /**
     * Speak {@code spokenText} in {@code langCode} and send it as a voice message to
     * {@code dialogId}. The caller is responsible for translating first (so the text and the
     * TTS voice locale agree); pass the already-translated text here.
     *
     * @param account    account index
     * @param dialogId   target dialog
     * @param spokenText the exact text to synthesize (already translated if desired)
     * @param langCode   BCP-47-ish language code that drives the TTS voice ("en", "zh-hans", ...)
     * @param cb         result callback (nullable)
     */
    public static void speakAndSend(final int account, final long dialogId, final String spokenText,
                                    final String langCode, final Callback cb) {
        final Context ctx = ApplicationLoader.applicationContext;
        if (ctx == null || TextUtils.isEmpty(spokenText)) {
            if (cb != null) cb.onError("empty");
            return;
        }
        final TextToSpeech[] holder = new TextToSpeech[1];
        try {
            holder[0] = new TextToSpeech(ctx, status -> {
                final TextToSpeech engine = holder[0];
                if (status != TextToSpeech.SUCCESS || engine == null) {
                    shutdownQuietly(engine);
                    if (cb != null) cb.onError("tts_init");
                    return;
                }
                try {
                    applyLanguage(engine, langCode);

                    final File wav = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE),
                            "lumina_tts_" + System.currentTimeMillis() + ".wav");
                    final String utteranceId = "luminaReverseVoice";

                    engine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override public void onStart(String id) {}

                        @Override public void onDone(String id) {
                            try {
                                encodeAndSend(account, dialogId, wav, cb);
                            } catch (Throwable e) {
                                FileLog.e(e);
                                if (cb != null) cb.onError("post_tts");
                            } finally {
                                shutdownQuietly(engine);
                            }
                        }

                        @Override public void onError(String id) {
                            safeDelete(wav);
                            shutdownQuietly(engine);
                            if (cb != null) cb.onError("tts_synth");
                        }

                        @Override public void onError(String id, int errorCode) {
                            onError(id);
                        }
                    });

                    final Bundle params = new Bundle();
                    final int r = engine.synthesizeToFile(spokenText, params, wav, utteranceId);
                    if (r != TextToSpeech.SUCCESS) {
                        safeDelete(wav);
                        shutdownQuietly(engine);
                        if (cb != null) cb.onError("tts_synth_start");
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                    shutdownQuietly(engine);
                    if (cb != null) cb.onError("tts_setup");
                }
            });
        } catch (Throwable e) {
            FileLog.e(e);
            shutdownQuietly(holder[0]);
            if (cb != null) cb.onError("tts_create");
        }
    }

    // ---- WAV -> OGG/Opus voice -> send ------------------------------------------------------

    private static void encodeAndSend(final int account, final long dialogId, final File wav,
                                      final Callback cb) {
        final short[] pcm = readWavAsMono48k(wav);
        safeDelete(wav);
        if (pcm == null || pcm.length == 0) {
            if (cb != null) cb.onError("wav_parse");
            return;
        }
        final File ogg = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_AUDIO),
                System.currentTimeMillis() + "_lumina_voice.ogg");
        final boolean ok = MediaController.getInstance().luminaEncodePcmToOgg(pcm, ogg.getAbsolutePath());
        if (!ok || !ogg.exists() || ogg.length() == 0) {
            safeDelete(ogg);
            if (cb != null) cb.onError("ogg_encode");
            return;
        }
        final double durationSec = pcm.length / 48000.0;
        AndroidUtilities.runOnUIThread(() -> sendVoiceDocument(account, dialogId, ogg, durationSec, cb));
    }

    private static void sendVoiceDocument(final int account, final long dialogId, final File ogg,
                                          final double durationSec, final Callback cb) {
        try {
            final TLRPC.TL_document doc = new TLRPC.TL_document();
            doc.dc_id = Integer.MIN_VALUE;
            doc.id = SharedConfig.getLastLocalId();
            SharedConfig.saveConfig();
            doc.user_id = UserConfig.getInstance(account).getClientUserId();
            doc.mime_type = "audio/ogg";
            doc.file_reference = new byte[0];
            doc.date = ConnectionsManager.getInstance(account).getCurrentTime();
            doc.size = (int) ogg.length();

            final TLRPC.TL_documentAttributeAudio attr = new TLRPC.TL_documentAttributeAudio();
            attr.voice = true;
            final byte[] waveform = MediaController.getWaveform(ogg.getAbsolutePath());
            if (waveform != null) {
                attr.waveform = waveform;
                attr.flags |= 4;
            }
            attr.duration = durationSec;
            doc.attributes.add(attr);

            // Same voice-send entry point the microphone recorder uses (see
            // MediaController.stopRecordingInternal): reply/thread threading is intentionally
            // left null for this WIP entry point (documented TODO).
            final SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(
                    doc, null, ogg.getAbsolutePath(), dialogId,
                    null, null, null, null, null, null,
                    true, 0, 0, 0, null, null, false);
            SendMessagesHelper.getInstance(account).sendMessage(params);
            if (cb != null) cb.onSent();
        } catch (Throwable e) {
            FileLog.e(e);
            safeDelete(ogg);
            if (cb != null) cb.onError("send");
        }
    }

    // ---- TTS locale ------------------------------------------------------------------------

    private static void applyLanguage(TextToSpeech engine, String langCode) {
        final Locale locale = localeForLang(langCode);
        if (locale == null) {
            return; // leave engine default
        }
        try {
            final int avail = engine.isLanguageAvailable(locale);
            if (avail != TextToSpeech.LANG_MISSING_DATA && avail != TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(locale);
            }
            // else: keep the engine's default voice rather than failing the whole send.
        } catch (Throwable ignore) {
        }
    }

    /** Map a Lumina/Telegram language code to a TTS {@link Locale}. */
    static Locale localeForLang(String langCode) {
        if (TextUtils.isEmpty(langCode) || "auto".equalsIgnoreCase(langCode)) {
            return null;
        }
        final String c = langCode.trim().toLowerCase();
        switch (c) {
            case "zh":
            case "zh-hans":
            case "zh_cn":
            case "zh-cn":
                return Locale.SIMPLIFIED_CHINESE;
            case "zh-hant":
            case "zh_tw":
            case "zh-tw":
            case "zh-hk":
                return Locale.TRADITIONAL_CHINESE;
            default:
                try {
                    // "pt-br" -> pt-BR etc.
                    return Locale.forLanguageTag(c.replace('_', '-'));
                } catch (Throwable e) {
                    return new Locale(c);
                }
        }
    }

    // ---- WAV parsing + resampling ----------------------------------------------------------

    /**
     * Parse a 16-bit PCM RIFF/WAV file, down-mix to mono and linearly resample to 48&nbsp;kHz.
     * Returns {@code null} on any format we do not handle (non-PCM, non-16-bit, malformed).
     */
    static short[] readWavAsMono48k(File file) {
        if (file == null || !file.exists() || file.length() < 44) {
            return null;
        }
        byte[] data;
        FileInputStream in = null;
        try {
            data = new byte[(int) Math.min(file.length(), Integer.MAX_VALUE)];
            in = new FileInputStream(file);
            int off = 0;
            int read;
            while (off < data.length && (read = in.read(data, off, data.length - off)) > 0) {
                off += read;
            }
        } catch (IOException e) {
            FileLog.e(e);
            return null;
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignore) {}
            }
        }

        // RIFF....WAVE
        if (!(data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'A' && data[10] == 'V' && data[11] == 'E')) {
            return null;
        }

        int channels = 0;
        int sampleRate = 0;
        int bitsPerSample = 0;
        int audioFormat = 0;
        int dataOffset = -1;
        int dataLength = 0;

        int p = 12;
        while (p + 8 <= data.length) {
            final int id0 = data[p] & 0xff, id1 = data[p + 1] & 0xff, id2 = data[p + 2] & 0xff, id3 = data[p + 3] & 0xff;
            final long chunkSize = u32le(data, p + 4);
            final int body = p + 8;
            if (id0 == 'f' && id1 == 'm' && id2 == 't' && id3 == ' ') {
                if (body + 16 > data.length) return null;
                audioFormat = u16le(data, body);
                channels = u16le(data, body + 2);
                sampleRate = (int) u32le(data, body + 4);
                bitsPerSample = u16le(data, body + 14);
            } else if (id0 == 'd' && id1 == 'a' && id2 == 't' && id3 == 'a') {
                dataOffset = body;
                dataLength = (int) Math.min(chunkSize, (long) (data.length - body));
                break; // audio data reached
            }
            // chunks are word-aligned (pad byte if odd)
            long advance = chunkSize + (chunkSize & 1);
            p = body + (int) advance;
        }

        // Accept PCM (1) 16-bit, or WAVE_FORMAT_EXTENSIBLE (0xFFFE) that still carries 16-bit PCM.
        if (dataOffset < 0 || channels <= 0 || sampleRate <= 0 || bitsPerSample != 16
                || (audioFormat != 1 && audioFormat != 0xFFFE)) {
            return null;
        }

        final int bytesPerSample = 2;
        final int frameBytes = bytesPerSample * channels;
        final int frames = dataLength / frameBytes;
        if (frames <= 0) {
            return null;
        }

        // Down-mix to mono.
        final short[] mono = new short[frames];
        int sp = dataOffset;
        for (int i = 0; i < frames; i++) {
            int acc = 0;
            for (int ch = 0; ch < channels; ch++) {
                acc += (short) (u16le(data, sp));
                sp += 2;
            }
            mono[i] = (short) (acc / channels);
        }

        return resampleTo48k(mono, sampleRate);
    }

    /** Linear resample mono 16-bit PCM to 48 kHz. Good enough for speech voice notes. */
    static short[] resampleTo48k(short[] in, int srcRate) {
        if (in == null || in.length == 0) {
            return in;
        }
        if (srcRate == 48000) {
            return in;
        }
        final long outLenL = (long) in.length * 48000L / srcRate;
        if (outLenL <= 0) {
            return null;
        }
        final int outLen = (int) Math.min(outLenL, Integer.MAX_VALUE);
        final short[] out = new short[outLen];
        final double ratio = (double) srcRate / 48000.0;
        for (int i = 0; i < outLen; i++) {
            final double srcPos = i * ratio;
            final int i0 = (int) srcPos;
            final int i1 = Math.min(i0 + 1, in.length - 1);
            final double frac = srcPos - i0;
            out[i] = (short) (in[i0] * (1.0 - frac) + in[i1] * frac);
        }
        return out;
    }

    private static int u16le(byte[] b, int o) {
        return (b[o] & 0xff) | ((b[o + 1] & 0xff) << 8);
    }

    private static long u32le(byte[] b, int o) {
        return (b[o] & 0xffL) | ((b[o + 1] & 0xffL) << 8) | ((b[o + 2] & 0xffL) << 16) | ((b[o + 3] & 0xffL) << 24);
    }

    private static void safeDelete(File f) {
        try {
            if (f != null && f.exists()) {
                f.delete();
            }
        } catch (Throwable ignore) {
        }
    }

    private static void shutdownQuietly(TextToSpeech engine) {
        if (engine == null) {
            return;
        }
        try {
            engine.stop();
        } catch (Throwable ignore) {
        }
        try {
            engine.shutdown();
        } catch (Throwable ignore) {
        }
    }
}
