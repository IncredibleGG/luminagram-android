package org.telegram.messenger;

/**
 * LuminaGram — pluggable translation backend.
 *
 * A {@code LuminaTranslator} turns a source string into a translation for a target
 * language, running its own network work off the UI thread and always delivering the
 * result back on the UI thread through {@link Callback}. Implementations are stateless
 * singletons registered in {@link LuminaTranslators}; the selected one is read from
 * {@link LuminaConfig} ({@code translateProvider}).
 *
 * The default provider is {@link TelegramTranslator}, which wraps Telegram's own
 * {@code TL_messages_translateText} RPC, so leaving the setting at "telegram" is a
 * byte-for-byte no-op versus the fork's previous hardcoded behaviour.
 */
public interface LuminaTranslator {

    /** Stable key persisted in LuminaConfig (e.g. "telegram", "google_web", "deepl", "llm"). */
    String id();

    /** Human-readable brand label shown in the provider picker. */
    CharSequence displayName();

    /** True when the provider needs a user-supplied API key (masked in the UI). */
    boolean needsKey();

    /** True when the provider needs a configurable base URL (self-hosted / LLM). */
    boolean needsBaseUrl();

    /** True when the provider needs a configurable model name (LLM). */
    boolean needsModel();

    /**
     * Translate {@code text} into {@code toLang} (an app ISO tag from
     * {@link org.telegram.ui.Components.TranslateAlert2#getToLanguage()} such as
     * "en" / "zh" / "pt-BR"; the provider maps it to its own dialect). The callback
     * is always invoked on the UI thread exactly once.
     */
    void translate(String text, String toLang, Callback cb);

    interface Callback {
        /** Success. {@code detectedSourceLang} may be null when the provider does not report it. */
        void onResult(String translated, String detectedSourceLang);

        /** Failure. {@code rateLimited} is true for HTTP 429 / provider quota codes. */
        void onError(boolean rateLimited, String message);
    }
}
