package org.telegram.messenger;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.util.HashMap;
import java.util.Map;

/**
 * LuminaGram homoglyph / look-alike impersonation detector.
 *
 * A purely LOCAL, on-device heuristic that flags display names and usernames built
 * from characters that merely *look* like Latin letters but are not - the classic
 * trick used to pass a fake account off as a trusted brand or person ("&#1072;pple",
 * "p&#1072;&#1091;pal", "micr0soft", fullwidth "google").
 *
 * Nothing here touches the network: it is pure character comparison against a
 * built-in confusable table plus {@link Character.UnicodeBlock} script inspection.
 * No external library is used.
 *
 * Signals (any one is enough to mark a name as suspicious):
 *   1. Mixed script inside a single word - Latin letters sitting next to Cyrillic
 *      or Greek letters (e.g. a Cyrillic look-alike inside an otherwise-Latin word).
 *   2. A known confusable letter (Cyrillic / Greek look-alike of a Latin letter)
 *      appearing in a name that also contains ordinary ASCII Latin letters.
 *   3. Fullwidth digits / fullwidth Latin letters (U+FF10.., U+FF21..).
 *   4. The digits 0 / 1 used *as letters* - sandwiched between letters inside a
 *      word (e.g. "micr0soft", "g00gle", "l0l").
 */
public final class LuminaHomoglyph {

    private LuminaHomoglyph() {}

    // Highlight colour for the offending characters: a clearly-visible red that
    // reads on both the light and dark profile background.
    private static final int HIGHLIGHT_COLOR = 0xFFE53935;

    // Script tags used by scriptOf().
    private static final int OTHER = 0;
    private static final int LATIN = 1;
    private static final int CYRILLIC = 2;
    private static final int GREEK = 3;

