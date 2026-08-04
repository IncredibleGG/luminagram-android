package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LuminaConfig;
import org.telegram.messenger.LuminaLocale;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.io.File;
import java.util.ArrayList;

/**
 * LuminaSecurityActivity — anti-censorship security tools.
 *
 * Panic wipe (Kaboom): one tap logs out every account (server + local) and erases
 * local chats and cached media, then returns to the login screen.
 *
 * Disguise: swaps the launcher icon/label to a plain Calculator using the same
 * activity-alias mechanism as the app-icon system ({@link LauncherIconController}).
 *
 * Toggles are persisted through {@link LuminaConfig#getBoolean}/{@link LuminaConfig#putBoolean}.
 */
public class LuminaSecurityActivity extends BaseFragment {

    private static final int ID_PANIC_WIPE = 1;
    private static final int ID_DISGUISE = 2;

    private static final String KEY_DISGUISE = "disguiseIcon";

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaSecurityTitle));
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
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaSecurityPanicHeader)));
        items.add(UItem.asButton(ID_PANIC_WIPE, LuminaLocale.getString(R.string.LuminaSecurityPanicWipe)).red());
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaSecurityPanicWipeInfo)));

        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaSecurityDisguiseHeader)));
        items.add(UItem.asSwitch(ID_DISGUISE, LuminaLocale.getString(R.string.LuminaSecurityDisguise))
                .setChecked(LuminaConfig.getBoolean(KEY_DISGUISE, false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaSecurityDisguiseInfo)));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_PANIC_WIPE:
                showPanicConfirm();
                break;
            case ID_DISGUISE:
                boolean enable = !LuminaConfig.getBoolean(KEY_DISGUISE, false);
                LuminaConfig.putBoolean(KEY_DISGUISE, enable);
                // Reversible: enabling swaps to the Calculator alias, disabling restores DEFAULT.
                LauncherIconController.setIcon(enable
                        ? LauncherIconController.LauncherIcon.CALCULATOR
                        : LauncherIconController.LauncherIcon.DEFAULT);
                break;
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private void showPanicConfirm() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LuminaLocale.getString(R.string.LuminaSecurityPanicConfirmTitle));
        builder.setMessage(LuminaLocale.getString(R.string.LuminaSecurityPanicConfirmMessage));
        builder.setPositiveButton(LuminaLocale.getString(R.string.LuminaSecurityPanicConfirmButton),
                (d, w) -> performPanicWipe());
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
        showDialog(dialog);
    }

    /**
     * Panic wipe: clear cached media, then log out every activated account. Each
     * {@link MessagesController#performLogout(int)} drops that account's local message
     * database and config; the final logout posts {@code appDidLogout}, which
     * LaunchActivity turns into a switch back to the intro/login screen.
     */
    private void performPanicWipe() {
        clearLocalMediaCache();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                // type 1 = full logout: unregister push + server-side auth.logOut + local wipe.
                MessagesController.getInstance(a).performLogout(1);
            }
        }
    }

    /** Best-effort recursive wipe of every local media/cache directory. */
    private void clearLocalMediaCache() {
        int[] mediaDirs = {
                FileLoader.MEDIA_DIR_IMAGE,
                FileLoader.MEDIA_DIR_AUDIO,
                FileLoader.MEDIA_DIR_VIDEO,
                FileLoader.MEDIA_DIR_DOCUMENT,
                FileLoader.MEDIA_DIR_CACHE,
                FileLoader.MEDIA_DIR_FILES,
                FileLoader.MEDIA_DIR_STORIES,
        };
        for (int type : mediaDirs) {
            try {
                wipeContents(FileLoader.getDirectory(type));
            } catch (Exception ignored) {
            }
        }
        try {
            wipeContents(ApplicationLoader.applicationContext.getCacheDir());
        } catch (Exception ignored) {
        }
    }

    /** Delete everything inside dir, keeping the top-level directory itself. */
    private static void wipeContents(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            deleteRecursive(child);
        }
    }

    private static void deleteRecursive(File file) {
        if (file == null) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
