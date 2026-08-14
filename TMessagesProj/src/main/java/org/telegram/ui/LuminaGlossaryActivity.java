package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.telegram.messenger.AndroidUtilities;
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
 * LuminaGram — translation glossary / do-not-translate list.
 * Mirrors {@link LuminaReplacerActivity} (UItem-based settings fragment). Lets the user
 * add / edit / delete terms that must never be translated (brand names, product models,
 * people's names, handles). The whole list is a flat JSON array of strings persisted in
 * the app-private "luminagram" prefs under {@link LuminaConfig#KEY_GLOSSARY_TERMS}, and is
 * consumed by {@link org.telegram.messenger.LuminaGlossary} at translate time. The list is
 * global — it applies to every chat. @usernames and links are always protected regardless
 * of this list, so it is only for the extra terms an engine would otherwise mangle.
 */
public class LuminaGlossaryActivity extends BaseFragment {

    private static final int ITEM_ADD = 1;
    private static final int ITEM_TERM_BASE = 100;

    private UniversalRecyclerView listView;
    private final ArrayList<String> terms = new ArrayList<>();

    // ---- Persistence (shared JSON shape with LuminaConfig.getGlossaryTerms) ----

    private void loadTerms() {
        terms.clear();
        final JSONArray arr = LuminaConfig.getGlossaryTerms();
        for (int i = 0; i < arr.length(); i++) {
            final String t = arr.optString(i, "");
            if (t != null && t.trim().length() > 0) {
                terms.add(t.trim());
            }
        }
    }

    private void saveTerms() {
        final JSONArray arr = new JSONArray();
        for (int i = 0; i < terms.size(); i++) {
            arr.put(terms.get(i));
        }
        LuminaConfig.putString(LuminaConfig.KEY_GLOSSARY_TERMS, arr.toString());
    }

    // Case-insensitive membership test so the same term is not stored twice.
    private boolean containsTerm(String candidate, String ignore) {
        for (int i = 0; i < terms.size(); i++) {
            final String t = terms.get(i);
            if (t.equalsIgnoreCase(candidate) && !t.equals(ignore)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onFragmentCreate() {
        loadTerms();
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaGlossaryTitle));
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
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaGlossaryHeader)));
        items.add(UItem.asButton(ITEM_ADD, R.drawable.msg_add, LuminaLocale.getString(R.string.LuminaGlossaryAdd)).accent());
        for (int i = 0; i < terms.size(); i++) {
            final String t = terms.get(i);
            final UItem u = UItem.asButton(ITEM_TERM_BASE + i, preview(t));
            u.object = t;
            items.add(u);
        }
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaGlossaryInfo)));
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
        } else if (item.object instanceof String) {
            showEditDialog((String) item.object);
        }
    }

    // Add (existing == null) or edit an existing term. Editing offers a Delete action.
    private void showEditDialog(final String existing) {
        final Context context = getParentActivity();
        if (context == null) {
            return;
        }

        final LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        final EditTextBoldCursor field = makeField(context,
                LuminaLocale.getString(R.string.LuminaGlossaryTerm), existing == null ? "" : existing);
        container.addView(field, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                24, 6, 24, 0));

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LuminaLocale.getString(existing == null ? R.string.LuminaGlossaryAdd : R.string.LuminaGlossaryEdit));
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            final String term = field.getText().toString().trim();
            if (term.length() == 0) {
                return;
            }
            if (existing == null) {
                if (!containsTerm(term, null)) {
                    terms.add(term);
                }
            } else {
                final int idx = terms.indexOf(existing);
                if (idx >= 0) {
                    if (containsTerm(term, existing)) {
                        terms.remove(idx);   // merge into the existing duplicate
                    } else {
                        terms.set(idx, term);
                    }
                }
            }
            saveTerms();
            update();
            AndroidUtilities.hideKeyboard(field);
        });
        if (existing != null) {
            builder.setNeutralButton(LocaleController.getString(R.string.Delete), (dialog, which) -> {
                terms.remove(existing);
                saveTerms();
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
