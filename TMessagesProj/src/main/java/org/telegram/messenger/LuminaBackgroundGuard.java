package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

/**
 * Keeps LuminaGram reachable when it is not on screen.
 *
 * Stock Telegram does not need any of this. It receives pushes through Firebase,
 * and Google Play services is a system process that is always running - Telegram's
 * own process can be dead and the notification still arrives. That is why upstream
 * ships both background switches defaulting to false and never asks for a battery
 * exemption.
 *
 * We changed the application id, so Firebase will not issue us a token
 * (PushListenerController records "__FIREBASE_FAILED__" and registers a null one).
 * That leaves the MTProto background connection and the foreground service as the
 * only delivery paths, and both of them live or die with our own process. Doze will
 * suspend it, and the vendor ROMs will simply kill it.
 *
 * So two things have to happen that upstream never has to do: the switches have to
 * default on, and the user has to be told - visibly, and more than once if needed -
 * that the system will otherwise silence us.
 */
public class LuminaBackgroundGuard {

    // Upstream's own preference keys. We are changing their defaults, not adding
    // parallel settings, so the switches in Notifications keep working and a user
    // who deliberately turns delivery off stays off.
    private static final String KEY_PUSH_SERVICE = "pushService";
    private static final String KEY_PUSH_CONNECTION = "pushConnection";
    private static final String KEY_DEFAULTS_APPLIED = "luminaBackgroundDefaultsApplied";

    private LuminaBackgroundGuard() {
    }

    /**
     * Turns both delivery paths on, once per install. Guarded by its own flag rather
     * than by "is the value absent", because a user who turns delivery off must not
     * have it turned back on by the next launch.
     */
    public static void applyDefaultsOnce() {
        try {
            final SharedPreferences prefs = MessagesController.getGlobalNotificationsSettings();
            if (prefs == null || prefs.getBoolean(KEY_DEFAULTS_APPLIED, false)) {
                return;
            }
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_DEFAULTS_APPLIED, true);
            if (!prefs.contains(KEY_PUSH_SERVICE)) {
                editor.putBoolean(KEY_PUSH_SERVICE, true);
            }
            if (!prefs.contains(KEY_PUSH_CONNECTION)) {
                editor.putBoolean(KEY_PUSH_CONNECTION, true);
            }
            editor.commit();
        } catch (Throwable ignore) {
            // Delivery defaults are not worth crashing a launch over.
        }
    }

    /** True when the user has turned off both ways we could reach them. */
    public static boolean deliveryDisabledByUser() {
        try {
            final SharedPreferences prefs = MessagesController.getGlobalNotificationsSettings();
            if (prefs == null) {
                return false;
            }
            return !prefs.getBoolean(KEY_PUSH_SERVICE, true)
                    && !prefs.getBoolean(KEY_PUSH_CONNECTION, true);
        } catch (Throwable ignore) {
            return false;
        }
    }

    /**
     * Whether the system has agreed to stop suspending us. Below API 23 there is no
     * such thing to ask about, and nothing suspends us either, so report true.
     */
    public static boolean batteryUnrestricted(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        try {
            final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm == null || pm.isIgnoringBatteryOptimizations(context.getPackageName());
        } catch (Throwable ignore) {
            return true;
        }
    }

    /**
     * Asks the system for the exemption. Some ROMs remove this dialog, so fall back
     * to the settings list, and to our own app details page after that.
     */
    public static boolean requestBatteryExemption(Activity activity) {
        if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        final Intent direct = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:" + activity.getPackageName()));
        if (startIfPossible(activity, direct)) {
            return true;
        }
        if (startIfPossible(activity, new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) {
            return true;
        }
        return openAppDetails(activity);
    }

    /**
     * The battery exemption is a documented Android setting. "Autostart" is not - it
     * is a vendor addition with no API, no way to read its current state, and an
     * activity name that moves between ROM releases. So this only tries to open the
     * right screen; whether the user then grants it is something we cannot observe,
     * which is why the UI has to explain rather than verify.
     */
    public static boolean vendorRestrictsBackground() {
        final String maker = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase();
        return maker.contains("xiaomi") || maker.contains("redmi") || maker.contains("poco")
                || maker.contains("huawei") || maker.contains("honor")
                || maker.contains("oppo") || maker.contains("realme") || maker.contains("oneplus")
                || maker.contains("vivo") || maker.contains("iqoo")
                || maker.contains("meizu") || maker.contains("samsung")
                || maker.contains("asus") || maker.contains("letv") || maker.contains("smartisan");
    }

    /** Opens the vendor's autostart screen, falling back to our app details page. */
    public static boolean openAutostartSettings(Activity activity) {
        if (activity == null) {
            return false;
        }
        final String maker = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase();
        final String[][] candidates;
        if (maker.contains("xiaomi") || maker.contains("redmi") || maker.contains("poco")) {
            candidates = new String[][]{
                    {"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"}};
        } else if (maker.contains("huawei") || maker.contains("honor")) {
            candidates = new String[][]{
                    {"com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
                    {"com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"}};
        } else if (maker.contains("oppo") || maker.contains("realme")) {
            candidates = new String[][]{
                    {"com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"},
                    {"com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"},
                    {"com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"}};
        } else if (maker.contains("vivo") || maker.contains("iqoo")) {
            candidates = new String[][]{
                    {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"},
                    {"com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"}};
        } else if (maker.contains("letv")) {
            candidates = new String[][]{
                    {"com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"}};
        } else if (maker.contains("asus")) {
            candidates = new String[][]{
                    {"com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity"}};
        } else {
            candidates = new String[0][];
        }
        for (String[] pair : candidates) {
            final Intent intent = new Intent().setClassName(pair[0], pair[1]);
            if (startIfPossible(activity, intent)) {
                return true;
            }
        }
        // Samsung, Meizu and anything unrecognised: the app details page at least
        // gets the user to the battery section for this app.
        return openAppDetails(activity);
    }

    private static boolean openAppDetails(Activity activity) {
        return startIfPossible(activity, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:" + activity.getPackageName())));
    }

    private static boolean startIfPossible(Activity activity, Intent intent) {
        try {
            if (activity.getPackageManager().resolveActivity(intent, 0) == null) {
                return false;
            }
            activity.startActivity(intent);
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }
}
