package org.telegram.messenger;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * Default provider: Telegram's own {@code TL_messages_translateText} RPC. This
 * reproduces the fork's previous hardcoded translate-before-send behaviour exactly,
 * so selecting "telegram" is a no-op regression-wise. Also serves as the universal
 * fallback when another provider fails.
 */
final class TelegramTranslator implements LuminaTranslator {

    @Override
    public String id() {
        return "telegram";
    }

    @Override
    public CharSequence displayName() {
        return "Telegram";
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
        final TLRPC.TL_messages_translateText req = new TLRPC.TL_messages_translateText();
        req.flags |= 2;
        final TLRPC.TL_textWithEntities twe = new TLRPC.TL_textWithEntities();
        twe.text = text;
        req.text.add(twe);
        req.to_lang = TranslateController.normalizeLanguage(toLang);
        ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (res instanceof TLRPC.TL_messages_translateResult) {
                final ArrayList<TLRPC.TL_textWithEntities> result = ((TLRPC.TL_messages_translateResult) res).result;
                if (result != null && !result.isEmpty() && result.get(0) != null) {
                    cb.onResult(result.get(0).text, null);
                    return;
                }
            }
            final String text2 = err != null ? err.text : null;
            cb.onError(text2 != null && text2.contains("QUOTA"), text2);
        }));
    }
}
