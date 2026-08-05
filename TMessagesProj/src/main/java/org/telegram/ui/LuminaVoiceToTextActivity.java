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
import org.telegram.messenger.LuminaVoskModelManager;
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

import java.io.File;
import java.util.ArrayList;

/**
 * LuminaGram — voice-to-text (speech transcription) settings.
 * Mirrors {@link LuminaTranslateActivity}'s idiom: a UniversalRecyclerView of UItem rows,
 * a provider/engine picker, a masked bring-your-own-key row and reusable text-input dialog.
 *
 * Engines:
 *   - vosk  : offline, free — needs a one-time voice-model download (managed here).
 *   - whisper / google : cloud, more accurate, use the user's own API key + quota.
 * The chat-side transcription trigger and the engines/decoders themselves live elsewhere;
 * this screen only owns the settings and their config keys.
 */
public class LuminaVoiceToTextActivity extends BaseFragment {

    // Config keys owned by this screen (consumed by the transcription pipeline elsewhere).
    private static final String KEY_ENABLED = "voiceToTextEnabled";   // default true
    private static final String KEY_ENGINE = "sttEngine";             // "vosk" | "whisper" | "google", default "vosk"
    private static final String KEY_CLOUD_BASE_URL = "sttCloudBaseUrl"; // whisper only
    private static final String KEY_MODEL = "sttModel";               // whisper only
    private static final String KEY_VOSK_LANG = "voskModelLang";      // chosen offline model language

    private static final String ENGINE_VOSK = "vosk";
    private static final String ENGINE_WHISPER = "whisper";
    private static final String ENGINE_GOOGLE = "google";

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "whisper-1";

