package org.telegram.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.widget.TextView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LuminaLocale;
import org.telegram.messenger.LuminaSessionGuard;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;

/**
 * LuminaGram login-guard dialogs.
 *
 * <p>Two separate jobs, both purely presentational — the network calls live in
 * {@link LuminaSessionGuard}:
 * <ul>
 *   <li>{@link #show} — the "a new device signed in" alert listing device / app / IP / country /
 *       time, with a red <i>Not me — terminate now</i> button and a plain <i>That was me</i>
 *       button. Termination happens only on that explicit tap, and is followed by a nudge to
 *       switch on Two-Step Verification.</li>
 *   <li>{@link #confirmQrAuthorisation} — the one-shot warning shown before the QR-login camera
 *       opens, because scanning a login QR someone sent you hands them a live session.</li>
 * </ul>
 *
 * <p>Note that {@link BaseFragment#showDialog} silently refuses while a fragment transition is in
 * flight and overwrites any dismiss listener set on the builder, so the dismiss callback is passed
 * through {@code showDialog} and a {@code null} return is treated as "not shown".
 */
public final class LuminaSessionAlertBox {

    private LuminaSessionAlertBox() {
    }

    public interface Listener {
        /**
         * @param remember true only when the user answered "That was me" — the session is then
         *                 added to the known list and never prompts again.
         */
        void onFinished(boolean remember);
    }

