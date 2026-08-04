package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
 * LuminaGram — text replacer / auto-substitution.
 * Mirrors LuminaQuickRepliesActivity (UItem-based settings fragment). Lets the user
 * add / edit / delete substitution rules ("brb" -> "be right back"). Each rule is a
 * {from, to} pair; the whole list is persisted as a JSON array string in the
 * app-private "luminagram" prefs under {@link LuminaConfig#KEY_TEXT_REPLACEMENTS}.
 * The rules are applied to outgoing plain-text messages in SendMessagesHelper
 * (see {@link LuminaConfig#applyTextReplacements(String)}).
 */
public class LuminaReplacerActivity extends BaseFragment {

    private static final int ITEM_ADD = 1;
    private static final int ITEM_RULE_BASE = 100;

    private UniversalRecyclerView listView;
    private final ArrayList<Rule> rules = new ArrayList<>();

    // A single substitution rule (mutable so edit-in-place keeps list order).
    private static class Rule {
        String from;
        String to;
        Rule(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }

    // ---- Persistence (shared JSON shape with LuminaConfig.applyTextReplacements) ----

    private void loadRules() {
        rules.clear();
        final JSONArray arr = LuminaConfig.getTextReplacements();
        for (int i = 0; i < arr.length(); i++) {
            final JSONObject o = arr.optJSONObject(i);
            if (o == null) {
                continue;
            }
            final String from = o.optString("from", "");
            final String to = o.optString("to", "");
            if (from.length() > 0) {
                rules.add(new Rule(from, to));
            }
        }
    }

    private void saveRules() {
        final JSONArray arr = new JSONArray();
        for (int i = 0; i < rules.size(); i++) {
            final Rule r = rules.get(i);
            try {
                final JSONObject o = new JSONObject();
                o.put("from", r.from);
                o.put("to", r.to);
                arr.put(o);
            } catch (JSONException e) {
                FileLog.e(e);
            }
        }
        LuminaConfig.putString(LuminaConfig.KEY_TEXT_REPLACEMENTS, arr.toString());
    }

    @Override
    public boolean onFragmentCreate() {
        loadRules();
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaReplacerTitle));
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
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaReplacerHeader)));
        items.add(UItem.asButton(ITEM_ADD, R.drawable.msg_add, LuminaLocale.getString(R.string.LuminaReplacerAdd)).accent());
        for (int i = 0; i < rules.size(); i++) {
            final Rule r = rules.get(i);
            final UItem u = UItem.asButton(ITEM_RULE_BASE + i, preview(r.from), preview(r.to));
            u.object = r;
            items.add(u);
        }
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaReplacerInfo)));
    }

    // Single-line preview for a list row.
    private static CharSequence preview(String t) {
        if (t == null) {
            return "";
        }
        String s = t.replace('\n', ' ').trim();
        if (s.length() > 40) {
            s = s.substring(0, 40) + "…";
        }
        return s;
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ITEM_ADD) {
            showEditDialog(null);
        } else if (item.object instanceof Rule) {
            showEditDialog((Rule) item.object);
        }
    }

    // Add (existing == null) or edit an existing rule. Editing offers a Delete action.
    private void showEditDialog(final Rule existing) {
        final Context context = getParentActivity();
        if (context == null) {
            return;
        }

        final LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        final EditTextBoldCursor fromEdit = makeField(context,
                LuminaLocale.getString(R.string.LuminaReplacerFrom), existing == null ? "" : existing.from);
        final EditTextBoldCursor toEdit = makeField(context,
                LuminaLocale.getString(R.string.LuminaReplacerTo), existing == null ? "" : existing.to);

        container.addView(fromEdit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                24, 6, 24, 0));
        container.addView(toEdit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                24, 12, 24, 0));

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LuminaLocale.getString(existing == null ? R.string.LuminaReplacerAdd : R.string.LuminaReplacerEdit));
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            final String from = fromEdit.getText().toString().trim();
            final String to = toEdit.getText().toString();
            if (from.length() == 0) {
                return;
            }
            if (existing == null) {
                rules.add(new Rule(from, to));
            } else {
                existing.from = from;
                existing.to = to;
            }
            saveRules();
            update();
            AndroidUtilities.hideKeyboard(fromEdit);
        });
        if (existing != null) {
            builder.setNeutralButton(LocaleController.getString(R.string.Delete), (dialog, which) -> {
                rules.remove(existing);
                saveRules();
                update();
            });
        }
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private EditTextBoldCursor makeField(Context context, CharSequence hint, String value) {
        final EditTextBoldCursor edit = new EditTextBoldCursor(context);
        edit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        edit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        edit.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setCursorSize(AndroidUtilities.dp(20));
        edit.setCursorWidth(1.5f);
        edit.setBackgroundDrawable(null);
        edit.setInputType(InputType.TYPE_CLASS_TEXT);
        edit.setSingleLine(true);
        edit.setHint(hint);
        edit.setText(value == null ? "" : value);
        edit.setSelection(edit.length());
        return edit;
    }

    private void update() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
