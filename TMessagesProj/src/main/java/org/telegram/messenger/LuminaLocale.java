package org.telegram.messenger;

import androidx.annotation.StringRes;

import java.util.HashMap;
import java.util.Map;

/**
 * LuminaLocale — in-code localization for LuminaGram's custom strings.
 *
 * Telegram's in-app language is independent of the Android device locale, and our
 * custom Lumina* strings exist only as base (English) Android resources — they are
 * never present in any langpack, so {@link LocaleController#getString(int)} always
 * falls through to the device-locale resource (English on most devices) even when
 * the user picked e.g. Chinese in-app, producing the reported mixed CN/EN menus.
 *
 * This helper consults an in-code map keyed off the in-app language
 * ({@code LocaleController.getInstance().getCurrentLocaleInfo().getLangCode()}), so
 * our strings always track the user's in-app selection and survive remote-langpack
 * reloads. Languages we do not translate fall back to
 * {@link LocaleController#getString(int)} (English) — identical to today's behavior,
 * so there is no regression for English / device-locale users.
 */
public final class LuminaLocale {

    private LuminaLocale() {}

    private static final Map<String, Map<String, String>> T = new HashMap<>();

    static {
        Map<String, String> zhHans = new HashMap<>();
        zhHans.put("LuminaGramSettings", "LuminaGram 设置");
        zhHans.put("LuminaGramSettingsInfo", "专属功能");
        zhHans.put("LuminaGramChatList", "聊天列表");
        zhHans.put("LuminaHideTabs", "隐藏文件夹标签");
        zhHans.put("LuminaHideStories", "隐藏动态");
        zhHans.put("LuminaCompactChatList", "紧凑聊天列表");
        zhHans.put("LuminaPrivacyTitle", "隐私与隐身");
        zhHans.put("LuminaPrivacyGhostHeader", "幽灵模式");
        zhHans.put("LuminaPrivacySendReadReceipts", "发送已读回执");
        zhHans.put("LuminaPrivacySendReadReceiptsInfo", "关闭后，消息仅对你标记为已读，对方不会看到双勾。");
        zhHans.put("LuminaPrivacySendTyping", "发送输入状态");
        zhHans.put("LuminaPrivacySendTypingInfo", "关闭后，其他人将看不到你的输入、录制或上传状态。");
        zhHans.put("LuminaPrivacySendOnline", "发送在线状态");
        zhHans.put("LuminaPrivacySendOnlineInfo", "关闭后，你在使用应用时始终显示为离线。");
        zhHans.put("LuminaPrivacyProfileHeader", "个人资料");
        zhHans.put("LuminaPrivacyShowRegistrationDate", "显示注册日期");
        zhHans.put("LuminaPrivacyShowRegistrationDateInfo", "在用户资料上显示估算的账号创建日期。该日期为近似值。");
        zhHans.put("ProfileRegistrationDate", "注册日期");
        zhHans.put("LuminaTranslateTitle", "自动翻译");
        zhHans.put("LuminaTranslateHeader", "翻译");
        zhHans.put("LuminaTranslateTo", "翻译为");
        zhHans.put("LuminaChatSettings", "聊天与媒体");
        zhHans.put("LuminaMessageActions", "消息操作");
        zhHans.put("LuminaForwardNoAuthorTitle", "转发时隐藏作者");
        zhHans.put("LuminaForwardNoCaptionTitle", "转发时移除说明文字");
        zhHans.put("LuminaSaveToCloudTitle", "保存到收藏夹");
        zhHans.put("LuminaSelectFromAuthorTitle", "选择该作者的全部消息");
        zhHans.put("LuminaMediaSaving", "媒体");
        zhHans.put("LuminaSaveStickers", "保存贴纸");
        zhHans.put("LuminaForwardNoAuthor", "转发时隐藏作者");
        zhHans.put("LuminaForwardNoCaption", "转发时移除说明文字");
        zhHans.put("LuminaSaveToCloud", "保存到收藏夹");
        zhHans.put("LuminaSelectFromAuthor", "选择该作者的全部消息");
        zhHans.put("LuminaCheckUpdate", "检查更新");
        zhHans.put("LuminaUpdateAvailable", "有可用更新");
        zhHans.put("LuminaUpdateNewVersion", "新版本");
        zhHans.put("LuminaUpdateNow", "立即更新");
        zhHans.put("LuminaUpdateDownloading", "正在下载更新…");
        zhHans.put("LuminaUpdateFailed", "更新下载失败");
        T.put("zh-hans", zhHans);
    }

    /**
     * Localized lookup for a LuminaGram custom string. The key is derived from the
     * resource id, so call sites change only the class name
     * ({@code LocaleController.getString} → {@code LuminaLocale.getString}).
     * Falls back to {@link LocaleController#getString(int)} (English) for any
     * language or key we do not translate.
     */
    public static String getString(@StringRes int res) {
        Map<String, String> m = mapForCurrentLanguage();
        if (m != null) {
            String key = ApplicationLoader.applicationContext
                    .getResources().getResourceEntryName(res);
            String v = m.get(key);
            if (v != null) {
                return v;
            }
        }
        return LocaleController.getString(res);
    }

    private static Map<String, String> mapForCurrentLanguage() {
        LocaleController.LocaleInfo info = LocaleController.getInstance().getCurrentLocaleInfo();
        if (info == null) {
            return null;
        }
        String c = info.getLangCode() == null ? "" : info.getLangCode().toLowerCase();
        if (c.startsWith("zh")) {
            boolean hant = c.contains("hant") || c.contains("tw") || c.contains("hk") || c.contains("mo");
            // Only Simplified is bundled; Traditional falls back to English via getString().
            return T.get(hant ? "zh-hant" : "zh-hans");
        }
        return null;
    }
}
