package org.telegram.messenger;

import org.telegram.ui.Components.TranslateAlert2;

/**
 * Free Google web endpoint. Delegates to the fork's existing
 * {@link TranslateAlert2#alternativeTranslate} implementation (background thread,
 * 5000-char chunking, org.json parsing, UI-thread callback), only adapting the
 * {@code (result, rateLimit)} shape to {@link LuminaTranslator.Callback}. No API key.
 */
final class GoogleWebTranslator implements LuminaTranslator {

    @Override
    public String id() {
        return "google_web";
    }

    @Override
    public CharSequence displayName() {
        return "Google";
    }

    @Override
    public boolean needsKey() {
        return false;
    }

    @Override
    public boolean needsBaseUrl() {
        return false;
    }

    @Override
    public boolean needsModel() {
        return false;
    }

    @Override
    public void translate(String text, String toLang, Callback cb) {
        // fromLng = null -> alternativeTranslate auto-detects the source language.
        TranslateAlert2.alternativeTranslate(text, null, LuminaLang.google(toLang), (result, rateLimit) -> {
            if (result == null) {
                cb.onError(rateLimit != null && rateLimit, null);
            } else {
                cb.onResult(result, null);
            }
        });
    }
}
