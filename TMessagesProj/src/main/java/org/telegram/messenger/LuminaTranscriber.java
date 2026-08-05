package org.telegram.messenger;

import java.io.File;

/**
 * LuminaGram — pluggable voice-note transcription backend ("voice-to-text").
 *
 * <p>A {@code LuminaTranscriber} turns a received voice note / round-video audio file
 * into text using LuminaGram's OWN engine — never Telegram's server-side / premium
 * transcription. Implementations run their heavy work off the UI thread and deliver the
 * result through {@link Callback}, which is invoked on a background thread (callers that
 * touch UI must marshal back onto the main thread themselves).
 *
 * <p>The engines are registered in {@link LuminaTranscribers}; the selected one is read
 * from {@link LuminaConfig} ({@code sttEngine}), defaulting to the offline Vosk engine.
 * This interface is the shared contract the parallel network-engine implementations
 * (Whisper, Google Speech) code against — keep its shape stable.
 */
public interface LuminaTranscriber {

    /** Stable key persisted in LuminaConfig (e.g. "vosk", "whisper", "google"). */
    String id();

    /** Human-readable engine label shown in the engine picker. */
    String displayName();

    /** True when transcription runs fully on-device with no network call. */
    boolean isOffline();

    /** True when the engine needs a user-supplied API key. */
    boolean needsKey();

    /** True when the engine needs a configurable base URL (self-hosted / OpenAI-compatible). */
    boolean needsBaseUrl();

    /** True when the engine needs a configurable model name / language model. */
    boolean needsModel();

    /**
     * Transcribe {@code audio} (an OGG/Opus voice note or a round-video mp4). {@code langHint}
     * is an optional ISO-639 language tag (e.g. "en", "zh"); pass {@code null} to let the
     * engine resolve it from settings / the app language. Runs asynchronously; {@code cb}
     * is invoked on a background thread exactly once via {@code onResult} or {@code onError},
     * with any number of preceding {@code onProgress} calls.
     */
    void transcribe(File audio, String langHint, Callback cb);

    interface Callback {
        /** Success: the full recognized text (may be empty for silent audio). */
        void onResult(String text);

        /** Failure: a short human-readable reason (never carries secrets). */
        void onError(String message);

        /** Progress in [0f, 1f]; best-effort, not guaranteed monotonic across engines. */
        void onProgress(float fraction);
    }
}
