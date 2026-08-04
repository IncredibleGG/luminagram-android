package org.telegram.messenger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Shared HTTP + parsing plumbing for the network-backed {@link LuminaTranslator}s
 * (DeepL, LLM, …). Mirrors {@code TranslateAlert2.alternativeTranslateInternal} but
 * runs on {@link Utilities#globalQueue} with an explicit 15s timeout, and always
 * marshals the callback back onto the UI thread.
 *
 * Security: never log request headers, bodies or the raw response — they may carry
 * the user's API key. Errors surfaced to the callback contain only the HTTP status.
 */
final class LuminaTranslatorUtil {

    private LuminaTranslatorUtil() {}

    private static final int TIMEOUT = 15000;

    /** Invoked on the UI thread with the successful raw response body. */
    interface OnBody {
        void run(String body) throws Exception;
    }

    /**
     * Perform an HTTP request off the UI thread. On HTTP < 400, {@code onOk} runs on
     * the UI thread with the response body; any 4xx/5xx or transport error routes to
     * {@code cb.onError(...)} on the UI thread. Both {@code cb} and {@code onOk} fire
     * exactly once.
     */
    static void http(final String method, final String url, final Map<String, String> headers,
                     final String body, final LuminaTranslator.Callback cb, final OnBody onOk) {
        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URI(url).toURL().openConnection();
                c.setConnectTimeout(TIMEOUT);
                c.setReadTimeout(TIMEOUT);
                c.setRequestMethod(method);
                if (headers != null) {
                    for (Map.Entry<String, String> h : headers.entrySet()) {
                        c.setRequestProperty(h.getKey(), h.getValue());
                    }
                }
                if (body != null) {
                    c.setDoOutput(true);
                    OutputStream os = c.getOutputStream();
                    try {
                        os.write(body.getBytes(StandardCharsets.UTF_8));
                    } finally {
                        os.close();
                    }
                }
                final int code = c.getResponseCode();
                final InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
                final String raw = readAll(in);
                if (code >= 400) {
                    final boolean rl = code == 429 || code == 456 || code == 403;
                    AndroidUtilities.runOnUIThread(() -> cb.onError(rl, "HTTP " + code));
                    return;
                }
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        onOk.run(raw);
                    } catch (Exception e) {
                        cb.onError(false, e.getMessage());
                    }
                });
            } catch (Exception e) {
                final String msg = e.getMessage();
                AndroidUtilities.runOnUIThread(() -> cb.onError(false, msg));
            } finally {
                if (c != null) {
                    try {
                        c.disconnect();
                    } catch (Exception ignore) {
                    }
                }
            }
        });
    }

    static String readAll(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        try {
            final StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }

    /** Trim a trailing slash so {@code base + "/path"} never doubles up. */
    static String trimTrailingSlash(String s) {
        if (s == null) {
            return "";
        }
        s = s.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
