package org.telegram.messenger;

import java.io.File;

import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.TranscribeButton;

/**
 * LuminaGram: local (on-device / own-key) voice-to-text orchestrator.
 *
 * Resolves the already-downloaded audio file for a voice / round-video message, runs the
 * currently selected local transcriber (see {@link LuminaTranscribers#current()}), and writes
 * the resulting text under the existing voice bubble by REUSING Telegram's own display path
 * ({@link TranscribeButton#finishTranscription}). Everything is LOCAL: no server RPC is issued,
 * so this is ToS-safe. v1 requires the voice note to already be on disk.
 */
public final class LuminaVoiceToText {

    private LuminaVoiceToText() {
    }

    public static void transcribe(final MessageObject mo, final int currentAccount, final BaseFragment fragment) {
        if (mo == null || mo.messageOwner == null) {
            return;
        }
        try {
            // 1. resolve the local audio file (v1 requires it already downloaded)
            File f = FileLoader.getInstance(currentAccount).getPathToMessage(mo.messageOwner);
            if (f == null || !f.exists()) {
                final String attach = mo.messageOwner.attachPath;
                if (attach != null && attach.length() > 0) {
                    final File af = new File(attach);
                    if (af.exists()) {
                        f = af;
                    }
                }
            }
            if (f == null || !f.exists()) {
                showError(fragment, LuminaLocale.getString(R.string.LuminaSttUiError));
                return;
            }

            // 2. lightweight "transcribing..." note
            if (fragment != null) {
                try {
                    BulletinFactory.of(fragment)
                            .createSimpleBulletin(R.raw.chats_infotip, LuminaLocale.getString(R.string.LuminaSttUiTranscribing))
                            .show();
                } catch (Exception ignore) {
                }
            }

            // 3. run the currently selected local transcriber
            final String langHint = resolveLangHint();
            final LuminaTranscriber transcriber = LuminaTranscribers.current();
            if (transcriber == null) {
                showError(fragment, LuminaLocale.getString(R.string.LuminaSttUiError));
                return;
            }
            transcriber.transcribe(f, langHint, new LuminaTranscriber.Callback() {
                @Override
                public void onResult(final String text) {
                    AndroidUtilities.runOnUIThread(() -> {
                        if (text == null || text.trim().isEmpty()) {
                            showError(fragment, LuminaLocale.getString(R.string.LuminaSttUiNoText));
                        } else {
                            try {
                                // REUSE Telegram's own display path (local write, no RPC).
                                TranscribeButton.finishTranscription(mo, Utilities.random.nextLong(), text);
                            } catch (Exception e) {
                                FileLog.e(e);
                                showError(fragment, LuminaLocale.getString(R.string.LuminaSttUiError));
                            }
                        }
                    });
                }

                @Override
                public void onError(final String message) {
                    AndroidUtilities.runOnUIThread(() -> showError(fragment, LuminaLocale.getString(R.string.LuminaSttUiError)));
                }

                @Override
                public void onProgress(float progress) {
                    // optional; ignored in v1
                }
            });
        } catch (Exception e) {
            FileLog.e(e);
            showError(fragment, LuminaLocale.getString(R.string.LuminaSttUiError));
        }
    }

    private static void showError(final BaseFragment fragment, final String message) {
        if (fragment == null) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            try {
                BulletinFactory.of(fragment).createErrorBulletin(message).show();
            } catch (Exception ignore) {
            }
        });
    }

    private static String resolveLangHint() {
        String appLang = "en";
        try {
            final LocaleController.LocaleInfo info = LocaleController.getInstance().getCurrentLocaleInfo();
            if (info != null && info.getLangCode() != null && info.getLangCode().length() > 0) {
                appLang = info.getLangCode();
            }
        } catch (Exception ignore) {
        }
        final int dash = appLang.indexOf('-');
        if (dash > 0) {
            appLang = appLang.substring(0, dash);
        }
        return LuminaConfig.getString("voskModelLang", appLang);
    }
}
