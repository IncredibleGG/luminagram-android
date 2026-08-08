package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.LuminaConfig;
import org.telegram.messenger.LuminaLocale;
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
 * LuminaInterfaceActivity - interface & display preferences.
 *
 * Three independent toggles, each persisted through
 * {@link LuminaConfig#getBoolean}/{@link LuminaConfig#putBoolean} (all default false):
 *   - systemEmoji            use the device emoji set instead of Telegram's (impl elsewhere)
 *   - confirmSendVoiceVideo  ask before sending a voice / video message (impl elsewhere)
 *   - disableNumberRounding  show exact counts instead of 1.2K/3.4M (impl in AndroidUtilities)
 *
 * Structure mirrors {@link LuminaSecurityActivity}: a UItem list rendered by a
 * {@link UniversalRecyclerView}. This page only renders the switches and persists
 * the keys; the emoji and voice/video behaviours are wired by other components.
 */
public class LuminaInterfaceActivity extends BaseFragment {

    private static final int ID_SYSTEM_EMOJI = 1;
    private static final int ID_CONFIRM_VOICE_VIDEO = 2;
    private static final int ID_DISABLE_NUMBER_ROUNDING = 3;
    private static final int ID_UNREAD_DIGEST = 4;
    private static final int ID_PHOTO_UPLOAD_DATE = 5;

    private static final String KEY_SYSTEM_EMOJI = "systemEmoji";
    private static final String KEY_CONFIRM_VOICE_VIDEO = "confirmSendVoiceVideo";
    private static final String KEY_DISABLE_NUMBER_ROUNDING = "disableNumberRounding";
    private static final String KEY_PHOTO_UPLOAD_DATE = "showPhotoUploadDate";

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaInterfaceTitle));
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
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaInterfaceEmojiHeader)));
        items.add(UItem.asSwitch(ID_SYSTEM_EMOJI, LuminaLocale.getString(R.string.LuminaInterfaceSystemEmoji))
                .setChecked(LuminaConfig.getBoolean(KEY_SYSTEM_EMOJI, false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaInterfaceSystemEmojiInfo)));

        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaInterfaceMessagingHeader)));
        items.add(UItem.asSwitch(ID_CONFIRM_VOICE_VIDEO, LuminaLocale.getString(R.string.LuminaInterfaceConfirmVoiceVideo))
                .setChecked(LuminaConfig.getBoolean(KEY_CONFIRM_VOICE_VIDEO, false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaInterfaceConfirmVoiceVideoInfo)));

        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaInterfaceNumbersHeader)));
        items.add(UItem.asSwitch(ID_DISABLE_NUMBER_ROUNDING, LuminaLocale.getString(R.string.LuminaInterfaceExactNumbers))
                .setChecked(LuminaConfig.getBoolean(KEY_DISABLE_NUMBER_ROUNDING, false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaInterfaceExactNumbersInfo)));

        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaDigestTitle)));
        items.add(UItem.asSwitch(ID_UNREAD_DIGEST, LuminaLocale.getString(R.string.LuminaDigestTitle))
                .setChecked(LuminaConfig.getBoolean("unreadDigest", true)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaDigestInfo)));

        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaShowPhotoUploadDate)));
        items.add(UItem.asSwitch(ID_PHOTO_UPLOAD_DATE, LuminaLocale.getString(R.string.LuminaShowPhotoUploadDate))
                .setChecked(LuminaConfig.getBoolean(KEY_PHOTO_UPLOAD_DATE, false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaShowPhotoUploadDateInfo)));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        String key;
        switch (item.id) {
            case ID_SYSTEM_EMOJI:
                key = KEY_SYSTEM_EMOJI;
                break;
            case ID_CONFIRM_VOICE_VIDEO:
                key = KEY_CONFIRM_VOICE_VIDEO;
                break;
            case ID_DISABLE_NUMBER_ROUNDING:
                key = KEY_DISABLE_NUMBER_ROUNDING;
                break;
            case ID_PHOTO_UPLOAD_DATE:
                key = KEY_PHOTO_UPLOAD_DATE;
                break;
            case ID_UNREAD_DIGEST:
                LuminaConfig.putBoolean("unreadDigest", !LuminaConfig.getBoolean("unreadDigest", true));
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                return;
            default:
                return;
        }
        LuminaConfig.putBoolean(key, !LuminaConfig.getBoolean(key, false));
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
