package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LuminaConfig;
import org.telegram.messenger.LuminaLocale;
import org.telegram.messenger.LuminaTranslator;
import org.telegram.messenger.LuminaTranslators;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.TranslateController;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.TranslateAlert2;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

/**
 * LuminaGram — auto-translate settings.
 * Mirrors LuminaGramSettingsActivity. Exposes the safe "Translate to" target-language
 * selection (a global setting reused from TranslateAlert2).
 */
public class LuminaTranslateActivity extends BaseFragment {

    private static final int ITEM_TARGET_LANGUAGE = 1;
    private static final int ITEM_SHOW_BUTTON = 2;
    private static final int ITEM_TRANSLATE_BEFORE_SEND = 3;
    private static final int ITEM_TRANSLATE_BEFORE_SEND_CONFIRM = 4;
    private static final int ITEM_PROVIDER = 5;
    private static final int ITEM_API_KEY = 6;
    private static final int ITEM_BASE_URL = 7;
    private static final int ITEM_MODEL = 8;
    private static final int ITEM_SYSTEM_PROMPT = 9;
    private static final int ITEM_TEST = 10;
    private static final int ITEM_DUAL_LANGUAGE = 11;

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaTranslateTitle));
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
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaTranslateHeader)));
        items.add(UItem.asButton(ITEM_TARGET_LANGUAGE, LuminaLocale.getString(R.string.LuminaTranslateTo), currentTargetLanguageName()));
        items.add(UItem.asSwitch(ITEM_SHOW_BUTTON, LuminaLocale.getString(R.string.ShowTranslateButton))
                .setChecked(getMessagesController().getTranslateController().isContextTranslateEnabled()));
        items.add(UItem.asSwitch(ITEM_DUAL_LANGUAGE, LuminaLocale.getString(R.string.LuminaDualLanguageDisplay))
                .setChecked(LuminaConfig.getBoolean("dualLanguageDisplay", false)));
        items.add(UItem.asSwitch(ITEM_TRANSLATE_BEFORE_SEND, LuminaLocale.getString(R.string.LuminaTranslateBeforeSend))
                .setChecked(LuminaConfig.translateBeforeSend));
        items.add(UItem.asSwitch(ITEM_TRANSLATE_BEFORE_SEND_CONFIRM, LuminaLocale.getString(R.string.LuminaTranslateBeforeSendConfirm))
                .setChecked(LuminaConfig.translateBeforeSendConfirm));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaTranslateBeforeSendConfirmInfo)));

        // ---- Multi-provider translation (bring-your-own key) ----
        final LuminaTranslator provider = LuminaTranslators.current();
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaTranslateProviderHeader)));
        items.add(UItem.asButton(ITEM_PROVIDER, LuminaLocale.getString(R.string.LuminaTranslateProvider), provider.displayName()));
        if (provider.needsKey()) {
            items.add(UItem.asButton(ITEM_API_KEY, LuminaLocale.getString(R.string.LuminaTranslateApiKey), maskKey(currentKey(provider))));
        }
        if (provider.needsBaseUrl()) {
            items.add(UItem.asButton(ITEM_BASE_URL, LuminaLocale.getString(R.string.LuminaTranslateBaseUrl),
                    LuminaConfig.getString("translateBaseUrl", LuminaTranslators.LLM_DEFAULT_BASE_URL)));
        }
        if (provider.needsModel()) {
            items.add(UItem.asButton(ITEM_MODEL, LuminaLocale.getString(R.string.LuminaTranslateModel),
                    LuminaConfig.getString("translateModel", LuminaTranslators.LLM_DEFAULT_MODEL)));
        }
        if ("llm".equals(provider.id())) {
            items.add(UItem.asButton(ITEM_SYSTEM_PROMPT, LuminaLocale.getString(R.string.LuminaTranslateSystemPrompt)));
        }
        items.add(UItem.asButton(ITEM_TEST, LuminaLocale.getString(R.string.LuminaTranslateTest)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaTranslateProviderSecurityInfo)));
    }

    private String currentKey(LuminaTranslator provider) {
        return LuminaConfig.getString("translateKey_" + provider.id(), "");
    }

    private CharSequence maskKey(String key) {
        if (key == null || key.length() == 0) {
            return LuminaLocale.getString(R.string.LuminaTranslateApiKeyNotSet);
        }
        final int keep = Math.min(4, key.length());
        final StringBuilder sb = new StringBuilder();
        final int dots = Math.min(6, Math.max(0, key.length() - keep));
        for (int i = 0; i < dots; i++) {
            sb.append('•');
        }
        sb.append(key.substring(key.length() - keep));
        return sb.toString();
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ITEM_TARGET_LANGUAGE:
                showLanguagePicker();
                break;
            case ITEM_SHOW_BUTTON:
                TranslateController tc = getMessagesController().getTranslateController();
                tc.setContextTranslateEnabled(!tc.isContextTranslateEnabled());
                update();
                break;
            case ITEM_DUAL_LANGUAGE:
                LuminaConfig.putBoolean("dualLanguageDisplay", !LuminaConfig.getBoolean("dualLanguageDisplay", false));
                update();
                break;
            case ITEM_TRANSLATE_BEFORE_SEND:
                LuminaConfig.toggleTranslateBeforeSend();
                update();
                break;
            case ITEM_TRANSLATE_BEFORE_SEND_CONFIRM:
                LuminaConfig.toggleTranslateBeforeSendConfirm();
                update();
                break;
            case ITEM_PROVIDER:
                showProviderPicker();
                break;
            case ITEM_API_KEY:
                showTextInputDialog("translateKey_" + LuminaTranslators.current().id(),
                        LuminaLocale.getString(R.string.LuminaTranslateApiKey), "", true, false);
                break;
            case ITEM_BASE_URL:
                showTextInputDialog("translateBaseUrl", LuminaLocale.getString(R.string.LuminaTranslateBaseUrl),
                        LuminaTranslators.LLM_DEFAULT_BASE_URL, false, false);
                break;
            case ITEM_MODEL:
                showTextInputDialog("translateModel", LuminaLocale.getString(R.string.LuminaTranslateModel),
                        LuminaTranslators.LLM_DEFAULT_MODEL, false, false);
                break;
            case ITEM_SYSTEM_PROMPT:
                showTextInputDialog("translatePrompt", LuminaLocale.getString(R.string.LuminaTranslateSystemPrompt),
                        LuminaTranslators.LLM_DEFAULT_PROMPT, false, true);
                break;
            case ITEM_TEST:
                runProviderTest();
                break;
        }
    }

    private void showProviderPicker() {
        if (getParentActivity() == null) {
            return;
        }
        final ArrayList<LuminaTranslator> providers = new ArrayList<>(LuminaTranslators.all());
        final CharSequence[] names = new CharSequence[providers.size()];
        for (int i = 0; i < providers.size(); ++i) {
            names[i] = providers.get(i).displayName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LuminaLocale.getString(R.string.LuminaTranslateProvider));
        builder.setItems(names, (dialog, which) -> {
            if (which >= 0 && which < providers.size()) {
                LuminaConfig.putString("translateProvider", providers.get(which).id());
                update();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    // Reusable single-field input dialog (API key / base URL / model / system prompt).
    private void showTextInputDialog(final String prefKey, final CharSequence title, final String def,
                                     final boolean password, final boolean multiline) {
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
        int inputType = InputType.TYPE_CLASS_TEXT;
        if (password) {
            inputType |= InputType.TYPE_TEXT_VARIATION_PASSWORD;
            edit.setSingleLine(true);
        } else if (multiline) {
            inputType |= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
            edit.setSingleLine(false);
            edit.setMaxLines(6);
        } else {
            edit.setSingleLine(true);
        }
        edit.setInputType(inputType);
        edit.setText(LuminaConfig.getString(prefKey, def));
        edit.setSelection(edit.length());

        final FrameLayout container = new FrameLayout(context);
        container.addView(edit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL, 24, 6, 24, 0));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            LuminaConfig.putString(prefKey, edit.getText().toString().trim());
            update();
            AndroidUtilities.hideKeyboard(edit);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    // Translate a sample string through the current provider and report via a bulletin.
    private void runProviderTest() {
        if (getParentActivity() == null) {
            return;
        }
        final String sample = "Hello, world!";
        final String toLang = TranslateAlert2.getToLanguage();
        LuminaTranslators.current().translate(sample, toLang, new LuminaTranslator.Callback() {
            @Override
            public void onResult(String translated, String detectedSourceLang) {
                if (getParentActivity() == null) {
                    return;
                }
                BulletinFactory.of(LuminaTranslateActivity.this)
                        .createSimpleBulletin(R.raw.done, translated).show();
            }

            @Override
            public void onError(boolean rateLimited, String message) {
                if (getParentActivity() == null) {
                    return;
                }
                final String prefix = LuminaLocale.getString(R.string.LuminaTranslateTestFailed);
                final CharSequence text = (message == null || message.length() == 0) ? prefix : (prefix + ": " + message);
                BulletinFactory.of(LuminaTranslateActivity.this).createErrorBulletin(text).show();
            }
        });
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
        builder.setTitle(LuminaLocale.getString(R.string.LuminaTranslateTo));
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
