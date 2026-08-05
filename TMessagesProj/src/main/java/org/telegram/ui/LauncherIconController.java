package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LuminaConfig;
import org.telegram.messenger.LuminaDisguiseController;
import org.telegram.messenger.R;

public class LauncherIconController {
    /**
     * Runs on every cold start from {@link ApplicationLoader#onCreate()}. It reconciles the
     * launcher aliases so exactly one component stays enabled and the enabled one matches the
     * persisted state.
     *
     * FAIL-SAFE: the entire body is wrapped in a catch-all. This method executes inside
     * {@code Application.onCreate()}; an exception escaping here would crash the process before
     * any Activity is shown (a hard 闪退 on every launch that cannot self-heal). Reconciling
     * launcher icons is never important enough to justify that, so any error is swallowed.
     */
    public static void tryFixLauncherIconIfNeeded() {
        try {
            // Consolidation: fold the legacy single "disguiseIcon" toggle (LuminaSecurityActivity)
            // into the new preset system so only ONE controller ever drives the CalculatorIcon
            // alias. Idempotent + guarded; a no-op once migrated.
            try {
                LuminaDisguiseController.migrateStaleDisguiseToggle(ApplicationLoader.applicationContext);
            } catch (Throwable ignore) {
            }

            // If a disguise is active, force the launcher to exactly the persisted preset's alias.
            // applyDisguise() enables the target first, then disables every sibling, guaranteeing
            // one launcher component that matches the persisted disguise state.
            boolean disguiseOn = false;
            try {
                disguiseOn = LuminaConfig.getBoolean(LuminaDisguiseController.KEY_ENABLED, false);
            } catch (Throwable ignore) {
            }
            if (disguiseOn) {
                String preset = LuminaDisguiseController.PRESET_DEFAULT;
                try {
                    preset = LuminaConfig.getString(LuminaDisguiseController.KEY_PRESET, LuminaDisguiseController.PRESET_DEFAULT);
                } catch (Throwable ignore) {
                }
                LuminaDisguiseController.applyDisguise(ApplicationLoader.applicationContext, preset);
                return;
            }

            // Disguise off: keep whatever cosmetic app-icon the user picked; only self-heal when
            // NOTHING is enabled (which would otherwise hide the app / make it unopenable).
            for (LauncherIcon icon : LauncherIcon.values()) {
                if (isEnabled(icon)) {
                    return;
                }
            }
            setIcon(LauncherIcon.DEFAULT);
        } catch (Throwable ignore) {
            // Never crash app startup because of launcher-icon reconciliation.
        }
    }

    /**
     * Whether {@code icon}'s alias is the active launcher component. FAIL-SAFE: a null context or
     * any PackageManager error returns false (except that a missing context still reports DEFAULT
     * as enabled, preserving the "at least the real icon is on" invariant) so callers — including
     * {@link #tryFixLauncherIconIfNeeded()} on the startup path — can never be crashed by it.
     */
    public static boolean isEnabled(LauncherIcon icon) {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx == null) {
                return icon == LauncherIcon.DEFAULT;
            }
            int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
            return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Switch the launcher to exactly {@code icon}. FAIL-SAFE: the target is ENABLED first and only
     * then are the siblings DISABLED, each in its own try/catch, so there is never an instant with
     * zero launcher entries and a PackageManager error on one component can neither crash the caller
     * nor brick the app (hide every icon). If even enabling the target fails, the current launcher
     * state is left untouched rather than risk disabling everything.
     */
    public static void setIcon(LauncherIcon icon) {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx == null) {
                return;
            }
            PackageManager pm = ctx.getPackageManager();
            try {
                pm.setComponentEnabledSetting(icon.getComponentName(ctx),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            } catch (Throwable t) {
                // Could not enable the requested icon: do not disable the others, or the app could
                // end up with no launcher entry at all.
                return;
            }
            for (LauncherIcon i : LauncherIcon.values()) {
                if (i == icon) {
                    continue;
                }
                try {
                    pm.setComponentEnabledSetting(i.getComponentName(ctx),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable ignore) {
        }
    }

    public enum LauncherIcon {
        DEFAULT("DefaultIcon", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconDefault),
        VINTAGE("VintageIcon", R.drawable.icon_6_background_sa, R.mipmap.icon_6_foreground_sa, R.string.AppIconVintage),
        AQUA("AquaIcon", R.drawable.icon_4_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconAqua),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.mipmap.icon_3_foreground_sa, R.string.AppIconPremium),
        TURBO("TurboIcon", R.drawable.icon_5_background_sa, R.mipmap.icon_5_foreground_sa, R.string.AppIconTurbo),
        NOX("NoxIcon", R.mipmap.icon_2_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconNox),
        AURORA("AuroraIcon", R.drawable.icon_aurora_background_sa, R.mipmap.icon_aurora_foreground_sa, R.string.AppIconAurora),
        SUNSET("SunsetIcon", R.drawable.icon_sunset_background_sa, R.mipmap.icon_sunset_foreground_sa, R.string.AppIconSunset),
        MIDNIGHT("MidnightIcon", R.drawable.icon_midnight_background_sa, R.mipmap.icon_midnight_foreground_sa, R.string.AppIconMidnight),
        MONO("MonoIcon", R.drawable.icon_mono_background_sa, R.mipmap.icon_mono_foreground_sa, R.string.AppIconMono),
        // LuminaGram disguise: Calculator camouflage alias. The launcher icon/label come
        // from the CalculatorIcon <activity-alias> in the manifest; the preview fields here
        // reuse existing adaptive assets so the shared app-icon system keeps compiling.
        CALCULATOR("CalculatorIcon", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.LuminaDisguiseAppLabel, false, true),
        // LuminaGram disguise: Notes / Clock camouflage aliases (preset switcher in LuminaDisguiseActivity).
        // The launcher icon + label come from the matching <activity-alias> in the manifest; the preview
        // fields here reuse existing adaptive assets so the shared app-icon system keeps compiling and so
        // tryFixLauncherIconIfNeeded() recognises an active Notes/Clock disguise instead of re-enabling
        // the real icon behind it.
        NOTES("NotesIcon", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.LuminaDisguiseNotesLabel, false, true),
        CLOCK("ClockIcon", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.LuminaDisguiseClockLabel, false, true);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;
        // LuminaGram: true for the CALCULATOR/NOTES/CLOCK camouflage aliases. These are
        // stealth launcher disguises driven by LuminaDisguiseActivity, NOT user-selectable
        // themes, so the stock App-Icon picker must hide them (see AppIconsSelectorCell).
        public final boolean disguise;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        public boolean isDisguise() {
            return disguise;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this(key, background, foreground, title, premium, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium, boolean disguise) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
            this.disguise = disguise;
        }
    }
}