    /**
     * Show the new-login alert.
     *
     * @return false when no UI is available right now (nothing shown, nothing changed, and the
     *         listener is NOT called).
     */
    public static boolean show(final int account, final TLRPC.TL_authorization auth, final Listener listener) {
        if (auth == null) {
            return false;
        }
        try {
            if (SharedConfig.appLocked) {
                // Passcode screen is up: an alert here would render the IP/device of the intruder
                // on the lock screen. Report "not shown" and let the guard try again later.
                return false;
            }
        } catch (Throwable ignore) {
        }
        final BaseFragment fragment = LaunchActivity.getSafeLastFragment();
        if (fragment == null) {
            return false;
        }
        final Activity activity = fragment.getParentActivity();
        if (activity == null) {
            return false;
        }
        try {
            final boolean[] answered = new boolean[]{false};
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(LuminaLocale.getString(R.string.LuminaSessionAlertTitle));
            builder.setMessage(buildMessage(auth));
            builder.setPositiveButton(LuminaLocale.getString(R.string.LuminaSessionAlertNotMe), (d, w) -> {
                answered[0] = true;
                terminate(fragment, account, auth, listener);
            });
            builder.setNegativeButton(LuminaLocale.getString(R.string.LuminaSessionAlertItsMe), (d, w) -> {
                answered[0] = true;
                if (listener != null) {
                    listener.onFinished(true);
                }
            });
            final AlertDialog dialog = builder.create();
            // Dismissed without answering (back button / tap outside): do NOT mark the session
            // known, so the next check asks again instead of silently trusting an unknown login.
            final Dialog shown = fragment.showDialog(dialog, d -> {
                if (!answered[0]) {
                    answered[0] = true;
                    if (listener != null) {
                        listener.onFinished(false);
                    }
                }
            });
            if (shown == null) {
                return false;
            }
            // Buttons only exist once the dialog is shown.
            final TextView positive = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (positive != null) {
                positive.setTextColor(Theme.getColor(Theme.key_text_RedBold));
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * One confirmation before a QR login is authorised. Runs {@code onConfirmed} only after the
     * user explicitly accepts. Fails open — when the guard is off, no activity is available or the
     * dialog cannot be shown, the normal flow continues untouched.
     */
    public static void confirmQrAuthorisation(final BaseFragment fragment, final Runnable onConfirmed) {
        if (onConfirmed == null) {
            return;
        }
        try {
            final Activity activity = fragment == null ? null : fragment.getParentActivity();
            if (activity == null || !LuminaSessionGuard.isEnabled()) {
                onConfirmed.run();
                return;
            }
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(LuminaLocale.getString(R.string.LuminaSessionQrConfirmTitle));
            builder.setMessage(LuminaLocale.getString(R.string.LuminaSessionQrConfirmMessage));
            builder.setPositiveButton(LuminaLocale.getString(R.string.LuminaSessionQrConfirmContinue), (d, w) -> {
                try {
                    onConfirmed.run();
                } catch (Throwable ignore) {
                }
            });
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            if (fragment.showDialog(builder.create()) == null) {
                onConfirmed.run();
            }
        } catch (Throwable t) {
            // Never let the guard break the built-in flow.
            try {
                onConfirmed.run();
            } catch (Throwable ignore) {
            }
        }
    }

    // ---- internals ----

    private static void terminate(final BaseFragment fragment, final int account,
                                  final TLRPC.TL_authorization auth, final Listener listener) {
        LuminaSessionGuard.terminate(account, auth.hash, ok -> {
            if (ok) {
                showTwoStepTip(fragment, account, listener);
            } else {
                try {
                    AlertsCreator.showSimpleAlert(fragment,
                            LuminaLocale.getString(R.string.LuminaSessionAlertTitle),
                            LuminaLocale.getString(R.string.LuminaSessionTerminateFailed));
                } catch (Throwable ignore) {
                }
                if (listener != null) {
                    listener.onFinished(false);
                }
            }
        });
    }

    /** Confirms the kill and points the user at Two-Step Verification, which stops a repeat. */
    private static void showTwoStepTip(final BaseFragment fragment, final int account, final Listener listener) {
        final Activity activity = fragment == null ? null : fragment.getParentActivity();
        if (activity == null) {
            if (listener != null) {
                listener.onFinished(false);
            }
            return;
        }
        try {
            final boolean[] done = new boolean[]{false};
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(LuminaLocale.getString(R.string.LuminaSessionTerminated));
            builder.setMessage(LuminaLocale.getString(R.string.LuminaSession2FAMessage));
            builder.setPositiveButton(LuminaLocale.getString(R.string.LuminaSession2FAButton), (d, w) -> {
                try {
                    fragment.presentFragment(new TwoStepVerificationActivity(account));
                } catch (Throwable ignore) {
                }
            });
            builder.setNegativeButton(LuminaLocale.getString(R.string.LuminaSessionLater), null);
            final Dialog shown = fragment.showDialog(builder.create(), d -> {
                if (!done[0]) {
                    done[0] = true;
                    if (listener != null) {
                        listener.onFinished(false);
                    }
                }
            });
            if (shown == null && !done[0]) {
                done[0] = true;
                if (listener != null) {
                    listener.onFinished(false);
                }
            }
        } catch (Throwable t) {
            if (listener != null) {
                listener.onFinished(false);
            }
        }
    }

    private static CharSequence buildMessage(TLRPC.TL_authorization auth) {
        final StringBuilder sb = new StringBuilder();
        sb.append(LuminaLocale.getString(R.string.LuminaSessionAlertIntro));
        sb.append("\n");
        appendRow(sb, LuminaLocale.getString(R.string.LuminaSessionAlertDevice), deviceLine(auth));
        appendRow(sb, LuminaLocale.getString(R.string.LuminaSessionAlertApp), appLine(auth));
        appendRow(sb, LuminaLocale.getString(R.string.LuminaSessionAlertIp), auth.ip);
        appendRow(sb, LuminaLocale.getString(R.string.LuminaSessionAlertLocation), locationLine(auth));
        appendRow(sb, LuminaLocale.getString(R.string.LuminaSessionAlertTime), dateLine(auth));
        return sb.toString().trim();
    }

    private static void appendRow(StringBuilder sb, String label, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        sb.append("\n").append(label).append(": ").append(value);
    }

    private static String deviceLine(TLRPC.TL_authorization auth) {
        final StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(auth.device_model)) {
            sb.append(auth.device_model);
        }
        final StringBuilder os = new StringBuilder();
        if (!TextUtils.isEmpty(auth.platform)) {
            os.append(auth.platform);
        }
        if (!TextUtils.isEmpty(auth.system_version)) {
            if (os.length() > 0) {
                os.append(" ");
            }
            os.append(auth.system_version);
        }
        if (os.length() > 0) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(os);
        }
        return sb.toString();
    }

    private static String appLine(TLRPC.TL_authorization auth) {
        final StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(auth.app_name)) {
            sb.append(auth.app_name);
        }
        if (!TextUtils.isEmpty(auth.app_version)) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(auth.app_version);
        }
        return sb.toString();
    }

    private static String locationLine(TLRPC.TL_authorization auth) {
        final StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(auth.country)) {
            sb.append(auth.country);
        }
        if (!TextUtils.isEmpty(auth.region)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(auth.region);
        }
        return sb.toString();
    }

    private static String dateLine(TLRPC.TL_authorization auth) {
        try {
            final int date = auth.date_created != 0 ? auth.date_created : auth.date_active;
            if (date == 0) {
                return "";
            }
            return LocaleController.formatDateTime(date, true);
        } catch (Throwable t) {
            return "";
        }
    }
}
