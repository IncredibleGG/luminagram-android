package org.telegram.messenger;

/**
 * LuminaGram private contact notes &amp; tags (Wave 10).
 *
 * A purely LOCAL, on-device annotation you can attach to any user: a free-form
 * private note plus a simple comma-separated list of tags. Nothing here is ever
 * sent to Telegram — the data lives only in the app-private "luminagram" prefs as
 * a single JSON object string under the "contactNotes" key (via
 * {@link LuminaConfig#getString}/{@link LuminaConfig#putString}), mirroring the
 * message-bookmarks storage pattern.
 *
 * Layout: { "&lt;userId&gt;": { "note": &lt;String&gt;, "tags": &lt;String&gt; }, ... }
 */
public final class LuminaContactNotes {

    private LuminaContactNotes() {}

    public static final String KEY_CONTACT_NOTES = "contactNotes";

    /** The whole userId -&gt; {note,tags} map. Never null; empty on missing / parse error. */
    private static org.json.JSONObject getAll() {
        String raw = LuminaConfig.getString(KEY_CONTACT_NOTES, "");
        if (raw != null && raw.length() > 0) {
            try {
                return new org.json.JSONObject(raw);
            } catch (org.json.JSONException ignore) {
            }
        }
        return new org.json.JSONObject();
    }

    private static org.json.JSONObject entry(long userId) {
        return getAll().optJSONObject(Long.toString(userId));
    }

    /** The saved private note for a user, or "" when none. Never null. */
    public static String getNote(long userId) {
        org.json.JSONObject o = entry(userId);
        return o == null ? "" : o.optString("note", "");
    }

    /** The saved comma-separated tags for a user, or "" when none. Never null. */
    public static String getTags(long userId) {
        org.json.JSONObject o = entry(userId);
        return o == null ? "" : o.optString("tags", "");
    }

    /** True when the user has a non-empty note or non-empty tags. */
    public static boolean hasNote(long userId) {
        org.json.JSONObject o = entry(userId);
        if (o == null) {
            return false;
        }
        return o.optString("note", "").length() > 0 || o.optString("tags", "").length() > 0;
    }

    /**
     * Persist (or clear) the private note and tags for a user. Blank note AND blank
     * tags removes the entry entirely, keeping the stored map compact.
     */
    public static void setNote(long userId, String note, String tags) {
        org.json.JSONObject all = getAll();
        String key = Long.toString(userId);
        String n = note == null ? "" : note.trim();
        String t = tags == null ? "" : tags.trim();
        try {
            if (n.length() == 0 && t.length() == 0) {
                all.remove(key);
            } else {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("note", n);
                o.put("tags", t);
                all.put(key, o);
            }
        } catch (org.json.JSONException ignore) {
        }
        LuminaConfig.putString(KEY_CONTACT_NOTES, all.toString());
    }

    /**
     * A short single-line summary for the profile row value: the note collapsed to
     * one line (truncated) with any tags appended. Returns "" when the user has no
     * note and no tags (the caller then shows the "add a note" placeholder).
     */
    public static String getSummary(long userId) {
        String note = getNote(userId);
        String tags = getTags(userId);
        StringBuilder sb = new StringBuilder();
        if (note.length() > 0) {
            String single = note.replaceAll("\\s+", " ").trim();
            if (single.length() > 60) {
                single = single.substring(0, 60) + "…";
            }
            sb.append(single);
        }
        if (tags.length() > 0) {
            if (sb.length() > 0) {
                sb.append("  ·  ");
            }
            sb.append(tags);
        }
        return sb.toString();
    }
}
