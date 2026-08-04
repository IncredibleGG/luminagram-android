package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

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
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

/**
 * LuminaProfileCardActivity — a LOCAL "about me" card for cross-language socializing.
 *
 * The user fills a small self-description (tagline, languages they speak, interests /
 * tags, a short bio). Everything is stored ONLY on this device as a JSON object in the
 * app-private "luminagram" prefs under {@link #KEY_PROFILE_CARD} (via
 * {@link LuminaConfig#getString}/{@link LuminaConfig#putString}) — nothing is ever sent
 * to Telegram and the real Telegram profile is never touched.
 *
 * "Share card" builds a plain-text summary and hands it to an {@link Intent#ACTION_SEND}
 * chooser so it can be pasted into any chat; "Copy to clipboard" copies the same summary.
 *
 * UItem structure mirrors {@link LuminaSecurityActivity}; the edit dialog mirrors
 * {@link LuminaReplacerActivity}'s field dialog.
 */
public class LuminaProfileCardActivity extends BaseFragment {

    private static final int ID_TAGLINE = 1;
    private static final int ID_LANGUAGES = 2;
    private static final int ID_INTERESTS = 3;
    private static final int ID_BIO = 4;
    private static final int ID_SHARE = 5;
    private static final int ID_COPY = 6;

    /** App-private "luminagram" prefs key holding the whole card as a JSON object string. */
    private static final String KEY_PROFILE_CARD = "profileCard";

    // JSON field names inside the stored card object.
    private static final String F_TAGLINE = "tagline";
    private static final String F_LANGUAGES = "languages";
    private static final String F_INTERESTS = "interests";
    private static final String F_BIO = "bio";

    private UniversalRecyclerView listView;
    private JSONObject card = new JSONObject();

    // ---- Persistence (single JSON object under KEY_PROFILE_CARD) ----

    private void loadCard() {
        final String raw = LuminaConfig.getString(KEY_PROFILE_CARD, "");
        if (raw != null && raw.length() > 0) {
            try {
                card = new JSONObject(raw);
                return;
            } catch (JSONException ignore) {
                // Corrupt value: start from an empty card rather than crash.
            }
        }
        card = new JSONObject();
    }

    private String field(String key) {
        return card.optString(key, "");
    }

    private void setField(String key, String value) {
        try {
            card.put(key, value == null ? "" : value);
        } catch (JSONException e) {
            FileLog.e(e);
        }
        LuminaConfig.putString(KEY_PROFILE_CARD, card.toString());
    }

    @Override
    public boolean onFragmentCreate() {
        loadCard();
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaProfileCardTitle));
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
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaProfileCardHeader)));
        items.add(fieldRow(ID_TAGLINE, R.string.LuminaProfileCardTagline, F_TAGLINE));
        items.add(fieldRow(ID_LANGUAGES, R.string.LuminaProfileCardLanguages, F_LANGUAGES));
        items.add(fieldRow(ID_INTERESTS, R.string.LuminaProfileCardInterests, F_INTERESTS));
        items.add(fieldRow(ID_BIO, R.string.LuminaProfileCardBio, F_BIO));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaProfileCardInfo)));

        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaProfileCardShareHeader)));
        items.add(UItem.asButton(ID_SHARE, R.drawable.msg_share, LuminaLocale.getString(R.string.LuminaProfileCardShare)).accent());
        items.add(UItem.asButton(ID_COPY, R.drawable.msg_copy, LuminaLocale.getString(R.string.LuminaProfileCardCopy)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaProfileCardShareInfo)));
    }

    // A single card field shown as a button row: title on the left, current value (or a
    // "tap to set" hint when empty) on the right.
    private UItem fieldRow(int id, int titleRes, String jsonKey) {
        final String value = field(jsonKey);
        final CharSequence right = value.length() == 0
                ? LuminaLocale.getString(R.string.LuminaProfileCardTapToSet)
                : preview(value);
        return UItem.asButton(id, LuminaLocale.getString(titleRes), right);
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
        switch (item.id) {
            case ID_TAGLINE:
                showFieldDialog(F_TAGLINE, R.string.LuminaProfileCardTagline, R.string.LuminaProfileCardTaglineHint, false);
                break;
            case ID_LANGUAGES:
                showFieldDialog(F_LANGUAGES, R.string.LuminaProfileCardLanguages, R.string.LuminaProfileCardLanguagesHint, false);
                break;
            case ID_INTERESTS:
                showFieldDialog(F_INTERESTS, R.string.LuminaProfileCardInterests, R.string.LuminaProfileCardInterestsHint, false);
                break;
            case ID_BIO:
                showFieldDialog(F_BIO, R.string.LuminaProfileCardBio, R.string.LuminaProfileCardBioHint, true);
                break;
            case ID_SHARE:
                shareCard();
                break;
            case ID_COPY:
                copyCard();
                break;
        }
    }

    // Edit one field. Mirrors LuminaReplacerActivity's field dialog (EditTextBoldCursor
    // inside a container, Save / Cancel). Bio allows multiple lines.
    private void showFieldDialog(final String jsonKey, int titleRes, int hintRes, boolean multiline) {
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
        if (multiline) {
            edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            edit.setSingleLine(false);
            edit.setMaxLines(5);
        } else {
            edit.setInputType(InputType.TYPE_CLASS_TEXT);
            edit.setSingleLine(true);
        }
        edit.setHint(LuminaLocale.getString(hintRes));
        edit.setText(field(jsonKey));
        edit.setSelection(edit.length());

        final FrameLayout container = new FrameLayout(context);
        container.addView(edit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL, 24, 6, 24, 0));

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LuminaLocale.getString(titleRes));
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            setField(jsonKey, edit.getText().toString().trim());
            update();
            AndroidUtilities.hideKeyboard(edit);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    // ---- Share / copy ----

    /**
     * Plain-text summary of the card: one "Label: value" line per non-empty field, in a
     * fixed order. Empty when nothing has been filled in yet.
     */
    private String buildShareText() {
        final StringBuilder sb = new StringBuilder();
        appendLine(sb, R.string.LuminaProfileCardTagline, field(F_TAGLINE));
        appendLine(sb, R.string.LuminaProfileCardLanguages, field(F_LANGUAGES));
        appendLine(sb, R.string.LuminaProfileCardInterests, field(F_INTERESTS));
        appendLine(sb, R.string.LuminaProfileCardBio, field(F_BIO));
        return sb.toString().trim();
    }

    private void appendLine(StringBuilder sb, int labelRes, String value) {
        if (value == null || value.trim().length() == 0) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(LuminaLocale.getString(labelRes)).append(": ").append(value.trim());
    }

    private void shareCard() {
        final String text = buildShareText();
        if (text.length() == 0) {
            showEmptyBulletin();
            return;
        }
        final Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        try {
            final Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            activity.startActivity(Intent.createChooser(intent,
                    LuminaLocale.getString(R.string.LuminaProfileCardShare)));
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void copyCard() {
        final String text = buildShareText();
        if (text.length() == 0) {
            showEmptyBulletin();
            return;
        }
        AndroidUtilities.addToClipboard(text);
        BulletinFactory.of(this)
                .createCopyBulletin(LuminaLocale.getString(R.string.LuminaProfileCardCopied))
                .show();
    }

    private void showEmptyBulletin() {
        BulletinFactory.of(this)
                .createErrorBulletin(LuminaLocale.getString(R.string.LuminaProfileCardEmptyShare))
                .show();
    }

    private void update() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
