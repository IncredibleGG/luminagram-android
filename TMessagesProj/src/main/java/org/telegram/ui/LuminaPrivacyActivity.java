package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LuminaConfig;
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

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.LuminaPrivacyTitle));
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
        items.add(UItem.asHeader(LocaleController.getString(R.string.LuminaPrivacyGhostHeader)));
        items.add(UItem.asSwitch(ID_READ, LocaleController.getString(R.string.LuminaPrivacySendReadReceipts))
                .setChecked(LuminaConfig.getBoolean("sendReadPackets", true)));
        items.add(UItem.asSwitch(ID_TYPING, LocaleController.getString(R.string.LuminaPrivacySendTyping))
                .setChecked(LuminaConfig.getBoolean("sendTyping", true)));
        items.add(UItem.asSwitch(ID_ONLINE, LocaleController.getString(R.string.LuminaPrivacySendOnline))
                .setChecked(LuminaConfig.getBoolean("sendOnlineStatus", true)));
        items.add(UItem.asShadow(LocaleController.getString(R.string.LuminaPrivacySendOnlineInfo)));

        items.add(UItem.asHeader(LocaleController.getString(R.string.LuminaPrivacyProfileHeader)));
        items.add(UItem.asSwitch(ID_REGDATE, LocaleController.getString(R.string.LuminaPrivacyShowRegistrationDate))
                .setChecked(LuminaConfig.getBoolean("showRegistrationDate", true)));
        items.add(UItem.asShadow(LocaleController.getString(R.string.LuminaPrivacyShowRegistrationDateInfo)));
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
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
        getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
    }
}
