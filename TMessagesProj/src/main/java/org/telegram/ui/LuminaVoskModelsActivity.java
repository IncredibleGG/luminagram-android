package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LuminaConfig;
import org.telegram.messenger.LuminaLocale;
import org.telegram.messenger.LuminaVoskModelManager;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.TranslateAlert2;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

/**
 * LuminaGram — offline (Vosk) voice model management.
 *
 * <p>Replaces the old picker, which listed every language the <i>translation</i> provider knows
 * (~100 of them) even though Vosk publishes acoustic models for a few dozen. Picking Zulu there
 * stored "zu" and then silently downloaded the English model, so transcription came back as
 * nonsense. This screen offers only {@link LuminaVoskModelManager#models()} — the hard-coded
 * catalogue taken from Vosk's own model-list.json — and shows, for each one, whether it is
 * installed and how much space it takes, with long press to delete.
 *
 * <p>Sizes are measured off the main thread in {@link #refreshInstalled()} and cached, so
 * {@code fillItems} never walks the model directories while the list is being laid out.
 */
public class LuminaVoskModelsActivity extends BaseFragment {

    private static final int ITEM_MODEL_BASE = 100;

    private UniversalRecyclerView listView;

    /** The catalogue, sorted once by localized display name; row id == BASE + index here. */
    private final ArrayList<LuminaVoskModelManager.VoskModel> models = new ArrayList<>();

    /** lang -> bytes on disk, for installed models only. Refreshed off the main thread. */
    private final HashMap<String, Long> installedSizes = new HashMap<>();

    /** Bytes every installed model occupies, for the footer. */
    private long totalBytes;

