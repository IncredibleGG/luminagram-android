package org.telegram.messenger;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LuminaGram — translation glossary / do-not-translate list.
 *
 * Some spans of a message must survive translation byte-for-byte: brand names,
 * product models, people's names, {@code @usernames} and links. A translation
 * engine happily mangles those ("iPhone" -> "i电话", "@durov" reflowed, a URL
 * word-split). This helper masks every such span with a placeholder before the
 * text reaches the engine and swaps the originals back into the returned
 * translation.
 *
 * Protected in every message, always:
 *   - {@code http(s)://} links,
 *   - {@code @username} mentions,
 * plus every user-defined term from the global list persisted by
 * {@link LuminaConfig#KEY_GLOSSARY_TERMS}.
 *
 * Placeholder scheme: each protected span is replaced by ONE code point from the
 * Unicode Private Use Area ({@code U+E000 + index}). PUA characters carry no
 * linguistic meaning, so engines pass them through untouched; using a single
 * character per span (rather than a {@code {12}} marker) makes {@link #restore}
 * unambiguous — there are no digit-boundary collisions and no re-encoding to undo.
 *
 * Everything here is pure local string processing; nothing leaves the device.
 */
public final class LuminaGlossary {

    private LuminaGlossary() {}

    // Base of the Private Use Area block used for placeholders. Span i -> (TOKEN_BASE + i).
    private static final char TOKEN_BASE = '\uE000';
    // The BMP PUA runs U+E000..U+F8FF, so at most this many spans can be masked in one
    // message. Far beyond any real message; past it we simply stop masking (never crash).
    private static final int TOKEN_MAX = 0xF8FF - 0xE000;

    // Compiled once. URLs first (most specific). ASCII-only character class so the match
    // stops at CJK/space instead of a greedy \S+ swallowing a whole space-less CJK tail.
    private static final Pattern URL = Pattern.compile(
            "https?://[A-Za-z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+",
            Pattern.CASE_INSENSITIVE);
    // @username: not preceded by a word char or another '@', so it never fires inside an
    // e-mail local part (user@host) or a doubled "@@". Telegram handles are [A-Za-z0-9_].
    private static final Pattern MENTION = Pattern.compile("(?<![A-Za-z0-9_@])@[A-Za-z0-9_]+");

    // Recompile the user-term pattern only when the stored list actually changes.
    private static final Object cacheLock = new Object();
    private static String cachedRaw;
    private static Pattern cachedTerms;

    /** Masked text plus the ordered original spans needed by {@link #restore}. */
    public static final class Protected {
        public final String masked;
        final ArrayList<String> originals;
        Protected(String masked, ArrayList<String> originals) {
            this.masked = masked;
            this.originals = originals;
        }
        /** True when nothing was masked — the caller can send the original text unchanged. */
        public boolean isEmpty() {
            return originals.isEmpty();
        }
    }

    /**
     * Replace every protected span (URLs, {@code @usernames}, glossary terms) in
     * {@code text} with a private-use placeholder. Returns the masked text and the
     * restore table. When nothing matches, {@code masked} is the original string and
     * {@link Protected#isEmpty()} is true (a byte-for-byte no-op path for the caller).
     */
    public static Protected protect(String text) {
        final ArrayList<String> originals = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return new Protected(text, originals);
        }
        String s = text;
        s = mask(s, URL, originals);
        s = mask(s, MENTION, originals);
        final Pattern terms = termsPattern();
        if (terms != null) {
            s = mask(s, terms, originals);
        }
        return new Protected(s, originals);
    }

    /**
     * Put the original spans back into a translated string. Single forward pass over
     * {@code translated}: any character in the assigned PUA range is swapped for its
     * original span, everything else is copied verbatim. Because inserted originals are
     * never re-scanned, this stays correct even if an original itself contained a PUA
     * character. Symmetric with {@link #protect}: {@code restore(protect(t).masked, p)}
     * reproduces {@code t} when the engine preserves the placeholders.
     */
    public static String restore(String translated, Protected p) {
        if (translated == null || p == null || p.originals.isEmpty()) {
            return translated;
        }
        final int n = p.originals.size();
        final StringBuilder out = new StringBuilder(translated.length() + 16);
        for (int i = 0; i < translated.length(); i++) {
            final char c = translated.charAt(i);
            final int idx = c - TOKEN_BASE;
            if (idx >= 0 && idx < n) {
                out.append(p.originals.get(idx));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Wrap a translation provider so it transparently protects/restores the glossary.
     * This is the single shared hook: {@link LuminaTranslators#byId(String)} returns a
     * wrapped provider, so every translation path in the fork (before-send, live
     * preview, incoming messages, selected text, article viewer, voice-to-text) inherits
     * the behavior without any UI-side change. All non-translate methods are forwarded
     * verbatim so callers still see the real provider's id / capabilities.
     */
    public static LuminaTranslator wrap(LuminaTranslator delegate) {
        if (delegate == null) {
            return null;
        }
        return new GlossaryTranslator(delegate);
    }

    private static final class GlossaryTranslator implements LuminaTranslator {
        private final LuminaTranslator delegate;

        GlossaryTranslator(LuminaTranslator delegate) {
            this.delegate = delegate;
        }

        @Override public String id() { return delegate.id(); }
        @Override public CharSequence displayName() { return delegate.displayName(); }
        @Override public boolean needsKey() { return delegate.needsKey(); }
        @Override public boolean needsBaseUrl() { return delegate.needsBaseUrl(); }
        @Override public boolean needsModel() { return delegate.needsModel(); }

        @Override
        public void translate(String text, String toLang, final Callback cb) {
            final Protected p = protect(text);
            if (p.isEmpty()) {
                // Nothing to protect: exact passthrough, identical to the un-wrapped provider.
                delegate.translate(text, toLang, cb);
                return;
            }
            delegate.translate(p.masked, toLang, new Callback() {
                @Override
                public void onResult(String translated, String detectedSourceLang) {
                    cb.onResult(restore(translated, p), detectedSourceLang);
                }
                @Override
                public void onError(boolean rateLimited, String message) {
                    cb.onError(rateLimited, message);
                }
            });
        }
    }

    // ---- internals ----

    // Replace every match of p in s with a fresh placeholder, recording the original.
    private static String mask(String s, Pattern p, ArrayList<String> originals) {
        if (originals.size() >= TOKEN_MAX) {
            return s;
        }
        final Matcher m = p.matcher(s);
        StringBuffer out = null;
        while (m.find()) {
            if (originals.size() >= TOKEN_MAX) {
                break;
            }
            if (out == null) {
                out = new StringBuffer(s.length());
            }
            final char token = (char) (TOKEN_BASE + originals.size());
            originals.add(m.group());
            m.appendReplacement(out, Matcher.quoteReplacement(String.valueOf(token)));
        }
        if (out == null) {
            return s;  // no match — leave the string (and object) untouched
        }
        m.appendTail(out);
        return out.toString();
    }

    // Combined, cached alternation of all user terms, or null when the list is empty.
    private static Pattern termsPattern() {
        final String raw = LuminaConfig.getString(LuminaConfig.KEY_GLOSSARY_TERMS, "");
        synchronized (cacheLock) {
            if (raw == null || raw.isEmpty()) {
                cachedRaw = raw;
                cachedTerms = null;
                return null;
            }
            if (raw.equals(cachedRaw)) {
                return cachedTerms;
            }
            cachedRaw = raw;
            cachedTerms = build(raw);
            return cachedTerms;
        }
    }

    private static Pattern build(String raw) {
        final ArrayList<String> terms = new ArrayList<>();
        try {
            final JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                String t = arr.optString(i, "");
                if (t != null) {
                    t = t.trim();
                    if (t.length() > 0) {
                        terms.add(t);
                    }
                }
            }
        } catch (Exception ignore) {
            return null;
        }
        if (terms.isEmpty()) {
            return null;
        }
        // Longest first: in a regex alternation the first matching branch wins, so a longer
        // multi-word term ("Face ID") must precede a shorter one it contains ("Face").
        Collections.sort(terms, new Comparator<String>() {
            @Override public int compare(String a, String b) {
                return b.length() - a.length();
            }
        });
        // Boundary: the match may not be flanked by an ASCII word char. This gives strict
        // whole-word matching for Latin ("AI" ignored inside "SPAIN"/"AInews") while still
        // allowing substring matching for CJK terms, whose neighbours are never ASCII word
        // chars — the only sensible behavior for space-less scripts.
        final StringBuilder sb = new StringBuilder();
        sb.append("(?<![A-Za-z0-9_])(?:");
        for (int i = 0; i < terms.size(); i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(Pattern.quote(terms.get(i)));
        }
        sb.append(")(?![A-Za-z0-9_])");
        try {
            return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        } catch (Exception e) {
            return null;
        }
    }
}
