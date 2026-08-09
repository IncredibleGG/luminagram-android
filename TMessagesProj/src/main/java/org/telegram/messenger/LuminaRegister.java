package org.telegram.messenger;

import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

/**
 * LuminaGram: per-chat <b>register</b> — the relationship the chat stands in, and therefore the
 * tone a translation of it has to carry.
 *
 * Across languages the thing that breaks first is not the meaning, it is the politeness level.
 * "Can you send that over?" is one sentence in English and three different sentences in Japanese
 * depending on whether the reader is a client, a colleague or a friend; a translator that does not
 * know which one is talking is guessing every time. So each dialog may carry one register, chosen
 * once, and every translation of that dialog — incoming and outgoing — is asked to speak in it.
 *
 * <h3>What each engine can actually honour</h3>
 * <ul>
 *   <li><b>LLM</b> (OpenAI-compatible: GPT / Gemini / DeepSeek / self-hosted) — the register is
 *       appended to the user's own system prompt as an extra paragraph, so the global prompt is
 *       layered under it rather than replaced.</li>
 *   <li><b>DeepL</b> — no free-form instruction, but a native {@code formality} parameter. The six
 *       registers collapse onto its formal/informal axis, and only for the target languages DeepL
 *       documents as supporting it; everywhere else the parameter is simply omitted.</li>
 *   <li><b>Google (free web) / Telegram</b> — no channel for tone at all. Nothing is sent, and the
 *       menu row says so in grey rather than pretending the setting took effect.</li>
 * </ul>
 *
 * <h3>Which dialog</h3>
 * The provider interface ({@link LuminaTranslator#translate}) carries text and a target language,
 * not a dialog, and its call sites live in files this feature deliberately does not touch. So the
 * dialog is resolved from the chat that is actually on screen when the request is built — which is
 * the chat being translated in every path that exists today: the composer translates the chat you
 * are typing in, and incoming messages are translated because that chat is open and showing them.
 * When the top of the stack is not a chat the answer is "unknown", which is the same as "unset":
 * the translation goes out exactly as it does today. {@link #promptSuffixFor(long)} and
 * {@link #deeplFormalityFor(long, String)} take the dialog explicitly, so a call site that does
 * know it can pass it and skip the guess entirely.
 *
 * Nothing here may throw. A register that cannot be read is a register that was never set, and a
 * translation without a tone instruction is still a translation.
 */
public final class LuminaRegister {

    private LuminaRegister() {}

    /** No register chosen — the stored value for every dialog until the user picks one. */
    public static final String NONE = "";

    public static final String CLIENT = "client";
    public static final String COLLEAGUE = "colleague";
    public static final String FRIEND = "friend";
    public static final String FAMILY = "family";
    public static final String ELDER = "elder";
    public static final String ROMANCE = "romance";

    /** A user-written description. Stored as {@code custom:<their sentence>}. */
    public static final String CUSTOM = "custom";
    private static final String CUSTOM_PREFIX = "custom:";

    /** The preset registers, in the order the picker lists them. */
    public static final String[] CODES = {CLIENT, COLLEAGUE, FRIEND, FAMILY, ELDER, ROMANCE};

    // A free-form description is a prompt fragment the user writes themselves, against their own
    // API key, so the only real limit is that it stays a description: one line, bounded length.
    private static final int CUSTOM_MAX = 200;

    // ---- storage -----------------------------------------------------------------------------

    /** The stored register for a dialog: a preset code, {@code custom:…}, or {@link #NONE}. */
    public static String get(long dialogId) {
        try {
            final String stored = LuminaConfig.getDialogRegister(dialogId);
            return stored == null ? NONE : stored;
        } catch (Throwable t) {
            return NONE;
        }
    }

