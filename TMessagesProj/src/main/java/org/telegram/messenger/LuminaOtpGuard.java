package org.telegram.messenger;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * LuminaGram OTP Guard — warn before a Telegram login code leaves the composer.
 *
 * The single most damaging step in a Telegram account takeover is the victim typing the
 * 5–6 digit login code into a chat, because a scammer posing as "official support" asked
 * for it. Our existing scam-keyword warning only looks at INCOMING messages; this one
 * looks at what the user is about to SEND.
 *
 * Trigger = both of:
 *   1. the outgoing text contains a standalone run of 5–6 digits that survives the
 *      false-positive filters below, and
 *   2. the Telegram service account (user id 777000) delivered a message to this account
 *      within the last {@link #RECENT_WINDOW_SECONDS} — i.e. a login code really is in flight.
 *
 * Design constraints (deliberate, do not relax):
 *   - Pure local string matching. No ML, no model, no network — Telegram's ToS §1.5
 *     forbids deploying ML models on Telegram data.
 *   - Read-only advisory. Nothing here mutates, blocks or inspects incoming messages;
 *     it only lets the composer ask a question before the user's own text is dispatched.
 *   - Fail-open. Every entry point is wrapped: any unexpected state returns "do not warn",
 *     so a bug in this class can never stop a normal message from being sent.
 *
 * "Recently received from 777000" is answered from MessagesController's already-loaded
 * in-memory dialog state (TLRPC.Dialog.last_message_date for dialog 777000, plus the
 * cached top MessageObject for that dialog). That was chosen over persisting our own
 * timestamp because it needs no hook in any message-receive path (no shared hot files),
 * it survives an app restart for free (the dialog list is restored from the local DB on
 * launch), and it is a plain main-thread field read with no DB query and no I/O.
 */
public final class LuminaOtpGuard {

    private LuminaOtpGuard() {}

    /** Telegram's own service account. Login codes are always delivered from this id. */
    public static final long SERVICE_USER_ID = 777000L;

    /** Settings key; ON by default because this is a protection, not a preference. */
    public static final String KEY_ENABLED = "otpGuardEnabled";

    /** A code is only "in flight" for a short while after Telegram sent it. */
    private static final int RECENT_WINDOW_SECONDS = 10 * 60;

    /** Tolerance for a service message dated slightly ahead of us (clock skew). */
    private static final int FUTURE_SKEW_SECONDS = 60;

    /** Never scan an unbounded paste; a login code is always near the start anyway. */
    private static final int MAX_SCAN_CHARS = 4096;

    public static boolean isEnabled() {
        return LuminaConfig.getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(boolean enabled) {
        LuminaConfig.putBoolean(KEY_ENABLED, enabled);
    }

    /**
     * Should the composer stop and warn before dispatching {@code text} to {@code dialogId}?
     * Main thread only (it reads MessagesController's dialog maps). Never throws.
     */
    public static boolean shouldWarn(int currentAccount, long dialogId, CharSequence text) {
        try {
            if (!isEnabled()) {
                return false;
            }
            if (text == null || text.length() == 0) {
                return false;
            }
            // Sending "back" to Telegram itself, or to Saved Messages, leaks nothing.
            if (dialogId == SERVICE_USER_ID) {
                return false;
            }
            if (dialogId == UserConfig.getInstance(currentAccount).getClientUserId()) {
                return false;
            }
            // Cheap test first: most sends contain no 5–6 digit run at all.
            if (!containsLoginCode(text.toString())) {
                return false;
            }
            return receivedServiceMessageRecently(currentAccount);
        } catch (Throwable ignore) {
            return false; // fail-open: never block a send because of this class
        }
    }

    /**
     * True when dialog 777000 has a message dated inside the recency window. Read from
     * MessagesController's in-memory dialog state — no DB query, no network, no blocking.
     */
    public static boolean receivedServiceMessageRecently(int currentAccount) {
        try {
            final MessagesController controller = MessagesController.getInstance(currentAccount);
            if (controller == null) {
                return false;
            }
            int date = 0;
            if (controller.dialogs_dict != null) {
                final TLRPC.Dialog dialog = controller.dialogs_dict.get(SERVICE_USER_ID);
                if (dialog != null && dialog.last_message_date > date) {
                    date = dialog.last_message_date;
                }
            }
            // last_message_date can lag on a dialog rebuilt from cache; the cached top
            // message for the same dialog is the second, usually fresher, source.
            if (controller.dialogMessage != null) {
                final ArrayList<MessageObject> top = controller.dialogMessage.get(SERVICE_USER_ID);
                if (top != null) {
                    for (int i = 0; i < top.size(); i++) {
                        final MessageObject message = top.get(i);
                        if (message != null && message.messageOwner != null
                                && message.messageOwner.date > date) {
                            date = message.messageOwner.date;
                        }
                    }
                }
            }
            if (date <= 0) {
                return false;
            }
            int now = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
            if (now <= 0) {
                now = (int) (System.currentTimeMillis() / 1000L);
            }
            final int age = now - date;
            return age <= RECENT_WINDOW_SECONDS && age >= -FUTURE_SKEW_SECONDS;
        } catch (Throwable ignore) {
            return false;
        }
    }

    // ===================== local text matching (no ML, no network) =====================

    /**
     * True when the text holds at least one *maximal* run of 5–6 ASCII digits that still
     * looks like a login code after the false-positive filters. A run of 4 or of 7+ digits
     * is never a Telegram code, so years, card numbers and full phone numbers drop out here.
     * Package-visible so the filters can be reasoned about (and unit-tested) in isolation.
     */
    static boolean containsLoginCode(String text) {
        if (text == null) {
            return false;
        }
        final int n = Math.min(text.length(), MAX_SCAN_CHARS);
        int i = 0;
        while (i < n) {
            if (!isAsciiDigit(text.charAt(i))) {
                i++;
                continue;
            }
            final int start = i;
            while (i < n && isAsciiDigit(text.charAt(i))) {
                i++;
            }
            final int length = i - start;
            if (length >= 5 && length <= 6 && looksLikeLoginCode(text, start, i, n)) {
                return true;
            }
        }
        return false;
    }

    /**
     * False-positive filters, applied to one maximal digit run [start, end).
     * The rule of thumb is "rather miss a code than nag on every price": every ambiguous
     * shape is rejected.
     */
    private static boolean looksLikeLoginCode(String text, int start, int end, int n) {
        final char before = start > 0 ? text.charAt(start - 1) : '\0';
        final char after = end < n ? text.charAt(end) : '\0';
        // The same neighbours with runs of plain spaces skipped, so that a spaced-out amount
        // ("45678 元", "$ 12345") or a spaced phone group ("0912 345678") is still recognised.
        final char beforeSpaced = previousNonSpace(text, start - 1);
        final char afterSpaced = nextNonSpace(text, end, n);

        // Glued to Latin letters/underscore => an identifier, hash, filename or password
        // fragment ("abc123456", "123456th"), not something a user reads out as a code.
        if (isAsciiLetter(before) || before == '_') {
            return false;
        }
        if (isAsciiLetter(after) || after == '_') {
            return false;
        }

        // Money ("$12345", "12345元", "45678 元") and percentages.
        if (isCurrencyPrefix(beforeSpaced) || isCurrencySuffix(afterSpaced) || afterSpaced == '%') {
            return false;
        }

        // Part of a longer structured number: a separator with digits on the far side means
        // decimal / thousands group / phone group / date / version / order id, e.g.
        // "12.34567", "1,234,567", "2024-123456", "0912 345678".
        if (isNumberSeparator(before) && start - 2 >= 0 && isAsciiDigit(text.charAt(start - 2))) {
            return false;
        }
        if (isNumberSeparator(after) && end + 1 < n && isAsciiDigit(text.charAt(end + 1))) {
            return false;
        }
        if (before == ' ' && isAsciiDigit(beforeSpaced)) {
            return false;
        }
        if (after == ' ' && isAsciiDigit(afterSpaced)) {
            return false;
        }

        // Inside a URL, path, query string, e-mail or file name.
        return !insideUrlLikeToken(text, start, end, n);
    }

    /**
     * True when the run sits inside a token that looks like a URL / path / e-mail / filename.
     * Token boundaries are whitespace *and* CJK (U+2E80 and above), so a Chinese sentence
     * written without spaces — "我的驗證碼是123456，網址是https://…" — still isolates the digits
     * instead of swallowing the whole message into one "URL" token.
     */
    private static boolean insideUrlLikeToken(String text, int start, int end, int n) {
        int tokenStart = start;
        while (tokenStart > 0 && !isTokenBreak(text.charAt(tokenStart - 1))) {
            tokenStart--;
        }
        int tokenEnd = end;
        while (tokenEnd < n && !isTokenBreak(text.charAt(tokenEnd))) {
            tokenEnd++;
        }
        for (int i = tokenStart; i < tokenEnd; i++) {
            final char c = text.charAt(i);
            if (c == '/' || c == '\\' || c == '@' || c == '?' || c == '&' || c == '=' || c == '%' || c == '#') {
                return true;
            }
            // "example.com", "photo.jpg", "v1.2" — a dot glued to a Latin letter.
            if (c == '.' && i + 1 < tokenEnd && isAsciiLetter(text.charAt(i + 1))) {
                return true;
            }
        }
        return false;
    }

    /** Nearest non-space character at or before {@code from}, or '\0' at the start of the text. */
    private static char previousNonSpace(String text, int from) {
        int i = from;
        while (i >= 0 && text.charAt(i) == ' ') {
            i--;
        }
        return i >= 0 ? text.charAt(i) : '\0';
    }

    /** Nearest non-space character at or after {@code from}, or '\0' at the end of the text. */
    private static char nextNonSpace(String text, int from, int n) {
        int i = from;
        while (i < n && text.charAt(i) == ' ') {
            i++;
        }
        return i < n ? text.charAt(i) : '\0';
    }

    private static boolean isTokenBreak(char c) {
        return Character.isWhitespace(c) || c >= 0x2E80;
    }

    private static boolean isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isNumberSeparator(char c) {
        return c == '.' || c == ',' || c == '-' || c == '/' || c == '+' || c == ':' || c == '\'';
    }

    /** Symbols that, glued in front of a number, make it an amount of money. */
    private static final String CURRENCY_PREFIXES = "$＄¥￥€£￡₹₽₩￦฿₫₴";

    /** Characters that, glued after a number, make it an amount of money or a count. */
    private static final String CURRENCY_SUFFIXES = "元塊块圓円币幣원$¥€";

    private static boolean isCurrencyPrefix(char c) {
        return c != '\0' && CURRENCY_PREFIXES.indexOf(c) >= 0;
    }

    private static boolean isCurrencySuffix(char c) {
        return c != '\0' && CURRENCY_SUFFIXES.indexOf(c) >= 0;
    }
}
