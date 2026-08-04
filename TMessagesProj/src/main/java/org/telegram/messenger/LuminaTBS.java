package org.telegram.messenger;

/**
 * LuminaGram — translate-before-send original capture.
 *
 * When "translate before send" is enabled, ChatActivityEnterView translates the user's
 * typed ORIGINAL and sends only the TRANSLATION; the original would otherwise be lost from
 * the chat history. This store persists a small {random_id -> originalText} map so a sent
 * bubble can later reveal the pre-translation original.
 *
 * Capture is a two-step correlation, because the outgoing message's stable client id
 * (random_id) is assigned deep inside SendMessagesHelper, after the enter view has already
 * handed off the text:
 *   1. ChatActivityEnterView calls {@link #setPending} right before dispatching a
 *      translation, stashing {dialogId, translated, original}.
 *   2. SendMessagesHelper calls {@link #onOutgoingText} when it assigns the outgoing text
 *      message's random_id; if a pending entry matches (same dialog + exact sent text, which
 *      IS the translation), we bind random_id -> original via {@link #rememberOriginal} and
 *      drop the pending.
 *
 * Nothing is stored unless translate-before-send actually fired: only translation-send call
 * sites set a pending, so default behavior is unchanged when the feature is off or when the
 * user sends the original as typed.
 *
 * Persistence is JSON in the app-private "luminagram" prefs via LuminaConfig; the map is
 * pruned to the most recent {@link #MAX_ENTRIES} entries so it can't grow unbounded. Nothing
 * here is ever sent to Telegram.
 */
public final class LuminaTBS {

    private LuminaTBS() {}

    // Persisted map key inside LuminaConfig ("luminagram" SharedPreferences).
    private static final String STORE_KEY = "tbsOriginals";
    // Keep only the most recent originals; oldest are evicted first.
    private static final int MAX_ENTRIES = 500;
    // Safety caps for the transient pending list. Unmatched pendings are rare (e.g. a
    // translation split across multiple messages, or a send that never completes) but must
    // never accumulate.
    private static final int MAX_PENDING = 32;
    private static final long PENDING_TTL_MS = 60_000L;

    // A translation about to be sent, awaiting random_id correlation.
    private static final class Pending {
        final long dialogId;
        final String translated; // trimmed; matched against the actual sent text
        final String original;
        final long time;
        Pending(long dialogId, String translated, String original) {
            this.dialogId = dialogId;
            this.translated = translated;
            this.original = original;
            this.time = System.currentTimeMillis();
        }
    }

    private static final java.util.ArrayList<Pending> pending = new java.util.ArrayList<>();

    // random_id -> original, lazily loaded from prefs; insertion order == recency for pruning.
    private static java.util.LinkedHashMap<Long, String> store;

    /**
     * Stash the pre-translation original right before a translate-before-send TRANSLATION is
     * dispatched. Only translation-send call sites call this, so the store stays empty when the
     * feature is off or when the original is sent as-is.
     */
    public static synchronized void setPending(long dialogId, String translated, String original) {
        if (translated == null || original == null) {
            return;
        }
        final long now = System.currentTimeMillis();
        for (int i = pending.size() - 1; i >= 0; i--) {
            if (now - pending.get(i).time > PENDING_TTL_MS) {
                pending.remove(i);
            }
        }
        while (pending.size() >= MAX_PENDING) {
            pending.remove(0);
        }
        pending.add(new Pending(dialogId, translated.trim(), original));
    }

    /**
     * Called by SendMessagesHelper when an outgoing text message's random_id is assigned. If a
     * pending translation matches this dialog and the exact sent text, bind it to random_id.
     * Cheap no-op when there is no pending match (the common case — no active TBS send).
     */
    public static synchronized void onOutgoingText(long dialogId, long randomId, String sentText) {
        if (randomId == 0 || sentText == null || pending.isEmpty()) {
            return;
        }
        final String norm = sentText.trim();
        for (int i = 0; i < pending.size(); i++) {
            final Pending p = pending.get(i);
            if (p.dialogId == dialogId && p.translated.equals(norm)) {
                pending.remove(i);
                rememberOriginal(randomId, p.original);
                return;
            }
        }
    }

    /** Persist random_id -> original, evicting the oldest entries once past the cap. */
    public static synchronized void rememberOriginal(long randomId, String original) {
        if (randomId == 0 || original == null) {
            return;
        }
        ensureLoaded();
        store.remove(randomId);      // refresh recency if it somehow already existed
        store.put(randomId, original);
        while (store.size() > MAX_ENTRIES) {
            final java.util.Iterator<Long> it = store.keySet().iterator();
            if (!it.hasNext()) {
                break;
            }
            it.next();
            it.remove();
        }
        save();
    }

    /**
     * The stored pre-translation original for an outgoing translate-before-send message,
     * resolved by its random_id, or null if none is recorded.
     */
    public static synchronized String getOriginal(MessageObject mo) {
        if (mo == null || mo.messageOwner == null) {
            return null;
        }
        final long randomId = mo.messageOwner.random_id;
        if (randomId == 0) {
            return null;
        }
        ensureLoaded();
        return store.get(randomId);
    }

    private static void ensureLoaded() {
        if (store != null) {
            return;
        }
        store = new java.util.LinkedHashMap<>();
        final String raw = LuminaConfig.getString(STORE_KEY, null);
        if (raw == null || raw.length() == 0) {
            return;
        }
        try {
            // Ordered array of {"r": random_id, "o": original} objects, so recency-based
            // pruning survives a reload (JSON object key order would not be preserved).
            final org.json.JSONArray arr = new org.json.JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                final org.json.JSONObject o = arr.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                final long randomId = o.optLong("r", 0);
                final String original = o.optString("o", null);
                if (randomId != 0 && original != null) {
                    store.put(randomId, original);
                }
            }
        } catch (org.json.JSONException ignore) {
            // Corrupt/legacy payload: start from an empty store.
            store.clear();
        }
    }

    private static void save() {
        final org.json.JSONArray arr = new org.json.JSONArray();
        for (java.util.Map.Entry<Long, String> e : store.entrySet()) {
            try {
                final org.json.JSONObject o = new org.json.JSONObject();
                o.put("r", e.getKey());
                o.put("o", e.getValue());
                arr.put(o);
            } catch (org.json.JSONException ignore) {
                // Skip this entry on serialization failure; best-effort cache.
            }
        }
        LuminaConfig.putString(STORE_KEY, arr.toString());
    }
}
