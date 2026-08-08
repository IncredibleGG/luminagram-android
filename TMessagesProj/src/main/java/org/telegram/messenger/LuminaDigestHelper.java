package org.telegram.messenger;

import java.util.ArrayList;

/**
 * LuminaGram: unread digest helper.
 * When the user returns to the app after a period of absence, computes
 * a brief summary of unread messages across all dialogs.
 */
public class LuminaDigestHelper {

    private static final String KEY_LAST_OPEN = "digestLastOpen";
    private static final long MIN_AWAY_MS = 60 * 60 * 1000; // 1 hour default

    /**
     * Call when the main screen becomes visible.
     * Returns a summary string if the user has been away long enough and has unreads,
     * or null if nothing to show.
     */
    public static String getDigestAndUpdate(int currentAccount) {
        if (!LuminaConfig.getBoolean("unreadDigest", true)) {
            return null;
        }
        long now = System.currentTimeMillis();
        long lastOpen = 0;
        String stored = LuminaConfig.getString(KEY_LAST_OPEN, "");
        if (stored != null && stored.length() > 0) {
            try {
                lastOpen = Long.parseLong(stored);
            } catch (NumberFormatException ignore) {
            }
        }
        LuminaConfig.putString(KEY_LAST_OPEN, String.valueOf(now));

        if (lastOpen == 0 || now - lastOpen < MIN_AWAY_MS) {
            return null;
        }

        // Count unread dialogs and total unread messages
        MessagesController mc = MessagesController.getInstance(currentAccount);
        ArrayList<TLRPC.Dialog> dialogs = mc.getDialogs(0);
        int unreadChats = 0;
        int totalUnread = 0;

        if (dialogs != null) {
            for (TLRPC.Dialog d : dialogs) {
                if (d.unread_count > 0) {
                    unreadChats++;
                    totalUnread += d.unread_count;
                }
            }
        }

        if (totalUnread == 0) {
            return null;
        }

        // Format: "While you were away: X messages from Y chats"
        // Use LuminaLocale for the template
        String template = LuminaLocale.getString(R.string.LuminaDigestSummary);
        return template.replace("%1$d", String.valueOf(totalUnread))
                       .replace("%2$d", String.valueOf(unreadChats));
    }
}
