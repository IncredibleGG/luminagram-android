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
            try {
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
            } catch (Throwable e) {
                // A prefs failure here must never poison this class: an uncaught throw in the
                // static initializer becomes ExceptionInInitializerError and bricks EVERY
                // LuminaConfig access (incl. the passcode screen). Leave fields at their safe
                // defaults and null out the store; the accessors below null-guard it.
                preferences = null;
                editor = null;
            }
            // Push the persisted appearance into the render hooks (Theme accent + font override).
            applyAppearance();
        }
    }

    // Generic accessors (used by later feature batches)
    public static boolean getBoolean(String key, boolean def) {
        if (preferences == null) {
            return def;
        }
        return preferences.getBoolean(key, def);
    }

    public static void putBoolean(String key, boolean value) {
        if (editor == null) {
            return;
        }
        editor.putBoolean(key, value).apply();
    }

    // Generic string accessors — used by the multi-provider translation settings
    // (provider id, per-provider API keys, base URL, model, prompt). Values live only
    // in the app-private "luminagram" prefs and are never logged.
    public static String getString(String key, String def) {
        if (preferences == null) {
            return def;
        }
        return preferences.getString(key, def);
    }

    public static void putString(String key, String value) {
        if (editor == null) {
            return;
        }
        editor.putString(key, value).apply();
    }

    // Generic int accessors (percent-style feature values, e.g. sticker render scale).
    public static int getInt(String key, int def) {
        if (preferences == null) {
            return def;
        }
        return preferences.getInt(key, def);
    }

    public static void putInt(String key, int value) {
        if (editor == null) {
            return;
        }
        editor.putInt(key, value).apply();
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

    // ---- Text replacer / auto-substitution (Wave 8) ----
    // User-defined substitution rules ("brb" -> "be right back") applied to OUTGOING
    // plain-text messages just before send (see SendMessagesHelper.sendMessage). Rules
    // live only in the app-private "luminagram" prefs as a JSON array string under the
    // "textReplacements" key (via getString/putString) -- nothing is sent to Telegram.
    // Each entry: { "from": <String>, "to": <String> }.
    public static final String KEY_TEXT_REPLACEMENTS = "textReplacements";

    /** All substitution rules (insertion order). Never null; empty when none / parse error. */
    public static org.json.JSONArray getTextReplacements() {
        String raw = getString(KEY_TEXT_REPLACEMENTS, "");
        if (raw != null && raw.length() > 0) {
            try {
                return new org.json.JSONArray(raw);
            } catch (org.json.JSONException ignore) {
            }
        }
        return new org.json.JSONArray();
    }

    /**
     * Apply the user's substitution rules to an outgoing plain-text message.
     * Each rule replaces whole-word, case-sensitive occurrences of "from" with "to"
     * (regex word boundaries, so "brb" does not fire inside "abrbcd"). Returns the
     * input unchanged when there are no rules -- the default, zero-change behavior.
     */
    public static String applyTextReplacements(String message) {
        if (message == null || message.length() == 0) {
            return message;
        }
        org.json.JSONArray arr = getTextReplacements();
        if (arr.length() == 0) {
            return message;
        }
        String result = message;
        for (int i = 0; i < arr.length(); i++) {
            org.json.JSONObject o = arr.optJSONObject(i);
            if (o == null) {
                continue;
            }
            String from = o.optString("from", "");
            String to = o.optString("to", "");
            if (from.length() == 0) {
                continue;
            }
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                        "\\b" + java.util.regex.Pattern.quote(from) + "\\b");
                result = p.matcher(result).replaceAll(java.util.regex.Matcher.quoteReplacement(to));
            } catch (Exception ignore) {
                // Malformed rule: skip it, never block the send.
            }
        }
        return result;
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

    // ---- Encrypted local backup (Wave 11) ----
    // Helpers for LuminaBackupActivity's export/import. Everything LuminaGram stores
    // lives in the single app-private "luminagram" prefs file, so a backup is simply a
    // type-tagged snapshot of preferences.getAll() — bookmarks, contactNotes, quickReplies,
    // textReplacements and every toggle/int/string key, including future ones. Nothing is
    // ever sent to Telegram; the snapshot is encrypted with a passphrase before it leaves
    // the app (see LuminaBackupActivity).

    /**
     * Snapshot every key in the "luminagram" prefs as a type-tagged JSON object. Each
     * entry maps a key to {@code {"t":<type>,"v":<value>}} where type is one of
     * b(oolean)/i(nt)/l(ong)/f(loat)/s(tring)/ss(string-set). Never null.
     */
    // ---- Per-dialog translate-before-send language lock ("auto" mode) ----
    // In "auto" send-language mode, translate-before-send asks ONCE per chat which language to
    // translate outgoing messages into, then remembers the choice here so it never re-prompts.
    // Stored app-privately as a JSON object string { "<dialogId>": "<langCode>" } under the
    // "trSendLangDialog" key (via getString/putString) -- never sent to Telegram.
    public static final String KEY_TR_SEND_LANG_DIALOG = "trSendLangDialog";

    /** Locked outgoing-translation language for a dialog, or null when none is chosen yet. */
    public static String getDialogSendLang(long dialogId) {
        String raw = getString(KEY_TR_SEND_LANG_DIALOG, "");
        if (raw != null && raw.length() > 0) {
            try {
                org.json.JSONObject o = new org.json.JSONObject(raw);
                String v = o.optString(String.valueOf(dialogId), null);
                if (v != null && v.length() > 0) {
                    return v;
                }
            } catch (org.json.JSONException ignore) {
            }
        }
        return null;
    }

    /** Lock (or overwrite) the outgoing-translation language for a dialog; empty/null clears it. */
    public static void setDialogSendLang(long dialogId, String lang) {
        org.json.JSONObject o;
        String raw = getString(KEY_TR_SEND_LANG_DIALOG, "");
        try {
            o = (raw != null && raw.length() > 0) ? new org.json.JSONObject(raw) : new org.json.JSONObject();
        } catch (org.json.JSONException e) {
            o = new org.json.JSONObject();
        }
        try {
            if (lang == null || lang.length() == 0) {
                o.remove(String.valueOf(dialogId));
            } else {
                o.put(String.valueOf(dialogId), lang);
            }
        } catch (org.json.JSONException ignore) {
        }
        putString(KEY_TR_SEND_LANG_DIALOG, o.toString());
    }

    public static org.json.JSONObject exportAll() {
        org.json.JSONObject out = new org.json.JSONObject();
        try {
            java.util.Map<String, ?> all = preferences.getAll();
            for (java.util.Map.Entry<String, ?> e : all.entrySet()) {
                Object val = e.getValue();
                if (val == null) {
                    continue;
                }
                org.json.JSONObject entry = new org.json.JSONObject();
                if (val instanceof Boolean) {
                    entry.put("t", "b").put("v", ((Boolean) val).booleanValue());
                } else if (val instanceof Integer) {
                    entry.put("t", "i").put("v", ((Integer) val).intValue());
                } else if (val instanceof Long) {
                    entry.put("t", "l").put("v", ((Long) val).longValue());
                } else if (val instanceof Float) {
                    entry.put("t", "f").put("v", (double) ((Float) val).floatValue());
                } else if (val instanceof String) {
                    entry.put("t", "s").put("v", (String) val);
                } else if (val instanceof java.util.Set) {
                    org.json.JSONArray a = new org.json.JSONArray();
                    for (Object s : (java.util.Set<?>) val) {
                        a.put(String.valueOf(s));
                    }
                    entry.put("t", "ss").put("v", a);
                } else {
                    continue; // unknown type: skip it rather than guess
                }
                out.put(e.getKey(), entry);
            }
        } catch (org.json.JSONException ignore) {
        }
        return out;
    }

    /**
     * Restore keys produced by {@link #exportAll()} back into the "luminagram" prefs,
     * writing each value with its original type. Returns the number of keys applied.
     * Cached static fields are refreshed via {@link #reloadFromPreferences()} afterward
     * so a restored appearance/toggle takes effect without a restart.
     */
    public static int importAll(org.json.JSONObject data) {
        if (data == null) {
            return 0;
        }
        int applied = 0;
        java.util.Iterator<String> keys = data.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            org.json.JSONObject entry = data.optJSONObject(key);
            if (entry == null) {
                continue;
            }
            String t = entry.optString("t", "s");
            try {
                switch (t) {
                    case "b":
                        editor.putBoolean(key, entry.optBoolean("v", false));
                        break;
                    case "i":
                        editor.putInt(key, entry.optInt("v", 0));
                        break;
                    case "l":
                        editor.putLong(key, entry.optLong("v", 0L));
                        break;
                    case "f":
                        editor.putFloat(key, (float) entry.optDouble("v", 0));
                        break;
                    case "ss": {
                        org.json.JSONArray a = entry.optJSONArray("v");
                        java.util.HashSet<String> set = new java.util.HashSet<>();
                        if (a != null) {
                            for (int i = 0; i < a.length(); i++) {
                                set.add(a.optString(i));
                            }
                        }
                        editor.putStringSet(key, set);
                        break;
                    }
                    default:
                        editor.putString(key, entry.optString("v", ""));
                        break;
                }
                applied++;
            } catch (Exception ignore) {
                // Malformed entry: skip it, never abort the whole restore.
            }
        }
        editor.apply();
        reloadFromPreferences();
        return applied;
    }

    /** Re-read every cached field from the "luminagram" prefs (used after an import). */
    public static void reloadFromPreferences() {
        synchronized (sync) {
            configLoaded = false;
            loadConfig();
        }
    }
}