    /** Language currently downloading, so a second tap does not start a parallel download. */
    private String downloading;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LuminaLocale.getString(R.string.LuminaSttModelsTitle));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        buildModelList();

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = frameLayout;

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, this::onLongClick);
        listView.setSections();
        actionBar.setAdaptiveBackground(listView);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        refreshInstalled();

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshInstalled();
    }

    private void buildModelList() {
        models.clear();
        LuminaVoskModelManager.VoskModel[] all = LuminaVoskModelManager.models();
        for (int i = 0; i < all.length; ++i) {
            models.add(all[i]);
        }
        try {
            final Locale locale = currentLocale();
            final Collator collator = Collator.getInstance(locale);
            Collections.sort(models, (a, b) -> collator.compare(displayName(a), displayName(b)));
        } catch (Throwable e) {
            // Sorting is cosmetic; the catalogue order (English names) is a fine fallback.
        }
    }

    /**
     * Localized name of the model's language, qualified by the region for the variants Vosk ships
     * separately ("English (United States)" vs "English (India)"). Both halves come from the
     * platform / langpack, so no new translatable strings are needed per language.
     */
    public static String displayName(LuminaVoskModelManager.VoskModel model) {
        if (model == null) {
            return "";
        }
        String name = TranslateAlert2.capitalFirst(TranslateAlert2.languageName(model.iso));
        if (name == null || name.length() == 0) {
            // englishName already spells out the region when there is one.
            return model.englishName;
        }
        if (model.region != null) {
            String region = model.region;
            try {
                String country = new Locale(model.iso, model.region).getDisplayCountry(currentLocale());
                if (country != null && country.length() > 0) {
                    region = country;
                }
            } catch (Throwable ignore) {
            }
            name = name + " (" + region + ")";
        }
        return name;
    }

    private static Locale currentLocale() {
        try {
            Locale locale = LocaleController.getInstance().getCurrentLocale();
            if (locale != null) {
                return locale;
            }
        } catch (Throwable ignore) {
        }
        return Locale.getDefault();
    }

    /** Measure what is on disk without blocking the main thread, then redraw. */
    private void refreshInstalled() {
        Utilities.globalQueue.postRunnable(() -> {
            final HashMap<String, Long> sizes = new HashMap<>();
            final LuminaVoskModelManager.VoskModel[] all = LuminaVoskModelManager.models();
            for (int i = 0; i < all.length; ++i) {
                final long bytes = LuminaVoskModelManager.installedBytes(all[i].lang);
                if (bytes > 0) {
                    sizes.put(all[i].lang, bytes);
                }
            }
            final long total = LuminaVoskModelManager.totalInstalledBytes();
            AndroidUtilities.runOnUIThread(() -> {
                installedSizes.clear();
                installedSizes.putAll(sizes);
                totalBytes = total;
                update();
            });
        });
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LuminaLocale.getString(R.string.LuminaSttModelsTitle)));

        final String selected = LuminaVoskModelManager.selectedLang();
        for (int i = 0; i < models.size(); ++i) {
            final LuminaVoskModelManager.VoskModel model = models.get(i);
            final Long installed = installedSizes.get(model.lang);
            final CharSequence value;
            if (installed != null) {
                value = String.format(LuminaLocale.getString(R.string.LuminaSttModelInstalled),
                        AndroidUtilities.formatFileSize(installed));
            } else {
                value = String.format(LuminaLocale.getString(R.string.LuminaSttModelDownloadSize),
                        AndroidUtilities.formatFileSize(model.downloadBytes));
            }
            items.add(UItem.asRadio(ITEM_MODEL_BASE + i, displayName(model), value)
                    .setChecked(model.lang.equals(selected)));
        }

        String info = LuminaLocale.getString(R.string.LuminaSttModelsInfo);
        if (totalBytes > 0) {
            info = info + "\n\n" + String.format(LuminaLocale.getString(R.string.LuminaSttModelsStorage),
                    AndroidUtilities.formatFileSize(totalBytes));
        }
        items.add(UItem.asShadow(info));
    }

    private LuminaVoskModelManager.VoskModel modelOf(UItem item) {
        if (item == null) {
            return null;
        }
        final int index = item.id - ITEM_MODEL_BASE;
        if (index < 0 || index >= models.size()) {
            return null;
        }
        return models.get(index);
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        final LuminaVoskModelManager.VoskModel model = modelOf(item);
        if (model == null) {
            return;
        }
        // Choosing is instant; downloading only happens when it is not on disk yet.
        LuminaConfig.putString(LuminaVoskModelManager.CONFIG_KEY_LANG, model.lang);
        update();
        if (installedSizes.get(model.lang) == null && !model.lang.equals(downloading)) {
            download(model);
        }
    }

    private boolean onLongClick(UItem item, View view, int position, float x, float y) {
        final LuminaVoskModelManager.VoskModel model = modelOf(item);
        if (model == null) {
            return false;
        }
        final Long installed = installedSizes.get(model.lang);
        if (installed == null) {
            return false;
        }
        confirmDelete(model, installed);
        return true;
    }

    private void confirmDelete(final LuminaVoskModelManager.VoskModel model, final long bytes) {
        final Context context = getParentActivity();
        if (context == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
        builder.setTitle(LuminaLocale.getString(R.string.LuminaSttDeleteModel));
        builder.setMessage(String.format(LuminaLocale.getString(R.string.LuminaSttDeleteModelConfirm),
                displayName(model), AndroidUtilities.formatFileSize(bytes)));
        builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialog, which) ->
                Utilities.globalQueue.postRunnable(() -> {
                    LuminaVoskModelManager.deleteModel(model.lang);
                    AndroidUtilities.runOnUIThread(() -> {
                        refreshInstalled();
                        if (getParentActivity() != null) {
                            BulletinFactory.of(LuminaVoskModelsActivity.this).createSimpleBulletin(
                                    R.raw.ic_delete,
                                    String.format(LuminaLocale.getString(R.string.LuminaSttModelDeleted),
                                            displayName(model))).show();
                        }
                    });
                }));
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.makeRed(AlertDialog.BUTTON_POSITIVE);
        builder.show();
    }

    private void download(final LuminaVoskModelManager.VoskModel model) {
        final Context context = getParentActivity();
        if (context == null) {
            return;
        }
        downloading = model.lang;

        final AlertDialog progress = new AlertDialog(context, AlertDialog.ALERT_TYPE_LOADING, getResourceProvider());
        progress.setTitle(LuminaLocale.getString(R.string.LuminaSttDownloadModel));
        progress.setMessage(LuminaLocale.getString(R.string.LuminaSttUiDownloading));
        progress.setCanCancel(false);
        progress.setProgress(0);
        progress.show();

        LuminaVoskModelManager.ensureModel(model.lang, new LuminaVoskModelManager.ModelCallback() {
            @Override
            public void onReady(final File dir) {
                AndroidUtilities.runOnUIThread(() -> {
                    downloading = null;
                    progress.dismiss();
                    refreshInstalled();
                    if (getParentActivity() != null) {
                        BulletinFactory.of(LuminaVoskModelsActivity.this)
                                .createSimpleBulletin(R.raw.done, displayName(model)).show();
                    }
                });
            }

            @Override
            public void onProgress(final float value) {
                AndroidUtilities.runOnUIThread(() -> {
                    // The manager may report a 0..1 fraction or a 0..100 percent — normalise both.
                    int pct = value <= 1f ? Math.round(value * 100f) : Math.round(value);
                    if (pct < 0) {
                        pct = 0;
                    } else if (pct > 100) {
                        pct = 100;
                    }
                    progress.setProgress(pct);
                });
            }

            @Override
            public void onError(final String message) {
                AndroidUtilities.runOnUIThread(() -> {
                    downloading = null;
                    progress.dismiss();
                    if (getParentActivity() == null) {
                        return;
                    }
                    final String prefix = LuminaLocale.getString(R.string.LuminaSttUiError);
                    final CharSequence text = (message == null || message.length() == 0)
                            ? prefix : (prefix + ": " + message);
                    BulletinFactory.of(LuminaVoskModelsActivity.this).createErrorBulletin(text).show();
                });
            }
        });
    }

    private void update() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