    /**
     * Confusable table: a look-alike letter (Cyrillic / Greek) mapped to the plain
     * ASCII Latin letter it imitates. Used for membership tests and to keep each
     * entry self-documenting. A few dozen of the most-abused pairs.
     */
    private static final Map<Character, Character> CONFUSABLES = new HashMap<>();
    static {
        // ---- Cyrillic (lowercase) -> Latin ----
        CONFUSABLES.put('а', 'a'); // CYRILLIC SMALL A
        CONFUSABLES.put('е', 'e'); // CYRILLIC SMALL IE
        CONFUSABLES.put('о', 'o'); // CYRILLIC SMALL O
        CONFUSABLES.put('р', 'p'); // CYRILLIC SMALL ER
        CONFUSABLES.put('с', 'c'); // CYRILLIC SMALL ES
        CONFUSABLES.put('х', 'x'); // CYRILLIC SMALL HA
        CONFUSABLES.put('у', 'y'); // CYRILLIC SMALL U
        CONFUSABLES.put('і', 'i'); // CYRILLIC SMALL BYELORUSSIAN-UKRAINIAN I
        CONFUSABLES.put('ј', 'j'); // CYRILLIC SMALL JE
        CONFUSABLES.put('ѕ', 's'); // CYRILLIC SMALL DZE
        CONFUSABLES.put('ԛ', 'q'); // CYRILLIC SMALL QA
        CONFUSABLES.put('ԝ', 'w'); // CYRILLIC SMALL WE
        CONFUSABLES.put('к', 'k'); // CYRILLIC SMALL KA
        CONFUSABLES.put('һ', 'h'); // CYRILLIC SMALL SHHA
        CONFUSABLES.put('ԁ', 'd'); // CYRILLIC SMALL KOMI DE
        // ---- Cyrillic (uppercase) -> Latin ----
        CONFUSABLES.put('А', 'A'); // CYRILLIC CAPITAL A
        CONFUSABLES.put('В', 'B'); // CYRILLIC CAPITAL VE
        CONFUSABLES.put('Е', 'E'); // CYRILLIC CAPITAL IE
        CONFUSABLES.put('К', 'K'); // CYRILLIC CAPITAL KA
        CONFUSABLES.put('М', 'M'); // CYRILLIC CAPITAL EM
        CONFUSABLES.put('Н', 'H'); // CYRILLIC CAPITAL EN
        CONFUSABLES.put('О', 'O'); // CYRILLIC CAPITAL O
        CONFUSABLES.put('Р', 'P'); // CYRILLIC CAPITAL ER
        CONFUSABLES.put('С', 'C'); // CYRILLIC CAPITAL ES
        CONFUSABLES.put('Т', 'T'); // CYRILLIC CAPITAL TE
        CONFUSABLES.put('У', 'Y'); // CYRILLIC CAPITAL U
        CONFUSABLES.put('Х', 'X'); // CYRILLIC CAPITAL HA
        CONFUSABLES.put('І', 'I'); // CYRILLIC CAPITAL BYELORUSSIAN-UKRAINIAN I
        CONFUSABLES.put('Ј', 'J'); // CYRILLIC CAPITAL JE
        CONFUSABLES.put('Ѕ', 'S'); // CYRILLIC CAPITAL DZE
        // ---- Greek (lowercase) -> Latin ----
        CONFUSABLES.put('ο', 'o'); // GREEK SMALL OMICRON
        CONFUSABLES.put('α', 'a'); // GREEK SMALL ALPHA
        CONFUSABLES.put('ρ', 'p'); // GREEK SMALL RHO
        CONFUSABLES.put('ν', 'v'); // GREEK SMALL NU
        CONFUSABLES.put('υ', 'u'); // GREEK SMALL UPSILON
        CONFUSABLES.put('τ', 't'); // GREEK SMALL TAU
        CONFUSABLES.put('κ', 'k'); // GREEK SMALL KAPPA
        CONFUSABLES.put('ε', 'e'); // GREEK SMALL EPSILON
        CONFUSABLES.put('ι', 'i'); // GREEK SMALL IOTA
        CONFUSABLES.put('χ', 'x'); // GREEK SMALL CHI
        CONFUSABLES.put('γ', 'y'); // GREEK SMALL GAMMA
        // ---- Greek (uppercase) -> Latin ----
        CONFUSABLES.put('Α', 'A'); // GREEK CAPITAL ALPHA
        CONFUSABLES.put('Β', 'B'); // GREEK CAPITAL BETA
        CONFUSABLES.put('Ε', 'E'); // GREEK CAPITAL EPSILON
        CONFUSABLES.put('Ζ', 'Z'); // GREEK CAPITAL ZETA
        CONFUSABLES.put('Η', 'H'); // GREEK CAPITAL ETA
        CONFUSABLES.put('Ι', 'I'); // GREEK CAPITAL IOTA
        CONFUSABLES.put('Κ', 'K'); // GREEK CAPITAL KAPPA
        CONFUSABLES.put('Μ', 'M'); // GREEK CAPITAL MU
        CONFUSABLES.put('Ν', 'N'); // GREEK CAPITAL NU
        CONFUSABLES.put('Ο', 'O'); // GREEK CAPITAL OMICRON
        CONFUSABLES.put('Ρ', 'P'); // GREEK CAPITAL RHO
        CONFUSABLES.put('Τ', 'T'); // GREEK CAPITAL TAU
        CONFUSABLES.put('Υ', 'Y'); // GREEK CAPITAL UPSILON
        CONFUSABLES.put('Χ', 'X'); // GREEK CAPITAL CHI
    }

    /**
     * True when {@code name} contains at least one visually-deceptive character.
     * A plain {@link String} is accepted (String implements CharSequence).
     */
    public static boolean containsSuspiciousChars(CharSequence name) {
        boolean[] mask = suspiciousMask(name);
        if (mask == null) {
            return false;
        }
        for (boolean b : mask) {
            if (b) {
                return true;
            }
        }
        return false;
    }

    /**
     * A copy of {@code name} with every suspicious character painted red via a
     * {@link ForegroundColorSpan}. Never null; an unmodified copy when nothing is
     * suspicious (or when {@code name} is null / empty).
     */
    public static SpannableStringBuilder highlight(CharSequence name) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(name == null ? "" : name);
        boolean[] mask = suspiciousMask(name);
        if (mask == null) {
            return ssb;
        }
        int i = 0;
        while (i < mask.length) {
            if (mask[i]) {
                int j = i + 1;
                while (j < mask.length && mask[j]) {
                    j++;
                }
                ssb.setSpan(new ForegroundColorSpan(HIGHLIGHT_COLOR), i, j, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                i = j;
            } else {
                i++;
            }
        }
        return ssb;
    }

