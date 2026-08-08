package org.telegram.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LuminaConfig;
import org.telegram.messenger.LuminaCrypto;
import org.telegram.messenger.LuminaLocale;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/**
 * LuminaChatExportActivity — passphrase-encrypted local archive of a single chat.
 *
 * A privacy/archival tool: the transcript is read straight out of Telegram local
 * message cache (messages_v2), rendered as a plain-text transcript, encrypted under a
 * user passphrase and written to a ".lgx" file on this device. Nothing is uploaded and
 * no network request is made at any point; the exporter never even touches
 * MessagesController message loading, only the on-disk cache.
 *
 * Crypto is NOT re-implemented here: {@link LuminaCrypto} produces the exact same
 * envelope the settings backup ({@link LuminaBackupActivity}) uses, so there is a
 * single key-derivation/cipher implementation to audit.
 *
 * What ends up in the file is deliberately limited:
 *  - only what is already cached locally (Telegram does not keep the full server
 *    history on device, so this is "what this device knows", not "everything ever sent");
 *  - at most {@link #MAX_MESSAGES} messages, newest first, so a huge channel cannot OOM
 *    the device;
 *  - media files are never copied. With chatExportIncludeMedia on, a media message
 *    becomes a short localized descriptor plus its caption (and the local file path when
 *    the file happens to already be downloaded); with it off, only text is written.
 *
 * Reachable as {@code presentFragment(new LuminaChatExportActivity(dialogId))}, or via a
 * Bundle carrying a {@code dialog_id} long (the argument name ChatActivity itself uses).
 */
public class LuminaChatExportActivity extends BaseFragment {

    private static final int ID_INCLUDE_MEDIA = 1;
    private static final int ID_INCLUDE_SERVICE = 2;
    private static final int ID_EXPORT = 3;

    /** Gate for the chat-menu entry point (see ChatActivity). Default on. */
    public static final String KEY_ENABLED = "chatExportEnabled";
    private static final String KEY_INCLUDE_MEDIA = "chatExportIncludeMedia";
    private static final String KEY_INCLUDE_SERVICE = "chatExportIncludeService";

    // Envelope tag, distinct from the settings backup so a wrong file is spotted early.
    private static final String MAGIC = "LuminaGramChatExport";
    private static final int MAX_MESSAGES = 20000;

    private long dialogId;
    private UniversalRecyclerView listView;
    private boolean exporting;

    private interface PassphraseCallback {
        void onPassphrase(String passphrase);
    }

    public LuminaChatExportActivity(long dialogId) {
        super();
        this.dialogId = dialogId;
    }

