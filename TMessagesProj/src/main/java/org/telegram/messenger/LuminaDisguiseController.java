package org.telegram.messenger;

import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.ui.LauncherIconController;

/**
 * LuminaDisguiseController — switches the launcher icon + app name between the real
 * LuminaGram identity and a camouflage "app" (Calculator / Notes / Clock).
 *
 * Each preset maps to one {@code <activity-alias>} in AndroidManifest.xml and to one
 * {@link LauncherIconController.LauncherIcon} enum entry. Reusing that enum matters:
 * {@link LauncherIconController#tryFixLauncherIconIfNeeded()} runs on every cold start
 * and re-enables the real DEFAULT icon whenever it finds no enum alias enabled, so a
 * disguise alias that is NOT an enum member would be silently un-hidden on restart.
 *
 * {@link #applyDisguise(Context, String)} enables exactly the chosen alias and disables
 * every sibling, guaranteeing that precisely one launcher component stays enabled and
 * that the real icon is never revealed behind an active disguise.
 */
public final class LuminaDisguiseController {

    public static final String PRESET_DEFAULT = "default";
    public static final String PRESET_CALCULATOR = "calculator";
    public static final String PRESET_NOTES = "notes";
    public static final String PRESET_CLOCK = "clock";

    /** LuminaConfig string key holding the active preset id. */
    public static final String KEY_PRESET = "disguisePreset";
    /** LuminaConfig boolean key for the master disguise switch. */
    public static final String KEY_ENABLED = "disguiseEnabled";

    /**
     * Legacy LuminaConfig boolean key from the old LuminaSecurityActivity "Disguise" switch, which
     * drove the CalculatorIcon alias directly. Kept only so {@link #migrateStaleDisguiseToggle} can
     * fold it into this preset system; no code should read it as a live toggle any more.
     */
    public static final String KEY_LEGACY_DISGUISE_ICON = "disguiseIcon";

    private LuminaDisguiseController() {}

    /** Map a preset id to its launcher alias; null / unknown / "default" -> the real DEFAULT icon. */
    public static LauncherIconController.LauncherIcon iconForPreset(String presetId) {
        if (PRESET_CALCULATOR.equals(presetId)) {
            return LauncherIconController.LauncherIcon.CALCULATOR;
        }
        if (PRESET_NOTES.equals(presetId)) {
            return LauncherIconController.LauncherIcon.NOTES;
        }
        if (PRESET_CLOCK.equals(presetId)) {
            return LauncherIconController.LauncherIcon.CLOCK;
        }
        return LauncherIconController.LauncherIcon.DEFAULT;
    }

    /**
     * The disguise preset whose launcher icon + name match a {@link LuminaDecoy} decoy skin,
     * so that turning the vault on can make the home-screen entry look like the decoy app the
     * user will actually see ({@code notepad} -> Notes, {@code calculator} -> Calculator).
     *
     * Notepad is the fallback because it is also {@code decoySkin}'s default; this never
     * returns {@link #PRESET_DEFAULT}, since a decoy skin always has a camouflage icon.
     */
    public static String presetForDecoySkin(String skin) {
        return LuminaDecoy.SKIN_CALCULATOR.equals(skin) ? PRESET_CALCULATOR : PRESET_NOTES;
    }

    /**
     * Apply the disguise for {@code presetId}: enable exactly its launcher alias and disable
     * every sibling (the other disguise aliases AND the icon-pack aliases), leaving precisely
     * one launcher component enabled. A null / unknown preset and the "default" preset restore
     * the real LuminaGram icon and name.
     *
     * The target alias is enabled first, so there is never an instant with zero launcher
     * entries; siblings are disabled only after the target is confirmed enabled.
     */
    public static void applyDisguise(Context context, String presetId) {
        if (context == null) {
            context = ApplicationLoader.applicationContext;
        }
        if (context == null) {
            return;
        }
        final PackageManager pm = context.getPackageManager();
        final LauncherIconController.LauncherIcon target = iconForPreset(presetId);

        try {
            pm.setComponentEnabledSetting(target.getComponentName(context),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        } catch (Exception e) {
            // Could not enable the target: leave the current launcher state untouched rather
            // than risk disabling every alias and ending up with zero launcher entries.
            return;
        }
        for (LauncherIconController.LauncherIcon icon : LauncherIconController.LauncherIcon.values()) {
            if (icon == target) {
                continue;
            }
            try {
                pm.setComponentEnabledSetting(icon.getComponentName(context),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * One-time consolidation of the retired LuminaSecurityActivity "Disguise" switch.
     *
     * That old toggle wrote {@link #KEY_LEGACY_DISGUISE_ICON} and flipped the CalculatorIcon alias
     * through {@link LauncherIconController#setIcon}, i.e. a SECOND controller fighting this preset
     * system over the same alias. This migrates any tester who had it on into the preset system —
     * {@code disguiseEnabled = true}, {@code disguisePreset = calculator} — then clears the legacy
     * flag so the two systems can never contend again. It never re-enables anything on its own; the
     * caller ({@link LauncherIconController#tryFixLauncherIconIfNeeded}) applies the resulting state.
     *
     * FAIL-SAFE and idempotent: guarded end-to-end, and a no-op once the legacy flag is cleared (the
     * common case), so it is cheap to call on every cold start.
     */
    public static void migrateStaleDisguiseToggle(Context context) {
        try {
            if (!LuminaConfig.getBoolean(KEY_LEGACY_DISGUISE_ICON, false)) {
                return;
            }
            // Only adopt the legacy disguise if the user has not already configured the new system,
            // so we never clobber an explicit new-system choice.
            if (!LuminaConfig.getBoolean(KEY_ENABLED, false)) {
                LuminaConfig.putBoolean(KEY_ENABLED, true);
                LuminaConfig.putString(KEY_PRESET, PRESET_CALCULATOR);
            }
            // Retire the legacy flag so only the preset system drives the launcher aliases now.
            LuminaConfig.putBoolean(KEY_LEGACY_DISGUISE_ICON, false);
        } catch (Throwable ignore) {
        }
    }
}
