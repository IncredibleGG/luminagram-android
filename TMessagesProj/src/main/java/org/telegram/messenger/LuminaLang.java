package org.telegram.messenger;

import org.telegram.ui.Components.TranslateAlert2;

/**
 * Per-provider target-language mapping. The app stores the target as an ISO tag
 * (e.g. "en", "zh", "pt-BR") from {@link TranslateAlert2#getToLanguage()}; each
 * provider expects its own dialect spelling. Keeping the table here means the
 * mapping lives in exactly one place.
 */
final class LuminaLang {

    private LuminaLang() {}

    private static String norm(String iso) {
        if (iso == null) {
            return "";
        }
        return iso.trim().toLowerCase().replace('_', '-');
    }

    private static String base(String s) {
        int i = s.indexOf('-');
        return i < 0 ? s : s.substring(0, i);
    }

    private static boolean traditional(String s) {
        return s.contains("tw") || s.contains("hant") || s.contains("hk") || s.contains("mo");
    }

    /** Google free web endpoint dialects. */
    static String google(String iso) {
        final String s = norm(iso);
        if (s.startsWith("zh")) {
            return traditional(s) ? "zh-TW" : "zh-CN";
        }
        return base(s);
    }

    /** DeepL target codes: upper-case, with region defaults for EN / PT / ZH. */
    static String deepl(String iso) {
        final String s = norm(iso);
        if (s.startsWith("en")) {
            return s.equals("en-gb") ? "EN-GB" : "EN-US";
        }
        if (s.startsWith("pt")) {
            return s.equals("pt-pt") ? "PT-PT" : "PT-BR";
        }
        if (s.startsWith("zh")) {
            return traditional(s) ? "ZH-HANT" : "ZH";
        }
        return base(s).toUpperCase();
    }

    /** Human-readable language name for LLM prompts (e.g. "Chinese", "Portuguese"). */
    static String name(String iso) {
        String n = TranslateAlert2.capitalFirst(TranslateAlert2.languageName(iso));
        return n != null ? n : (iso == null ? "" : iso);
    }
}
