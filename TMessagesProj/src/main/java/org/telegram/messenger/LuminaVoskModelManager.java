package org.telegram.messenger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads, unpacks and locates the per-language Vosk acoustic models used by
 * {@link VoskTranscriber}. Models live under the app-private files dir at
 * {@code vosk-models/<lang>/} and are fetched once from alphacephei.com "small" model zips.
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

    private static final int CONNECT_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 30000;

    /** lang (ISO-639) -> alphacephei "small" model zip URL. Unknown langs fall back to "en". */
    private static final HashMap<String, String> URLS = new HashMap<>();
    static {
        URLS.put("en", "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip");
        URLS.put("zh", "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip");
        URLS.put("ru", "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip");
        URLS.put("es", "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip");
        URLS.put("fr", "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip");
        URLS.put("de", "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip");
        URLS.put("pt", "https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip");
    }

    /** Reduce an ISO tag ("en-US", "zh_CN") to a supported base lang, defaulting to "en". */
    public static String normalizeLang(String lang) {
        if (lang == null) {
            return "en";
        }
        String s = lang.trim().toLowerCase();
        int dash = s.indexOf('-');
        if (dash > 0) {
            s = s.substring(0, dash);
        }
        int underscore = s.indexOf('_');
        if (underscore > 0) {
            s = s.substring(0, underscore);
        }
        return URLS.containsKey(s) ? s : "en";
    }

    /** {@code <filesDir>/vosk-models/<lang>} — where the unpacked model files live. */
    public static File modelDir(String lang) {
        return new File(ApplicationLoader.getFilesDirFixed(), "vosk-models/" + normalizeLang(lang));
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
        String url = URLS.get(lang);
        if (url == null) {
            url = URLS.get("en");
        }
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
