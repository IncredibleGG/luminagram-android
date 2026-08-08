package org.telegram.messenger;

import java.util.HashSet;
import java.util.Set;

/**
 * Render-only auto-blur for incoming media.
 *
 * Tracks reveal state independently of Telegram's native spoiler system
 * ({@code messageOwner.media.spoiler}). No TL objects are modified.
 * Revealed state resets on app restart (privacy by design).
 */
public final class LuminaSpoilerManager {

    private static final Set<String> revealed = new HashSet<>();

    private LuminaSpoilerManager() {}

    /**
     * Whether this message should currently show a LuminaGram auto-blur overlay.
     *
     * Returns {@code true} when:
     * <ul>
     *   <li>autoBlurIncoming is ON in settings</li>
     *   <li>the message is incoming (not sent by us)</li>
     *   <li>the message has a photo, video, GIF, or round-video</li>
     *   <li>it has NOT already been revealed this session</li>
     *   <li>it does NOT already have a native spoiler (avoid double-blurring)</li>
     * </ul>
     */
    public static boolean shouldBlur(MessageObject mo) {
        if (mo == null) return false;
        if (!LuminaConfig.getBoolean("autoBlurIncoming", false)) return false;
        if (mo.isOut()) return false;
        if (mo.hasMediaSpoilers()) return false; // native spoiler already present
        if (mo.isMediaSpoilersRevealed) return false;
        if (!hasVisualMedia(mo)) return false;
        return !revealed.contains(key(mo));
    }

    /** Mark the message as revealed for this session. */
    public static void reveal(MessageObject mo) {
        if (mo != null) {
            revealed.add(key(mo));
        }
    }

    private static String key(MessageObject mo) {
        return mo.getDialogId() + "_" + mo.getId();
    }

    private static boolean hasVisualMedia(MessageObject mo) {
        return mo.isPhoto() || mo.isVideo() || mo.isGif() || mo.isRoundVideo();
    }
}
