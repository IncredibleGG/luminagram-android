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
 * LuminaMediaActivity — media &amp; download preferences.
 *
 * Three independent toggles, each persisted through
 * {@link LuminaConfig#getBoolean}/{@link LuminaConfig#putBoolean} (all default false):
 *   keepOriginalFilename — save downloads under the sender's original filename.
 *   autoPauseBgVideo     — pause the playing video when the app is backgrounded.
 *   unlockAudioSpeed     — show the speed control for music / long audio.
 *
 * This page only renders the switches and persists their keys. The
 * keepOriginalFilename behaviour lives in FileLoader; the other two are
 * wired up by their own hooks that read the same keys.
 */
public class LuminaMediaActivity extends BaseFragment {

    private static final int ID_KEEP_ORIGINAL_FILENAME = 1;
    private static final int ID_AUTO_PAUSE_BG_VIDEO = 2;
    private static final int ID_UNLOCK_AUDIO_SPEED = 3;

    private static final String KEY_KEEP_ORIGINAL_FILENAME = "keepOriginalFilename";
    private static final String KEY_AUTO_PAUSE_BG_VIDEO = "autoPauseBgVideo";
    private static final String KEY_UNLOCK_AUDIO_SPEED = "unlockAudioSpeed";

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaMediaTitle));
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
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaMediaHeader)));

        items.add(UItem.asSwitch(ID_KEEP_ORIGINAL_FILENAME, LuminaLocale.getString(R.string.LuminaMediaKeepOriginalFilename))
                .setChecked(LuminaConfig.getBoolean(KEY_KEEP_ORIGINAL_FILENAME, false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaMediaKeepOriginalFilenameInfo)));

        items.add(UItem.asSwitch(ID_AUTO_PAUSE_BG_VIDEO, LuminaLocale.getString(R.string.LuminaMediaAutoPauseBgVideo))
                .setChecked(LuminaConfig.getBoolean(KEY_AUTO_PAUSE_BG_VIDEO, false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaMediaAutoPauseBgVideoInfo)));

        items.add(UItem.asSwitch(ID_UNLOCK_AUDIO_SPEED, LuminaLocale.getString(R.string.LuminaMediaUnlockAudioSpeed))
                .setChecked(LuminaConfig.getBoolean(KEY_UNLOCK_AUDIO_SPEED, false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaMediaUnlockAudioSpeedInfo)));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        final String key;
        switch (item.id) {
            case ID_KEEP_ORIGINAL_FILENAME:
                key = KEY_KEEP_ORIGINAL_FILENAME;
                break;
            case ID_AUTO_PAUSE_BG_VIDEO:
                key = KEY_AUTO_PAUSE_BG_VIDEO;
                break;
            case ID_UNLOCK_AUDIO_SPEED:
                key = KEY_UNLOCK_AUDIO_SPEED;
                break;
            default:
                return;
        }
        LuminaConfig.putBoolean(key, !LuminaConfig.getBoolean(key, false));
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
