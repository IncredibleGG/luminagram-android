package org.telegram.messenger;

import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * Registry of the available {@link LuminaTranslator} providers and the currently
 * selected one. Insertion order (Telegram first) is preserved for the picker.
 */
public final class LuminaTranslators {

    private LuminaTranslators() {}

    // Defaults for the OpenAI-compatible LLM provider, exposed publicly so the settings
    // UI (a different package) can prefill/display them without touching the provider class.
    public static final String LLM_DEFAULT_BASE_URL = "https://api.openai.com/v1";
    public static final String LLM_DEFAULT_MODEL = "gpt-4o-mini";
    public static final String LLM_DEFAULT_PROMPT =
            "You are a professional translator. Translate the user's message into {lang}. " +
            "Output ONLY the translation, with no quotes, no notes, and no explanations. " +
            "Preserve tone, emojis and formatting.";

    private static final LinkedHashMap<String, LuminaTranslator> REG = new LinkedHashMap<>();

    static {
        register(new TelegramTranslator());   // default, zero-config
        register(new GoogleWebTranslator());  // free Google web endpoint
        register(new DeepLTranslator());      // API key
        register(new LlmTranslator());        // OpenAI-compatible (OpenAI / Gemini / DeepSeek / self-host)
    }

    private static void register(LuminaTranslator t) {
        REG.put(t.id(), t);
    }

    public static Collection<LuminaTranslator> all() {
        return REG.values();
    }

    public static LuminaTranslator byId(String id) {
        LuminaTranslator t = id == null ? null : REG.get(id);
        return t != null ? t : REG.get("telegram");
    }

    /** The provider chosen in settings, falling back to Telegram when unset/unknown. */
    public static LuminaTranslator current() {
        // Default to the free, keyless Google web engine (registered id "google_web") so
        // translation works out-of-box for non-premium users instead of defaulting to
        // Telegram's engine. byId() still falls back to Telegram for an unknown stored id.
        return byId(LuminaConfig.getString("translateProvider", "google_web"));
    }
}
