package org.telegram.messenger;

import java.io.File;
import java.io.FileInputStream;

/**
 * LuminaFileGuard - purely local, on-device detection of "masqueraded" documents:
 * files whose displayed name / type hides what they really are. Two classic attacks
 * are covered without any network access:
 *
 *  1. Bidirectional (RTL) filename spoofing. A right-to-left override (U+202E) or an
 *     isolate control makes {@code "evil<RLO>gpj.apk"} render as {@code "evilapk.jpg"},
 *     so the victim opens what looks like an image but is really an APK/EXE.
 *
 *  2. Extension / MIME masquerade (a.k.a. "EvilVideo"). An executable (apk/exe/scr/js/...)
 *     is presented as a photo, video or PDF - either because the declared MIME lies, or
 *     because a benign-looking extension hides a second executable extension, or because
 *     the file's own magic bytes betray an executable while the name says otherwise.
 *
 * The two primary checks (extension-vs-MIME and RTL) are pure string work and do zero IO.
 * A cheap, optional magic-byte peek (a handful of header bytes) is used only to *strengthen*
 * detection and never to weaken it.
 *
 * Everything here is fail-open by contract: any error, or an inability to decide, yields a
 * non-suspicious result so the guard can never block a user from opening a legitimate file.
 * The caller (ChatActivity) turns a suspicious result into a dismissible confirmation dialog;
 * this class renders no UI and holds no state.
 */
public final class LuminaFileGuard {

    private LuminaFileGuard() {}

    // ---- Reason codes (the UI maps these to localized strings) ----
    public static final int REASON_NONE = 0;
    public static final int REASON_RTL = 1;         // name carries bidi / RTL-override control characters
    public static final int REASON_EXECUTABLE = 2;  // real type is an app / program disguised as media / doc
    public static final int REASON_MISMATCH = 3;    // media extension conflicts with a different media MIME

    // ---- Internal type categories ----
    private static final int OTHER = 0;
    private static final int IMAGE = 1;
    private static final int VIDEO = 2;
    private static final int AUDIO = 3;
    private static final int PDF = 4;
    private static final int DOCUMENT = 5;
    private static final int ARCHIVE = 6;
    private static final int EXECUTABLE = 7;

    /**
     * Outcome of a check. Immutable. When {@link #suspicious} is false every other field is
     * meaningless. All user-facing text is built by the caller from {@link #reason} plus these
     * fields, so this class stays free of localization and Android UI concerns.
     */
    public static final class Result {
        public final boolean suspicious;
        public final int reason;
        /** Filename with bidi/control characters stripped - always safe to render. */
        public final String safeName;
        /** Short technical token describing what the file really is, e.g. ".apk", ".exe", "Android app (.dex)". */
        public final String realType;
        /** Short technical token describing what the file claims to be, e.g. ".mp4" or "video/mp4". */
        public final String claimedType;
        /** Concise English description, for logs / fallback only (never the primary user text). */
        public final String detail;

        Result(boolean suspicious, int reason, String safeName, String realType, String claimedType, String detail) {
            this.suspicious = suspicious;
            this.reason = reason;
            this.safeName = safeName;
            this.realType = realType;
            this.claimedType = claimedType;
            this.detail = detail;
        }
    }

    private static final Result SAFE = new Result(false, REASON_NONE, null, null, null, null);

    /** Zero-IO convenience overload (extension-vs-MIME + RTL only). */
    public static Result check(String fileName, String mimeType) {
        return check(fileName, mimeType, null);
    }

