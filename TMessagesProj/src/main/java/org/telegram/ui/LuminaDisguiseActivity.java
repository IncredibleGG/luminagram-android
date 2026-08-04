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
import org.telegram.messenger.LuminaDisguiseController;
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
 * LuminaDisguiseActivity — "App disguise" settings.
 *
 * Mirrors {@link LuminaAppearanceActivity} / {@link LuminaSecurityActivity}'s
 * UItem / UniversalRecyclerView pattern. Two sections:
 *
 *  1. App disguise: a master switch ({@code disguiseEnabled}) plus selectable preset
 *     rows (Default / Calculator / Notes / Clock). Picking a preset stores
 *     {@code disguisePreset} and swaps the launcher alias via
 *     {@link LuminaDisguiseController#applyDisguise(Context, String)}. While the switch
 *     is off the launcher is forced back to the real LuminaGram (Default) icon.
 *
 *  2. Decoy lock (calculator): a switch ({@code decoyLockEnabled}) and a "Set unlock
 *     code" row that stores a numeric {@code decoyUnlockCode}. The decoy-calculator
 *     behaviour itself is implemented separately; this page only renders the toggles
 *     and persists the config keys.
 *
 * All values are persisted through {@link LuminaConfig}'s generic accessors.
 */
public class LuminaDisguiseActivity extends BaseFragment {

    private static final int ID_DISGUISE_ENABLED = 1;
    private static final int ID_PRESET_DEFAULT = 2;
    private static final int ID_PRESET_CALCULATOR = 3;
    private static final int ID_PRESET_NOTES = 4;
    private static final int ID_PRESET_CLOCK = 5;
    private static final int ID_DECOY_ENABLED = 6;
    private static final int ID_DECOY_SET_CODE = 7;

    private static final String KEY_DISGUISE_ENABLED = LuminaDisguiseController.KEY_ENABLED;
    private static final String KEY_DISGUISE_PRESET = LuminaDisguiseController.KEY_PRESET;
    private static final String KEY_DECOY_ENABLED = "decoyLockEnabled";
    private static final String KEY_DECOY_CODE = "decoyUnlockCode";

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaDisguiseTitle));
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
        boolean enabled = LuminaConfig.getBoolean(KEY_DISGUISE_ENABLED, false);

        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaDisguiseSectionHeader)));
        items.add(UItem.asSwitch(ID_DISGUISE_ENABLED, LuminaLocale.getString(R.string.LuminaDisguiseEnable))
                .setChecked(enabled));
        if (enabled) {
            String preset = LuminaConfig.getString(KEY_DISGUISE_PRESET, LuminaDisguiseController.PRESET_DEFAULT);
            items.add(UItem.asRadio(ID_PRESET_DEFAULT, LuminaLocale.getString(R.string.LuminaDisguisePresetDefault))
                    .setChecked(LuminaDisguiseController.PRESET_DEFAULT.equals(preset)));
            items.add(UItem.asRadio(ID_PRESET_CALCULATOR, LuminaLocale.getString(R.string.LuminaDisguisePresetCalculator))
                    .setChecked(LuminaDisguiseController.PRESET_CALCULATOR.equals(preset)));
            items.add(UItem.asRadio(ID_PRESET_NOTES, LuminaLocale.getString(R.string.LuminaDisguisePresetNotes))
                    .setChecked(LuminaDisguiseController.PRESET_NOTES.equals(preset)));
            items.add(UItem.asRadio(ID_PRESET_CLOCK, LuminaLocale.getString(R.string.LuminaDisguisePresetClock))
                    .setChecked(LuminaDisguiseController.PRESET_CLOCK.equals(preset)));
        }
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaDisguiseInfo)));

        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaDisguiseDecoyHeader)));
        boolean decoyEnabled = LuminaConfig.getBoolean(KEY_DECOY_ENABLED, false);
        items.add(UItem.asSwitch(ID_DECOY_ENABLED, LuminaLocale.getString(R.string.LuminaDisguiseDecoyEnable))
                .setChecked(decoyEnabled));
        if (decoyEnabled) {
            items.add(UItem.asButton(ID_DECOY_SET_CODE,
                    LuminaLocale.getString(R.string.LuminaDisguiseDecoySetCode), decoyCodeValueText()));
        }
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaDisguiseDecoyInfo)));
    }

    private CharSequence decoyCodeValueText() {
        String code = LuminaConfig.getString(KEY_DECOY_CODE, "");
        boolean set = code != null && code.length() > 0;
        return LuminaLocale.getString(set
                ? R.string.LuminaDisguiseDecoyCodeSet
                : R.string.LuminaDisguiseDecoyCodeNotSet);
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_DISGUISE_ENABLED: {
                boolean enable = !LuminaConfig.getBoolean(KEY_DISGUISE_ENABLED, false);
                LuminaConfig.putBoolean(KEY_DISGUISE_ENABLED, enable);
                if (enable) {
                    // Re-apply the stored preset (defaults to the real icon until one is picked).
                    LuminaDisguiseController.applyDisguise(getParentActivity(),
                            LuminaConfig.getString(KEY_DISGUISE_PRESET, LuminaDisguiseController.PRESET_DEFAULT));
                } else {
                    // Off -> force the real LuminaGram icon + name.
                    LuminaDisguiseController.applyDisguise(getParentActivity(), LuminaDisguiseController.PRESET_DEFAULT);
                }
                update();
                break;
            }
            case ID_PRESET_DEFAULT:
                selectPreset(LuminaDisguiseController.PRESET_DEFAULT);
                break;
            case ID_PRESET_CALCULATOR:
                selectPreset(LuminaDisguiseController.PRESET_CALCULATOR);
                break;
            case ID_PRESET_NOTES:
                selectPreset(LuminaDisguiseController.PRESET_NOTES);
                break;
            case ID_PRESET_CLOCK:
                selectPreset(LuminaDisguiseController.PRESET_CLOCK);
                break;
            case ID_DECOY_ENABLED:
                LuminaConfig.putBoolean(KEY_DECOY_ENABLED, !LuminaConfig.getBoolean(KEY_DECOY_ENABLED, false));
                update();
                break;
            case ID_DECOY_SET_CODE:
                showUnlockCodeDialog();
                break;
        }
    }

    private void selectPreset(String presetId) {
        LuminaConfig.putString(KEY_DISGUISE_PRESET, presetId);
        // Preset rows are only shown while the master switch is on, so applying here is safe.
        LuminaDisguiseController.applyDisguise(getParentActivity(), presetId);
        update();
    }

    private void update() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private void showUnlockCodeDialog() {
        final Context context = getParentActivity();
        if (context == null) {
            return;
        }
        final EditTextBoldCursor edit = new EditTextBoldCursor(context);
        edit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        edit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        edit.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setCursorSize(AndroidUtilities.dp(20));
        edit.setCursorWidth(1.5f);
        edit.setBackgroundDrawable(null);
        edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        edit.setSingleLine(true);
        edit.setText(LuminaConfig.getString(KEY_DECOY_CODE, ""));
        edit.setSelection(edit.length());

        final FrameLayout container = new FrameLayout(context);
        container.addView(edit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL, 24, 6, 24, 0));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LuminaLocale.getString(R.string.LuminaDisguiseDecoyCodeDialogTitle));
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            LuminaConfig.putString(KEY_DECOY_CODE, edit.getText().toString().trim());
            update();
            AndroidUtilities.hideKeyboard(edit);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }
}
