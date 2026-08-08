package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LuminaConfig;
import org.telegram.messenger.LuminaLocale;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

/**
 * LuminaGram — What's New / version changelog.
 * Shows version history cards. Hub entry id 26 in LuminaGramSettingsActivity.
 * Auto-triggers on first launch after update (compares lastSeenChangelog vs current versionName).
 */
public class LuminaChangelogActivity extends BaseFragment {

    // Changelog entries: newest first. Each entry is {versionName, versionCode, changes[]}.
    // Changes are NOT localized — changelog is always in English (standard practice).
    private static final Object[][] CHANGELOG = {
        {"1.2.7", 7019, new String[]{
            "Drag-to-reorder photos and files before sending",
            "Forward-origin warning label on forwarded messages",
            "Unread digest banner when returning after absence",
        }},
        {"1.2.6", 7018, new String[]{
            "Auto-blur incoming media until you tap to reveal",
            "Screenshot detection alerts in private chats",
            "What's New changelog screen",
        }},
        {"1.2.5", 7017, new String[]{
            "Voice-to-text transcription (Vosk offline + your own cloud key)",
            "Settings page for speech-to-text engines and model downloads",
        }},
        {"1.2.4", 7016, new String[]{
            "Unified vault: password-door mode + decoy app mode",
            "Functional notepad decoy activity",
            "Vault settings redesign (mode/skin/code)",
        }},
        {"1.2.3", 7015, new String[]{
            "7-agent security audit + all fixes applied",
            "Translation mode gate fix (trMode bypass on resume)",
            "Translate-before-send 20s watchdog (prevents stuck sends)",
            "Preview language cache invalidation",
            "Undo-send zero-length guard + persistence",
            "Same-language re-translate prevention",
            "Panic wipe: logout-first + background erase",
            "Fake crash code collision check vs real passcode",
            "Disguise icons hidden from icon selector + premium preview",
        }},
        {"1.2.2", 7014, new String[]{
            "Translate toggle icon in chat title bar",
            "Translation mode: All (auto) vs Manual (per-chat toggle)",
            "Dual-language subtitle time overlap fix",
        }},
        {"1.2.1", 7013, new String[]{
            "Bidirectional translation: separate send/read languages",
            "Auto-detect recipient language for outgoing translation",
            "Send/read language selectors in translate settings",
            "Private/group scope toggles for translation",
        }},
        {"1.2.0", 7012, new String[]{
            "Brand protection: cloud language pack no longer overwrites LuminaGram branding",
            "Dynamic brand replacement in LocaleController",
        }},
        {"1.1.9", 7011, new String[]{
            "Major stability release: decoy vault crash fix",
            "Launcher icon brick prevention",
            "Receive-side translation: premium gate bypass for non-Telegram engines",
            "Send-side original text persistence (dialogId+mid)",
            "In-flight translation lock (no duplicate sends)",
            "Grey stealth features parked for Safe milestone",
        }},
        {"1.1.2", 7004, new String[]{
            "Link safety inspector (anti-phishing URL check)",
            "Fake-crash unlock (duress protection)",
            "Cross-language search (NFKD transliteration)",
            "Private contact notes and tags",
            "Scrollable keyboard dismiss on chat scroll",
            "Adjustable sticker size slider",
        }},
        {"1.0.8", 7000, new String[]{
            "System emoji option",
            "Voice/video send confirmation",
            "Disable number rounding",
            "One-tap unblock all",
            "Interface settings page",
        }},
        {"1.0.5", 6997, new String[]{
            "Quick reply templates",
            "Message bookmarks",
            "Chat list compact mode and list options",
            "Multi-engine incoming message translation",
        }},
        {"1.0.0", 6992, new String[]{
            "Initial release: LuminaGram settings hub",
            "Hide tabs and Stories toggles",
            "In-app updater from R2",
            "10-language localization",
        }},
    };

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaChangelogTitle));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
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
        for (int i = 0; i < CHANGELOG.length; i++) {
            String version = (String) CHANGELOG[i][0];
            int code = (int) CHANGELOG[i][1];
            String[] changes = (String[]) CHANGELOG[i][2];

            items.add(UItem.asHeader("v" + version + " (build " + code + ")"));
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < changes.length; j++) {
                sb.append("• ").append(changes[j]);
                if (j < changes.length - 1) sb.append("\n");
            }
            items.add(UItem.asShadow(sb.toString()));
        }
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        // No clickable items in changelog
    }

    /**
     * Mark the current version as seen. Called after showing the changelog.
     */
    public static void markCurrentVersionSeen() {
        LuminaConfig.putString("lastSeenChangelog", BuildVars.BUILD_VERSION_STRING);
    }

    /**
     * Returns true if the user has NOT seen the changelog for the current version.
     */
    public static boolean hasUnseenChangelog() {
        String lastSeen = LuminaConfig.getString("lastSeenChangelog", "");
        return !BuildVars.BUILD_VERSION_STRING.equals(lastSeen);
    }
}
