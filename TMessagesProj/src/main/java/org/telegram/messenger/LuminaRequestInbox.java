package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

/**
 * LuminaRequestInbox — "stranger request inbox".
 *
 * A purely LOCAL, DISPLAY-ONLY triage layer for unsolicited 1:1 messages. A conversation
 * counts as a "stranger request" when the peer is a plain user who is NOT in your contacts,
 * with whom you share NO group (as far as we can tell locally), and whom you have not yet
 * triaged. While the feature is on, such conversations are diverted out of the main chat list
 * into a dedicated inbox reachable from a banner at the top of the list. The user accepts them
 * (let through to the main list), blocks them, or reports them — reusing Telegram's own APIs.
 *
 * COMPLIANCE (ToS §1.4): this class is a display predicate plus a persisted "already-triaged"
 * set. It performs NO server call of its own, and it NEVER touches receive / unread / read /
 * typing / online / last-seen state. Diverted chats keep receiving messages and keep their
 * unread badge exactly as before; they are simply drawn in a different place. Block and report
 * are only ever triggered by an explicit user tap in {@link org.telegram.ui.LuminaRequestsActivity}
 * and go through Telegram's existing {@code blockPeer} / {@code reportSpam} APIs.
 *
 * FAIL-OPEN: every predicate swallows errors and errs toward "not a stranger" (show the chat
 * normally in the main list), so a corrupt config or a missing field can never make a
 * conversation vanish. The feature is also opt-in ({@code strangerInbox} defaults to false),
 * so the stock build diverts nothing at all.
 *
 * State model:
 *   - master switch : {@link LuminaConfig#strangerInbox} (default false).
 *   - allowed set   : persisted dialogIds the user has triaged (accepted/blocked/reported).
 *                     Stored app-privately as a JSON array of dialogIds under
 *                     {@link #KEY_ALLOWED}. Membership only; nothing is ever sent to Telegram.
 *
 * NOTE / TODO (notification muting): this batch does the LIST diversion only. Suppressing the
 * ring/notification for stranger requests would require hooking the notification build path
 * (NotificationsController), which is intentionally left untouched here to keep the change safe
 * and to avoid altering any receive-side behavior. Tracked as a follow-up.
 */
public final class LuminaRequestInbox {

    private LuminaRequestInbox() {}

    /** JSON array of dialogIds the user has already triaged (accepted / blocked / reported). */
    public static final String KEY_ALLOWED = "strangerAllowedIds";

    // ---- master switch ---------------------------------------------------------------

    /** Whether the stranger-request inbox is enabled. Plain static-field read (draw path). */
    public static boolean isEnabled() {
        try {
            return LuminaConfig.strangerInbox;
        } catch (Throwable ignore) {
            return false;
        }
    }

    // ---- allowed / triaged set -------------------------------------------------------

    /** Snapshot of the triaged dialogIds as a set, so a list filter parses the JSON once. */
    public static java.util.HashSet<Long> allowedSet() {
        java.util.HashSet<Long> set = new java.util.HashSet<>();
        try {
            org.json.JSONArray arr = allowedArray();
            for (int i = 0; i < arr.length(); i++) {
                set.add(arr.optLong(i));
            }
        } catch (Throwable ignore) {
        }
        return set;
    }

    private static org.json.JSONArray allowedArray() {
        String raw = LuminaConfig.getString(KEY_ALLOWED, "");
        if (raw != null && raw.length() > 0) {
            try {
                return new org.json.JSONArray(raw);
            } catch (org.json.JSONException ignore) {
            }
        }
        return new org.json.JSONArray();
    }

