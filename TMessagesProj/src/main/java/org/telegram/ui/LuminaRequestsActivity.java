package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LuminaLocale;
import org.telegram.messenger.LuminaRequestInbox;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

/**
 * LuminaRequestsActivity — the "stranger request inbox" screen.
 *
 * Lists the pending stranger requests computed by {@link LuminaRequestInbox} (non-contact 1:1
 * users, no known common group, not yet triaged). Each row is one request; tapping it opens a
 * chooser to Open the chat, Accept it (let it through to the main list), Block, or Report.
 *
 * COMPLIANCE (ToS §1.4): nothing here changes receive / unread / read / typing / online state.
 * "Open chat" is an explicit user choice (it marks read exactly like tapping any chat would);
 * Accept only flips a local display flag; Block and Report reuse Telegram's own
 * {@code blockPeer} / {@code reportSpam} APIs and are only ever fired by an explicit tap here.
 *
 * Launched via {@code presentFragment(new LuminaRequestsActivity())}. The optional
 * {@link #onChanged} callback lets the presenting {@link DialogsActivity} refresh its filtered
 * list and its top banner after a triage action.
 */
public class LuminaRequestsActivity extends BaseFragment {

    private UniversalRecyclerView listView;
    private final ArrayList<Long> requests = new ArrayList<>();

    /** Invoked after any triage action so the caller can refresh its list + banner. */
    public Runnable onChanged;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaRequestInboxTitle));
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

    private void reloadRequests() {
        requests.clear();
        requests.addAll(LuminaRequestInbox.pendingDialogIds(currentAccount));
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        reloadRequests();
        if (requests.isEmpty()) {
            items.add(UItem.asCenterShadow(LuminaLocale.getString(R.string.LuminaRequestInboxEmpty)));
            return;
        }
        for (int i = 0; i < requests.size(); i++) {
            long did = requests.get(i);
            TLRPC.User user = getMessagesController().getUser(did);
            if (user == null) {
                continue;
            }
            String name = UserObject.getUserName(user);
            String username = UserObject.getPublicUsername(user);
            String subtitle = TextUtils.isEmpty(username) ? "" : "@" + username;
            // id == index into the freshly loaded list (kept in sync by adapter.update()).
            items.add(UItem.asButton(i, name, subtitle));
        }
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id < 0 || item.id >= requests.size()) {
            return;
        }
        showActionsFor(requests.get(item.id));
    }

    /** Chooser: Open chat / Accept / Block / Report for a single request. */
    private void showActionsFor(final long did) {
        if (getParentActivity() == null) {
            return;
        }
        TLRPC.User user = getMessagesController().getUser(did);
        if (user == null) {
            return;
        }
        final String name = UserObject.getUserName(user);
        CharSequence[] options = new CharSequence[] {
                LuminaLocale.getString(R.string.LuminaRequestInboxOpen),
                LuminaLocale.getString(R.string.LuminaRequestInboxAccept),
                LuminaLocale.getString(R.string.LuminaRequestInboxBlock),
                LuminaLocale.getString(R.string.LuminaRequestInboxReport),
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(name);
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: openChat(did); break;
                case 1: acceptRequest(did, name); break;
                case 2: confirmBlock(did, name); break;
                case 3: confirmReport(did, name); break;
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    /** Explicit "open" — behaves exactly like tapping the chat anywhere else. */
    private void openChat(long did) {
        Bundle args = new Bundle();
        args.putLong("user_id", did);
        presentFragment(new ChatActivity(args));
    }

    /** Let the request through: it stops being a stranger and returns to the main list. */
    private void acceptRequest(long did, String name) {
        LuminaRequestInbox.allow(did);
        afterTriage();
        try {
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info,
                    LuminaLocale.getString(R.string.LuminaRequestInboxAcceptedToast)).show();
        } catch (Throwable ignore) {
        }
    }

    private void confirmBlock(final long did, final String name) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LuminaLocale.getString(R.string.LuminaRequestInboxBlock));
        builder.setMessage(LocaleController.formatString(R.string.AreYouSureBlockContact2, name));
        builder.setPositiveButton(LuminaLocale.getString(R.string.LuminaRequestInboxBlock), (d, w) -> {
            try {
                getMessagesController().blockPeer(did);
            } catch (Throwable ignore) {
            }
            LuminaRequestInbox.allow(did);
            afterTriage();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        showDialog(dialog);
        TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    private void confirmReport(final long did, final String name) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LuminaLocale.getString(R.string.LuminaRequestInboxReport));
        builder.setMessage(LuminaLocale.getString(R.string.LuminaRequestInboxReportConfirm));
        builder.setPositiveButton(LuminaLocale.getString(R.string.LuminaRequestInboxReport), (d, w) -> {
            TLRPC.User user = getMessagesController().getUser(did);
            try {
                getMessagesController().reportSpam(did, user, null, null, false);
            } catch (Throwable ignore) {
            }
            // Reporting a stranger also blocks them (mirrors Telegram's "Report Spam and Block").
            try {
                getMessagesController().blockPeer(did);
            } catch (Throwable ignore) {
            }
            LuminaRequestInbox.allow(did);
            afterTriage();
            try {
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_info,
                        LuminaLocale.getString(R.string.LuminaRequestInboxReportedToast)).show();
            } catch (Throwable ignore) {
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        showDialog(dialog);
        TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    /** Refresh this screen and let the caller (DialogsActivity) refresh its list + banner. */
    private void afterTriage() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
        if (onChanged != null) {
            try {
                onChanged.run();
            } catch (Throwable ignore) {
            }
        }
    }

    @Override
    public boolean onFragmentCreate() {
        return super.onFragmentCreate();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Requests can be resolved elsewhere (e.g. the peer became a contact); rebuild on return.
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
