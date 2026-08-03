package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LuminaConfig;
import org.telegram.messenger.LuminaGate;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.TranslateController;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.TranslateAlert2;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

/**
 * LuminaGram — auto-translate settings.
 * Mirrors LuminaGramSettingsActivity. The sensitive "auto-translate all chats"
 * row is keyed on the "autoTranslateAll" pref that LuminaGate.unlockPremiumTranslate()
 * reads, and is only shown in the full build (LuminaGate.FULL). The target-language
 * row is a safe global setting reused from TranslateAlert2.
 */
public class LuminaTranslateActivity extends BaseFragment {

    private static final String KEY_AUTO_TRANSLATE_ALL = "autoTranslateAll";

    private static final int ITEM_TARGET_LANGUAGE = 1;
    private static final int ITEM_AUTO_TRANSLATE_ALL = 2;

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.LuminaTranslateTitle));
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

    private CharSequence currentTargetLanguageName() {
        String code = TranslateAlert2.getToLanguage();
        String name = TranslateAlert2.capitalFirst(TranslateAlert2.languageName(code));
        return name != null ? name : code;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.LuminaTranslateHeader)));
        if (LuminaGate.FULL) {
            items.add(UItem.asSwitch(ITEM_AUTO_TRANSLATE_ALL, LocaleController.getString(R.string.LuminaAutoTranslateAll))
                    .setChecked(LuminaConfig.getBoolean(KEY_AUTO_TRANSLATE_ALL, false)));
        }
        items.add(UItem.asButton(ITEM_TARGET_LANGUAGE, LocaleController.getString(R.string.LuminaTranslateTo), currentTargetLanguageName()));
        if (LuminaGate.FULL) {
            items.add(UItem.asShadow(LocaleController.getString(R.string.LuminaAutoTranslateAllInfo)));
        } else {
            items.add(UItem.asShadow(null));
        }
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ITEM_AUTO_TRANSLATE_ALL:
                LuminaConfig.putBoolean(KEY_AUTO_TRANSLATE_ALL, !LuminaConfig.getBoolean(KEY_AUTO_TRANSLATE_ALL, false));
                update();
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                break;
            case ITEM_TARGET_LANGUAGE:
                showLanguagePicker();
                break;
        }
    }

    private void showLanguagePicker() {
        if (getParentActivity() == null) {
            return;
        }
        final ArrayList<TranslateController.Language> languages = TranslateController.getLanguages();
        final CharSequence[] names = new CharSequence[languages.size()];
        for (int i = 0; i < languages.size(); ++i) {
            names[i] = languages.get(i).displayName;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.LuminaTranslateTo));
        builder.setItems(names, (dialog, which) -> {
            if (which >= 0 && which < languages.size()) {
                TranslateAlert2.setToLanguage(languages.get(which).code);
                update();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void update() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
