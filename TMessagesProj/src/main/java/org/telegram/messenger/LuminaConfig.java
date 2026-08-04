package org.telegram.messenger;

import android.app.Activity;
import android.content.SharedPreferences;

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

            configLoaded = true;
        }
    }

    // Generic accessors (used by later feature batches)
    public static boolean getBoolean(String key, boolean def) {
        return preferences.getBoolean(key, def);
    }

    public static void putBoolean(String key, boolean value) {
        editor.putBoolean(key, value).apply();
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
}
