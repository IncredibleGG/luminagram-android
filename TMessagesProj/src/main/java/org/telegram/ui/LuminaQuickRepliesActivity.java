package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LuminaConfig;
import org.telegram.messenger.LuminaLocale;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

/**
 * LuminaGram — quick reply templates.
 * Mirrors LuminaTranslateActivity (UItem-based settings fragment). Lets the user
 * add / edit / delete / reorder short text templates that can be inserted into the
 * compose field from a chat (long-press the emoji button). The list is persisted as
 * a JSON array string in the app-private "luminagram" prefs under {@link #KEY}.
 */
public class LuminaQuickRepliesActivity extends BaseFragment {

    /** LuminaConfig string key holding the JSON array of templates. */
    public static final String KEY = "quickReplies";

    private static final int ITEM_ADD = 1;
    private static final int ITEM_TEMPLATE_BASE = 100;

    private UniversalRecyclerView listView;
    private int templatesOrderId;
    private final ArrayList<String> templates = new ArrayList<>();

    // ---- Persistence (shared with ChatActivityEnterView) ----

    /** Loads the persisted templates (never null; empty on first run or parse error). */
    public static ArrayList<String> getTemplates() {
        final ArrayList<String> list = new ArrayList<>();
        final String raw = LuminaConfig.getString(KEY, null);
        if (raw == null || raw.length() == 0) {
            return list;
        }
        try {
            final JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                final String s = arr.optString(i, null);
                if (s != null && s.length() > 0) {
                    list.add(s);
                }
            }
        } catch (JSONException e) {
            FileLog.e(e);
        }
        return list;
    }

    private static void saveTemplates(ArrayList<String> list) {
        final JSONArray arr = new JSONArray();
        for (int i = 0; i < list.size(); i++) {
            arr.put(list.get(i));
        }
        LuminaConfig.putString(KEY, arr.toString());
    }

    // Reference-identity lookup so duplicate template texts stay distinguishable.
    private static int indexOfIdentity(ArrayList<String> list, String s) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == s) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean onFragmentCreate() {
        templates.clear();
        templates.addAll(getTemplates());
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaQuickRepliesTitle));
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
        listView.listenReorder(this::onReordered);
        listView.allowReorder(true);
        actionBar.setAdaptiveBackground(listView);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaQuickRepliesHeader)));
        items.add(UItem.asButton(ITEM_ADD, R.drawable.msg_add, LuminaLocale.getString(R.string.LuminaQuickRepliesAdd)).accent());
        templatesOrderId = adapter.reorderSectionStart();
        for (int i = 0; i < templates.size(); i++) {
            final String t = templates.get(i);
            final UItem u = UItem.asButton(ITEM_TEMPLATE_BASE + i, preview(t));
            u.object = t;
            items.add(u);
        }
        adapter.reorderSectionEnd();
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaQuickRepliesInfo)));
    }

    // Single-line preview of a possibly multi-line template for the list row.
    private static CharSequence preview(String t) {
        String s = t.replace('\n', ' ').trim();
        if (s.length() > 60) {
            s = s.substring(0, 60) + "…";
        }
        return s;
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ITEM_ADD) {
            showEditDialog(null);
        } else if (item.object instanceof String) {
            showEditDialog((String) item.object);
        }
    }

    // ItemTouchHelper delivers the reordered items; rebuild + persist the model order.
    private void onReordered(int id, ArrayList<UItem> items) {
        if (id != templatesOrderId) {
            return;
        }
        final ArrayList<String> newOrder = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            final Object o = items.get(i).object;
            if (o instanceof String) {
                newOrder.add((String) o);
            }
        }
        templates.clear();
        templates.addAll(newOrder);
        saveTemplates(templates);
    }

    // Add (existing == null) or edit an existing template. Editing offers a Delete action.
    private void showEditDialog(final String existing) {
        final Context context = getParentActivity();
        if (context == null) {
            return;
        }
        final EditTextBoldCursor edit = new EditTextBoldCursor(context);
        edit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        edit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        edit.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setCursorSize(AndroidUtilities.dp(20));
        edit.setCursorWidth(1.5f);
        edit.setBackgroundDrawable(null);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edit.setSingleLine(false);
        edit.setMaxLines(6);
        edit.setText(existing == null ? "" : existing);
        edit.setSelection(edit.length());

        final FrameLayout container = new FrameLayout(context);
        container.addView(edit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL, 24, 6, 24, 0));

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LuminaLocale.getString(existing == null ? R.string.LuminaQuickRepliesAdd : R.string.LuminaQuickRepliesEdit));
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            final String v = edit.getText().toString().trim();
            if (v.length() == 0) {
                return;
            }
            if (existing == null) {
                templates.add(v);
            } else {
                final int idx = indexOfIdentity(templates, existing);
                if (idx >= 0) {
                    templates.set(idx, v);
                } else {
                    templates.add(v);
                }
            }
            saveTemplates(templates);
            update();
            AndroidUtilities.hideKeyboard(edit);
        });
        if (existing != null) {
            builder.setNeutralButton(LocaleController.getString(R.string.Delete), (dialog, which) -> {
                final int idx = indexOfIdentity(templates, existing);
                if (idx >= 0) {
                    templates.remove(idx);
                    saveTemplates(templates);
                    update();
                }
            });
        }
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private void update() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
