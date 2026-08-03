package org.telegram.messenger;

/**
 * LuminaGate — FULL flavor.
 * Master switches for ToS-sensitive features. Present ONLY in the `full` build's
 * source set (src/full). The `store` build ships a different LuminaGate that
 * hard-returns false, so the sensitive decision logic is absent from the store APK.
 *
 * Each method still honours the user's own runtime toggle (off by default) — full
 * build merely makes the capability reachable.
 */
public class LuminaGate {

    /** True only in the full (non-store) build. */
    public static final boolean FULL = true;

    /** Allow saving media from chats that disable saving (noforwards). */
    public static boolean allowSaveRestricted() {
        return LuminaConfig.getBoolean("saveRestrictedMedia", false);
    }

    /** Allow copying text from restricted (noforwards) chats. */
    public static boolean allowCopyRestricted() {
        return LuminaConfig.getBoolean("copyRestricted", false);
    }

    /** Allow always-on auto-translate (bypasses the premium gate). */
    public static boolean unlockPremiumTranslate() {
        return LuminaConfig.getBoolean("autoTranslateAll", false);
    }
}
