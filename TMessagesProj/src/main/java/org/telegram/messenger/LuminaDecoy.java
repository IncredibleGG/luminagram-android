package org.telegram.messenger;

import android.content.Context;

/**
 * LuminaDecoy — process-lifetime state for the "calculator vault" decoy lock.
 *
 * This is a fully local gate that is SEPARATE from Telegram's own passcode. When
 * {@code decoyLockEnabled} is on and a non-empty {@code decoyUnlockCode} is set, a
 * cold launch first shows {@link org.telegram.ui.LuminaCalculatorActivity} (a working
 * calculator). Typing the secret code and pressing "=" flips {@link #unlocked} true
 * for the rest of the process and lets {@link org.telegram.ui.LaunchActivity} proceed
 * into the real app; any other input just computes normally.
 *
 * Everything here is intentionally tiny and defensive: {@link #shouldGate(Context)}
 * can NEVER throw (it swallows any error and returns false) so a misconfiguration can
 * never make the app unopenable — the gate fails OPEN.
 */
public final class LuminaDecoy {

    private LuminaDecoy() {}

    /**
     * True once the correct code has been entered in this process. Volatile because it
     * is written from the calculator Activity and read from LaunchActivity.onCreate.
     * Resets to false on every fresh process (cold launch), which re-arms the lock.
     */
    public static volatile boolean unlocked = false;

    /** Prefs key: master toggle for the decoy calculator lock. */
    public static final String KEY_ENABLED = "decoyLockEnabled";
    /** Prefs key: the secret code that unlocks the real app when entered + "=". */
    public static final String KEY_CODE = "decoyUnlockCode";

    /**
     * Whether LaunchActivity should divert to the decoy calculator instead of showing
     * the real UI. True ONLY when the lock is enabled, a non-empty code is configured
     * and this process has not been unlocked yet. Fail-open: any exception (e.g. prefs
     * not ready) is swallowed and returns false so the real app always launches.
     *
     * @param context launch context (unused today; kept so the gate can read prefs
     *                directly should LuminaConfig ever be unavailable).
     */
    public static boolean shouldGate(Context context) {
        if (unlocked) {
            return false;
        }
        try {
            if (!LuminaConfig.getBoolean(KEY_ENABLED, false)) {
                return false;
            }
            String code = LuminaConfig.getString(KEY_CODE, "");
            return code != null && code.length() > 0;
        } catch (Throwable ignore) {
            // Never block launch because of a config/prefs error.
            return false;
        }
    }
}
