package org.telegram.messenger;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * LuminaGram global feature-flag store.
 * Style mirrors SharedConfig / exteraGram's ExteraConfig: a private prefs file
 * ("luminagram") cached in public static fields, reachable from any render hook
 * without a currentAccount in scope.
 */
public class LuminaConfig {

    private static final Object sync = new Object();
    private static boolean configLoaded;

    public static SharedPreferences preferences;
    public static SharedPreferences.Editor editor;

    // ---- Chat-list / tabs / stories customization ----
    public static boolean hideTabs;
    public static boolean hideStories;
    public static boolean compactChatList;

    // ---- Translation ----
    public static boolean translateBeforeSend;
    public static boolean translateBeforeSendConfirm;

    // ---- Appearance (Wave 2) ----
    public static boolean materialYouEnabled;   // Android 12+ dynamic colors (Monet)
    public static boolean customAccentEnabled;   // manual accent override (when Material You off)
    public static int customAccentColor;         // 0 = none picked yet
    public static int appFont;                   // 0 = Telegram default, 1 = System, 2 = Serif, 3 = Monospace

    static {
        loadConfig();
    }

    public static void loadConfig() {
        synchronized (sync) {
            if (configLoaded) {
                return;
            }
            preferences = ApplicationLoader.applicationContext
                    .getSharedPreferences("luminagram", Activity.MODE_PRIVATE);
            editor = preferences.edit();

            hideTabs = preferences.getBoolean("hideTabs", false);
            hideStories = preferences.getBoolean("hideStories", false);
            compactChatList = preferences.getBoolean("compactChatList", false);
            translateBeforeSend = preferences.getBoolean("translateBeforeSend", false);
            translateBeforeSendConfirm = preferences.getBoolean("translateBeforeSendConfirm", false);
            materialYouEnabled = preferences.getBoolean("materialYouEnabled", false);
            customAccentEnabled = preferences.getBoolean("customAccentEnabled", false);
            customAccentColor = preferences.getInt("customAccentColor", 0);
            appFont = preferences.getInt("appFont", 0);

            configLoaded = true;
            // Push the persisted appearance into the render hooks (Theme accent + font override).
            applyAppearance();
        }
    }

    // Generic accessors (used by later feature batches)
    public static boolean getBoolean(String key, boolean def) {
        return preferences.getBoolean(key, def);
    }

    public static void putBoolean(String key, boolean value) {
        editor.putBoolean(key, value).apply();
    }

    // Generic string accessors — used by the multi-provider translation settings
    // (provider id, per-provider API keys, base URL, model, prompt). Values live only
    // in the app-private "luminagram" prefs and are never logged.
    public static String getString(String key, String def) {
        return preferences.getString(key, def);
    }

    public static void putString(String key, String value) {
        editor.putString(key, value).apply();
    }

    // Typed toggles keep the static field and the persisted value in sync (XOR idiom)
    public static void toggleHideTabs() {
        editor.putBoolean("hideTabs", hideTabs ^= true).apply();
    }

    public static void toggleHideStories() {
        editor.putBoolean("hideStories", hideStories ^= true).apply();
    }

    public static void toggleCompactChatList() {
        editor.putBoolean("compactChatList", compactChatList ^= true).apply();
    }

    public static void toggleTranslateBeforeSend() {
        editor.putBoolean("translateBeforeSend", translateBeforeSend ^= true).apply();
    }

    public static void toggleTranslateBeforeSendConfirm() {
        editor.putBoolean("translateBeforeSendConfirm", translateBeforeSendConfirm ^= true).apply();
    }

    // ---- Appearance (Wave 2) ----

    /** The Android 12+ Material You primary accent, or 0 when unavailable. */
    public static int getMaterialYouAccent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                return ApplicationLoader.applicationContext.getColor(android.R.color.system_accent1_500) | 0xff000000;
            } catch (Exception ignore) {
            }
        }
        return 0;
    }

    /** Effective app accent: Material You wins, then a manual override, else 0 (no override). */
    public static int getEffectiveAccent() {
        if (materialYouEnabled) {
            int c = getMaterialYouAccent();
            if (c != 0) {
                return c;
            }
        }
        if (customAccentEnabled && customAccentColor != 0) {
            return customAccentColor | 0xff000000;
        }
        return 0;
    }

    /** Publish the current appearance selection into the render hooks. */
    public static void applyAppearance() {
        try {
            org.telegram.ui.ActionBar.Theme.luminaAccentColor = getEffectiveAccent();
        } catch (Throwable ignore) {
        }
        try {
            AndroidUtilities.luminaFont = appFont;
            AndroidUtilities.mediumTypeface = null; // recompute bold() with the new family
        } catch (Throwable ignore) {
        }
    }

    public static void toggleMaterialYou() {
        editor.putBoolean("materialYouEnabled", materialYouEnabled ^= true).apply();
        applyAppearance();
    }

    public static void toggleCustomAccent() {
        editor.putBoolean("customAccentEnabled", customAccentEnabled ^= true).apply();
        applyAppearance();
    }

    public static void setCustomAccentColor(int color) {
        customAccentColor = color;
        editor.putInt("customAccentColor", color).apply();
        applyAppearance();
    }

    public static void setAppFont(int font) {
        appFont = font;
        editor.putInt("appFont", font).apply();
        applyAppearance();
    }
}
