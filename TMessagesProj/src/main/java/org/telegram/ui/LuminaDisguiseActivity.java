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
 *  2. Disguise vault: a unified "vault" that hides LuminaGram behind a harmless-looking
 *     decoy app. A master switch ({@code vaultEnabled}) reveals three rows — a vault
 *     mode ({@code vaultMode}: {@code passwordDoor} / {@code decoyApp}), a decoy skin
 *     ({@code decoySkin}: {@code notepad} / {@code calculator}) and a secret code
 *     ({@code decoyUnlockCode}). The decoy/unlock behaviour itself is implemented
 *     separately (see {@code LuminaDecoy}); this page only renders the controls and
 *     persists the config keys.
 *
 *     Legacy migration: earlier builds only had a single {@code decoyLockEnabled} toggle.
 *     When {@code vaultEnabled} has never been written we fall back to that flag for the
 *     initial switch state and, if it was on, present the vault as decoyApp + calculator.
 *     Nothing is overwritten until the user actually changes a vault setting, at which
 *     point the legacy state is snapshotted and {@code vaultEnabled} is set to true.
 *
 * All values are persisted through {@link LuminaConfig}'s generic accessors.
 */
public class LuminaDisguiseActivity extends BaseFragment {

    private static final int ID_DISGUISE_ENABLED = 1;
    private static final int ID_PRESET_DEFAULT = 2;
    private static final int ID_PRESET_CALCULATOR = 3;
    private static final int ID_PRESET_NOTES = 4;
    private static final int ID_PRESET_CLOCK = 5;
    private static final int ID_VAULT_ENABLED = 6;
    private static final int ID_VAULT_MODE = 7;
    private static final int ID_VAULT_SKIN = 8;
    private static final int ID_VAULT_SET_CODE = 9;

    private static final String KEY_DISGUISE_ENABLED = LuminaDisguiseController.KEY_ENABLED;
    private static final String KEY_DISGUISE_PRESET = LuminaDisguiseController.KEY_PRESET;
    // Legacy single-toggle key from earlier builds (read-only fallback for migration).
    private static final String KEY_DECOY_ENABLED = "decoyLockEnabled";
    // Unified disguise-vault keys.
    private static final String KEY_VAULT_ENABLED = "vaultEnabled";
    private static final String KEY_VAULT_MODE = "vaultMode";
    private static final String KEY_DECOY_SKIN = "decoySkin";
    private static final String KEY_DECOY_CODE = "decoyUnlockCode";

