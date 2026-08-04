package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LuminaConfig;
import org.telegram.messenger.LuminaLocale;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * LuminaBookmarksActivity — a local, client-side list of bookmarked messages.
 *
 * A lightweight alternative to Saved Messages that never touches the network: each
 * bookmark is a reference {dialogId, messageId, snippet, date} persisted as JSON in
 * the app-private "luminagram" prefs (see {@link LuminaConfig#getBookmarks()}). The
 * message context menu in {@link ChatActivity} adds/removes entries; here we list them
 * newest-first. Tapping a row reopens the source chat scrolled to that message (same
 * user_id/chat_id + message_id bundle Telegram's search uses); long-press deletes.
 *
 * Self-contained and launchable via {@code presentFragment(new LuminaBookmarksActivity())}.
 */
public class LuminaBookmarksActivity extends BaseFragment {

    private UniversalRecyclerView listView;
    private final List<Bookmark> bookmarks = new ArrayList<>();

    private static class Bookmark {
        long dialogId;
        int messageId;
        String snippet;
        long date;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaBookmarksTitle));
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

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, this::onLongClick);
        listView.setSections();
        actionBar.setAdaptiveBackground(listView);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    /** Reload the in-memory list from storage, newest first (entries are appended on save). */
    private void loadBookmarks() {
        bookmarks.clear();
        JSONArray arr = LuminaConfig.getBookmarks();
        for (int i = arr.length() - 1; i >= 0; i--) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) {
                continue;
            }
            Bookmark b = new Bookmark();
            b.dialogId = o.optLong("dialogId");
            b.messageId = o.optInt("messageId");
            b.snippet = o.optString("snippet", "");
            b.date = o.optLong("date");
            bookmarks.add(b);
        }
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        loadBookmarks();
        if (bookmarks.isEmpty()) {
            items.add(UItem.asCenterShadow(LuminaLocale.getString(R.string.LuminaBookmarksEmpty)));
            return;
        }
        for (int i = 0; i < bookmarks.size(); i++) {
            Bookmark b = bookmarks.get(i);
            String date = formatDate(b.date);
            boolean hasSnippet = !TextUtils.isEmpty(b.snippet);
            // id == index into the freshly loaded list (kept in sync by adapter.update()).
            items.add(UItem.asButton(i, hasSnippet ? b.snippet : date, hasSnippet ? date : ""));
        }
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id < 0 || item.id >= bookmarks.size()) {
            return;
        }
        openBookmark(bookmarks.get(item.id));
    }

    private boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id < 0 || item.id >= bookmarks.size()) {
            return false;
        }
        confirmDelete(bookmarks.get(item.id));
        return true;
    }

    /** Open the source chat scrolled to the bookmarked message (mirrors Telegram's search jump). */
    private void openBookmark(Bookmark b) {
        Bundle args = new Bundle();
        if (b.dialogId >= 0) {
            args.putLong("user_id", b.dialogId);
        } else {
            args.putLong("chat_id", -b.dialogId);
        }
        args.putInt("message_id", b.messageId);
        presentFragment(new ChatActivity(args));
    }

    private void confirmDelete(Bookmark b) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LuminaLocale.getString(R.string.LuminaBookmarkDeleteTitle));
        if (!TextUtils.isEmpty(b.snippet)) {
            builder.setMessage(b.snippet);
        }
        builder.setPositiveButton(LocaleController.getString(R.string.Delete), (d, w) -> {
            LuminaConfig.removeBookmark(b.dialogId, b.messageId);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
        showDialog(dialog);
    }

    private static String formatDate(long ms) {
        if (ms <= 0) {
            return "";
        }
        try {
            return new java.text.SimpleDateFormat("d MMM yyyy, HH:mm", java.util.Locale.getDefault())
                    .format(new java.util.Date(ms));
        } catch (Exception e) {
            return "";
        }
    }
}
