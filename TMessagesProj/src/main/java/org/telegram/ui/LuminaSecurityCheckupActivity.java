package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LuminaLocale;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

/**
 * LuminaSecurityCheckupActivity — read-only account security checkup.
 *
 * A single scrollable list that reports the state of a few key account-security
 * settings and deep-links to the OFFICIAL Telegram page that owns each one. This
 * page never changes anything itself: every row just opens the real settings
 * screen (two-step verification, privacy, linked devices) so the user makes the
 * change there. Status is read through official APIs only:
 *
 *   two-step verification / recovery email  -> account.getPassword
 *                                              (Password.has_password / has_recovery)
 *   who can add me to groups / call me /
 *   see my phone number                     -> ContactsController privacy rules
 *                                              (PrivacySettingsActivity.formatRulesString)
 *   active sessions (devices)               -> account.getAuthorizations (device count)
 *
 * Anything we cannot read yet shows "Tap to view" rather than a guessed value, and
 * a slow or failed network never blocks the page or crashes it — the summary simply
 * keeps saying "checking" until real data arrives.
 */
public class LuminaSecurityCheckupActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int ID_2FA = 1;
    private static final int ID_RECOVERY_EMAIL = 2;
    private static final int ID_PRIVACY_GROUPS = 3;
    private static final int ID_PRIVACY_CALLS = 4;
    private static final int ID_PRIVACY_PHONE = 5;
    private static final int ID_SESSIONS = 6;

    private UniversalRecyclerView listView;

    // account.getPassword result: null until loaded. passwordLoaded is set true only on a real
    // response, so a slow/failed request never makes us claim 2FA is off when we do not know yet.
    private TL_account.Password currentPassword;
    private boolean passwordLoaded;

    // account.getAuthorizations device count: -1 = unknown (not loaded / failed).
    private int sessionCount = -1;

    @Override
    public boolean onFragmentCreate() {
        getNotificationCenter().addObserver(this, NotificationCenter.privacyRulesUpdated);
        loadPasswordSettings();
        loadSessionCount();
        // Kick the official privacy-rules load so the "who can..." rows can fill in; the
        // privacyRulesUpdated notification refreshes the list when they arrive.
        getContactsController().loadPrivacySettings();
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.privacyRulesUpdated);
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaSecCheckupTitle));
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
        // Top summary line.
        items.add(UItem.asShadow(summaryText()));

        // --- Two-step verification (account.getPassword) ---
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaSecCheckup2FAHeader)));
        items.add(UItem.asButton(ID_2FA,
                LuminaLocale.getString(R.string.LuminaSecCheckup2FA), twoFaValue()));
        items.add(UItem.asButton(ID_RECOVERY_EMAIL,
                LuminaLocale.getString(R.string.LuminaSecCheckupRecoveryEmail), recoveryEmailValue()));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaSecCheckup2FAInfo)));

        // --- Privacy (ContactsController privacy rules) ---
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaSecCheckupPrivacyHeader)));
        items.add(UItem.asButton(ID_PRIVACY_GROUPS,
                LuminaLocale.getString(R.string.LuminaSecCheckupWhoCanAddGroups),
                privacyValue(ContactsController.PRIVACY_RULES_TYPE_INVITE)));
        items.add(UItem.asButton(ID_PRIVACY_CALLS,
                LuminaLocale.getString(R.string.LuminaSecCheckupWhoCanCall),
                privacyValue(ContactsController.PRIVACY_RULES_TYPE_CALLS)));
        items.add(UItem.asButton(ID_PRIVACY_PHONE,
                LuminaLocale.getString(R.string.LuminaSecCheckupWhoCanSeePhone),
                privacyValue(ContactsController.PRIVACY_RULES_TYPE_PHONE)));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaSecCheckupPrivacyInfo)));

        // --- Active sessions (account.getAuthorizations) ---
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaSecCheckupSessionsHeader)));
        items.add(UItem.asButton(ID_SESSIONS,
                LuminaLocale.getString(R.string.LuminaSecCheckupSessions), sessionsValue()));
        items.add(UItem.asShadow(LuminaLocale.getString(R.string.LuminaSecCheckupSessionsInfo)));
    }

    /**
     * One-line summary at the top. Counts only the two things we can read deterministically
     * from account.getPassword (two-step verification + recovery email). Until that request
     * has actually returned we stay on the neutral "checking" line rather than guess.
     */
    private CharSequence summaryText() {
        if (!passwordLoaded || currentPassword == null) {
            return LuminaLocale.getString(R.string.LuminaSecCheckupSummaryChecking);
        }
        int issues = 0;
        if (!currentPassword.has_password) {
            issues++;
        }
        if (!currentPassword.has_recovery) {
            issues++;
        }
        if (issues == 0) {
            return LuminaLocale.getString(R.string.LuminaSecCheckupSummaryAllGood);
        }
        return String.format(LuminaLocale.getString(R.string.LuminaSecCheckupSummaryIssues), issues);
    }

    private CharSequence twoFaValue() {
        if (!passwordLoaded || currentPassword == null) {
            return LuminaLocale.getString(R.string.LuminaSecCheckupValueUnknown);
        }
        return LuminaLocale.getString(currentPassword.has_password
                ? R.string.LuminaSecCheckupValueOn
                : R.string.LuminaSecCheckupValueOff);
    }

    private CharSequence recoveryEmailValue() {
        if (!passwordLoaded || currentPassword == null) {
            return LuminaLocale.getString(R.string.LuminaSecCheckupValueUnknown);
        }
        return LuminaLocale.getString(currentPassword.has_recovery
                ? R.string.LuminaSecCheckupValueSet
                : R.string.LuminaSecCheckupValueNotSet);
    }

    private CharSequence privacyValue(int rulesType) {
        // Only report a value once the official rules for this type are actually loaded;
        // otherwise formatRulesString would fall back to a misleading "Nobody".
        if (getContactsController().getPrivacyRules(rulesType) == null) {
            return LuminaLocale.getString(R.string.LuminaSecCheckupValueUnknown);
        }
        return PrivacySettingsActivity.formatRulesString(getAccountInstance(), rulesType);
    }

    private CharSequence sessionsValue() {
        if (sessionCount < 0) {
            return LuminaLocale.getString(R.string.LuminaSecCheckupValueUnknown);
        }
        return String.valueOf(sessionCount);
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        // Read-only: every row just deep-links to the OFFICIAL page that owns the setting.
        switch (item.id) {
            case ID_2FA:
            case ID_RECOVERY_EMAIL:
                presentFragment(new TwoStepVerificationActivity());
                break;
            case ID_PRIVACY_GROUPS:
            case ID_PRIVACY_CALLS:
            case ID_PRIVACY_PHONE:
                presentFragment(new PrivacySettingsActivity());
                break;
            case ID_SESSIONS:
                presentFragment(new SessionsActivity(SessionsActivity.TYPE_DEVICES));
                break;
        }
    }

    /** account.getPassword — reads has_password / has_recovery. Never blocks the UI. */
    private void loadPasswordSettings() {
        TL_account.getPassword req = new TL_account.getPassword();
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (response instanceof TL_account.Password) {
                currentPassword = (TL_account.Password) response;
                passwordLoaded = true;
            }
            updateList();
        }), ConnectionsManager.RequestFlagFailOnServerErrors | ConnectionsManager.RequestFlagWithoutLogin);
    }

    /** account.getAuthorizations — we only need the number of signed-in devices. */
    private void loadSessionCount() {
        TL_account.getAuthorizations req = new TL_account.getAuthorizations();
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (response instanceof TL_account.authorizations) {
                sessionCount = ((TL_account.authorizations) response).authorizations.size();
            }
            updateList();
        }), ConnectionsManager.RequestFlagFailOnServerErrors);
    }

    private void updateList() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.privacyRulesUpdated) {
            updateList();
        }
    }
}