    // vaultMode values.
    private static final String VAULT_MODE_PASSWORD_DOOR = "passwordDoor";
    private static final String VAULT_MODE_DECOY_APP = "decoyApp";
    // decoySkin values.
    private static final String DECOY_SKIN_NOTEPAD = "notepad";
    private static final String DECOY_SKIN_CALCULATOR = "calculator";

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

        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaVaultHeader)));
        boolean vaultEnabled = vaultEnabled();
        items.add(UItem.asSwitch(ID_VAULT_ENABLED, LuminaLocale.getString(R.string.LuminaVaultEnable))
                .setChecked(vaultEnabled));
        if (vaultEnabled) {
            items.add(UItem.asButton(ID_VAULT_MODE,
                    LuminaLocale.getString(R.string.LuminaVaultMode), vaultModeValueText()));
            items.add(UItem.asButton(ID_VAULT_SKIN,
                    LuminaLocale.getString(R.string.LuminaVaultSkin), decoySkinValueText()));
            items.add(UItem.asButton(ID_VAULT_SET_CODE,
                    LuminaLocale.getString(R.string.LuminaVaultSecretCode), decoyCodeValueText()));
        }
        items.add(UItem.asShadow(vaultInfoText(vaultEnabled)));
    }

    /**
     * Initial checked state of the vault switch. Reads {@code vaultEnabled}, falling back
     * to the legacy {@code decoyLockEnabled} flag so existing decoy-lock users still show ON.
     */
    private boolean vaultEnabled() {
        return LuminaConfig.getBoolean(KEY_VAULT_ENABLED,
                LuminaConfig.getBoolean(KEY_DECOY_ENABLED, false));
    }

    /**
     * True while a legacy decoy-lock user has not yet been migrated: {@code vaultEnabled}
     * was never written but the old {@code decoyLockEnabled} flag is on. In that window the
     * vault is presented as decoyApp + calculator without persisting anything.
     */
    private boolean legacyDecoyPending() {
        return !LuminaConfig.contains(KEY_VAULT_ENABLED)
                && LuminaConfig.getBoolean(KEY_DECOY_ENABLED, false);
    }

    private String currentVaultMode() {
        return LuminaConfig.getString(KEY_VAULT_MODE,
                legacyDecoyPending() ? VAULT_MODE_DECOY_APP : VAULT_MODE_PASSWORD_DOOR);
    }

    private String currentDecoySkin() {
        return LuminaConfig.getString(KEY_DECOY_SKIN,
                legacyDecoyPending() ? DECOY_SKIN_CALCULATOR : DECOY_SKIN_NOTEPAD);
    }

    private CharSequence vaultModeValueText() {
        return LuminaLocale.getString(VAULT_MODE_DECOY_APP.equals(currentVaultMode())
                ? R.string.LuminaVaultModeDecoyApp
                : R.string.LuminaVaultModePasswordDoor);
    }

    private CharSequence decoySkinValueText() {
        return LuminaLocale.getString(DECOY_SKIN_CALCULATOR.equals(currentDecoySkin())
                ? R.string.LuminaVaultSkinCalculator
                : R.string.LuminaVaultSkinNotepad);
    }

    /** Footer info: general (local, ToS-safe) blurb plus the selected mode's one-line hint. */
    private CharSequence vaultInfoText(boolean vaultEnabled) {
        CharSequence info = LuminaLocale.getString(R.string.LuminaVaultInfo);
        if (vaultEnabled) {
            int modeInfo = VAULT_MODE_DECOY_APP.equals(currentVaultMode())
                    ? R.string.LuminaVaultModeDecoyAppInfo
                    : R.string.LuminaVaultModePasswordDoorInfo;
            info = info + "\n\n" + LuminaLocale.getString(modeInfo);
        }
        return info;
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
            case ID_VAULT_ENABLED: {
                boolean newValue = !vaultEnabled();
                // Snapshot legacy decoy state before the first write flips it out of range.
                migrateLegacyIfNeeded();
                LuminaConfig.putBoolean(KEY_VAULT_ENABLED, newValue);
                update();
                break;
            }
            case ID_VAULT_MODE:
                showVaultModePicker();
                break;
            case ID_VAULT_SKIN:
                showDecoySkinPicker();
                break;
            case ID_VAULT_SET_CODE:
                showUnlockCodeDialog();
                break;
        }
    }

    /**
     * Migrate a legacy decoy-lock user into the vault the first time they touch any vault
     * control. Snapshots the inferred mode/skin so later reads stay stable once
     * {@code vaultEnabled} is written. No-op once {@code vaultEnabled} exists.
     */
    private void migrateLegacyIfNeeded() {
        if (LuminaConfig.contains(KEY_VAULT_ENABLED)) {
            return;
        }
        if (LuminaConfig.getBoolean(KEY_DECOY_ENABLED, false)) {
            if (!LuminaConfig.contains(KEY_VAULT_MODE)) {
                LuminaConfig.putString(KEY_VAULT_MODE, VAULT_MODE_DECOY_APP);
            }
            if (!LuminaConfig.contains(KEY_DECOY_SKIN)) {
                LuminaConfig.putString(KEY_DECOY_SKIN, DECOY_SKIN_CALCULATOR);
            }
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

    private void showVaultModePicker() {
        final Context context = getParentActivity();
        if (context == null) {
            return;
        }
        // Each choice shows its localized label plus a one-line explanation.
        CharSequence[] names = new CharSequence[]{
                LuminaLocale.getString(R.string.LuminaVaultModePasswordDoor) + "\n"
                        + LuminaLocale.getString(R.string.LuminaVaultModePasswordDoorInfo),
                LuminaLocale.getString(R.string.LuminaVaultModeDecoyApp) + "\n"
                        + LuminaLocale.getString(R.string.LuminaVaultModeDecoyAppInfo)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LuminaLocale.getString(R.string.LuminaVaultMode));
        builder.setItems(names, (dialog, which) -> {
            // Any vault change migrates a legacy user and turns the vault on.
            migrateLegacyIfNeeded();
            LuminaConfig.putString(KEY_VAULT_MODE,
                    which == 1 ? VAULT_MODE_DECOY_APP : VAULT_MODE_PASSWORD_DOOR);
            LuminaConfig.putBoolean(KEY_VAULT_ENABLED, true);
            update();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showDecoySkinPicker() {
        final Context context = getParentActivity();
        if (context == null) {
            return;
        }
        CharSequence[] names = new CharSequence[]{
                LuminaLocale.getString(R.string.LuminaVaultSkinNotepad),
                LuminaLocale.getString(R.string.LuminaVaultSkinCalculator)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LuminaLocale.getString(R.string.LuminaVaultSkin));
        builder.setItems(names, (dialog, which) -> {
            migrateLegacyIfNeeded();
            LuminaConfig.putString(KEY_DECOY_SKIN,
                    which == 1 ? DECOY_SKIN_CALCULATOR : DECOY_SKIN_NOTEPAD);
            LuminaConfig.putBoolean(KEY_VAULT_ENABLED, true);
            update();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
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
        builder.setTitle(LuminaLocale.getString(R.string.LuminaVaultSecretCodeDialogTitle));
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            // Any vault change migrates a legacy user and turns the vault on.
            migrateLegacyIfNeeded();
            LuminaConfig.putString(KEY_DECOY_CODE, edit.getText().toString().trim());
            LuminaConfig.putBoolean(KEY_VAULT_ENABLED, true);
            update();
            AndroidUtilities.hideKeyboard(edit);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }
}
