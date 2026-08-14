package org.telegram.messenger;

import android.graphics.Bitmap;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

/**
 * LuminaGram — on-device OCR (optical character recognition).
 *
 * Turns a {@link Bitmap} into its recognized text using Google ML Kit's bundled
 * text-recognition models. Everything runs locally: the models ship inside the APK,
 * so recognition works offline and sends nothing off the device. Results are always
 * delivered on the UI thread through {@link Callback} (ML Kit's default executor).
 *
 * The recognizer uses the Chinese bundled model, which also recognizes Latin script,
 * so a single pass covers both the app's CJK audience and Western text.
 *
 * Fail-safe by construction: any missing model, decode error or ML Kit failure is
 * reported through {@link Callback#onError(String)} instead of throwing, and a null /
 * recycled bitmap short-circuits to onError. Nothing here can crash the caller.
 */
public final class LuminaOcr {

    private LuminaOcr() {}

    public interface Callback {
        /** Recognized text ("" when the image contained no readable text). Never null. */
        void onResult(String text);

        /** Recognition could not run / failed. {@code message} may be null. */
        void onError(String message);
    }

    // Created lazily and shared; ML Kit recognizers are thread-safe and cheap to keep.
    private static volatile TextRecognizer recognizer;

    private static TextRecognizer recognizer() {
        TextRecognizer r = recognizer;
        if (r == null) {
            synchronized (LuminaOcr.class) {
                r = recognizer;
                if (r == null) {
                    r = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
                    recognizer = r;
                }
            }
        }
        return r;
    }

    /** Recognize text in {@code bitmap} (no rotation applied). */
    public static void recognize(Bitmap bitmap, Callback cb) {
        recognize(bitmap, 0, cb);
    }

    /**
     * Recognize text in {@code bitmap}, telling ML Kit how the image is rotated
     * ({@code rotationDegrees} is normalized to one of 0/90/180/270; anything else
     * is treated as 0). The callback fires exactly once, on the UI thread.
     */
    public static void recognize(Bitmap bitmap, int rotationDegrees, Callback cb) {
        if (cb == null) {
            return;
        }
        if (bitmap == null || bitmap.isRecycled()) {
            cb.onError(null);
            return;
        }
        int rot = ((rotationDegrees % 360) + 360) % 360;
        if (rot != 0 && rot != 90 && rot != 180 && rot != 270) {
            rot = 0;
        }
        try {
            InputImage image = InputImage.fromBitmap(bitmap, rot);
            recognizer().process(image)
                    .addOnSuccessListener(visionText -> {
                        String out = visionText == null ? null : visionText.getText();
                        cb.onResult(out == null ? "" : out);
                    })
                    .addOnFailureListener(e -> {
                        FileLog.e(e);
                        cb.onError(e == null ? null : e.getMessage());
                    });
        } catch (Throwable t) {
            FileLog.e(t);
            cb.onError(t.getMessage());
        }
    }
}
