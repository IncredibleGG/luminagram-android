package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LuminaConfig;
import org.telegram.messenger.LuminaGate;
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
 * LuminaGram chat &amp; media enhancements settings.
 * Mirrors {@link LuminaGramSettingsActivity} (UItem / UniversalRecyclerView).
 * SAFE toggles are persisted via {@link LuminaConfig} generic accessors.
 * The two SENSITIVE rows (copyRestricted / saveRestrictedMedia) are the prefs
 * read by {@link LuminaGate}; they are only shown in the full build
 * ({@code LuminaGate.FULL}), so they never appear in the store APK.
 */
public class LuminaChatActivity extends BaseFragment {

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.LuminaChatSettings));
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
        items.add(UItem.asHeader(LocaleController.getString(R.string.LuminaMessageActions)));
        items.add(UItem.asSwitch(1, LocaleController.getString(R.string.LuminaForwardNoAuthorTitle)).setChecked(LuminaConfig.getBoolean("forwardNoAuthor", false)));
        items.add(UItem.asSwitch(2, LocaleController.getString(R.string.LuminaForwardNoCaptionTitle)).setChecked(LuminaConfig.getBoolean("forwardNoCaption", false)));
        items.add(UItem.asSwitch(3, LocaleController.getString(R.string.LuminaSaveToCloudTitle)).setChecked(LuminaConfig.getBoolean("saveToCloud", true)));
        items.add(UItem.asSwitch(4, LocaleController.getString(R.string.LuminaSelectFromAuthorTitle)).setChecked(LuminaConfig.getBoolean("selectFromAuthor", true)));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.LuminaMediaSaving)));
        items.add(UItem.asSwitch(5, LocaleController.getString(R.string.LuminaSaveStickers)).setChecked(LuminaConfig.getBoolean("saveStickers", true)));
        items.add(UItem.asShadow(null));

        if (LuminaGate.FULL) {
            items.add(UItem.asHeader(LocaleController.getString(R.string.LuminaSensitive)));
            items.add(UItem.asSwitch(6, LocaleController.getString(R.string.LuminaCopyRestricted)).setChecked(LuminaConfig.getBoolean("copyRestricted", false)));
            items.add(UItem.asSwitch(7, LocaleController.getString(R.string.LuminaSaveRestrictedMedia)).setChecked(LuminaConfig.getBoolean("saveRestrictedMedia", false)));
            items.add(UItem.asShadow(LocaleController.getString(R.string.LuminaSensitiveInfo)));
        }
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case 1:
                LuminaConfig.putBoolean("forwardNoAuthor", !LuminaConfig.getBoolean("forwardNoAuthor", false));
                break;
            case 2:
                LuminaConfig.putBoolean("forwardNoCaption", !LuminaConfig.getBoolean("forwardNoCaption", false));
                break;
            case 3:
                LuminaConfig.putBoolean("saveToCloud", !LuminaConfig.getBoolean("saveToCloud", true));
                break;
            case 4:
                LuminaConfig.putBoolean("selectFromAuthor", !LuminaConfig.getBoolean("selectFromAuthor", true));
                break;
            case 5:
                LuminaConfig.putBoolean("saveStickers", !LuminaConfig.getBoolean("saveStickers", true));
                break;
            case 6:
                LuminaConfig.putBoolean("copyRestricted", !LuminaConfig.getBoolean("copyRestricted", false));
                break;
            case 7:
                LuminaConfig.putBoolean("saveRestrictedMedia", !LuminaConfig.getBoolean("saveRestrictedMedia", false));
                break;
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
        getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
    }
}
