package org.telegram.messenger;

import static org.telegram.messenger.AndroidUtilities.isInAirplaneMode;
import static org.telegram.ui.PremiumPreviewFragment.applyNewSpan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.view.ViewGroup;

import androidx.core.content.FileProvider;

import org.json.JSONObject;
import org.telegram.messenger.web.BuildConfig;
import org.telegram.messenger.web.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TL_smsjobs;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.UpdateAppAlertDialog;
import org.telegram.ui.Components.UpdateLayout;
import org.telegram.ui.IUpdateLayout;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.SMSStatsActivity;
import org.telegram.ui.SMSSubscribeSheet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApplicationLoaderImpl extends ApplicationLoader {

    // --- LuminaGram in-app self-updater (R2 version.json -> download -> install) ---
    private static final String LUMINA_UPDATE_MANIFEST_URL = "https://pub-d6a54d2e5f5947e2b0b23fb8e27ce0a5.r2.dev/version.json";

    private volatile BetaUpdate pendingUpdate;
    private volatile String pendingUpdateUrl;
    private volatile File downloadedApk;
    private volatile boolean downloadingUpdate;
    private volatile boolean cancelUpdateDownload;
    private volatile float updateDownloadProgress;
    private long lastUpdateCheckTime;
    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }

    @Override
    protected boolean isStandalone() {
        return true;
    }

    @Override
    protected void startAppCenterInternal(Activity context) {

    }

    @Override
    protected void checkForUpdatesInternal() {

    }

    protected void appCenterLogInternal(Throwable e) {

    }

    protected void logDualCameraInternal(boolean success, boolean vendor) {

    }

    @Override
    public boolean checkApkInstallPermissions(final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ApplicationLoader.applicationContext.getPackageManager().canRequestPackageInstalls()) {
            AlertsCreator.createApkRestrictedDialog(context, null).show();
            return false;
        }
        return true;
    }

    @Override
    public boolean openApkInstall(Activity activity, TLRPC.Document document) {
        boolean exists = false;
        try {
            String fileName = FileLoader.getAttachFileName(document);
            File f = FileLoader.getInstance(UserConfig.selectedAccount).getPathToAttach(document, true);
            if (exists = f.exists()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (Build.VERSION.SDK_INT >= 24) {
                    intent.setDataAndType(FileProvider.getUriForFile(activity, ApplicationLoader.getApplicationId() + ".provider", f), "application/vnd.android.package-archive");
                } else {
                    intent.setDataAndType(Uri.fromFile(f), "application/vnd.android.package-archive");
                }
                try {
                    activity.startActivityForResult(intent, 500);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return exists;
    }

    @Override
    public boolean showUpdateAppPopup(Context context, TLRPC.TL_help_appUpdate update, int account) {
        try {
            (new UpdateAppAlertDialog(context, update, account)).show();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return true;
    }

    @Override
    public IUpdateLayout takeUpdateLayout(Activity activity, ViewGroup sideMenuContainer) {
        return new UpdateLayout(activity, sideMenuContainer);
    }

    @Override
    public TLRPC.Update parseTLUpdate(int constructor) {
        if (constructor == TL_smsjobs.TL_updateSmsJob.constructor) {
            return new TL_smsjobs.TL_updateSmsJob();
        }
        return super.parseTLUpdate(constructor);
    }

    @Override
    public void processUpdate(int currentAccount, TLRPC.Update update) {
        if (update instanceof TL_smsjobs.TL_updateSmsJob) {
            SMSJobController.getInstance(currentAccount).processJobUpdate(((TL_smsjobs.TL_updateSmsJob) update).job_id);
        }
    }

    @Override
    public void addItemOptions(ItemOptions itemOptions) {
        if (SMSJobController.getInstance(UserConfig.selectedAccount).isAvailable()) {
            CharSequence text = LocaleController.getString(R.string.SmsJobsMenu);
            if (MessagesController.getGlobalMainSettings().getBoolean("newppsms", true)) {
                text = applyNewSpan(text.toString());
            }
            boolean withError = isInAirplaneMode(LaunchActivity.instance) || SMSJobController.getInstance(UserConfig.selectedAccount).hasError();
            itemOptions.add(R.drawable.left_sms, text, withError, () -> {
                MessagesController.getGlobalMainSettings().edit().putBoolean("newppsms", false).apply();
                SMSJobController controller = (SMSJobController) SMSJobController.getInstance(UserConfig.selectedAccount);
                final int state = controller.currentState;
                if (state == SMSJobController.STATE_NONE) {
                    SMSSubscribeSheet.show(LaunchActivity.instance, SMSJobController.getInstance(UserConfig.selectedAccount).isEligible, null, null);
                    return;
                } else if (state == SMSJobController.STATE_NO_SIM) {
                    controller.checkSelectedSIMCard();
                    if (controller.getSelectedSIM() == null) {
                        new AlertDialog.Builder(LaunchActivity.instance)
                                .setTitle(LocaleController.getString(R.string.SmsNoSimTitle))
                                .setMessage(AndroidUtilities.replaceTags(LocaleController.getString(R.string.SmsNoSimMessage)))
                                .setPositiveButton(LocaleController.getString(R.string.OK), null)
                                .show();
                        return;
                    }
                } else if (state == SMSJobController.STATE_ASKING_PERMISSION) {
                    SMSSubscribeSheet.requestSMSPermissions(LaunchActivity.instance, () -> {
                        controller.checkSelectedSIMCard();
                        if (controller.getSelectedSIM() == null) {
                            controller.setState(SMSJobController.STATE_NO_SIM);
                            new AlertDialog.Builder(LaunchActivity.instance)
                                    .setTitle(LocaleController.getString(R.string.SmsNoSimTitle))
                                    .setMessage(AndroidUtilities.replaceTags(LocaleController.getString(R.string.SmsNoSimMessage)))
                                    .setPositiveButton(LocaleController.getString(R.string.OK), null)
                                    .show();
                            return;
                        }
                        ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(new TL_smsjobs.TL_smsjobs_join(), (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                            if (err != null) {
                                BulletinFactory.showError(err);
                            } else if (res instanceof TLRPC.TL_boolFalse) {
                                BulletinFactory.global().createErrorBulletin(LocaleController.getString(R.string.UnknownError)).show();
                            } else {
                                controller.setState(SMSJobController.STATE_JOINED);
                                controller.loadStatus(true);
                                SMSSubscribeSheet.showSubscribed(LaunchActivity.instance, null);
                                BaseFragment lastFragment = LaunchActivity.getLastFragment();
                                if (lastFragment != null) {
                                    lastFragment.presentFragment(new SMSStatsActivity());
                                }
                            }
                        }));
                    }, false);
                    return;
                }
                BaseFragment lastFragment = LaunchActivity.getLastFragment();
                if (lastFragment != null) {
                    lastFragment.presentFragment(new SMSStatsActivity());
                }
            });
        }
    }

    @Override
    public boolean checkRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (SMSSubscribeSheet.checkSMSPermissions(requestCode, permissions, grantResults)) {
            return true;
        }
        return super.checkRequestPermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onSuggestionFill(String suggestion, CharSequence[] output, boolean[] closeable) {
        if (suggestion == null && SMSJobController.getInstance(UserConfig.selectedAccount).hasError()) {
            output[0] = new SpannableStringBuilder().append(SMSStatsActivity.error(17)).append("  ").append(LocaleController.getString(R.string.SmsJobsErrorHintTitle));
            output[1] = LocaleController.getString(R.string.SmsJobsErrorHintMessage);
            closeable[0] = false;
            return true;
        }
        if ("PREMIUM_SMSJOBS".equals(suggestion) && SMSJobController.getInstance(UserConfig.selectedAccount).currentState != SMSJobController.STATE_JOINED) {
            output[0] = LocaleController.getString(R.string.SmsJobsPremiumHintTitle);
            output[1] = LocaleController.getString(R.string.SmsJobsPremiumHintMessage);
            closeable[0] = true;
            return true;
        }
        return super.onSuggestionFill(suggestion, output, closeable);
    }

    @Override
    public boolean onSuggestionClick(String suggestion) {
        if (suggestion == null) {
            BaseFragment lastFragment = LaunchActivity.getLastFragment();
            if (lastFragment != null) {
                SMSJobController.getInstance(UserConfig.selectedAccount).seenError();
                SMSStatsActivity fragment = new SMSStatsActivity();
                lastFragment.presentFragment(fragment);
                AndroidUtilities.runOnUIThread(() -> {
                    fragment.showDialog(new SMSStatsActivity.SMSHistorySheet(fragment));
                }, 800);
            }
            return true;
        } else if ("PREMIUM_SMSJOBS".equals(suggestion)) {
            SMSJobController controller = SMSJobController.getInstance(UserConfig.selectedAccount);
            if (controller.isEligible != null) {
                SMSSubscribeSheet.show(LaunchActivity.instance, controller.isEligible, null, null);
            } else {
                controller.checkIsEligible(true, isEligible -> {
                    if (isEligible == null) {
                        MessagesController.getInstance(UserConfig.selectedAccount).removeSuggestion(0, "PREMIUM_SMSJOBS");
                        return;
                    }
                    SMSSubscribeSheet.show(LaunchActivity.instance, isEligible, null, null);
                });
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean consumePush(int account, JSONObject json) {
        try {
            if (json != null && "SMSJOB".equals(json.getString("loc_key"))) {
                JSONObject custom = json.getJSONObject("custom");
                String job_id = custom.getString("job_id");
                SMSJobController.getInstance(UserConfig.selectedAccount).processJobUpdate(job_id);
                return true;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return false;
    }

    @Override
    public boolean onPause() {
        super.onPause();
        return SMSJobsNotification.check();
    }

    @Override
    public void onResume() {
        super.onResume();
        SMSJobsNotification.check();
    }

    @Override
    public BaseFragment openSettings(int n) {
        if (n == 13) {
            if (SMSJobController.getInstance(UserConfig.selectedAccount).getState() == SMSJobController.STATE_JOINED) {
                return new SMSStatsActivity();
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------
    // LuminaGram custom self-updater. Activates the scaffold already wired into
    // LaunchActivity.checkAppUpdate() by returning true from isCustomUpdate(). Reads a
    // JSON manifest on Cloudflare R2, compares versions, downloads a newer APK over
    // HTTPS and hands it to the system installer via AndroidUtilities.openForView().
    // ---------------------------------------------------------------------------------

    @Override
    public boolean isCustomUpdate() {
        return true;
    }

    @Override
    public BetaUpdate getUpdate() {
        return pendingUpdate;
    }

    @Override
    public void checkUpdate(boolean force, Runnable whenDone) {
        // Rate-limit background (non-forced) checks so the resume hook doesn't hit the
        // network every time; manual "Check for updates" passes force=true.
        if (!force && System.currentTimeMillis() - lastUpdateCheckTime < 60L * 60L * 1000L) {
            if (whenDone != null) {
                AndroidUtilities.runOnUIThread(whenDone);
            }
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            BetaUpdate parsed = null;
            String parsedUrl = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(LUMINA_UPDATE_MANIFEST_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                int statusCode = connection.getResponseCode();
                InputStream stream = (statusCode >= 200 && statusCode < 300) ? connection.getInputStream() : connection.getErrorStream();
                StringBuilder body = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(body.toString());
                String versionName = json.getString("versionName");
                int versionCode = json.getInt("versionCode");
                String notes = json.optString("notes", null);
                String apkUrl = json.getString("url");

                // Compare against the installed build. The standalone flavor overrides the
                // versionCode as base*10+abi (see TMessagesProj_AppStandalone/build.gradle),
                // and the manifest carries the base code, so divide the installed code by 10.
                PackageInfo packageInfo = ApplicationLoader.applicationContext.getPackageManager()
                        .getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
                int installedBaseCode = packageInfo.versionCode / 10;
                String installedName = packageInfo.versionName;

                boolean newer = versionCode > installedBaseCode
                        || (versionCode == installedBaseCode
                            && SharedConfig.versionBiggerOrEqual(versionName, installedName)
                            && !versionName.equals(installedName));
                if (newer) {
                    parsed = new BetaUpdate(versionName, versionCode, notes);
                    parsedUrl = apkUrl;
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (connection != null) {
                    try {
                        connection.disconnect();
                    } catch (Exception ignore) {
                    }
                }
            }
            lastUpdateCheckTime = System.currentTimeMillis();
            final BetaUpdate result = parsed;
            final String resultUrl = parsedUrl;
            AndroidUtilities.runOnUIThread(() -> {
                if (result != null) {
                    pendingUpdate = result;
                    pendingUpdateUrl = resultUrl;
                }
                if (whenDone != null) {
                    whenDone.run();
                }
            });
        });
    }

    @Override
    public void downloadUpdate() {
        startDownload(null, null);
    }

    @Override
    public void cancelDownloadingUpdate() {
        cancelUpdateDownload = true;
    }

    @Override
    public boolean isDownloadingUpdate() {
        return downloadingUpdate;
    }

    @Override
    public float getDownloadingUpdateProgress() {
        return updateDownloadProgress;
    }

    @Override
    public File getDownloadedUpdateFile() {
        return downloadedApk;
    }

    private void startDownload(Utilities.Callback<Float> onProgress, Utilities.Callback<File> onComplete) {
        if (downloadingUpdate) {
            return;
        }
        final String downloadUrl = pendingUpdateUrl;
        if (downloadUrl == null || pendingUpdate == null) {
            if (onComplete != null) {
                AndroidUtilities.runOnUIThread(() -> onComplete.run(null));
            }
            return;
        }
        if (downloadedApk != null && downloadedApk.exists()) {
            if (onComplete != null) {
                final File cached = downloadedApk;
                AndroidUtilities.runOnUIThread(() -> onComplete.run(cached));
            }
            return;
        }
        downloadingUpdate = true;
        cancelUpdateDownload = false;
        updateDownloadProgress = 0f;
        Utilities.globalQueue.postRunnable(() -> {
            File result = null;
            HttpURLConnection connection = null;
            try {
                File out = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), "LuminaGram-update.apk");
                connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setInstanceFollowRedirects(true);
                connection.connect();
                int total = connection.getContentLength();
                InputStream in = connection.getInputStream();
                FileOutputStream fos = new FileOutputStream(out);
                byte[] buffer = new byte[16 * 1024];
                long downloaded = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (cancelUpdateDownload) {
                        break;
                    }
                    fos.write(buffer, 0, read);
                    downloaded += read;
                    if (total > 0) {
                        final float progress = Math.min(1f, (float) downloaded / (float) total);
                        updateDownloadProgress = progress;
                        if (onProgress != null) {
                            AndroidUtilities.runOnUIThread(() -> onProgress.run(progress));
                        }
                    }
                }
                fos.flush();
                fos.close();
                in.close();
                if (!cancelUpdateDownload) {
                    result = out;
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (connection != null) {
                    try {
                        connection.disconnect();
                    } catch (Exception ignore) {
                    }
                }
                downloadingUpdate = false;
            }
            final File finalResult = result;
            downloadedApk = finalResult;
            if (onComplete != null) {
                AndroidUtilities.runOnUIThread(() -> onComplete.run(finalResult));
            }
        });
    }

    @Override
    public boolean showCustomUpdateAppPopup(Context context, BetaUpdate update, int account) {
        if (context == null || update == null) {
            return false;
        }
        AndroidUtilities.runOnUIThread(() -> {
            try {
                final Activity activity = (context instanceof Activity) ? (Activity) context : LaunchActivity.instance;
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(LuminaLocale.getString(R.string.LuminaUpdateAvailable));
                StringBuilder message = new StringBuilder();
                message.append(LuminaLocale.getString(R.string.LuminaUpdateNewVersion)).append(' ').append(update.version);
                if (update.changelog != null && update.changelog.length() > 0) {
                    message.append("\n\n").append(update.changelog);
                }
                builder.setMessage(message.toString());
                builder.setPositiveButton(LuminaLocale.getString(R.string.LuminaUpdateNow), (dialog, which) -> {
                    if (activity == null) {
                        return;
                    }
                    if (!checkApkInstallPermissions(activity)) {
                        return;
                    }
                    beginDownloadAndInstall(activity);
                });
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                builder.show();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
        return true;
    }

    private void beginDownloadAndInstall(final Activity activity) {
        if (downloadedApk != null && downloadedApk.exists()) {
            installApk(activity, downloadedApk);
            return;
        }
        final AlertDialog progressDialog = new AlertDialog(activity, AlertDialog.ALERT_TYPE_LOADING);
        progressDialog.setCanCancel(true);
        progressDialog.setMessage(LuminaLocale.getString(R.string.LuminaUpdateDownloading));
        progressDialog.setOnCancelListener(dialog -> cancelDownloadingUpdate());
        progressDialog.show();
        startDownload(
                progress -> progressDialog.setProgress((int) (progress * 100)),
                file -> {
                    try {
                        progressDialog.dismiss();
                    } catch (Exception ignore) {
                    }
                    if (file != null) {
                        installApk(activity, file);
                    } else if (!cancelUpdateDownload) {
                        BaseFragment fragment = LaunchActivity.getLastFragment();
                        if (fragment != null) {
                            BulletinFactory.of(fragment).createErrorBulletin(LuminaLocale.getString(R.string.LuminaUpdateFailed)).show();
                        }
                    }
                }
        );
    }

    private void installApk(Activity activity, File apk) {
        AndroidUtilities.openForView(apk, "LuminaGram.apk", "application/vnd.android.package-archive", activity, null, false);
    }
}
