package org.telegram.messenger;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

/**
 * Detects screenshots taken on this device while a chat is open.
 * Uses ContentObserver on MediaStore.Images to detect new screenshot files.
 * Safe under Telegram ToS — purely local awareness, no server interaction.
 */
public class LuminaScreenshotDetector {

    public interface Listener {
        void onScreenshotDetected();
    }

    private final ContentResolver contentResolver;
    private ContentObserver observer;
    private Listener listener;
    private boolean registered;
    private long lastNotifyTime;

    public LuminaScreenshotDetector(ContentResolver resolver) {
        this.contentResolver = resolver;
    }

    public void start(Listener l) {
        if (registered) return;
        this.listener = l;
        observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (uri == null || listener == null) return;
                String path = uri.toString().toLowerCase();
                // Check if this is a screenshot (common paths contain "screenshot")
                if (path.contains("screenshot") || path.contains("screen_shot") || path.contains("screen-shot") || path.contains("captures")) {
                    long now = System.currentTimeMillis();
                    // Debounce: don't fire more than once per 3 seconds
                    if (now - lastNotifyTime > 3000) {
                        lastNotifyTime = now;
                        AndroidUtilities.runOnUIThread(() -> {
                            if (listener != null) listener.onScreenshotDetected();
                        });
                    }
                }
            }
        };
        try {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true, observer);
            registered = true;
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void stop() {
        if (!registered || observer == null) return;
        try {
            contentResolver.unregisterContentObserver(observer);
        } catch (Exception e) {
            FileLog.e(e);
        }
        registered = false;
        listener = null;
        observer = null;
    }
}