    /**
     * @param fileName the document's stored (logical) name as shown to the user; may be null/empty
     * @param mimeType the document's declared MIME type; may be null/empty
     * @param file     optional local file for a cheap magic-byte peek; may be null or not-yet-downloaded
     * @return a never-null result; {@link #SAFE} on anything unclear or on any internal error
     */
    public static Result check(String fileName, String mimeType, File file) {
        try {
            final String rawName = fileName == null ? "" : fileName;

            // (1) Bidi / RTL-override spoofing. Highest-signal: the very presence of these
            // controls in a filename has no legitimate purpose and is the clearest tell.
            if (containsBidiControl(rawName)) {
                String safe = stripBidiControls(rawName);
                String realExt = extensionOf(safe);
                return new Result(true, REASON_RTL, safe,
                        realExt.isEmpty() ? null : "." + realExt, null,
                        "filename contains bidi/RTL-override control characters");
            }

            final String safeName = stripBidiControls(rawName); // == rawName here, but keep it uniform
            final String ext = extensionOf(safeName);
            final int extCat = categoryForExt(ext);
            final int mimeCat = categoryForMime(mimeType);

            // (2a) Executable hidden behind a benign-looking MIME (classic "EvilVideo":
            // extension .apk while the declared MIME is video/*, image/*, audio/* or PDF).
            if (extCat == EXECUTABLE && isBenignViewable(mimeCat)) {
                return new Result(true, REASON_EXECUTABLE, safeName, "." + ext, mimeType,
                        "executable extension declared as benign media/pdf mime");
            }

            // (2b) Reverse: MIME says installer/executable while the name wears a benign extension.
            if (mimeCat == EXECUTABLE && isBenignNamed(extCat)) {
                return new Result(true, REASON_EXECUTABLE, safeName, mimeExecToken(mimeType),
                        ext.isEmpty() ? null : "." + ext,
                        "executable mime hidden behind benign extension");
            }

            // (2c) Double extension: a benign extension immediately followed by an executable one,
            // e.g. "invoice.pdf.apk" or "holiday.jpg.exe". Independent of the (possibly absent) MIME.
            if (extCat == EXECUTABLE) {
                String penultimate = penultimateExtension(safeName);
                if (isBenignExt(penultimate)) {
                    return new Result(true, REASON_EXECUTABLE, safeName, "." + ext, "." + penultimate,
                            "benign extension chained in front of an executable extension");
                }
            }

            // (2d) Optional magic-byte peek. Only escalates when the name looks like harmless media
            // but the header is an unambiguous executable (PE / ELF / DEX / shebang). Never lowers
            // suspicion; never throws; skips silently when the file is absent or unreadable.
            if (isBenignViewable(extCat)) {
                String magic = executableMagicToken(file);
                if (magic != null) {
                    return new Result(true, REASON_EXECUTABLE, safeName, magic,
                            ext.isEmpty() ? null : "." + ext,
                            "file header is an executable while the name claims media");
                }
            }

            // (3) Narrow media-vs-media mismatch: the extension names one concrete media kind while
            // the MIME names a *different* concrete media kind (e.g. ".mp4" declared image/jpeg).
            // Restricted to IMAGE/VIDEO/AUDIO/PDF, where legitimate files reliably agree, to keep
            // false positives near zero.
            if (isConcreteMedia(extCat) && isConcreteMedia(mimeCat) && extCat != mimeCat) {
                return new Result(true, REASON_MISMATCH, safeName,
                        mimeType, "." + ext,
                        "extension category conflicts with declared mime category");
            }

            return SAFE;
        } catch (Throwable ignore) {
            // Fail-open: never let a detection bug block the user from opening a file.
            return SAFE;
        }
    }

    // ---------------------------------------------------------------------
    // Bidi / RTL handling
    // ---------------------------------------------------------------------

    /** Bidi controls abused for filename spoofing: LRM/RLM, the embeddings/overrides, the
     *  isolates, and the Arabic letter mark. None have any legitimate place in a filename. */
    private static boolean isBidiControl(char c) {
        return c == '\u200E' || c == '\u200F'           // LRM, RLM
                || (c >= '\u202A' && c <= '\u202E')     // LRE, RLE, PDF, LRO, RLO
                || (c >= '\u2066' && c <= '\u2069')     // LRI, RLI, FSI, PDI
                || c == '\u061C';                         // ALM (Arabic Letter Mark)
    }

