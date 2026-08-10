package org.telegram.messenger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads, unpacks, inventories and deletes the per-language Vosk acoustic models used by
 * {@link VoskTranscriber}. Models live under the app-private files dir at
 * {@code vosk-models/<voskLang>/} and are fetched once from alphacephei.com "small" model zips.
 *
 * <p><b>The catalogue is deliberately hard-coded.</b> Vosk only publishes acoustic models for a
 * few dozen languages; offering anything else (as the old picker did, by reusing the ~100-entry
 * translation language list) produced downloads that could only 404 or, worse, silently fetch the
 * English model under another language's name. {@link #MODELS} is transcribed from Vosk's own
 * machine-readable catalogue, <a href="https://alphacephei.com/vosk/models/model-list.json">
 * model-list.json</a> (every entry with {@code type == "small"} and {@code obsolete == false},
 * as of 2026-08), which is also what <a href="https://alphacephei.com/vosk/models">the models
 * page</a> renders. Only the "small" models are listed: the big ones are 1-4 GB and are meant for
 * servers, not phones.
 *
 * <p>The key of a catalogue entry ({@link VoskModel#lang}) is Vosk's own language id, which is not
 * always an ISO-639 code — {@code cn} for Chinese, {@code ua} for Ukrainian, {@code kz} for Kazakh,
 * {@code vn} for Vietnamese. It doubles as the on-disk directory name and as the value stored in
 * the {@code voskModelLang} preference. {@link #matchLang(String)} maps arbitrary ISO tags onto it.
 *
 * <p>All network / disk work runs on {@link Utilities#globalQueue}; callbacks fire on that
 * background thread (callers that touch UI must marshal back onto the main thread).
 */
public final class LuminaVoskModelManager {

    private LuminaVoskModelManager() {}

    public interface ModelCallback {
        void onReady(File dir);
        void onProgress(float f);
        void onError(String m);
    }

    /** Preference holding the user's chosen offline model language (a {@link VoskModel#lang}). */
    public static final String CONFIG_KEY_LANG = "voskModelLang";

    /** Used when nothing is chosen and the spoken language is not one Vosk covers. */
    public static final String DEFAULT_LANG = "en-us";

    private static final String BASE_URL = "https://alphacephei.com/vosk/models/";

    private static final int CONNECT_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 30000;

    /** One downloadable Vosk model. Immutable; the whole catalogue is static data. */
    public static final class VoskModel {
        /** Vosk's language id — on-disk dir name and stored preference value ("en-us", "cn", …). */
        public final String lang;
        /** ISO-639-1 code of the language, for looking up a localized display name. */
        public final String iso;
        /** ISO-3166 region for regional variants, or null. Used only to qualify the name. */
        public final String region;
        /** English name, used when the platform has no localized name for {@link #iso}. */
        public final String englishName;
        /** Model archive name, e.g. "vosk-model-small-en-us-0.15" (without the ".zip"). */
        public final String modelName;
        /** Size of the zip in bytes, as published by Vosk. */
        public final long downloadBytes;

        VoskModel(String lang, String iso, String region, String englishName, String modelName, long downloadBytes) {
            this.lang = lang;
            this.iso = iso;
            this.region = region;
            this.englishName = englishName;
            this.modelName = modelName;
            this.downloadBytes = downloadBytes;
        }

        public String url() {
            return BASE_URL + modelName + ".zip";
        }
    }

    /**
     * Vosk's "small" (mobile) models, one per language, taken verbatim from model-list.json.
     * Ordered by English name; the UI re-sorts by the localized name.
     */
    private static final VoskModel[] MODELS = new VoskModel[]{
        new VoskModel("ar", "ar", null, "Arabic", "vosk-model-small-ar-0.3", 104351896L),
        new VoskModel("ar-tn", "ar", "TN", "Arabic (Tunisian)", "vosk-model-small-ar-tn-0.1-linto", 165703754L),
        new VoskModel("ca", "ca", null, "Catalan", "vosk-model-small-ca-0.4", 43405881L),
        new VoskModel("cn", "zh", null, "Chinese", "vosk-model-small-cn-0.22", 43898754L),
        new VoskModel("cs", "cs", null, "Czech", "vosk-model-small-cs-0.4-rhasspy", 46088666L),
        new VoskModel("nl", "nl", null, "Dutch", "vosk-model-small-nl-0.22", 40441176L),
        new VoskModel("en-in", "en", "IN", "English (India)", "vosk-model-small-en-in-0.4", 37573330L),
        new VoskModel("en-gb", "en", "GB", "English (UK)", "vosk-model-small-en-gb-0.15", 42757500L),
        new VoskModel("en-us", "en", "US", "English (US)", "vosk-model-small-en-us-0.15", 41205931L),
        new VoskModel("eo", "eo", null, "Esperanto", "vosk-model-small-eo-0.42", 43839401L),
        new VoskModel("fr", "fr", null, "French", "vosk-model-small-fr-0.22", 42233323L),
        new VoskModel("ka", "ka", null, "Georgian", "vosk-model-small-ka-0.42", 45682310L),
        new VoskModel("de", "de", null, "German", "vosk-model-small-de-0.15", 46499967L),
        new VoskModel("gu", "gu", null, "Gujarati", "vosk-model-small-gu-0.42", 108054987L),
        new VoskModel("hi", "hi", null, "Hindi", "vosk-model-small-hi-0.22", 44458845L),
        new VoskModel("it", "it", null, "Italian", "vosk-model-small-it-0.22", 49665141L),
        new VoskModel("ja", "ja", null, "Japanese", "vosk-model-small-ja-0.22", 49704573L),
        new VoskModel("kz", "kk", null, "Kazakh", "vosk-model-small-kz-0.42", 59697294L),
        new VoskModel("ko", "ko", null, "Korean", "vosk-model-small-ko-0.22", 86914329L),
        new VoskModel("ky", "ky", null, "Kyrgyz", "vosk-model-small-ky-0.42", 51041096L),
        new VoskModel("fa", "fa", null, "Persian", "vosk-model-small-fa-0.42", 53431220L),
        new VoskModel("pl", "pl", null, "Polish", "vosk-model-small-pl-0.22", 52979372L),
        new VoskModel("pt", "pt", null, "Portuguese", "vosk-model-small-pt-0.3", 32453112L),
        new VoskModel("ru", "ru", null, "Russian", "vosk-model-small-ru-0.22", 46236750L),
        new VoskModel("es", "es", null, "Spanish", "vosk-model-small-es-0.42", 39817833L),
        new VoskModel("sv", "sv", null, "Swedish", "vosk-model-small-sv-rhasspy-0.15", 303504931L),
        new VoskModel("tg", "tg", null, "Tajik", "vosk-model-small-tg-0.22", 51879043L),
        new VoskModel("te", "te", null, "Telugu", "vosk-model-small-te-0.42", 60544249L),
        new VoskModel("tr", "tr", null, "Turkish", "vosk-model-small-tr-0.3", 36855784L),
        new VoskModel("ua", "uk", null, "Ukrainian", "vosk-model-small-uk-v3-small", 143914407L),
        new VoskModel("uz", "uz", null, "Uzbek", "vosk-model-small-uz-0.22", 51061189L),
        new VoskModel("vn", "vi", null, "Vietnamese", "vosk-model-small-vn-0.4", 33656337L),
    };

    private static final Map<String, VoskModel> BY_LANG = new LinkedHashMap<>();

    /** ISO tags that do not spell themselves the way Vosk spells them. */
    private static final Map<String, String> ALIASES = new LinkedHashMap<>();

    static {
        for (VoskModel m : MODELS) {
            BY_LANG.put(m.lang, m);
        }
        ALIASES.put("en", "en-us");   // plain "en" -> US English
        ALIASES.put("zh", "cn");
        ALIASES.put("cmn", "cn");
        ALIASES.put("uk", "ua");
        ALIASES.put("kk", "kz");
        ALIASES.put("vi", "vn");
    }

    /** The full catalogue, in declaration order. Never null, never empty. */
    public static VoskModel[] models() {
        return MODELS.clone();
    }

    /** Catalogue entry for a Vosk language id, or null when it is not one of ours. */
    public static VoskModel modelFor(String lang) {
        if (lang == null) {
            return null;
        }
        return BY_LANG.get(lang);
    }

    /**
     * Map an arbitrary language tag ("en", "en-US", "zh_CN", "uk") onto the Vosk language id of a
     * model we can actually download, or null when Vosk publishes no model for it. This is the
     * strict form — use it whenever "we have nothing for this language" must stay visible.
     */
    public static String matchLang(String lang) {
        if (lang == null) {
            return null;
        }
        String s = lang.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (s.length() == 0) {
            return null;
        }
        if (BY_LANG.containsKey(s)) {
            return s;
        }
        String alias = ALIASES.get(s);
        if (alias != null) {
            return alias;
        }
        int dash = s.indexOf('-');
        if (dash > 0) {
            String base = s.substring(0, dash);
            if (BY_LANG.containsKey(base)) {
                return base;
            }
            alias = ALIASES.get(base);
            if (alias != null) {
                return alias;
            }
        }
        return null;
    }

    /**
     * The ISO-639-1 / BCP-47 spelling of a Vosk language id, for the cloud engines and anything
     * else that expects a standard tag rather than Vosk's own ids ("cn" -> "zh", "ua" -> "uk",
     * "en-us" -> "en-US"). Unknown input is passed through untouched.
     */
    public static String isoTag(String lang) {
        VoskModel model = modelFor(matchLang(lang));
        if (model == null) {
            return lang;
        }
        return model.region != null ? (model.iso + "-" + model.region) : model.iso;
    }

    /** The user's chosen offline model language, or null when none is chosen (or it is stale). */
    public static String selectedLang() {
        return matchLang(LuminaConfig.getString(CONFIG_KEY_LANG, ""));
    }

    /**
     * Lenient form of {@link #matchLang(String)} for the transcription path, which must end up
     * with <i>some</i> model: an unsupported spoken language falls back to whatever the user
     * downloaded, and only then to {@link #DEFAULT_LANG}. Never returns null.
     */
    public static String normalizeLang(String lang) {
        String matched = matchLang(lang);
        if (matched != null) {
            return matched;
        }
        matched = selectedLang();
        if (matched != null) {
            return matched;
        }
        return DEFAULT_LANG;
    }

    /** {@code <filesDir>/vosk-models} — the parent of every unpacked model. */
    private static File modelsRoot() {
        File root = new File(ApplicationLoader.getFilesDirFixed(), "vosk-models");
        if (!legacyMigrated) {
            legacyMigrated = true;
            try {
                migrateLegacyDirs(root);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        return root;
    }

    private static volatile boolean legacyMigrated;

    /**
     * Before the catalogue existed, dirs were keyed by base ISO code and only seven languages were
     * reachable. Six of those keys ("ru", "es", "fr", "de", "pt") already match Vosk's ids; rename
     * the two that do not so an existing download is not orphaned (and invisible to the new
     * management screen) after the update.
     */
    private static void migrateLegacyDirs(File root) {
        renameLegacy(new File(root, "en"), new File(root, "en-us"));
        renameLegacy(new File(root, "zh"), new File(root, "cn"));
    }

    private static void renameLegacy(File from, File to) {
        if (!from.isDirectory() || to.exists()) {
            return;
        }
        File[] files = from.listFiles();
        if (files == null || files.length == 0) {
            deleteRecursive(from);
            return;
        }
        if (!from.renameTo(to)) {
            FileLog.e(new Exception("vosk: could not migrate " + from + " -> " + to));
        }
    }

    /** {@code <filesDir>/vosk-models/<voskLang>} — where the unpacked model files live. */
    public static File modelDir(String lang) {
        return new File(modelsRoot(), normalizeLang(lang));
    }

    /** True when the model dir exists and is non-empty (a usable model is present). */
    public static boolean isModelReady(String lang) {
        File dir = modelDir(lang);
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }
        File[] files = dir.listFiles();
        return files != null && files.length > 0;
    }

    /** Bytes the unpacked model for {@code lang} occupies on disk, or 0 when it is not installed. */
    public static long installedBytes(String lang) {
        return dirSize(modelDir(lang));
    }

    /** Bytes every installed model occupies on disk, including anything we no longer list. */
    public static long totalInstalledBytes() {
        return dirSize(modelsRoot());
    }

    /** Remove an installed model. Returns true when nothing is left on disk afterwards. */
    public static boolean deleteModel(String lang) {
        File dir = modelDir(lang);
        deleteRecursive(dir);
        return !dir.exists();
    }

    /**
     * Ensure the model for {@code lang} is present, downloading + unzipping it if not.
     * Fires exactly one of {@code onReady}/{@code onError} (with any number of {@code onProgress}).
     */
    public static void ensureModel(final String lang, final ModelCallback cb) {
        final String norm = normalizeLang(lang);
        if (isModelReady(norm)) {
            cb.onReady(modelDir(norm));
            return;
        }
        Utilities.globalQueue.postRunnable(() -> downloadAndUnzip(norm, cb));
    }

    private static void downloadAndUnzip(String lang, ModelCallback cb) {
        VoskModel model = BY_LANG.get(lang);
        if (model == null) {
            model = BY_LANG.get(DEFAULT_LANG);
        }
        if (model == null) {
            cb.onError("no Vosk model for '" + lang + "'");
            return;
        }
        String url = model.url();
        File modelDir = modelDir(lang);
        File parent = modelDir.getParentFile();
        File tmpZip = new File(parent, lang + ".zip.tmp");
        File tmpDir = new File(parent, lang + ".unpack.tmp");
        HttpURLConnection connection = null;
        try {
            if (parent != null) {
                parent.mkdirs();
            }
            deleteRecursive(tmpDir);
            if (tmpZip.exists()) {
                tmpZip.delete();
            }

            connection = (HttpURLConnection) new URI(url).toURL().openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(true);
            connection.connect();
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                cb.onError("HTTP " + code);
                return;
            }

            long contentLength = connection.getContentLength();
            if (contentLength <= 0) {
                // Chunked / compressed responses hide the length; fall back on the catalogue size.
                contentLength = model.downloadBytes;
            }
            InputStream in = connection.getInputStream();
            FileOutputStream fos = new FileOutputStream(tmpZip);
            try {
                byte[] buf = new byte[64 * 1024];
                long readTotal = 0;
                int r;
                while ((r = in.read(buf)) != -1) {
                    fos.write(buf, 0, r);
                    readTotal += r;
                    if (contentLength > 0) {
                        // Reserve the top 10% of the bar for unzip.
                        float frac = (float) ((double) readTotal / contentLength) * 0.9f;
                        cb.onProgress(Math.min(0.9f, frac));
                    }
                }
                fos.flush();
            } finally {
                try {
                    fos.close();
                } catch (Exception ignore) {
                }
                try {
                    in.close();
                } catch (Exception ignore) {
                }
            }

            unzip(tmpZip, tmpDir);
            tmpZip.delete();
            cb.onProgress(0.95f);

            // A Vosk zip nests everything under one folder (e.g. vosk-model-small-en-us-0.15/);
            // flatten so modelDir directly holds am/, conf/, graph/, ...
            File contentRoot = flattenRoot(tmpDir);

            deleteRecursive(modelDir);
            if (!contentRoot.renameTo(modelDir)) {
                copyRecursive(contentRoot, modelDir);
            }
            deleteRecursive(tmpDir);

            if (isModelReady(lang)) {
                cb.onProgress(1f);
                cb.onReady(modelDir);
            } else {
                cb.onError("unzip produced no model files");
            }
        } catch (Throwable e) {
            FileLog.e(e);
            deleteRecursive(tmpDir);
            if (tmpZip.exists()) {
                tmpZip.delete();
            }
            cb.onError(e.getMessage() != null ? e.getMessage() : e.toString());
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /** If the unpacked tree is a single wrapper folder, descend into it. */
    private static File flattenRoot(File dir) {
        File[] entries = dir.listFiles();
        if (entries != null && entries.length == 1 && entries[0].isDirectory()) {
            return entries[0];
        }
        return dir;
    }

    private static void unzip(File zip, File targetDir) throws Exception {
        targetDir.mkdirs();
        String canonicalTarget = targetDir.getCanonicalPath();
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)));
        try {
            byte[] buf = new byte[64 * 1024];
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File out = new File(targetDir, entry.getName());
                // Guard against zip-slip path traversal.
                String canonicalOut = out.getCanonicalPath();
                if (!canonicalOut.equals(canonicalTarget)
                        && !canonicalOut.startsWith(canonicalTarget + File.separator)) {
                    throw new Exception("zip entry escapes target: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    out.mkdirs();
                } else {
                    File p = out.getParentFile();
                    if (p != null) {
                        p.mkdirs();
                    }
                    FileOutputStream fos = new FileOutputStream(out);
                    try {
                        int r;
                        while ((r = zis.read(buf)) != -1) {
                            fos.write(buf, 0, r);
                        }
                    } finally {
                        fos.close();
                    }
                }
                zis.closeEntry();
            }
        } finally {
            zis.close();
        }
    }

    private static void copyRecursive(File src, File dst) throws Exception {
        if (src.isDirectory()) {
            dst.mkdirs();
            File[] children = src.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyRecursive(child, new File(dst, child.getName()));
                }
            }
        } else {
            File p = dst.getParentFile();
            if (p != null) {
                p.mkdirs();
            }
            FileInputStream fis = new FileInputStream(src);
            FileOutputStream fos = new FileOutputStream(dst);
            try {
                byte[] buf = new byte[64 * 1024];
                int r;
                while ((r = fis.read(buf)) != -1) {
                    fos.write(buf, 0, r);
                }
            } finally {
                try {
                    fis.close();
                } catch (Exception ignore) {
                }
                try {
                    fos.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    private static long dirSize(File f) {
        if (f == null || !f.exists()) {
            return 0;
        }
        if (f.isFile()) {
            return f.length();
        }
        long total = 0;
        File[] children = f.listFiles();
        if (children != null) {
            for (File child : children) {
                total += dirSize(child);
            }
        }
        return total;
    }

    private static void deleteRecursive(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        f.delete();
    }
}
