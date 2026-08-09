package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LuminaConfig;
import org.telegram.messenger.LuminaLocale;
import org.telegram.messenger.R;
import org.telegram.messenger.TranslateController;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.TranslateAlert2;

import java.util.ArrayList;

/**
 * LuminaGram: the two-row menu behind the translate icon in a chat's title bar, ported from
 * the desktop client (lumina/lumina_chat_language_menu.cpp) so both platforms behave alike.
 *
 * One row per direction — what arrives, what is sent — each naming its own current state and
 * opening the language list when pressed. The rows are built fresh on every open, so they
 * never carry stale text; nothing here is retained between openings.
 */
public final class LuminaChatLanguageMenu {

    // The code the "not translated" entry stores. It has to be one no language owns, and it
    // has to differ from the empty string, which already means "follow the interface
    // language" everywhere the read language is read.
    private static final String OFF_CODE = "off";

    private static final String KEY_READ_LANG = "trReadLang";
    private static final String KEY_SEND_LANG = "trSendLang";
    private static final String AUTO = "auto";

    private LuminaChatLanguageMenu() {}

    /**
     * Open the menu under {@code anchor} (the header translate icon).
     *
     * @param onChanged run on the UI thread after either direction was edited, so the caller
     *                  can refresh the icon tint and the translate top panel. May be null.
     */
    public static void show(BaseFragment fragment, View anchor, long dialogId, Runnable onChanged) {
        if (fragment == null || anchor == null) {
            return;
        }
        final Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        final TranslateController controller = translateController(fragment);
        if (controller == null) {
            return;
        }
        try {
            final Theme.ResourcesProvider resourcesProvider = fragment.getResourceProvider();
            final ActionBarPopupWindow.ActionBarPopupWindowLayout layout =
                    new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, R.drawable.popup_fixed_alert2, resourcesProvider);
            // The window is created below, after its content exists, so the row callbacks reach
            // it through this holder rather than through a field that would outlive the menu.
            final ActionBarPopupWindow[] window = new ActionBarPopupWindow[1];

            final ActionBarMenuSubItem incoming = new ActionBarMenuSubItem(context, true, false, resourcesProvider);
            incoming.setTextAndIcon(incomingRowText(controller, dialogId), R.drawable.msg_download);
            // Rows wide enough to say "Them, translated into Traditional Chinese" without
            // eliding: the language at the end of the row is the whole point of the row.
            incoming.setMultiline(false);
            incoming.setMinimumWidth(dp(220));
            incoming.setOnClickListener(v -> {
                dismiss(window);
                showIncomingPicker(fragment, controller, dialogId, onChanged);
            });
            layout.addView(incoming, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            final ActionBarMenuSubItem outgoing = new ActionBarMenuSubItem(context, false, true, resourcesProvider);
            outgoing.setTextAndIcon(outgoingRowText(dialogId), R.drawable.msg_send);
            outgoing.setMultiline(false);
            outgoing.setMinimumWidth(dp(220));
            outgoing.setOnClickListener(v -> {
                dismiss(window);
                showOutgoingPicker(fragment, dialogId, onChanged);
            });
            layout.addView(outgoing, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            layout.setupRadialSelectors(Theme.getColor(Theme.key_dialogButtonSelector, resourcesProvider));

            window[0] = new ActionBarPopupWindow(layout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
            window[0].setPauseNotifications(true);
            window[0].setDismissAnimationDuration(220);
            window[0].setOutsideTouchable(true);
            window[0].setClippingEnabled(true);
            window[0].setAnimationStyle(R.style.PopupContextAnimation);
            window[0].setFocusable(true);
            window[0].setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);

            layout.measure(
                    View.MeasureSpec.makeMeasureSpec(dp(1000), View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(dp(1000), View.MeasureSpec.AT_MOST));
            // Hang the menu off the icon's right edge, then pull it back on screen if that
            // would push it past the left one (narrow screens, RTL layouts).
            final int[] location = new int[2];
            anchor.getLocationInWindow(location);
            int offsetX = anchor.getWidth() - layout.getMeasuredWidth() + dp(8);
            if (location[0] + offsetX < dp(8)) {
                offsetX = dp(8) - location[0];
            }
            window[0].showAsDropDown(anchor, offsetX, -dp(8));
        } catch (Exception e) {
            // A menu that cannot be shown is a menu that is not shown; it is never a reason to
            // take the chat down with it.
            FileLog.e(e);
        }
    }

    private static TranslateController translateController(BaseFragment fragment) {
        try {
            return fragment.getMessagesController() == null
                    ? null
                    : fragment.getMessagesController().getTranslateController();
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    private static void dismiss(ActionBarPopupWindow[] window) {
        if (window[0] != null && window[0].isShowing()) {
            window[0].dismiss();
        }
    }

    // Display name for a stored code, falling back to the raw code when unknown.
    private static String languageName(String code) {
        if (code == null || code.length() == 0) {
            return "";
        }
        final String name = TranslateAlert2.capitalFirst(TranslateAlert2.languageName(code));
        return name != null ? name : code;
    }

    // ---- incoming ------------------------------------------------------------------------

    // What the incoming half reads, as a whole row.
    //
    // Off is its own string rather than the word "off" substituted into the "into {1}" one,
    // which would say "translated into not translated".
    //
    // The stored read language is empty until it has been chosen at least once, and the row
    // still has to name one: empty means whatever the chat would actually be translated into,
    // which is what getDialogTranslateTo() resolves.
    private static String incomingRowText(TranslateController controller, long dialogId) {
        if (controller.isTranslateDialogHidden(dialogId) || !controller.isTranslatingDialog(dialogId)) {
            return LuminaLocale.getString(R.string.LuminaChatLangThemOff);
        }
        final String stored = LuminaConfig.getString(KEY_READ_LANG, "");
        final String code = (stored == null || stored.length() == 0)
                ? controller.getDialogTranslateTo(dialogId)
                : stored;
        return String.format(LuminaLocale.getString(R.string.LuminaChatLangThem), languageName(code));
    }

    private static void showIncomingPicker(BaseFragment fragment, TranslateController controller, long dialogId, Runnable onChanged) {
        showLanguagePicker(
                fragment,
                LuminaLocale.getString(R.string.LuminaChatLangThemTitle),
                code -> {
                    if (OFF_CODE.equals(code)) {
                        // Excluding rather than merely stopping. Stopping lasts until the next
                        // message this chat is recognised from and the offer comes straight
                        // back, which reads as the choice not having been taken.
                        controller.toggleTranslatingDialog(dialogId, false);
                        controller.setHideTranslateDialog(dialogId, true);
                    } else {
                        // Read before writing: the row was already translating into something,
                        // and only a real change needs the pipeline restarted below.
                        final String previous = controller.getDialogTranslateTo(dialogId);
                        final boolean wasTranslating = controller.isTranslatingDialog(dialogId)
                                && !controller.isTranslateDialogHidden(dialogId);

                        // Order matters: the language is what this chat is about to be
                        // translated into, so it has to be stored before the translation
                        // starts reading it.
                        LuminaConfig.putString(KEY_READ_LANG, code);
                        if (controller.isTranslateDialogHidden(dialogId)) {
                            // Naming a language also takes the chat back off the excluded list.
                            controller.setHideTranslateDialog(dialogId, false, true);
                        }
                        // trReadLang only decides the target while bilingual display is on;
                        // with it off the target comes from the per-dialog store instead, so
                        // both have to be written or the choice would apply to one mode only.
                        controller.setDialogTranslateTo(dialogId, code);
                        if (wasTranslating && !TextUtils.equals(previous, code)) {
                            // Already translating into another language: stop first, so the
                            // messages already on screen are re-run into the new target rather
                            // than left sitting in the old one.
                            controller.toggleTranslatingDialog(dialogId, false);
                        }
                        controller.toggleTranslatingDialog(dialogId, true);
                    }
                    if (onChanged != null) {
                        onChanged.run();
                    }
                });
    }

    // ---- outgoing ------------------------------------------------------------------------

    // The language outgoing messages in this chat are actually translated into, resolved the
    // same way the send path resolves it (ChatActivityEnterView#luminaResolveSendLang): a
    // fixed global language wins, then this chat's locked one. Null means neither is set, so
    // the send flow asks once - "the recipient's language" is an answer, not a missing one.
    private static String effectiveSendLanguage(long dialogId) {
        final String global = LuminaConfig.getString(KEY_SEND_LANG, AUTO);
        if (global != null && global.length() > 0 && !AUTO.equals(global)) {
            return global;
        }
        final String locked = LuminaConfig.getDialogSendLang(dialogId);
        if (locked != null && locked.length() > 0) {
            return locked;
        }
        return null;
    }

    private static String outgoingRowText(long dialogId) {
        if (!LuminaConfig.translateBeforeSend) {
            return LuminaLocale.getString(R.string.LuminaChatLangMeOff);
        }
        final String code = effectiveSendLanguage(dialogId);
        final String name = (code == null)
                ? LuminaLocale.getString(R.string.LuminaTranslateSendLangAuto)
                : languageName(code);
        return String.format(LuminaLocale.getString(R.string.LuminaChatLangMe), name);
    }

    private static void showOutgoingPicker(BaseFragment fragment, long dialogId, Runnable onChanged) {
        showLanguagePicker(
                fragment,
                LuminaLocale.getString(R.string.LuminaChatLangMeTitle),
                code -> {
                    if (OFF_CODE.equals(code)) {
                        if (LuminaConfig.translateBeforeSend) {
                            LuminaConfig.toggleTranslateBeforeSend();
                        }
                    } else {
                        LuminaConfig.setDialogSendLang(dialogId, code);
                        // The send path prefers a fixed global send language over this chat's
                        // locked one, so when the global is what governs this chat, the global
                        // is the knob this row edits - otherwise the language just chosen
                        // would be silently overridden on the next send.
                        final String global = LuminaConfig.getString(KEY_SEND_LANG, AUTO);
                        if (global != null && global.length() > 0 && !AUTO.equals(global)) {
                            LuminaConfig.putString(KEY_SEND_LANG, code);
                        }
                        if (!LuminaConfig.translateBeforeSend) {
                            LuminaConfig.toggleTranslateBeforeSend();
                        }
                    }
                    if (onChanged != null) {
                        onChanged.run();
                    }
                });
    }

    // ---- shared picker -------------------------------------------------------------------

    // The same list the translation settings page opens, with "Not translated" sitting on top
    // as the entry that turns this direction off. A second list built somewhere else is a
    // second set of languages to keep in step.
    private static void showLanguagePicker(BaseFragment fragment, CharSequence title, Utilities.Callback<String> save) {
        final Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        try {
            final ArrayList<TranslateController.Language> languages = TranslateController.getLanguages();
            final CharSequence[] names = new CharSequence[languages.size() + 1];
            names[0] = LuminaLocale.getString(R.string.LuminaChatLangNone);
            for (int i = 0; i < languages.size(); ++i) {
                names[i + 1] = languages.get(i).displayName;
            }
            final AlertDialog.Builder builder = new AlertDialog.Builder(context, fragment.getResourceProvider());
            builder.setTitle(title);
            builder.setItems(names, (dialog, which) -> {
                try {
                    if (which == 0) {
                        save.run(OFF_CODE);
                    } else if (which - 1 >= 0 && which - 1 < languages.size()) {
                        save.run(languages.get(which - 1).code);
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            fragment.showDialog(builder.create());
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
}
