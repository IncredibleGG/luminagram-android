package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

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
 * LuminaGram chat-list customization.
 *
 * All toggles are client-side and default OFF (standard Telegram behavior):
 *   compactChatList (def false)  true = denser dialog rows (reduced row height in
 *                                       {@link org.telegram.ui.Cells.DialogCell})
 *   hideMuteIcon    (def false)  true = hide the muted-chat bell icon in the list
 *   showMutedCount  (def false)  true = draw the unread badge of muted chats in the
 *                                       normal accent color instead of the muted gray
 *
 * Persisted via LuminaConfig.getBoolean/putBoolean (no static fields, to avoid merge
 * conflicts). Toggling posts {@link NotificationCenter#reloadInterface} so the open
 * dialog list is rebuilt and re-measured live (needed because compact changes row height).
 *
 * Self-contained: not wired into any hub here (LuminaGramSettingsActivity links it later).
 */
public class LuminaChatListActivity extends BaseFragment {

    private static final int ID_COMPACT = 1;
    private static final int ID_HIDE_MUTE = 2;
    private static final int ID_SHOW_COUNT = 3;

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaGramChatList));
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
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaChatListRowHeader)));
        items.add(UItem.asSwitch(ID_COMPACT, LuminaLocale.getString(R.string.LuminaCompactChatList))
                .setChecked(LuminaConfig.getBoolean("compactChatList", false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaCompactChatListInfo)));

        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaChatListBadgesHeader)));
        items.add(UItem.asSwitch(ID_HIDE_MUTE, LuminaLocale.getString(R.string.LuminaHideMuteIcon))
                .setChecked(LuminaConfig.getBoolean("hideMuteIcon", false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaHideMuteIconInfo)));
        items.add(UItem.asSwitch(ID_SHOW_COUNT, LuminaLocale.getString(R.string.LuminaShowMutedCount))
                .setChecked(LuminaConfig.getBoolean("showMutedCount", false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaShowMutedCountInfo)));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_COMPACT:
                LuminaConfig.putBoolean("compactChatList", !LuminaConfig.getBoolean("compactChatList", false));
                break;
            case ID_HIDE_MUTE:
                LuminaConfig.putBoolean("hideMuteIcon", !LuminaConfig.getBoolean("hideMuteIcon", false));
                break;
            case ID_SHOW_COUNT:
                LuminaConfig.putBoolean("showMutedCount", !LuminaConfig.getBoolean("showMutedCount", false));
                break;
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
        // Rebuild the open dialog list so row height / badges / mute icons refresh live.
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface);
    }
}