    /** Store a register for a dialog. {@link #NONE} (or null) clears it. */
    public static void set(long dialogId, String value) {
        try {
            LuminaConfig.setDialogRegister(dialogId, value);
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    /** Build the stored form of a user-written description, or {@link #NONE} when it is blank. */
    public static String custom(String description) {
        final String cleaned = sanitize(description);
        return cleaned.length() == 0 ? NONE : (CUSTOM_PREFIX + cleaned);
    }

    public static boolean isCustom(String stored) {
        return stored != null && stored.startsWith(CUSTOM_PREFIX);
    }

    /** The user's own words out of a {@code custom:…} value; "" for anything else. */
    public static String customText(String stored) {
        return isCustom(stored) ? stored.substring(CUSTOM_PREFIX.length()) : "";
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        // One line, no quotes: this lands inside a quoted phrase in the system prompt, and a
        // newline there would read as the end of the instruction.
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < CUSTOM_MAX; ++i) {
            final char c = s.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
            } else if (c == '"' || c == '\\') {
                sb.append('\'');
            } else if (c >= ' ') {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    // ---- display -----------------------------------------------------------------------------

    /** Localized name of a preset code; {@code custom:…} shows the user's own sentence. */
    public static String displayName(String stored) {
        try {
            if (isCustom(stored)) {
                final String text = customText(stored);
                return text.length() > 0 ? text : LuminaLocale.getString(R.string.LuminaChatRegisterCustom);
            }
            final int res = nameRes(stored);
            return res == 0 ? LuminaLocale.getString(R.string.LuminaChatRegisterNone) : LuminaLocale.getString(res);
        } catch (Throwable t) {
            return "";
        }
    }

    /** String resource for a code's name, or 0 when the code is unknown/unset. */
    public static int nameRes(String code) {
        if (CLIENT.equals(code)) return R.string.LuminaChatRegisterClient;
        if (COLLEAGUE.equals(code)) return R.string.LuminaChatRegisterColleague;
        if (FRIEND.equals(code)) return R.string.LuminaChatRegisterFriend;
        if (FAMILY.equals(code)) return R.string.LuminaChatRegisterFamily;
        if (ELDER.equals(code)) return R.string.LuminaChatRegisterElder;
        if (ROMANCE.equals(code)) return R.string.LuminaChatRegisterRomance;
        if (CUSTOM.equals(code)) return R.string.LuminaChatRegisterCustom;
        return 0;
    }

    /** String resource for a code's one-line explanation, or 0 when the code is unknown/unset. */
    public static int infoRes(String code) {
        if (CLIENT.equals(code)) return R.string.LuminaChatRegisterClientInfo;
        if (COLLEAGUE.equals(code)) return R.string.LuminaChatRegisterColleagueInfo;
        if (FRIEND.equals(code)) return R.string.LuminaChatRegisterFriendInfo;
        if (FAMILY.equals(code)) return R.string.LuminaChatRegisterFamilyInfo;
        if (ELDER.equals(code)) return R.string.LuminaChatRegisterElderInfo;
        if (ROMANCE.equals(code)) return R.string.LuminaChatRegisterRomanceInfo;
        if (CUSTOM.equals(code)) return R.string.LuminaChatRegisterCustomInfo;
        return 0;
    }

    // ---- engine capability -------------------------------------------------------------------

    /** Full free-form tone control: only the LLM providers have a channel for it. */
    public static boolean engineSupportsPrompt() {
        try {
            return "llm".equals(LuminaTranslators.current().id());
        } catch (Throwable t) {
            return false;
        }
    }

    /** DeepL: a real but narrow knob — formal vs informal, on the languages it supports. */
    public static boolean engineIsDeepL() {
        try {
            return "deepl".equals(LuminaTranslators.current().id());
        } catch (Throwable t) {
            return false;
        }
    }

    /** True when the selected engine can carry no tone information whatsoever. */
    public static boolean engineIgnoresRegister() {
        return !engineSupportsPrompt() && !engineIsDeepL();
    }

    // ---- pipeline: LLM system prompt ---------------------------------------------------------

    /**
     * The paragraph to append to the LLM system prompt for the chat currently on screen, or null
     * when no register applies. Appended, never substituted: the user's global system prompt keeps
     * saying whatever it says, and this only adds how it should sound.
     */
    public static String promptSuffix() {
        try {
            return promptSuffixFor(ambientDialogId());
        } catch (Throwable t) {
            return null;
        }
    }

    /** As {@link #promptSuffix()}, for a dialog the caller already knows. */
    public static String promptSuffixFor(long dialogId) {
        try {
            if (dialogId == 0) {
                return null;
            }
            final String body = instruction(get(dialogId));
            if (body == null) {
                return null;
            }
            return "\n\nTone and register for this conversation: " + body
                    + " Adapt only the tone, the politeness level and the choice of words. Never change "
                    + "the meaning, never add or drop information, and never mention or explain the tone "
                    + "— output the translation only.";
        } catch (Throwable t) {
            return null;
        }
    }

    // The instruction itself is written in English on purpose: it is read by the model, not by the
    // user, and English is the language these models follow instructions in most reliably. Each one
    // names the relationship first and then spells out what that means in the languages where
    // politeness is grammar rather than word choice, because that is exactly where a translation
    // that is "correct" still lands wrong.
    private static String instruction(String stored) {
        if (stored == null || stored.length() == 0) {
            return null;
        }
        if (isCustom(stored)) {
            final String text = customText(stored);
            if (text.length() == 0) {
                return null;
            }
            return "the sender describes this relationship as \"" + text + "\". Match the tone, the "
                    + "formality and the vocabulary that relationship calls for, including the "
                    + "appropriate politeness level in languages that mark politeness grammatically.";
        }
        if (CLIENT.equals(stored)) {
            return "the other person is a business client or customer. Write the translation in polite, "
                    + "professional, formal business language. In languages that mark politeness "
                    + "grammatically, use the formal or honorific register (Japanese 敬語 with です・ます, "
                    + "Korean 하십시오체, Chinese 您, German Sie, French vous, Spanish usted). No slang, "
                    + "no over-familiar wording.";
        }
        if (COLLEAGUE.equals(stored)) {
            return "the other person is a work colleague of roughly equal standing. Write the "
                    + "translation in ordinary polite workplace language — courteous but relaxed, not "
                    + "stiff. In languages that mark politeness grammatically, use the standard polite "
                    + "register (Japanese です・ます, Korean 해요체, German Sie, French vous). Everyday "
                    + "workplace shorthand is fine; slang is not.";
        }
        if (FRIEND.equals(stored)) {
            return "the other person is a close friend. Write the translation in casual, informal, "
                    + "everyday spoken language, with the contractions and colloquialisms a friend would "
                    + "actually use. In languages that mark politeness grammatically, use the plain or "
                    + "casual register (Japanese 常体 / タメ口, Korean 반말, German du, French tu, "
                    + "Spanish tú).";
        }
        if (FAMILY.equals(stored)) {
            return "the other person is a family member. Write the translation in warm, familiar, "
                    + "everyday language of the kind used at home. In languages that mark politeness "
                    + "grammatically, use the plain or familiar register (Japanese 常体, German du, "
                    + "French tu), and keep kinship terms natural for the target culture.";
        }
        if (ELDER.equals(stored)) {
            return "the other person is an elder or a senior the sender owes respect to. Write the "
                    + "translation in respectful, deferential language that still sounds warm and "
                    + "personal rather than corporate. In languages with honorifics, use the honorific "
                    + "register (Japanese 敬語, Korean 존댓말, Chinese 您) and respectful forms of "
                    + "address.";
        }
        if (ROMANCE.equals(stored)) {
            return "the other person is someone the sender is romantically interested in. Write the "
                    + "translation in warm, playful, affectionate language with a light flirtatious "
                    + "touch — never crude or explicit. In languages that mark politeness "
                    + "grammatically, use the soft casual register (Japanese 常体, Korean 반말 or a "
                    + "gentle 해요체, French tu, Spanish tú).";
        }
        return null;
    }

    // ---- pipeline: DeepL formality -----------------------------------------------------------

    // The target languages DeepL documents formality support for. Sending the parameter for any
    // other target is an error response, i.e. a failed translation, so the list is a gate and not
    // an optimisation. Codes are as LuminaLang.deepl() spells them.
    private static final String[] DEEPL_FORMALITY_LANGS = {
            "DE", "FR", "IT", "ES", "NL", "PL", "PT-BR", "PT-PT", "JA", "RU"
    };

    /**
     * DeepL's {@code formality} value for the chat currently on screen and the given target
     * language, or null when it does not apply — no register, a register DeepL's one axis cannot
     * express, or a target language it does not support formality for.
     */
    public static String deeplFormality(String toLang) {
        try {
            return deeplFormalityFor(ambientDialogId(), toLang);
        } catch (Throwable t) {
            return null;
        }
    }

    /** As {@link #deeplFormality(String)}, for a dialog the caller already knows. */
    public static String deeplFormalityFor(long dialogId, String toLang) {
        try {
            if (dialogId == 0) {
                return null;
            }
            final String stored = get(dialogId);
            // A free-form description has no honest projection onto formal/informal — guessing one
            // would be inventing a setting the user did not make.
            final String value;
            if (CLIENT.equals(stored) || COLLEAGUE.equals(stored) || ELDER.equals(stored)) {
                value = "prefer_more";
            } else if (FRIEND.equals(stored) || FAMILY.equals(stored) || ROMANCE.equals(stored)) {
                value = "prefer_less";
            } else {
                return null;
            }
            final String target = LuminaLang.deepl(toLang);
            if (target == null) {
                return null;
            }
            for (int i = 0; i < DEEPL_FORMALITY_LANGS.length; ++i) {
                if (DEEPL_FORMALITY_LANGS[i].equals(target)) {
                    // "prefer_" rather than the strict form: if DeepL ever narrows the supported
                    // set under us, an unsupported target is ignored instead of failing the request.
                    return value;
                }
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    // ---- which dialog --------------------------------------------------------------------------

    /**
     * The dialog whose chat is on screen right now, or 0 when the top of the stack is not a chat.
     * Called while a translation request is being built, which is always on the UI thread.
     */
    public static long ambientDialogId() {
        try {
            final BaseFragment fragment = LaunchActivity.getSafeLastFragment();
            if (fragment instanceof ChatActivity) {
                return ((ChatActivity) fragment).getDialogId();
            }
        } catch (Throwable t) {
            // Never worth a crash: an unknown dialog simply means no register.
        }
        return 0;
    }
}
