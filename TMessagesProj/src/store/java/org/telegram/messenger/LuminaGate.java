package org.telegram.messenger;

/**
 * LuminaGate — STORE flavor.
 * Google-Play-safe build: every ToS-sensitive capability is hard-disabled and its
 * decision logic is absent (methods are constant `false`). The full build ships a
 * different LuminaGate under src/full.
 */
public class LuminaGate {

    /** False in the store build. */
    public static final boolean FULL = false;

    public static boolean allowSaveRestricted() {
        return false;
    }

    public static boolean allowCopyRestricted() {
        return false;
    }

    public static boolean unlockPremiumTranslate() {
        return false;
    }
}
