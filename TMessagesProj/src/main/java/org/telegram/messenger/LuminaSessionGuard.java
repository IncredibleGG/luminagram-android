package org.telegram.messenger;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * LuminaGram login guard.
 *
 * <p>The 2026 account-takeover playbook is no longer password guessing — the attacker sends you a
 * QR code, claims it is a "verification", and the moment you scan it they hold a live authorized
 * session and can read every chat you have. Almost nobody opens the "Linked devices" screen on
 * their own, so the new session sits there unnoticed.
 *
 * <p>This class closes that gap with a purely local diff:
 * <ol>
 *   <li>When the app comes to the foreground (throttled to {@link #MIN_INTERVAL_MS}) it asks the
 *       official {@code account.getAuthorizations} for the user's OWN authorization list.</li>
 *   <li>It compares the returned session hashes against the list stored locally in
 *       {@link LuminaConfig} and shows {@link org.telegram.ui.LuminaSessionAlertBox} for anything
 *       it has not seen before.</li>
 * </ol>
 *
 * <p><b>ToS boundaries (deliberate, do not "optimise" away):</b>
 * <ul>
 *   <li>Only official API calls are used: {@code account.getAuthorizations} and
 *       {@code account.resetAuthorization} — exactly what the built-in
 *       {@link org.telegram.ui.SessionsActivity} screen calls.</li>
 *   <li>{@link #terminate} is NEVER invoked automatically. It only runs from an explicit tap on
 *       the "Not me" button. Acting on the user's account without their knowledge would violate
 *       Telegram's ToS §1.4.</li>
 *   <li>Nothing is uploaded, logged or shared. The known-session list lives only in the
 *       app-private "luminagram" prefs.</li>
 * </ul>
 *
 * <p>The very first successful check per account only seeds the baseline (every existing session is
 * marked known) so a fresh install never opens a wall of alerts for sessions the user already has.
 *
 * <p>Every entry point is wrapped in try/catch and null guards: a failure here must never affect
 * app start-up or the login flow.
 */
public final class LuminaSessionGuard {

    private LuminaSessionGuard() {
    }

    /** Minimum gap between two automatic (foreground) checks, per account. Avoids API flood. */
    public static final long MIN_INTERVAL_MS = 30 * 60 * 1000L;

    /** Delay after onResume before the check fires, so the UI (and passcode screen) settles. */
    private static final long FOREGROUND_DELAY_MS = 4000L;

    /** How many times to retry presenting the alert while no fragment is available (3s apart). */
    private static final int MAX_SHOW_RETRIES = 5;

    private static volatile boolean running;

    /** Result of a manual "check now" run. */
    public interface Callback {
        /**
         * @param newLogins how many previously unknown sessions were found (alerts already queued)
         * @param failed    true when the request could not be completed at all
         */
        void onResult(int newLogins, boolean failed);
    }

    /** Result of a user-initiated session termination. */
    public interface TerminateCallback {
        void onDone(boolean ok);
    }

    public static boolean isEnabled() {
        try {
            return LuminaConfig.getBoolean(LuminaConfig.KEY_SESSION_GUARD_ENABLED, true);
        } catch (Throwable ignore) {
            return false;
        }
    }

    public static void setEnabled(boolean enabled) {
        try {
            LuminaConfig.putBoolean(LuminaConfig.KEY_SESSION_GUARD_ENABLED, enabled);
        } catch (Throwable ignore) {
        }
    }

    /**
     * Called once from {@code LaunchActivity.onResume}. Throttled: at most one network round-trip
     * per {@link #MIN_INTERVAL_MS} per account. Never throws.
     */
    public static void onAppForeground(final int account) {
        try {
            if (!isEnabled()) {
                return;
            }
            if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) {
                return;
            }
            if (!UserConfig.getInstance(account).isClientActivated()) {
                return; // not logged in yet — nothing to guard
            }
            final long now = System.currentTimeMillis();
            long last = LuminaConfig.getSessionGuardLastCheck(account);
            if (last > now) {
                last = 0; // device clock moved backwards: do not lock ourselves out forever
            }
            if (last > 0 && now - last < MIN_INTERVAL_MS) {
                return;
            }
            AndroidUtilities.runOnUIThread(() -> check(account, null), FOREGROUND_DELAY_MS);
        } catch (Throwable ignore) {
        }
    }

    /** Manual trigger from the security settings row. Ignores the throttle. Never throws. */
    public static void checkNow(final int account, final Callback callback) {
        try {
            check(account, callback);
        } catch (Throwable t) {
            if (callback != null) {
                callback.onResult(0, true);
            }
        }
    }

    private static void check(final int account, final Callback callback) {
        if (running) {
            if (callback != null) {
                callback.onResult(0, false);
            }
            return;
        }
        if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT
                || !UserConfig.getInstance(account).isClientActivated()) {
            if (callback != null) {
                callback.onResult(0, true);
            }
            return;
        }
        running = true;
        try {
            // Official API — the same request SessionsActivity.loadSessions() makes.
            final TL_account.getAuthorizations req = new TL_account.getAuthorizations();
            ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                running = false;
                try {
                    LuminaConfig.setSessionGuardLastCheck(account, System.currentTimeMillis());
                    if (error != null || !(response instanceof TL_account.authorizations)) {
                        if (callback != null) {
                            callback.onResult(0, true);
                        }
                        return;
                    }
                    handle(account, (TL_account.authorizations) response, callback);
                } catch (Throwable t) {
                    try {
                        FileLog.e(t);
                    } catch (Throwable ignore) {
                    }
                    if (callback != null) {
                        callback.onResult(0, true);
                    }
                }
            }));
        } catch (Throwable t) {
            running = false;
            if (callback != null) {
                callback.onResult(0, true);
            }
        }
    }

    /** Pure local diff of the returned authorization list against the stored known set. */
    private static void handle(int account, TL_account.authorizations res, Callback callback) {
        final ArrayList<TLRPC.TL_authorization> fresh = new ArrayList<>();
        final HashSet<String> present = new HashSet<>();
        final Set<String> known = LuminaConfig.getKnownSessions(account);
        final boolean firstRun = !LuminaConfig.hasKnownSessions(account);

        if (res != null && res.authorizations != null) {
            for (int a = 0, n = res.authorizations.size(); a < n; a++) {
                final TLRPC.TL_authorization auth = res.authorizations.get(a);
                if (auth == null) {
                    continue;
                }
                if (auth.current || (auth.flags & 1) != 0) {
                    continue; // this device
                }
                final String h = Long.toString(auth.hash);
                present.add(h);
                if (!known.contains(h)) {
                    fresh.add(auth);
                }
            }
        }

        if (firstRun) {
            // Baseline only: whatever the account already has is treated as known.
            LuminaConfig.setKnownSessions(account, present);
            if (callback != null) {
                callback.onResult(0, false);
            }
            return;
        }

        // Drop hashes that no longer exist server-side so the stored list cannot grow forever.
        known.retainAll(present);
        LuminaConfig.setKnownSessions(account, known);

        if (fresh.isEmpty()) {
            if (callback != null) {
                callback.onResult(0, false);
            }
            return;
        }
        final int count = fresh.size();
        showNext(account, fresh, 0, MAX_SHOW_RETRIES);
        if (callback != null) {
            callback.onResult(count, false);
        }
    }

    /**
     * Walk the queue of unknown sessions one dialog at a time. A session is only added to the
     * known set when the user answers "It was me" — if the alert can never be shown, or the user
     * dismisses it, the session stays unknown and the next check asks again.
     */
    private static void showNext(final int account, final ArrayList<TLRPC.TL_authorization> queue,
                                 final int index, final int retriesLeft) {
        if (queue == null || index < 0 || index >= queue.size()) {
            return;
        }
        final TLRPC.TL_authorization auth = queue.get(index);
        if (auth == null) {
            showNext(account, queue, index + 1, MAX_SHOW_RETRIES);
            return;
        }
        boolean shown;
        try {
            shown = org.telegram.ui.LuminaSessionAlertBox.show(account, auth, remember -> {
                if (remember) {
                    rememberSession(account, auth.hash);
                }
                showNext(account, queue, index + 1, MAX_SHOW_RETRIES);
            });
        } catch (Throwable t) {
            shown = false;
        }
        if (!shown && retriesLeft > 0) {
            // No fragment on screen yet (start-up, passcode, decoy mode): retry a few times, then
            // give up quietly — the session stays unknown and the next check will alert again.
            AndroidUtilities.runOnUIThread(() -> showNext(account, queue, index, retriesLeft - 1), 3000);
        }
    }

    /** Mark a session as approved by the user ("It was me") so it never prompts again. */
    public static void rememberSession(int account, long hash) {
        try {
            final Set<String> known = LuminaConfig.getKnownSessions(account);
            known.add(Long.toString(hash));
            LuminaConfig.setKnownSessions(account, known);
        } catch (Throwable ignore) {
        }
    }

    private static void forgetSession(int account, long hash) {
        try {
            final Set<String> known = LuminaConfig.getKnownSessions(account);
            if (known.remove(Long.toString(hash))) {
                LuminaConfig.setKnownSessions(account, known);
            }
        } catch (Throwable ignore) {
        }
    }

    /**
     * Terminate one authorization through the official {@code account.resetAuthorization}.
     *
     * <p><b>Only ever call this from a direct user tap.</b> The guard never terminates anything on
     * its own — it just shows what it found and lets the user decide.
     */
    public static void terminate(final int account, final long hash, final TerminateCallback cb) {
        try {
            final TL_account.resetAuthorization req = new TL_account.resetAuthorization();
            req.hash = hash;
            ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                final boolean ok = error == null;
                if (ok) {
                    forgetSession(account, hash);
                }
                if (cb != null) {
                    cb.onDone(ok);
                }
            }));
        } catch (Throwable t) {
            if (cb != null) {
                cb.onDone(false);
            }
        }
    }
}