    private static boolean containsBidiControl(String s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) {
            if (isBidiControl(s.charAt(i))) return true;
        }
        return false;
    }

    private static String stripBidiControls(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!isBidiControl(c)) sb.append(c);
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // Extension helpers
    // ---------------------------------------------------------------------

    private static String extensionOf(String name) {
        if (name == null) return "";
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        String base = slash >= 0 ? name.substring(slash + 1) : name;
        int dot = base.lastIndexOf('.');
        if (dot < 0 || dot == base.length() - 1) return "";
        return base.substring(dot + 1).trim().toLowerCase();
    }

    /** The extension segment just before the final one, e.g. "pdf" in "invoice.pdf.apk". */
    private static String penultimateExtension(String name) {
        if (name == null) return "";
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        String base = slash >= 0 ? name.substring(slash + 1) : name;
        int lastDot = base.lastIndexOf('.');
        if (lastDot <= 0) return "";
        int prevDot = base.lastIndexOf('.', lastDot - 1);
        if (prevDot < 0) return "";
        return base.substring(prevDot + 1, lastDot).trim().toLowerCase();
    }

    private static boolean isBenignExt(String ext) {
        int cat = categoryForExt(ext);
        return cat == IMAGE || cat == VIDEO || cat == AUDIO || cat == PDF || cat == DOCUMENT;
    }

    private static boolean isBenignViewable(int cat) {
        return cat == IMAGE || cat == VIDEO || cat == AUDIO || cat == PDF;
    }

    private static boolean isBenignNamed(int cat) {
        return cat == IMAGE || cat == VIDEO || cat == AUDIO || cat == PDF || cat == DOCUMENT;
    }

    private static boolean isConcreteMedia(int cat) {
        return cat == IMAGE || cat == VIDEO || cat == AUDIO || cat == PDF;
    }

    private static int categoryForExt(String ext) {
        if (ext == null || ext.isEmpty()) return OTHER;
        switch (ext) {
            case "jpg": case "jpeg": case "png": case "gif": case "webp": case "bmp":
            case "heic": case "heif": case "tiff": case "tif": case "ico": case "svg":
                return IMAGE;
            case "mp4": case "mkv": case "mov": case "avi": case "webm": case "3gp":
            case "m4v": case "flv": case "wmv": case "mpeg": case "mpg": case "ts":
                return VIDEO;
            case "mp3": case "m4a": case "aac": case "wav": case "flac": case "ogg":
            case "oga": case "opus": case "amr": case "wma":
                return AUDIO;
            case "pdf":
                return PDF;
            case "doc": case "docx": case "xls": case "xlsx": case "ppt": case "pptx":
            case "txt": case "rtf": case "odt": case "ods": case "odp": case "csv": case "epub":
                return DOCUMENT;
            case "zip": case "rar": case "7z": case "tar": case "gz": case "bz2": case "xz":
                return ARCHIVE;
            case "apk": case "xapk": case "apks": case "aab":
            case "exe": case "scr": case "msi": case "bat": case "cmd": case "com":
            case "js": case "jse": case "vbs": case "vbe": case "wsf": case "wsh":
            case "ps1": case "psm1": case "jar": case "sh": case "bin": case "dex":
            case "deb": case "dmg": case "pkg": case "app": case "run": case "hta":
            case "cpl": case "dll": case "so": case "elf":
                return EXECUTABLE;
            default:
                return OTHER;
        }
    }

    // ---------------------------------------------------------------------
    // MIME helpers
    // ---------------------------------------------------------------------

    private static int categoryForMime(String mime) {
        if (mime == null) return OTHER;
        String m = mime.trim().toLowerCase();
        if (m.isEmpty()) return OTHER;
        switch (m) {
            case "application/vnd.android.package-archive":
            case "application/x-msdownload":
            case "application/x-msdos-program":
            case "application/x-ms-installer":
            case "application/x-dosexec":
            case "application/vnd.microsoft.portable-executable":
            case "application/x-executable":
            case "application/x-elf":
            case "application/x-sharedlib":
            case "application/x-mach-binary":
            case "application/java-archive":
            case "application/x-java-archive":
            case "text/javascript":
            case "application/javascript":
            case "application/x-javascript":
            case "application/x-sh":
            case "application/x-shellscript":
            case "application/x-bat":
            case "application/bat":
            case "application/x-msi":
                return EXECUTABLE;
            case "application/pdf":
                return PDF;
            case "application/msword":
            case "application/rtf":
            case "text/rtf":
            case "application/epub+zip":
                return DOCUMENT;
            case "application/zip":
            case "application/x-zip-compressed":
            case "application/x-rar-compressed":
            case "application/vnd.rar":
            case "application/x-7z-compressed":
            case "application/x-tar":
            case "application/gzip":
            case "application/x-gzip":
                return ARCHIVE;
            case "application/ogg":
                return AUDIO;
        }
        if (m.startsWith("image/")) return IMAGE;
        if (m.startsWith("video/")) return VIDEO;
        if (m.startsWith("audio/")) return AUDIO;
        if (m.startsWith("text/")) return DOCUMENT;
        if (m.startsWith("application/vnd.openxmlformats-officedocument")
                || m.startsWith("application/vnd.ms-")
                || m.startsWith("application/vnd.oasis.opendocument")) {
            return DOCUMENT;
        }
        return OTHER;
    }

    /** Short, human-recognizable token for an executable/installer MIME type. */
    private static String mimeExecToken(String mime) {
        if (mime == null) return LuminaLocale.getString(R.string.LuminaFileTypeGeneric);
        String m = mime.trim().toLowerCase();
        switch (m) {
            case "application/vnd.android.package-archive":
                return LuminaLocale.getString(R.string.LuminaFileTypeApk);
            case "application/x-msdownload":
            case "application/x-msdos-program":
            case "application/x-dosexec":
            case "application/vnd.microsoft.portable-executable":
                return LuminaLocale.getString(R.string.LuminaFileTypeExe);
            case "application/x-ms-installer":
            case "application/x-msi":
                return LuminaLocale.getString(R.string.LuminaFileTypeMsi);
            case "application/java-archive":
            case "application/x-java-archive":
                return LuminaLocale.getString(R.string.LuminaFileTypeJar);
            case "text/javascript":
            case "application/javascript":
            case "application/x-javascript":
                return LuminaLocale.getString(R.string.LuminaFileTypeJs);
            case "application/x-sh":
            case "application/x-shellscript":
                return LuminaLocale.getString(R.string.LuminaFileTypeSh);
            default:
                return LuminaLocale.getString(R.string.LuminaFileTypeGeneric);
        }
    }

    // ---------------------------------------------------------------------
    // Optional magic-byte peek (cheap, fail-open)
    // ---------------------------------------------------------------------

    /**
     * Returns a token if {@code file}'s first bytes are an unambiguous executable format
     * (Windows PE, ELF, Android DEX, or a shell shebang); otherwise null. Deliberately
     * ignores ambiguous containers such as ZIP (docx/xlsx/apk/jar/epub all share "PK").
     * Reads at most a few bytes and swallows every error.
     */
    private static String executableMagicToken(File file) {
        if (file == null) return null;
        try {
            if (!file.exists() || !file.isFile() || file.length() < 4) return null;
            byte[] head = new byte[8];
            int n;
            FileInputStream in = new FileInputStream(file);
            try {
                n = in.read(head);
            } finally {
                in.close();
            }
            if (n < 4) return null;
            int b0 = head[0] & 0xFF, b1 = head[1] & 0xFF, b2 = head[2] & 0xFF, b3 = head[3] & 0xFF;
            // ELF: 0x7F 'E' 'L' 'F'
            if (b0 == 0x7F && b1 == 0x45 && b2 == 0x4C && b3 == 0x46) return LuminaLocale.getString(R.string.LuminaFileTypeElf);
            // Android Dalvik: "dex\n"
            if (b0 == 0x64 && b1 == 0x65 && b2 == 0x78 && b3 == 0x0A) return LuminaLocale.getString(R.string.LuminaFileTypeDex);
            // Windows PE: "MZ"
            if (b0 == 0x4D && b1 == 0x5A) return LuminaLocale.getString(R.string.LuminaFileTypeExe);
            // Shell/script shebang: "#!"
            if (b0 == 0x23 && b1 == 0x21) return LuminaLocale.getString(R.string.LuminaFileTypeScript);
            return null;
        } catch (Throwable ignore) {
            return null;
        }
    }
}