    private static final int ITEM_ENABLE = 1;
    private static final int ITEM_ENGINE = 2;
    private static final int ITEM_API_KEY = 3;
    private static final int ITEM_BASE_URL = 4;
    private static final int ITEM_MODEL = 5;
    private static final int ITEM_MANAGE_MODELS = 6;

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaVoiceToTextTitle));
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

    private String currentEngine() {
        return LuminaConfig.getString(KEY_ENGINE, ENGINE_VOSK);
    }

    // The label shown for a given engine id (also used as the engine-picker options).
    private CharSequence engineLabel(String engine) {
        if (ENGINE_WHISPER.equals(engine)) {
            return LuminaLocale.getString(R.string.LuminaSttEngineWhisper);
        }
        if (ENGINE_GOOGLE.equals(engine)) {
            return LuminaLocale.getString(R.string.LuminaSttEngineGoogle);
        }
        return LuminaLocale.getString(R.string.LuminaSttEngineVosk);
    }

    // Display name for a stored language code, falling back to the raw code when unknown.
    private CharSequence languageDisplayName(String code) {
        if (code == null || code.length() == 0) {
            return code;
        }
        String name = TranslateAlert2.capitalFirst(TranslateAlert2.languageName(code));
        return name != null ? name : code;
    }

    // Value shown on the "manage voice models" row: the chosen offline language, or none yet.
    private CharSequence currentVoskLangName() {
        String code = LuminaConfig.getString(KEY_VOSK_LANG, "");
        if (code == null || code.length() == 0) {
            return null;
        }
        return languageDisplayName(code);
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // ---- Enable ----
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaVoiceToTextTitle)));
        items.add(UItem.asSwitch(ITEM_ENABLE, LuminaLocale.getString(R.string.LuminaSttEnable))
                .setChecked(LuminaConfig.getBoolean(KEY_ENABLED, true)));
        items.add(UItem.asShadow(null));

        // ---- Engine ----
        final String engine = currentEngine();
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaSttEngine)));
        items.add(UItem.asButton(ITEM_ENGINE, LuminaLocale.getString(R.string.LuminaSttEngine), engineLabel(engine)));

        if (ENGINE_VOSK.equals(engine)) {
            // Offline engine: pick + download a voice model.
            items.add(UItem.asButton(ITEM_MANAGE_MODELS, LuminaLocale.getString(R.string.LuminaSttManageModels), currentVoskLangName()));
        } else {
            // Cloud engine (whisper / google): bring your own key.
            items.add(UItem.asButton(ITEM_API_KEY, LuminaLocale.getString(R.string.LuminaSttKey), maskKey(currentKey())));
            if (ENGINE_WHISPER.equals(engine)) {
                items.add(UItem.asButton(ITEM_BASE_URL, LuminaLocale.getString(R.string.LuminaSttBaseUrl),
                        LuminaConfig.getString(KEY_CLOUD_BASE_URL, DEFAULT_BASE_URL)));
                items.add(UItem.asButton(ITEM_MODEL, LuminaLocale.getString(R.string.LuminaSttModel),
                        LuminaConfig.getString(KEY_MODEL, DEFAULT_MODEL)));
            }
        }
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaSttInfo)));
    }

    // Per-engine key: "sttKey_whisper" / "sttKey_google" (mirrors translate's "translateKey_<id>").
    private String keyPref() {
        return "sttKey_" + currentEngine();
    }

    private String currentKey() {
        return LuminaConfig.getString(keyPref(), "");
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
            case ITEM_ENABLE:
                LuminaConfig.putBoolean(KEY_ENABLED, !LuminaConfig.getBoolean(KEY_ENABLED, true));
                update();
                break;
            case ITEM_ENGINE:
                showEnginePicker();
                break;
            case ITEM_API_KEY:
                showTextInputDialog(keyPref(), LuminaLocale.getString(R.string.LuminaSttKey), "", true, false);
                break;
            case ITEM_BASE_URL:
                showTextInputDialog(KEY_CLOUD_BASE_URL, LuminaLocale.getString(R.string.LuminaSttBaseUrl),
                        DEFAULT_BASE_URL, false, false);
                break;
            case ITEM_MODEL:
                showTextInputDialog(KEY_MODEL, LuminaLocale.getString(R.string.LuminaSttModel),
                        DEFAULT_MODEL, false, false);
                break;
            case ITEM_MANAGE_MODELS:
                showVoskModelPicker();
                break;
        }
    }

    private void showEnginePicker() {
        if (getParentActivity() == null) {
            return;
        }
        final String[] engines = new String[]{ENGINE_VOSK, ENGINE_WHISPER, ENGINE_GOOGLE};
        final CharSequence[] names = new CharSequence[engines.length];
        for (int i = 0; i < engines.length; ++i) {
            names[i] = engineLabel(engines[i]);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LuminaLocale.getString(R.string.LuminaSttEngine));
        builder.setItems(names, (dialog, which) -> {
            if (which >= 0 && which < engines.length) {
                LuminaConfig.putString(KEY_ENGINE, engines[which]);
                update();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    // Reusable single-field input dialog (API key / base URL / model). Copied from
    // LuminaTranslateActivity so the two screens share the same look and behaviour.
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

        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
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

    /**
     * Pick a language for the offline Vosk model, remember it under {@code voskModelLang},
     * then hand off to {@link LuminaVoskModelManager#ensureModel(String, LuminaVoskModelManager.ModelCallback)}
     * behind a progress dialog. The full translatable-language list is reused for the picker.
     */
    private void showVoskModelPicker() {
        final Context context = getParentActivity();
        if (context == null) {
            return;
        }
        final ArrayList<TranslateController.Language> languages = TranslateController.getLanguages();
        final CharSequence[] names = new CharSequence[languages.size()];
        for (int i = 0; i < languages.size(); ++i) {
            names[i] = languages.get(i).displayName;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
        builder.setTitle(LuminaLocale.getString(R.string.LuminaSttDownloadModel));
        builder.setItems(names, (dialog, which) -> {
            if (which >= 0 && which < languages.size()) {
                final String code = languages.get(which).code;
                LuminaConfig.putString(KEY_VOSK_LANG, code);
                update();
                downloadVoskModel(code);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void downloadVoskModel(final String lang) {
        final Context context = getParentActivity();
        if (context == null) {
            return;
        }
        final AlertDialog progress = new AlertDialog(context, AlertDialog.ALERT_TYPE_LOADING, getResourceProvider());
        progress.setTitle(LuminaLocale.getString(R.string.LuminaSttDownloadModel));
        progress.setMessage(LuminaLocale.getString(R.string.LuminaSttUiDownloading));
        progress.setCanCancel(false);
        progress.setProgress(0);
        progress.show();

        LuminaVoskModelManager.ensureModel(lang, new LuminaVoskModelManager.ModelCallback() {
            @Override
            public void onReady(final File model) {
                AndroidUtilities.runOnUIThread(() -> {
                    progress.dismiss();
                    update();
                    if (getParentActivity() != null) {
                        BulletinFactory.of(LuminaVoiceToTextActivity.this)
                                .createSimpleBulletin(R.raw.done, languageDisplayName(lang)).show();
                    }
                });
            }

            @Override
            public void onProgress(final float value) {
                AndroidUtilities.runOnUIThread(() -> {
                    // The manager may report a 0..1 fraction or a 0..100 percent — normalise both.
                    int pct = value <= 1f ? Math.round(value * 100f) : Math.round(value);
                    if (pct < 0) {
                        pct = 0;
                    } else if (pct > 100) {
                        pct = 100;
                    }
                    progress.setProgress(pct);
                });
            }

            @Override
            public void onError(final String message) {
                AndroidUtilities.runOnUIThread(() -> {
                    progress.dismiss();
                    if (getParentActivity() == null) {
                        return;
                    }
                    final String prefix = LuminaLocale.getString(R.string.LuminaSttUiError);
                    final CharSequence text = (message == null || message.length() == 0) ? prefix : (prefix + ": " + message);
                    BulletinFactory.of(LuminaVoiceToTextActivity.this).createErrorBulletin(text).show();
                });
            }
        });
    }

    private void update() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