    /** Membership test for the triaged set (does NOT run the stranger predicate). */
    public static boolean isAllowed(long dialogId) {
        try {
            org.json.JSONArray arr = allowedArray();
            for (int i = 0; i < arr.length(); i++) {
                if (arr.optLong(i) == dialogId) {
                    return true;
                }
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    /**
     * Mark a dialog as triaged so it stops being a stranger request. Used by every triage
     * action — accept, block and report — so the item leaves the inbox and returns to the
     * main list (a blocked chat staying in the list mirrors stock Telegram, and nothing is
     * ever hidden, honoring "no message may silently disappear").
     */
    public static void allow(long dialogId) {
        try {
            if (dialogId == 0) {
                return;
            }
            org.json.JSONArray arr = allowedArray();
            for (int i = 0; i < arr.length(); i++) {
                if (arr.optLong(i) == dialogId) {
                    return; // already present
                }
            }
            arr.put(dialogId);
            LuminaConfig.putString(KEY_ALLOWED, arr.toString());
        } catch (Throwable ignore) {
        }
    }

    // ---- predicate -------------------------------------------------------------------

    /**
     * Whether this dialog is a pending stranger request right now. Fail-open: any error, or
     * the feature being off, returns false (treat as a normal chat and show it in the main
     * list). Criteria — the peer is a plain user, not self, not a bot / service account, not
     * a contact, no known common group, and not yet triaged.
     */
    public static boolean isStrangerRequest(int account, TLRPC.Dialog d) {
        try {
            if (!isEnabled()) {
                return false;
            }
            if (d == null || (d instanceof TLRPC.TL_dialogFolder)) {
                return false;
            }
            long did = d.id;
            // Only genuine 1:1 user conversations — never groups, channels or secret chats.
            if (!DialogObject.isUserDialog(did) || DialogObject.isEncryptedDialog(did)) {
                return false;
            }
            if (isAllowed(did)) {
                return false; // already triaged
            }
            long self = UserConfig.getInstance(account).getClientUserId();
            if (did == self) {
                return false;
            }
            if (UserObject.isService(did)) {
                return false; // Telegram service / support numbers
            }
            MessagesController mc = MessagesController.getInstance(account);
            TLRPC.User u = mc.getUser(did);
            if (u == null) {
                return false; // unknown peer: fail-open, show normally
            }
            if (u.self || u.bot || u.support) {
                return false;
            }
            // A contact (one-way or mutual) is by definition not a stranger.
            if (u.contact || u.mutual_contact) {
                return false;
            }
            // No common group. common_chats_count lives on UserFull, which may not be cached;
            // we only ever READ what is already loaded (getUserFull never triggers a network
            // load) and never divert a peer we can prove we share a group with. When UserFull
            // is not loaded we cannot prove a common group, so we keep the peer as a candidate;
            // the user can always Accept to move it back — nothing is lost.
            TLRPC.UserFull uf = mc.getUserFull(did);
            if (uf != null && uf.common_chats_count > 0) {
                return false;
            }
            return true;
        } catch (Throwable ignore) {
            return false; // fail-open: never lose a chat
        }
    }

    // ---- aggregate helpers (banner count + inbox list) -------------------------------

    /** How many pending stranger requests exist right now (main folder only). */
    public static int countPending(int account) {
        int n = 0;
        try {
            if (!isEnabled()) {
                return 0;
            }
            java.util.ArrayList<TLRPC.Dialog> dialogs = MessagesController.getInstance(account).getDialogs(0);
            if (dialogs == null) {
                return 0;
            }
            for (int i = 0; i < dialogs.size(); i++) {
                if (isStrangerRequest(account, dialogs.get(i))) {
                    n++;
                }
            }
        } catch (Throwable ignore) {
        }
        return n;
    }

    /** The dialogIds of every pending stranger request (main folder only), display order. */
    public static java.util.ArrayList<Long> pendingDialogIds(int account) {
        java.util.ArrayList<Long> out = new java.util.ArrayList<>();
        try {
            if (!isEnabled()) {
                return out;
            }
            java.util.ArrayList<TLRPC.Dialog> dialogs = MessagesController.getInstance(account).getDialogs(0);
            if (dialogs == null) {
                return out;
            }
            for (int i = 0; i < dialogs.size(); i++) {
                TLRPC.Dialog d = dialogs.get(i);
                if (isStrangerRequest(account, d)) {
                    out.add(d.id);
                }
            }
        } catch (Throwable ignore) {
        }
        return out;
    }
}
