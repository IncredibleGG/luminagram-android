package org.telegram.messenger;

/**
 * LuminaChatLock — single-chat lock / "private folder".
 *
 * A purely LOCAL, DISPLAY-ONLY feature that extends the existing disguise-vault concept
 * down to the granularity of a single conversation. The user marks any dialog as "private";
 * while the session is not revealed, that dialog is simply not drawn in the chat list and is
 * excluded from search results. It reappears only after the secret reveal code is typed into
 * the chat search box (see {@link org.telegram.ui.DialogsActivity}).
 *
 * COMPLIANCE (ToS §1.4): this is a rendering filter and nothing else. Hidden chats keep
 * receiving messages, keep their unread state, and are NEVER marked read; typing, online and
 * last-seen state are never touched here. There is no server call anywhere in this class.
 *
 * FAIL-OPEN: every accessor swallows errors and errs toward showing the chat, so a corrupt
 * config or missing code can never make a conversation permanently disappear. Hiding is only
 * ever effective when a reveal code exists — without a code nothing is hidden, guaranteeing
 * the user always has a way to bring a chat back.
 *
 * State model:
 *   - locked set  : persisted dialogIds (LuminaConfig "lockedChats", JSON array). Membership only.
 *   - reveal flag : process-lifetime {@link #revealed}. Resets to false on every cold launch,
 *                   re-arming the folder exactly like the vault's {@code unlocked} flag.
 *   - reveal code : dedicated {@code chatLockCode}, falling back to the vault's
 *                   {@code decoyUnlockCode} when unset, so vault users get reveal for free.
 */
public final class LuminaChatLock {

    private LuminaChatLock() {}

    /**
     * True once the reveal code has been entered in this process. Volatile because it is
     * written from the search box (UI thread) and read from render hooks. Resets to false on
     * every fresh process, which re-arms the private folder.
     */
    private static volatile boolean revealed = false;

    // ---- reveal code resolution ------------------------------------------------------

    /**
     * The effective reveal code: the dedicated {@code chatLockCode} if set, otherwise the
     * disguise vault's {@code decoyUnlockCode}. Empty string when neither is configured.
     */
    public static String effectiveCode() {
        try {
            String own = LuminaConfig.getString(LuminaConfig.KEY_CHAT_LOCK_CODE, "");
            if (own != null && own.trim().length() > 0) {
                return own.trim();
            }
            String vault = LuminaConfig.getString(LuminaDecoy.KEY_CODE, "");
            if (vault != null && vault.trim().length() > 0) {
                return vault.trim();
            }
        } catch (Throwable ignore) {
        }
        return "";
    }

    /** Whether any reveal code (dedicated or inherited from the vault) is configured. */
    public static boolean hasCode() {
        return effectiveCode().length() > 0;
    }

    // ---- locked set (membership only) ------------------------------------------------

    /** Membership test for the locked set; does NOT consider the session reveal state. */
    public static boolean isLocked(long dialogId) {
        return LuminaConfig.isChatInLockedSet(dialogId);
    }

    /** True when at least one chat is in the locked set. */
    public static boolean hasAnyLocked() {
        return LuminaConfig.hasAnyLockedChat();
    }

    /** Snapshot of the locked dialogIds as a set, so a list filter parses the JSON once. */
    public static java.util.HashSet<Long> lockedIdSet() {
        java.util.HashSet<Long> set = new java.util.HashSet<>();
        try {
            org.json.JSONArray arr = LuminaConfig.getLockedChatIds();
            for (int i = 0; i < arr.length(); i++) {
                set.add(arr.optLong(i));
            }
        } catch (Throwable ignore) {
        }
        return set;
    }

    public static void lock(long dialogId) {
        LuminaConfig.setChatLocked(dialogId, true);
    }

    public static void unlock(long dialogId) {
        LuminaConfig.setChatLocked(dialogId, false);
    }

    public static void toggle(long dialogId) {
        LuminaConfig.setChatLocked(dialogId, !isLocked(dialogId));
    }

    // ---- session reveal --------------------------------------------------------------

    public static boolean isRevealed() {
        return revealed;
    }

    /** Re-arm the folder for this session (hide again). */
    public static void hideAgain() {
        revealed = false;
    }

    /**
     * Reveal the private folder if {@code code} matches the effective reveal code (compared
     * trimmed). Returns true on a successful reveal.
     */
    public static boolean reveal(String code) {
        try {
            String c = effectiveCode();
            if (c.length() > 0 && code != null && code.trim().equals(c)) {
                revealed = true;
                return true;
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    /**
     * Search-box helper: only attempt a reveal when it could actually do something (still
     * hidden, something is locked, a code exists), so ordinary searches never pay for a
     * string compare and a normal query is never mistaken for a reveal.
     */
    public static boolean tryRevealFromSearch(String text) {
        try {
            if (revealed || text == null || text.length() == 0) {
                return false;
            }
            if (!hasAnyLocked()) {
                return false;
            }
            return reveal(text);
        } catch (Throwable ignore) {
            return false;
        }
    }

    // ---- display predicate -----------------------------------------------------------

    /**
     * Whether the feature is doing any hiding right now: a code exists, at least one chat is
     * locked, and this session is not revealed. Used to keep the list filter a no-op (and the
     * returned list reference unchanged) for everyone who never locks a chat.
     */
    public static boolean isActive() {
        try {
            return !revealed && hasCode() && hasAnyLocked();
        } catch (Throwable ignore) {
            return false;
        }
    }

    /**
     * Should this dialog be hidden from the list/search right now? Fail-open: any error, or a
     * missing reveal code, returns false (show it) so a chat is never lost.
     */
    public static boolean isHidden(long dialogId) {
        try {
            if (revealed) {
                return false;
            }
            if (!hasCode()) {
                return false;
            }
            return isLocked(dialogId);
        } catch (Throwable ignore) {
            return false;
        }
    }
}
