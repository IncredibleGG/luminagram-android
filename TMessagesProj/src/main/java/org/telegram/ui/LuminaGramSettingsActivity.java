package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LuminaConfig;
import org.telegram.messenger.LuminaLocale;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class LuminaGramSettingsActivity extends BaseFragment {

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaGramSettings));
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
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaGramChatList)));
        items.add(UItem.asSwitch(1, LuminaLocale.getString(R.string.LuminaHideTabs)).setChecked(LuminaConfig.hideTabs));
        items.add(UItem.asSwitch(2, LuminaLocale.getString(R.string.LuminaHideStories)).setChecked(LuminaConfig.hideStories));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(10, LuminaLocale.getString(R.string.LuminaPrivacyTitle)));
        items.add(UItem.asButton(11, LuminaLocale.getString(R.string.LuminaChatSettings)));
        items.add(UItem.asButton(12, LuminaLocale.getString(R.string.LuminaTranslateTitle)));
        items.add(UItem.asButton(13, LuminaLocale.getString(R.string.LuminaSecurityTitle)));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(20, LuminaLocale.getString(R.string.LuminaCheckUpdate)));
        items.add(UItem.asShadow(null));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case 1:
                LuminaConfig.toggleHideTabs();
                // Live-refresh: DialogsActivity re-runs updateFilterTabs() on dialogFiltersUpdated.
                if (getNotificationCenter() != null) {
                    getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
                }
                break;
            case 2:
                LuminaConfig.toggleHideStories();
                // Live-refresh: DialogsActivity re-runs updateStoriesVisibility() on storiesUpdated.
                if (getNotificationCenter() != null) {
                    getNotificationCenter().postNotificationName(NotificationCenter.storiesUpdated);
                }
                break;
            case 10:
                presentFragment(new LuminaPrivacyActivity());
                break;
            case 11:
                presentFragment(new LuminaChatActivity());
                break;
            case 12:
                presentFragment(new LuminaTranslateActivity());
                break;
            case 13:
                presentFragment(new LuminaSecurityActivity());
                break;
            case 20:
                LaunchActivity launchActivity = LaunchActivity.instance;
                if (launchActivity != null) {
                    // force=true bypasses the CHECK_UPDATES gate / rate-limit; a non-null
                    // progress makes LaunchActivity show the "already latest" bulletin when
                    // no newer build is found. The custom updater path handles the popup.
                    launchActivity.checkAppUpdate(true, new Browser.Progress());
                }
                break;
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
