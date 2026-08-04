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
}
