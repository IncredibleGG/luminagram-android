package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LuminaConfig;
import org.telegram.messenger.LuminaLocale;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

/**
 * LuminaGram privacy / stealth settings.
 *
 * Toggle sense (all default ON = standard Telegram behavior; OFF = suppress):
 *   sendReadPackets      (def true)  false = don't send read receipts
 *   sendTyping           (def true)  false = don't send typing/recording/upload status
 *   sendOnlineStatus     (def true)  false = never advertise online (stay offline)
 *   showRegistrationDate (def true)  false = hide estimated registration date row
 *
 * Persisted via LuminaConfig.getBoolean/putBoolean (no static fields, to avoid merge conflicts).
 */
public class LuminaPrivacyActivity extends BaseFragment {

    private static final int ID_READ = 1;
    private static final int ID_TYPING = 2;
    private static final int ID_ONLINE = 3;
    private static final int ID_REGDATE = 4;
    private static final int ID_SECURE_SCREEN = 5;
    private static final int ID_DISABLE_LINK_PREVIEW = 6;
    private static final int ID_STRIP_METADATA = 7;
    private static final int ID_HIDE_OWN_PHONE = 8;
    private static final int ID_HIDE_NOTIF_CONTENT = 9;
    private static final int ID_LINK_SAFETY = 10;
    private static final int ID_CRYPTO_CLIPBOARD_GUARD = 11;
    private static final int ID_INCOGNITO_KEYBOARD = 12;
    private static final int ID_SCAM_KEYWORD_WARNING = 13;

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaPrivacyTitle));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = frameLayout;

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, null);
        listView.setSections();
        actionBar.setAdaptiveBackground(listView);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaPrivacyGhostHeader)));
        items.add(UItem.asSwitch(ID_READ, LuminaLocale.getString(R.string.LuminaPrivacySendReadReceipts))
                .setChecked(LuminaConfig.getBoolean("sendReadPackets", true)));
        items.add(UItem.asSwitch(ID_TYPING, LuminaLocale.getString(R.string.LuminaPrivacySendTyping))
                .setChecked(LuminaConfig.getBoolean("sendTyping", true)));
        items.add(UItem.asSwitch(ID_ONLINE, LuminaLocale.getString(R.string.LuminaPrivacySendOnline))
                .setChecked(LuminaConfig.getBoolean("sendOnlineStatus", true)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaPrivacySendOnlineInfo)));

        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaPrivacyProfileHeader)));
        items.add(UItem.asSwitch(ID_REGDATE, LuminaLocale.getString(R.string.LuminaPrivacyShowRegistrationDate))
                .setChecked(LuminaConfig.getBoolean("showRegistrationDate", true)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaPrivacyShowRegistrationDateInfo)));
        items.add(UItem.asSwitch(ID_HIDE_OWN_PHONE, LuminaLocale.getString(R.string.LuminaPrivacyHideOwnPhone))
                .setChecked(LuminaConfig.getBoolean("hideOwnPhone", false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaPrivacyHideOwnPhoneInfo)));

        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaPrivacySecurityHeader)));
        items.add(UItem.asSwitch(ID_SECURE_SCREEN, LuminaLocale.getString(R.string.LuminaPrivacySecureScreen))
                .setChecked(LuminaConfig.getBoolean("secureScreen", false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaPrivacySecureScreenInfo)));
        items.add(UItem.asSwitch(ID_DISABLE_LINK_PREVIEW, LuminaLocale.getString(R.string.LuminaPrivacyDisableLinkPreview))
                .setChecked(LuminaConfig.getBoolean("disableLinkPreview", false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaPrivacyDisableLinkPreviewInfo)));
        items.add(UItem.asSwitch(ID_STRIP_METADATA, LuminaLocale.getString(R.string.LuminaPrivacyStripMetadata))
                .setChecked(LuminaConfig.getBoolean("stripPhotoMetadata", true)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaPrivacyStripMetadataInfo)));
        items.add(UItem.asSwitch(ID_LINK_SAFETY, LuminaLocale.getString(R.string.LuminaPrivacyLinkSafety))
                .setChecked(LuminaConfig.getBoolean("linkSafetyCheck", false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaPrivacyLinkSafetyInfo)));
        items.add(UItem.asSwitch(ID_CRYPTO_CLIPBOARD_GUARD, LuminaLocale.getString(R.string.LuminaPrivacyCryptoClipboardGuard))
                .setChecked(LuminaConfig.getBoolean("cryptoClipboardGuard", false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaPrivacyCryptoClipboardGuardInfo)));
        items.add(UItem.asSwitch(ID_INCOGNITO_KEYBOARD, LuminaLocale.getString(R.string.LuminaPrivacyIncognitoKeyboard))
                .setChecked(LuminaConfig.getBoolean("incognitoKeyboard", false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaPrivacyIncognitoKeyboardInfo)));
        items.add(UItem.asSwitch(ID_SCAM_KEYWORD_WARNING, LuminaLocale.getString(R.string.LuminaPrivacyScamKeywordWarning))
                .setChecked(LuminaConfig.getBoolean("scamKeywordWarning", false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaPrivacyScamKeywordWarningInfo)));

        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaPrivacyNotificationsHeader)));
        items.add(UItem.asSwitch(ID_HIDE_NOTIF_CONTENT, LuminaLocale.getString(R.string.LuminaPrivacyHideNotifContent))
                .setChecked(LuminaConfig.getBoolean("hideNotifContent", false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaPrivacyHideNotifContentInfo)));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_READ:
                LuminaConfig.putBoolean("sendReadPackets", !LuminaConfig.getBoolean("sendReadPackets", true));
                break;
            case ID_TYPING:
                LuminaConfig.putBoolean("sendTyping", !LuminaConfig.getBoolean("sendTyping", true));
                break;
            case ID_ONLINE:
                LuminaConfig.putBoolean("sendOnlineStatus", !LuminaConfig.getBoolean("sendOnlineStatus", true));
                break;
            case ID_REGDATE:
                LuminaConfig.putBoolean("showRegistrationDate", !LuminaConfig.getBoolean("showRegistrationDate", true));
                break;
            case ID_HIDE_OWN_PHONE:
                LuminaConfig.putBoolean("hideOwnPhone", !LuminaConfig.getBoolean("hideOwnPhone", false));
                break;
            case ID_SECURE_SCREEN:
                LuminaConfig.putBoolean("secureScreen", !LuminaConfig.getBoolean("secureScreen", false));
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetPasscode);
                break;
            case ID_DISABLE_LINK_PREVIEW:
                LuminaConfig.putBoolean("disableLinkPreview", !LuminaConfig.getBoolean("disableLinkPreview", false));
                break;
            case ID_STRIP_METADATA:
                LuminaConfig.putBoolean("stripPhotoMetadata", !LuminaConfig.getBoolean("stripPhotoMetadata", true));
                break;
            case ID_HIDE_NOTIF_CONTENT:
                LuminaConfig.putBoolean("hideNotifContent", !LuminaConfig.getBoolean("hideNotifContent", false));
                break;
            case ID_LINK_SAFETY:
                LuminaConfig.putBoolean("linkSafetyCheck", !LuminaConfig.getBoolean("linkSafetyCheck", false));
                break;
            case ID_CRYPTO_CLIPBOARD_GUARD:
                LuminaConfig.putBoolean("cryptoClipboardGuard", !LuminaConfig.getBoolean("cryptoClipboardGuard", false));
                break;
            case ID_INCOGNITO_KEYBOARD:
                LuminaConfig.putBoolean("incognitoKeyboard", !LuminaConfig.getBoolean("incognitoKeyboard", false));
                break;
            case ID_SCAM_KEYWORD_WARNING:
                LuminaConfig.putBoolean("scamKeywordWarning", !LuminaConfig.getBoolean("scamKeywordWarning", false));
                break;
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
        getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
    }
}
