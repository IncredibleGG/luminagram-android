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
 * Reload survival: the random_id binding is only usable while the message still carries its
 * random_id in memory. Once the outgoing message is acknowledged by the server it gets a real
 * message id (mid) and MessagesStorage prunes the randoms_v2 row, so after leaving and
 * re-entering the chat the reloaded MessageObject has random_id == 0 and the random_id lookup
 * fails. To survive that, SendMessagesHelper calls {@link #onServerId} the moment the message
 * gets its real server id; we then also record the original under a STABLE {dialogId+"_"+mid}
 * key, which is exactly what a reloaded message can be looked up by. {@link #getOriginal} tries
 * random_id first (fresh sends, before the id swap) and falls back to the dialogId+mid key
 * (after reload).
 *
 * Nothing is stored unless translate-before-send actually fired: only translation-send call
 * sites set a pending, so default behavior is unchanged when the feature is off or when the
 * user sends the original as typed.
 *
 * Persistence is JSON in the app-private "luminagram" prefs via LuminaConfig; both maps are
 * pruned to the most recent {@link #MAX_ENTRIES} entries so they can't grow unbounded. Nothing
 * here is ever sent to Telegram.
 */
public final class LuminaTBS {

    private LuminaTBS() {}

    // Persisted map keys inside LuminaConfig ("luminagram" SharedPreferences).
    private static final String STORE_KEY = "tbsOriginals";       // random_id -> original
    private static final String STORE_KEY_MID = "tbsOriginalsMid"; // dialogId+"_"+mid -> original
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
    // "dialogId_mid" -> original, the reload-stable fallback; same recency-ordered pruning.
    private static java.util.LinkedHashMap<String, String> midStore;

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

    /**
     * Called by SendMessagesHelper the moment an outgoing message gets its real server id (mid).
     * If we captured an original for this random_id, also record it under the STABLE
     * {dialogId+"_"+mid} key so it survives the randoms_v2 prune and a chat reload. Cheap no-op
     * when this random_id has no captured original (i.e. not a translate-before-send message).
     */
    public static synchronized void onServerId(long dialogId, long randomId, long mid) {
        if (randomId == 0 || mid <= 0) {
            return;
        }
        ensureLoaded();
        final String original = store.get(randomId);
        if (original == null) {
            return;
        }
        rememberOriginalByMid(dialogId, mid, original);
    }

    /** Persist random_id -> original, evicting the oldest entries once past the cap. */
    public static synchronized void rememberOriginal(long randomId, String original) {
        if (randomId == 0 || original == null) {
            return;
        }
        ensureLoaded();
        store.remove(randomId);      // refresh recency if it somehow already existed
        store.put(randomId, original);
        prune(store);
        save();
    }

    /**
     * Persist a stable {dialogId+"_"+mid} -> original binding (the reload-survivable key).
     * Caller already holds the monitor.
     */
    private static void rememberOriginalByMid(long dialogId, long mid, String original) {
        final String key = midKey(dialogId, mid);
        midStore.remove(key);        // refresh recency if it somehow already existed
        midStore.put(key, original);
        prune(midStore);
        save();
    }

    /**
     * The stored pre-translation original for an outgoing translate-before-send message. Resolved
     * by random_id first (fresh sends, before the server id swap) and, failing that, by the stable
     * {dialogId+"_"+mid} key (reloaded messages, whose random_id is 0). Returns null if none.
     */
    public static synchronized String getOriginal(MessageObject mo) {
        if (mo == null || mo.messageOwner == null) {
            return null;
        }
        ensureLoaded();
        final long randomId = mo.messageOwner.random_id;
        if (randomId != 0) {
            final String s = store.get(randomId);
            if (s != null) {
                return s;
            }
        }
        final int mid = mo.getId();
        if (mid > 0) {
            return midStore.get(midKey(mo.getDialogId(), mid));
        }
        return null;
    }

    private static String midKey(long dialogId, long mid) {
        return dialogId + "_" + mid;
    }

    /** Drop the oldest entries until the map is within MAX_ENTRIES. */
    private static void prune(java.util.LinkedHashMap<?, ?> map) {
        while (map.size() > MAX_ENTRIES) {
            final java.util.Iterator<?> it = map.keySet().iterator();
            if (!it.hasNext()) {
                break;
            }
            it.next();
            it.remove();
        }
    }

    private static void ensureLoaded() {
        if (store != null) {
            return;
        }
        store = new java.util.LinkedHashMap<>();
        midStore = new java.util.LinkedHashMap<>();
        // Ordered array of {"r": random_id, "o": original} objects, so recency-based pruning
        // survives a reload (JSON object key order would not be preserved).
        final String raw = LuminaConfig.getString(STORE_KEY, null);
        if (raw != null && raw.length() != 0) {
            try {
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
        // Ordered array of {"k": "dialogId_mid", "o": original} objects (the reload-stable map).
        final String rawMid = LuminaConfig.getString(STORE_KEY_MID, null);
        if (rawMid != null && rawMid.length() != 0) {
            try {
                final org.json.JSONArray arr = new org.json.JSONArray(rawMid);
                for (int i = 0; i < arr.length(); i++) {
                    final org.json.JSONObject o = arr.optJSONObject(i);
                    if (o == null) {
                        continue;
                    }
                    final String key = o.optString("k", null);
                    final String original = o.optString("o", null);
                    if (key != null && key.length() != 0 && original != null) {
                        midStore.put(key, original);
                    }
                }
            } catch (org.json.JSONException ignore) {
                // Corrupt/legacy payload: start from an empty store.
                midStore.clear();
            }
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

        final org.json.JSONArray arrMid = new org.json.JSONArray();
        for (java.util.Map.Entry<String, String> e : midStore.entrySet()) {
            try {
                final org.json.JSONObject o = new org.json.JSONObject();
                o.put("k", e.getKey());
                o.put("o", e.getValue());
                arrMid.put(o);
            } catch (org.json.JSONException ignore) {
                // Skip this entry on serialization failure; best-effort cache.
            }
        }
        LuminaConfig.putString(STORE_KEY_MID, arrMid.toString());
    }
}
