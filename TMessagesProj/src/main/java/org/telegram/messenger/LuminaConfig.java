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

    // ---- Message bookmarks / collections (Wave 3) ----
    // A local, client-side alternative to Saved Messages. Bookmarks live only in the
    // app-private "luminagram" prefs as a JSON array string under the "bookmarks" key
    // (via getString/putString) — nothing is ever sent to Telegram. Each entry:
    //   { "dialogId": <long>, "messageId": <int>, "snippet": <String>, "date": <long ms> }
    public static final String KEY_BOOKMARKS = "bookmarks";

    /** All saved bookmarks (oldest first, matching insertion order). Never null. */
    public static org.json.JSONArray getBookmarks() {
        String raw = getString(KEY_BOOKMARKS, "");
        if (raw != null && raw.length() > 0) {
            try {
                return new org.json.JSONArray(raw);
            } catch (org.json.JSONException ignore) {
            }
        }
        return new org.json.JSONArray();
    }

    public static boolean isBookmarked(long dialogId, int messageId) {
        org.json.JSONArray arr = getBookmarks();
        for (int i = 0; i < arr.length(); i++) {
            org.json.JSONObject o = arr.optJSONObject(i);
            if (o != null && o.optLong("dialogId") == dialogId && o.optInt("messageId") == messageId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Toggle a bookmark: add it when absent, remove it when already present.
     * @return true if the message is bookmarked after this call, false if it was removed.
     */
    public static boolean toggleBookmark(long dialogId, int messageId, String snippet) {
        org.json.JSONArray arr = getBookmarks();
        org.json.JSONArray out = new org.json.JSONArray();
        boolean removed = false;
        for (int i = 0; i < arr.length(); i++) {
            org.json.JSONObject o = arr.optJSONObject(i);
            if (o == null) {
                continue;
            }
            if (o.optLong("dialogId") == dialogId && o.optInt("messageId") == messageId) {
                removed = true; // drop the existing entry
                continue;
            }
            out.put(o);
        }
        if (!removed) {
            try {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("dialogId", dialogId);
                o.put("messageId", messageId);
                o.put("snippet", snippet == null ? "" : snippet);
                o.put("date", System.currentTimeMillis());
                out.put(o);
            } catch (org.json.JSONException ignore) {
            }
        }
        putString(KEY_BOOKMARKS, out.toString());
        return !removed;
    }

    /** Remove a single bookmark if present (used by the Bookmarks list's delete action). */
    public static void removeBookmark(long dialogId, int messageId) {
        org.json.JSONArray arr = getBookmarks();
        org.json.JSONArray out = new org.json.JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            org.json.JSONObject o = arr.optJSONObject(i);
            if (o == null) {
                continue;
            }
            if (o.optLong("dialogId") == dialogId && o.optInt("messageId") == messageId) {
                continue;
            }
            out.put(o);
        }
        putString(KEY_BOOKMARKS, out.toString());
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