    /**
     * Per-character flag array: {@code mask[k]} is true when character k is a
     * look-alike / mixed-script offender. Shared by {@link #containsSuspiciousChars}
     * and {@link #highlight} so the two never disagree. Returns null for null / empty.
     */
    private static boolean[] suspiciousMask(CharSequence s) {
        if (s == null) {
            return null;
        }
        int n = s.length();
        if (n == 0) {
            return null;
        }
        boolean[] mask = new boolean[n];

        // Does the whole string contain any ordinary ASCII Latin letter? Used to gate
        // the "lone confusable" rule so we never flag a legitimately all-Cyrillic or
        // all-Greek name (only mixed / disguised-into-Latin names are of interest).
        boolean hasAsciiLatin = false;
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                hasAsciiLatin = true;
                break;
            }
        }

        // Walk word tokens (maximal runs of letters / digits / combining marks).
        int i = 0;
        while (i < n) {
            if (!isWordChar(s.charAt(i))) {
                i++;
                continue;
            }
            int start = i;
            boolean sawLatin = false, sawCyrillic = false, sawGreek = false;
            while (i < n && isWordChar(s.charAt(i))) {
                int script = scriptOf(s.charAt(i));
                if (script == LATIN) {
                    sawLatin = true;
                } else if (script == CYRILLIC) {
                    sawCyrillic = true;
                } else if (script == GREEK) {
                    sawGreek = true;
                }
                i++;
            }
            int end = i; // exclusive
            int distinctScripts = (sawLatin ? 1 : 0) + (sawCyrillic ? 1 : 0) + (sawGreek ? 1 : 0);
            boolean mixedScript = distinctScripts >= 2;

            for (int k = start; k < end; k++) {
                char w = s.charAt(k);
                // (1) Fullwidth digit / fullwidth Latin letter - always deceptive.
                if (isFullwidthLookalike(w)) {
                    mask[k] = true;
                    continue;
                }
                // (2) Mixed-script word: the Cyrillic / Greek letters are the disguise.
                int script = scriptOf(w);
                if (mixedScript && (script == CYRILLIC || script == GREEK)) {
                    mask[k] = true;
                    continue;
                }
                // (3) A known confusable letter in a name that also has ASCII Latin.
                if (hasAsciiLatin && CONFUSABLES.containsKey(w)) {
                    mask[k] = true;
                    continue;
                }
                // (4) 0 / 1 used as a letter: a real letter both before and after it
                //     inside the same word (catches micr0soft, g00gle, l0l; leaves
                //     trailing/leading digits such as "user1" alone).
                if (isConfusableDigit(w)
                        && hasLetterInRange(s, start, k)
                        && hasLetterInRange(s, k + 1, end)) {
                    mask[k] = true;
                }
            }
        }
        return mask;
    }

    /** Part of a word token: letters, digits, or combining marks (not spaces/punct). */
    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || Character.getType(c) == Character.NON_SPACING_MARK;
    }

    /** True if any character in [from, to) is a letter. */
    private static boolean hasLetterInRange(CharSequence s, int from, int to) {
        for (int i = from; i < to; i++) {
            if (Character.isLetter(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isConfusableDigit(char c) {
        return c == '0' || c == '1';
    }

    private static boolean isFullwidthLookalike(char c) {
        return (c >= '０' && c <= '９')  // fullwidth 0-9
            || (c >= 'Ａ' && c <= 'Ｚ')  // fullwidth A-Z
            || (c >= 'ａ' && c <= 'ｚ'); // fullwidth a-z
    }

    /** LATIN / CYRILLIC / GREEK / OTHER for a single char. */
    private static int scriptOf(char c) {
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            return LATIN;
        }
        Character.UnicodeBlock b = Character.UnicodeBlock.of(c);
        if (b == null) {
            return OTHER;
        }
        if (b == Character.UnicodeBlock.CYRILLIC
                || b == Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY) {
            return CYRILLIC;
        }
        if (b == Character.UnicodeBlock.GREEK
                || b == Character.UnicodeBlock.GREEK_EXTENDED) {
            return GREEK;
        }
        if (b == Character.UnicodeBlock.LATIN_1_SUPPLEMENT
                || b == Character.UnicodeBlock.LATIN_EXTENDED_A
                || b == Character.UnicodeBlock.LATIN_EXTENDED_B
                || b == Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL) {
            return LATIN;
        }
        // Fullwidth Latin letters are treated as Latin (each is separately flagged
        // by the fullwidth rule, but this keeps mixed-script accounting sane).
        if ((c >= 'Ａ' && c <= 'Ｚ') || (c >= 'ａ' && c <= 'ｚ')) {
            return LATIN;
        }
        return OTHER;
    }
}