    public LuminaChatExportActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        if (dialogId == 0 && getArguments() != null) {
            dialogId = getArguments().getLong("dialog_id", 0);
        }
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaChatExportTitle));
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

    private String chatTitle() {
        String name = dialogId == 0 ? "" : DialogObject.getName(currentAccount, dialogId);
        if (TextUtils.isEmpty(name)) {
            name = LuminaLocale.getString(R.string.LuminaChatExportTitle);
        }
        return name;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(chatTitle()));
        items.add(UItem.asSwitch(ID_INCLUDE_MEDIA, LuminaLocale.getString(R.string.LuminaChatExportIncludeMedia))
                .setChecked(LuminaConfig.getBoolean(KEY_INCLUDE_MEDIA, false)));
        items.add(UItem.asSwitch(ID_INCLUDE_SERVICE, LuminaLocale.getString(R.string.LuminaChatExportIncludeService))
                .setChecked(LuminaConfig.getBoolean(KEY_INCLUDE_SERVICE, false)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaChatExportOptionsInfo)));

        items.add(UItem.asButton(ID_EXPORT, R.drawable.msg_download,
                LuminaLocale.getString(R.string.LuminaChatExportButton)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaChatExportInfo)));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_INCLUDE_MEDIA:
                LuminaConfig.putBoolean(KEY_INCLUDE_MEDIA, !LuminaConfig.getBoolean(KEY_INCLUDE_MEDIA, false));
                break;
            case ID_INCLUDE_SERVICE:
                LuminaConfig.putBoolean(KEY_INCLUDE_SERVICE, !LuminaConfig.getBoolean(KEY_INCLUDE_SERVICE, false));
                break;
            case ID_EXPORT:
                if (!exporting) {
                    promptPassphrase(R.string.LuminaChatExportPassphraseTitle, this::startExport);
                }
                return;
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    // ---- Export ----

    /** Everything the background pass produces; nothing here touches views. */
    private static class Transcript {
        String text;
        int exported;
        int total;
    }

    private void startExport(String passphrase) {
        if (exporting || dialogId == 0) {
            return;
        }
        exporting = true;
        final MessagesStorage storage = getMessagesStorage();
        final long did = dialogId;
        final long selfId = getUserConfig().clientUserId;
        final String title = chatTitle();
        final boolean includeMedia = LuminaConfig.getBoolean(KEY_INCLUDE_MEDIA, false);
        final boolean includeService = LuminaConfig.getBoolean(KEY_INCLUDE_SERVICE, false);
        storage.getStorageQueue().postRunnable(() -> {
            Transcript transcript = null;
            try {
                transcript = buildTranscript(storage, did, selfId, title, includeMedia, includeService);
            } catch (Throwable e) {
                // A corrupt cache row must not take the app down; report a clean failure.
                FileLog.e(e);
            }
            final Transcript result = transcript;
            AndroidUtilities.runOnUIThread(() -> finishExport(result, passphrase));
        });
    }

    /** Runs on the storage queue. Reads the local cache only — never the network. */
    private Transcript buildTranscript(MessagesStorage storage, long did, long selfId, String title,
                                       boolean includeMedia, boolean includeService) throws Exception {
        if (storage.getDatabase() == null) {
            return null;
        }

        final ArrayList<TLRPC.Message> messages = new ArrayList<>();
        int total = 0;

        SQLiteCursor cursor = null;
        try {
            cursor = storage.getDatabase().queryFinalized(String.format(Locale.US,
                    "SELECT COUNT(mid) FROM messages_v2 WHERE uid = %d", did));
            if (cursor.next()) {
                total = cursor.intValue(0);
            }
        } finally {
            if (cursor != null) {
                cursor.dispose();
                cursor = null;
            }
        }

        try {
            // Newest first + LIMIT: on a huge chat the recent history is the part worth
            // keeping, and the cap is what stops a 500k-message channel from OOMing.
            cursor = storage.getDatabase().queryFinalized(String.format(Locale.US,
                    "SELECT data FROM messages_v2 WHERE uid = %d ORDER BY date DESC, mid DESC LIMIT %d",
                    did, MAX_MESSAGES));
            while (cursor.next()) {
                NativeByteBuffer data = cursor.byteBufferValue(0);
                if (data == null) {
                    continue;
                }
                try {
                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                    if (message != null) {
                        message.readAttachPath(data, selfId);
                        messages.add(message);
                    }
                } finally {
                    data.reuse();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }

        if (messages.isEmpty()) {
            return null;
        }
        Collections.reverse(messages); // back to chronological order for reading

        // Resolve sender names from the local users/chats cache (still on the storage queue).
        final HashSet<Long> userIds = new HashSet<>();
        final HashSet<Long> chatIds = new HashSet<>();
        for (int i = 0; i < messages.size(); i++) {
            long senderId = senderIdOf(messages.get(i));
            if (senderId > 0) {
                userIds.add(senderId);
            } else if (senderId < 0) {
                chatIds.add(-senderId);
            }
        }
        final HashMap<Long, String> names = new HashMap<>();
        try {
            ArrayList<TLRPC.User> users = new ArrayList<>();
            storage.getUsersInternal(userIds, users);
            for (int i = 0; i < users.size(); i++) {
                TLRPC.User user = users.get(i);
                if (user != null) {
                    names.put(user.id, UserObject.getUserName(user));
                }
            }
        } catch (Exception e) {
            FileLog.e(e); // names are cosmetic: fall back to raw ids below
        }
        try {
            ArrayList<TLRPC.Chat> chats = new ArrayList<>();
            storage.getChatsInternal(TextUtils.join(",", chatIds), chats);
            for (int i = 0; i < chats.size(); i++) {
                TLRPC.Chat chat = chats.get(i);
                if (chat != null) {
                    names.put(-chat.id, chat.title);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

        final SimpleDateFormat stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        final StringBuilder sb = new StringBuilder();
        sb.append("LuminaGram chat export\n");
        sb.append("Chat: ").append(title).append('\n');
        sb.append("Chat id: ").append(did).append('\n');
        sb.append("Exported: ").append(stamp.format(new Date())).append('\n');
        sb.append("Cached messages: ").append(total).append('\n');
        if (total > MAX_MESSAGES) {
            sb.append("Note: capped at the newest ").append(MAX_MESSAGES).append(" messages.\n");
        }
        sb.append("----------------------------------------\n");

        int exported = 0;
        for (int i = 0; i < messages.size(); i++) {
            TLRPC.Message message = messages.get(i);
            if (message instanceof TLRPC.TL_messageEmpty) {
                continue;
            }
            boolean service = message instanceof TLRPC.TL_messageService || message.action != null;
            if (service && !includeService) {
                continue;
            }
            String body = service
                    ? serviceLabel(message)
                    : messageBody(message, includeMedia);
            if (TextUtils.isEmpty(body)) {
                continue;
            }
            long senderId = senderIdOf(message);
            String sender = names.get(senderId);
            if (TextUtils.isEmpty(sender)) {
                sender = String.valueOf(senderId);
            }
            sb.append('[').append(stamp.format(new Date(message.date * 1000L))).append("] ");
            if (service) {
                sb.append("* ").append(sender).append(' ').append(body).append('\n');
            } else {
                sb.append(sender).append(": ").append(body.replace("\n", "\n    ")).append('\n');
            }
            exported++;
        }

        if (exported == 0) {
            return null;
        }
        Transcript out = new Transcript();
        out.text = sb.toString();
        out.exported = exported;
        out.total = total;
        return out;
    }

    private static long senderIdOf(TLRPC.Message message) {
        long senderId = 0;
        if (message.from_id != null) {
            senderId = DialogObject.getPeerDialogId(message.from_id);
        }
        if (senderId == 0 && message.peer_id != null) {
            // Channel posts carry no from_id: the channel itself is the author.
            senderId = DialogObject.getPeerDialogId(message.peer_id);
        }
        return senderId;
    }

    /**
     * Body of a normal message. With includeMedia off this is the plain text only, so a
     * pure-media message contributes nothing (an empty return skips the whole line).
     */
    private static String messageBody(TLRPC.Message message, boolean includeMedia) {
        String text = message.message == null ? "" : message.message;
        TLRPC.MessageMedia media = MessageObject.getMedia(message);
        boolean hasMedia = media != null
                && !(media instanceof TLRPC.TL_messageMediaEmpty)
                && !(media instanceof TLRPC.TL_messageMediaWebPage);
        if (!hasMedia || !includeMedia) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(mediaLabel(media)).append(']');
        if (!TextUtils.isEmpty(message.attachPath)) {
            // Local path only, and only when this device already downloaded the file.
            // The file itself is NOT copied into the export.
            sb.append(' ').append(message.attachPath);
        }
        if (!TextUtils.isEmpty(text)) {
            sb.append(' ').append(text);
        }
        return sb.toString();
    }

    private static String mediaLabel(TLRPC.MessageMedia media) {
        if (media instanceof TLRPC.TL_messageMediaPhoto) {
            return LocaleController.getString(R.string.AttachPhoto);
        }
        if (media instanceof TLRPC.TL_messageMediaGeo || media instanceof TLRPC.TL_messageMediaGeoLive
                || media instanceof TLRPC.TL_messageMediaVenue) {
            return LocaleController.getString(R.string.AttachLocation);
        }
        if (media instanceof TLRPC.TL_messageMediaContact) {
            String name = ((media.first_name == null ? "" : media.first_name) + " "
                    + (media.last_name == null ? "" : media.last_name)).trim();
            String label = LocaleController.getString(R.string.AttachContact);
            return TextUtils.isEmpty(name) ? label : label + ": " + name;
        }
        if (media instanceof TLRPC.TL_messageMediaPoll) {
            return LocaleController.getString(R.string.Poll);
        }
        if (media instanceof TLRPC.TL_messageMediaDocument) {
            TLRPC.Document document = media.document;
            if (document == null) {
                return LocaleController.getString(R.string.AttachDocument);
            }
            if (MessageObject.isVoiceDocument(document)) {
                return LocaleController.getString(R.string.AttachAudio);
            }
            if (MessageObject.isStickerDocument(document)) {
                return LocaleController.getString(R.string.AttachSticker);
            }
            if (MessageObject.isGifDocument(document)) {
                return LocaleController.getString(R.string.AttachGif);
            }
            if (MessageObject.isMusicDocument(document)) {
                return LocaleController.getString(R.string.AttachMusic);
            }
            if (MessageObject.isVideoDocument(document)) {
                return LocaleController.getString(R.string.AttachVideo);
            }
            String fileName = FileLoader.getDocumentFileName(document);
            String label = LocaleController.getString(R.string.AttachDocument);
            return TextUtils.isEmpty(fileName) ? label : label + ": " + fileName;
        }
        return LocaleController.getString(R.string.AttachDocument);
    }

    /**
     * Short descriptor for a service message. Deliberately plain ASCII rather than the
     * localized ChatActionCell text: building a MessageObject here would drag layout and
     * Theme work onto the storage queue for a line that is metadata, not conversation.
     */
    private static String serviceLabel(TLRPC.Message message) {
        TLRPC.MessageAction action = message.action;
        if (action == null) {
            return "service message";
        }
        if (action instanceof TLRPC.TL_messageActionChatAddUser) {
            return "added a member";
        }
        if (action instanceof TLRPC.TL_messageActionChatJoinedByLink) {
            return "joined via invite link";
        }
        if (action instanceof TLRPC.TL_messageActionChatDeleteUser) {
            return "removed a member";
        }
        if (action instanceof TLRPC.TL_messageActionPinMessage) {
            return "pinned a message";
        }
        if (action instanceof TLRPC.TL_messageActionChatCreate) {
            return "created the group";
        }
        if (action instanceof TLRPC.TL_messageActionChatEditTitle) {
            return "changed the title to " + (action.title == null ? "" : action.title);
        }
        if (action instanceof TLRPC.TL_messageActionChatEditPhoto) {
            return "changed the photo";
        }
        if (action instanceof TLRPC.TL_messageActionChatDeletePhoto) {
            return "removed the photo";
        }
        if (action instanceof TLRPC.TL_messageActionHistoryClear) {
            return "cleared the history";
        }
        if (action instanceof TLRPC.TL_messageActionScreenshotTaken) {
            return "took a screenshot";
        }
        if (action instanceof TLRPC.TL_messageActionPhoneCall) {
            return "call";
        }
        if (action instanceof TLRPC.TL_messageActionSetMessagesTTL) {
            return "changed the auto-delete timer";
        }
        return "service message";
    }

    /** Back on the UI thread: encrypt, write, report. */
    private void finishExport(Transcript transcript, String passphrase) {
        exporting = false;
        if (getParentActivity() == null) {
            return;
        }
        if (transcript == null || TextUtils.isEmpty(transcript.text)) {
            showMessage(R.string.LuminaChatExportTitle, LuminaLocale.getString(R.string.LuminaChatExportEmpty));
            return;
        }
        File outFile = null;
        try {
            byte[] plaintext = transcript.text.getBytes(StandardCharsets.UTF_8);
            org.json.JSONObject env = LuminaCrypto.seal(MAGIC, plaintext, passphrase);

            File dir = FileLoader.getDirectory(FileLoader.MEDIA_DIR_DOCUMENT);
            if (dir == null) {
                dir = FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE);
            }
            if (dir == null) {
                throw new java.io.IOException("no writable directory");
            }
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String day = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
            outFile = new File(dir, "LuminaGram-chat-" + dialogId + "-" + day + ".lgx");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(outFile);
                fos.write(env.toString().getBytes(StandardCharsets.UTF_8));
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
            if (outFile != null && outFile.exists()) {
                outFile.delete(); // never leave a half-written archive behind
            }
            showMessage(R.string.LuminaChatExportTitle, LuminaLocale.getString(R.string.LuminaChatExportFailed));
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append(outFile.getAbsolutePath()).append("\n\n");
        message.append(transcript.exported).append(" / ").append(transcript.total);
        if (transcript.total > MAX_MESSAGES) {
            message.append(" (").append(MAX_MESSAGES).append(" max)");
        }
        showMessage(R.string.LuminaChatExportDone, message.toString());
    }

    // ---- Dialog helpers (same shape as LuminaBackupActivity) ----

    private void promptPassphrase(int titleRes, PassphraseCallback callback) {
        final Context context = getParentActivity();
        if (context == null) {
            return;
        }
        final EditTextBoldCursor edit = new EditTextBoldCursor(context);
        edit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        edit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setHintText(LuminaLocale.getString(R.string.LuminaBackupPassphraseHint));
        edit.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        edit.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setCursorSize(AndroidUtilities.dp(20));
        edit.setCursorWidth(1.5f);
        edit.setBackgroundDrawable(null);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edit.setSingleLine(true);

        final FrameLayout container = new FrameLayout(context);
        container.addView(edit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL, 24, 6, 24, 0));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LuminaLocale.getString(titleRes));
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            String passphrase = edit.getText().toString();
            AndroidUtilities.hideKeyboard(edit);
            if (passphrase.length() < LuminaCrypto.MIN_PASSPHRASE_LENGTH) {
                showMessage(R.string.LuminaChatExportTitle,
                        LuminaLocale.getString(R.string.LuminaBackupPassphraseTooShort));
                return;
            }
            callback.onPassphrase(passphrase);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showMessage(int titleRes, CharSequence message) {
        final Context context = getParentActivity();
        if (context == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LuminaLocale.getString(titleRes));
        builder.setMessage(message);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        showDialog(builder.create());
    }
}
