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
        // ---- Simplified Chinese ----
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
        zhHans.put("ShowTranslateButton", "显示翻译按钮");
        zhHans.put("LuminaTranslateBeforeSend", "发送前翻译");
        zhHans.put("LuminaTranslateBeforeSendInfo", "开启后，你输入的消息会翻译成目标语言。发送前可以预览译文。");
        zhHans.put("LuminaSendTranslation", "发送译文");
        zhHans.put("LuminaSendOriginal", "发送原文");
        zhHans.put("LuminaTranslateOriginalLabel", "原文");
        zhHans.put("LuminaTranslatePreviewTranslating", "翻译中…");
        zhHans.put("LuminaTranslateBeforeSendConfirm", "发送前二次确认");
        zhHans.put("LuminaTranslateBeforeSendConfirmInfo", "开启后，发送前会显示译文预览，你可以选择发送译文或原文。关闭后，译文将一键直接发送。");
        zhHans.put("LuminaTranslateBeforeSendEnabled", "发送前翻译：开");
        zhHans.put("LuminaTranslateBeforeSendDisabled", "发送前翻译：关");
        zhHans.put("LuminaPrivacySecurityHeader", "安全");
        zhHans.put("LuminaPrivacySecureScreen", "在最近任务中模糊显示");
        zhHans.put("LuminaPrivacySecureScreenInfo", "在任务切换器中隐藏应用内容，并禁止在 LuminaGram 内截屏（对整个应用启用 FLAG_SECURE）。");
        zhHans.put("LuminaPrivacyDisableLinkPreview", "默认禁用链接预览");
        zhHans.put("LuminaPrivacyDisableLinkPreviewInfo", "撰写的新消息默认不带链接预览发送，服务器不会解析粘贴的链接。");
        zhHans.put("LuminaPrivacyStripMetadata", "移除照片位置和元数据");
        zhHans.put("LuminaPrivacyStripMetadataInfo", "从以文件形式发送的照片中移除 GPS 及其他 EXIF 元数据。压缩后的照片本就不含此类数据。");
        zhHans.put("LuminaInfoDensity", "信息密度");
        zhHans.put("LuminaShowDcId", "显示数据中心 ID");
        zhHans.put("LuminaShowDcIdInfo", "在用户、群组和频道资料上显示存储头像的数据中心。同时在消息菜单中添加“详情”操作。");
        zhHans.put("LuminaShowChatDate", "显示创建 / 加入日期");
        zhHans.put("LuminaShowMessageDetails", "消息详情菜单");
        zhHans.put("ProfileDcId", "数据中心");
        zhHans.put("ProfileChatCreated", "创建于");
        zhHans.put("ProfileChatJoined", "加入于");
        zhHans.put("LuminaMessageDetails", "详情");
        zhHans.put("LuminaDetailsDate", "日期");
        zhHans.put("LuminaDetailsMessageId", "消息 ID");
        zhHans.put("LuminaDetailsForwardedFrom", "转发自");
        zhHans.put("LuminaDetailsOriginalDate", "原始日期");
        zhHans.put("LuminaSecurityTitle", "安全");
        zhHans.put("LuminaSecurityPanicHeader", "紧急清除");
        zhHans.put("LuminaSecurityPanicWipe", "紧急清除 (Kaboom)");
        zhHans.put("LuminaSecurityPanicWipeInfo", "立即登出所有账号，并从本设备清除本地聊天和缓存文件，然后返回登录界面。你的账号仍保留在 Telegram 服务器上。此操作无法撤销。");
        zhHans.put("LuminaSecurityPanicConfirmTitle", "紧急清除？");
        zhHans.put("LuminaSecurityPanicConfirmMessage", "所有账号都将被登出，本设备上的所有本地聊天和缓存文件都将被清除。此操作无法撤销。是否继续？");
        zhHans.put("LuminaSecurityPanicConfirmButton", "立即清除");
        zhHans.put("LuminaSecurityDisguiseHeader", "伪装");
        zhHans.put("LuminaSecurityDisguise", "伪装成 Calculator");
        zhHans.put("LuminaSecurityDisguiseInfo", "将应用的启动图标和名称替换为普通的 Calculator。关闭以恢复正常的 LuminaGram 图标。");
        T.put("zh-hans", zhHans);

        // ---- Traditional Chinese ----
        Map<String, String> zhHant = new HashMap<>();
        zhHant.put("LuminaGramSettings", "LuminaGram 設定");
        zhHant.put("LuminaGramSettingsInfo", "專屬功能");
        zhHant.put("LuminaGramChatList", "聊天列表");
        zhHant.put("LuminaHideTabs", "隱藏資料夾分頁");
        zhHant.put("LuminaHideStories", "隱藏限時動態");
        zhHant.put("LuminaCompactChatList", "精簡聊天列表");
        zhHant.put("LuminaPrivacyTitle", "隱私與隱身");
        zhHant.put("LuminaPrivacyGhostHeader", "幽靈模式");
        zhHant.put("LuminaPrivacySendReadReceipts", "傳送已讀回條");
        zhHant.put("LuminaPrivacySendReadReceiptsInfo", "關閉後，訊息僅對你標示為已讀，對方不會看到雙勾。");
        zhHant.put("LuminaPrivacySendTyping", "傳送輸入狀態");
        zhHant.put("LuminaPrivacySendTypingInfo", "關閉後，其他人將看不到你的輸入、錄製或上傳狀態。");
        zhHant.put("LuminaPrivacySendOnline", "傳送上線狀態");
        zhHant.put("LuminaPrivacySendOnlineInfo", "關閉後，你在使用應用程式時將始終顯示為離線。");
        zhHant.put("LuminaPrivacyProfileHeader", "個人檔案");
        zhHant.put("LuminaPrivacyShowRegistrationDate", "顯示註冊日期");
        zhHant.put("LuminaPrivacyShowRegistrationDateInfo", "在使用者個人檔案上顯示估算的帳號建立日期。此日期為近似值。");
        zhHant.put("ProfileRegistrationDate", "註冊日期");
        zhHant.put("LuminaTranslateTitle", "自動翻譯");
        zhHant.put("LuminaTranslateHeader", "翻譯");
        zhHant.put("LuminaTranslateTo", "翻譯成");
        zhHant.put("LuminaChatSettings", "聊天與媒體");
        zhHant.put("LuminaMessageActions", "訊息操作");
        zhHant.put("LuminaForwardNoAuthorTitle", "轉發時隱藏作者");
        zhHant.put("LuminaForwardNoCaptionTitle", "轉發時移除說明文字");
        zhHant.put("LuminaSaveToCloudTitle", "儲存至「儲存的訊息」");
        zhHant.put("LuminaSelectFromAuthorTitle", "選擇該作者的所有訊息");
        zhHant.put("LuminaMediaSaving", "媒體");
        zhHant.put("LuminaSaveStickers", "儲存貼圖");
        zhHant.put("LuminaForwardNoAuthor", "轉發時隱藏作者");
        zhHant.put("LuminaForwardNoCaption", "轉發時移除說明文字");
        zhHant.put("LuminaSaveToCloud", "儲存至「儲存的訊息」");
        zhHant.put("LuminaSelectFromAuthor", "選擇該作者的所有訊息");
        zhHant.put("LuminaCheckUpdate", "檢查更新");
        zhHant.put("LuminaUpdateAvailable", "有可用更新");
        zhHant.put("LuminaUpdateNewVersion", "新版本");
        zhHant.put("LuminaUpdateNow", "立即更新");
        zhHant.put("LuminaUpdateDownloading", "正在下載更新…");
        zhHant.put("LuminaUpdateFailed", "更新下載失敗");
        zhHant.put("ShowTranslateButton", "顯示翻譯按鈕");
        zhHant.put("LuminaTranslateBeforeSend", "傳送前翻譯");
        zhHant.put("LuminaTranslateBeforeSendInfo", "開啟後，你輸入的訊息會翻譯成目標語言。傳送前可以預覽譯文。");
        zhHant.put("LuminaSendTranslation", "傳送譯文");
        zhHant.put("LuminaSendOriginal", "傳送原文");
        zhHant.put("LuminaTranslateOriginalLabel", "原文");
        zhHant.put("LuminaTranslatePreviewTranslating", "翻譯中…");
        zhHant.put("LuminaTranslateBeforeSendConfirm", "傳送前二次確認");
        zhHant.put("LuminaTranslateBeforeSendConfirmInfo", "開啟後，傳送前會顯示譯文預覽，你可以選擇傳送譯文或原文。關閉後，譯文將一鍵直接傳送。");
        zhHant.put("LuminaTranslateBeforeSendEnabled", "傳送前翻譯：開");
        zhHant.put("LuminaTranslateBeforeSendDisabled", "傳送前翻譯：關");
        zhHant.put("LuminaPrivacySecurityHeader", "安全");
        zhHant.put("LuminaPrivacySecureScreen", "在最近工作中模糊顯示");
        zhHant.put("LuminaPrivacySecureScreenInfo", "在工作切換器中隱藏應用程式內容，並禁止在 LuminaGram 內截圖（對整個應用程式啟用 FLAG_SECURE）。");
        zhHant.put("LuminaPrivacyDisableLinkPreview", "預設停用連結預覽");
        zhHant.put("LuminaPrivacyDisableLinkPreviewInfo", "撰寫的新訊息預設不帶連結預覽傳送，伺服器不會解析貼上的連結。");
        zhHant.put("LuminaPrivacyStripMetadata", "移除相片位置與中繼資料");
        zhHant.put("LuminaPrivacyStripMetadataInfo", "從以檔案形式傳送的相片中移除 GPS 及其他 EXIF 中繼資料。壓縮後的相片本就不含此類資料。");
        zhHant.put("LuminaInfoDensity", "資訊密度");
        zhHant.put("LuminaShowDcId", "顯示資料中心 ID");
        zhHant.put("LuminaShowDcIdInfo", "在使用者、群組和頻道個人檔案上顯示儲存大頭貼的資料中心。同時在訊息選單中新增「詳細資訊」操作。");
        zhHant.put("LuminaShowChatDate", "顯示建立 / 加入日期");
        zhHant.put("LuminaShowMessageDetails", "訊息詳細資訊選單");
        zhHant.put("ProfileDcId", "資料中心");
        zhHant.put("ProfileChatCreated", "建立於");
        zhHant.put("ProfileChatJoined", "加入於");
        zhHant.put("LuminaMessageDetails", "詳細資訊");
        zhHant.put("LuminaDetailsDate", "日期");
        zhHant.put("LuminaDetailsMessageId", "訊息 ID");
        zhHant.put("LuminaDetailsForwardedFrom", "轉發自");
        zhHant.put("LuminaDetailsOriginalDate", "原始日期");
        zhHant.put("LuminaSecurityTitle", "安全");
        zhHant.put("LuminaSecurityPanicHeader", "緊急清除");
        zhHant.put("LuminaSecurityPanicWipe", "緊急清除 (Kaboom)");
        zhHant.put("LuminaSecurityPanicWipeInfo", "立即登出所有帳號，並從本裝置清除本地聊天與快取檔案，然後返回登入畫面。你的帳號仍保留在 Telegram 伺服器上。此操作無法復原。");
        zhHant.put("LuminaSecurityPanicConfirmTitle", "緊急清除？");
        zhHant.put("LuminaSecurityPanicConfirmMessage", "所有帳號都將被登出，本裝置上的所有本地聊天與快取檔案都將被清除。此操作無法復原。是否繼續？");
        zhHant.put("LuminaSecurityPanicConfirmButton", "立即清除");
        zhHant.put("LuminaSecurityDisguiseHeader", "偽裝");
        zhHant.put("LuminaSecurityDisguise", "偽裝成 Calculator");
        zhHant.put("LuminaSecurityDisguiseInfo", "將應用程式的啟動圖示與名稱替換為普通的 Calculator。關閉以恢復正常的 LuminaGram 圖示。");
        T.put("zh-hant", zhHant);

        // ---- Arabic (RTL) ----
        Map<String, String> ar = new HashMap<>();
        ar.put("LuminaGramSettings", "إعدادات LuminaGram");
        ar.put("LuminaGramSettingsInfo", "ميزات حصرية");
        ar.put("LuminaGramChatList", "قائمة الدردشات");
        ar.put("LuminaHideTabs", "إخفاء علامات تبويب المجلدات");
        ar.put("LuminaHideStories", "إخفاء القصص");
        ar.put("LuminaCompactChatList", "قائمة دردشات مضغوطة");
        ar.put("LuminaPrivacyTitle", "الخصوصية والتخفّي");
        ar.put("LuminaPrivacyGhostHeader", "الوضع الشبح");
        ar.put("LuminaPrivacySendReadReceipts", "إرسال إيصالات القراءة");
        ar.put("LuminaPrivacySendReadReceiptsInfo", "عند الإيقاف، تُعلَّم الرسائل كمقروءة لك وحدك — لن يرى الآخرون علامة الصح المزدوجة.");
        ar.put("LuminaPrivacySendTyping", "إرسال حالة الكتابة");
        ar.put("LuminaPrivacySendTypingInfo", "عند الإيقاف، لن يرى الآخرون حالة كتابتك أو تسجيلك أو رفعك.");
        ar.put("LuminaPrivacySendOnline", "إرسال حالة الاتصال");
        ar.put("LuminaPrivacySendOnlineInfo", "عند الإيقاف، ستظهر دائمًا دون اتصال أثناء استخدامك للتطبيق.");
        ar.put("LuminaPrivacyProfileHeader", "الملف الشخصي");
        ar.put("LuminaPrivacyShowRegistrationDate", "إظهار تاريخ التسجيل");
        ar.put("LuminaPrivacyShowRegistrationDateInfo", "إظهار تاريخ إنشاء الحساب التقريبي في الملفات الشخصية. التاريخ تقريبي.");
        ar.put("ProfileRegistrationDate", "تاريخ التسجيل");
        ar.put("LuminaTranslateTitle", "الترجمة التلقائية");
        ar.put("LuminaTranslateHeader", "الترجمة");
        ar.put("LuminaTranslateTo", "الترجمة إلى");
        ar.put("LuminaChatSettings", "الدردشة والوسائط");
        ar.put("LuminaMessageActions", "إجراءات الرسائل");
        ar.put("LuminaForwardNoAuthorTitle", "إعادة التوجيه دون المؤلف");
        ar.put("LuminaForwardNoCaptionTitle", "إعادة التوجيه دون التعليق");
        ar.put("LuminaSaveToCloudTitle", "الحفظ في الرسائل المحفوظة");
        ar.put("LuminaSelectFromAuthorTitle", "تحديد كل رسائل المؤلف");
        ar.put("LuminaMediaSaving", "الوسائط");
        ar.put("LuminaSaveStickers", "حفظ الملصقات");
        ar.put("LuminaForwardNoAuthor", "إعادة التوجيه دون المؤلف");
        ar.put("LuminaForwardNoCaption", "إعادة التوجيه دون التعليق");
        ar.put("LuminaSaveToCloud", "الحفظ في الرسائل المحفوظة");
        ar.put("LuminaSelectFromAuthor", "تحديد كل رسائل المؤلف");
        ar.put("LuminaCheckUpdate", "التحقق من التحديثات");
        ar.put("LuminaUpdateAvailable", "يتوفر تحديث");
        ar.put("LuminaUpdateNewVersion", "إصدار جديد");
        ar.put("LuminaUpdateNow", "تحديث");
        ar.put("LuminaUpdateDownloading", "جارٍ تنزيل التحديث…");
        ar.put("LuminaUpdateFailed", "فشل تنزيل التحديث");
        ar.put("ShowTranslateButton", "إظهار زر الترجمة");
        ar.put("LuminaTranslateBeforeSend", "الترجمة قبل الإرسال");
        ar.put("LuminaTranslateBeforeSendInfo", "عند التفعيل، تُترجَم رسالتك المكتوبة إلى اللغة الهدف. يمكنك معاينة الترجمة قبل إرسالها.");
        ar.put("LuminaSendTranslation", "إرسال الترجمة");
        ar.put("LuminaSendOriginal", "إرسال النص الأصلي");
        ar.put("LuminaTranslateOriginalLabel", "النص الأصلي");
        ar.put("LuminaTranslatePreviewTranslating", "جارٍ الترجمة…");
        ar.put("LuminaTranslateBeforeSendConfirm", "التأكيد قبل الإرسال");
        ar.put("LuminaTranslateBeforeSendConfirmInfo", "عند التفعيل، تظهر معاينة للترجمة قبل الإرسال لتختار إرسال الترجمة أو النص الأصلي. عند الإيقاف، تُرسَل الترجمة مباشرةً بنقرة واحدة.");
        ar.put("LuminaTranslateBeforeSendEnabled", "الترجمة قبل الإرسال: تشغيل");
        ar.put("LuminaTranslateBeforeSendDisabled", "الترجمة قبل الإرسال: إيقاف");
        ar.put("LuminaPrivacySecurityHeader", "الأمان");
        ar.put("LuminaPrivacySecureScreen", "تعتيم التطبيق في قائمة التطبيقات الأخيرة");
        ar.put("LuminaPrivacySecureScreenInfo", "يُخفي محتوى التطبيق في مبدّل المهام ويمنع التقاط لقطات الشاشة داخل LuminaGram (يُطبّق FLAG_SECURE على التطبيق بالكامل).");
        ar.put("LuminaPrivacyDisableLinkPreview", "تعطيل معاينة الروابط افتراضيًا");
        ar.put("LuminaPrivacyDisableLinkPreviewInfo", "تُرسل الرسائل الجديدة التي تكتبها افتراضيًا دون معاينة رابط، فلا يُطلب من الخوادم فتح الروابط الملصقة.");
        ar.put("LuminaPrivacyStripMetadata", "إزالة موقع الصورة وبياناتها الوصفية");
        ar.put("LuminaPrivacyStripMetadataInfo", "يزيل بيانات GPS وبيانات EXIF الأخرى من الصور المُرسلة كملفات. الصور المضغوطة لا تتضمّن هذه البيانات أصلاً.");
        ar.put("LuminaInfoDensity", "كثافة المعلومات");
        ar.put("LuminaShowDcId", "إظهار معرّف مركز البيانات");
        ar.put("LuminaShowDcIdInfo", "إظهار مركز البيانات الذي يخزّن صورة الملف الشخصي في ملفات المستخدمين والمجموعات والقنوات. كما يضيف إجراء «التفاصيل» إلى قائمة الرسائل.");
        ar.put("LuminaShowChatDate", "إظهار تاريخ الإنشاء / الانضمام");
        ar.put("LuminaShowMessageDetails", "قائمة تفاصيل الرسالة");
        ar.put("ProfileDcId", "مركز البيانات");
        ar.put("ProfileChatCreated", "أُنشئت في");
        ar.put("ProfileChatJoined", "انضممت في");
        ar.put("LuminaMessageDetails", "التفاصيل");
        ar.put("LuminaDetailsDate", "التاريخ");
        ar.put("LuminaDetailsMessageId", "معرّف الرسالة");
        ar.put("LuminaDetailsForwardedFrom", "أُعيد توجيهها من");
        ar.put("LuminaDetailsOriginalDate", "التاريخ الأصلي");
        ar.put("LuminaSecurityTitle", "الأمان");
        ar.put("LuminaSecurityPanicHeader", "المحو الطارئ");
        ar.put("LuminaSecurityPanicWipe", "المحو الطارئ (Kaboom)");
        ar.put("LuminaSecurityPanicWipeInfo", "يسجّل الخروج فورًا من كل الحسابات ويمحو الدردشات المحلية والملفات المؤقتة من هذا الجهاز، ثم يعود إلى شاشة تسجيل الدخول. تبقى حساباتك على خوادم Telegram. لا يمكن التراجع عن هذا الإجراء.");
        ar.put("LuminaSecurityPanicConfirmTitle", "محو طارئ؟");
        ar.put("LuminaSecurityPanicConfirmMessage", "سيتم تسجيل الخروج من كل الحسابات ومحو كل دردشة محلية وملف مؤقت على هذا الجهاز. لا يمكن التراجع عن هذا. هل تريد المتابعة؟");
        ar.put("LuminaSecurityPanicConfirmButton", "امحُ الآن");
        ar.put("LuminaSecurityDisguiseHeader", "التمويه");
        ar.put("LuminaSecurityDisguise", "التمويه كـ Calculator");
        ar.put("LuminaSecurityDisguiseInfo", "استبدال أيقونة التطبيق واسمه بتطبيق Calculator عادي. أوقفه لاستعادة أيقونة LuminaGram العادية.");
        T.put("ar", ar);

        // ---- Russian ----
        Map<String, String> ru = new HashMap<>();
        ru.put("LuminaGramSettings", "Настройки LuminaGram");
        ru.put("LuminaGramSettingsInfo", "Эксклюзивные функции");
        ru.put("LuminaGramChatList", "Список чатов");
        ru.put("LuminaHideTabs", "Скрыть вкладки папок");
        ru.put("LuminaHideStories", "Скрыть истории");
        ru.put("LuminaCompactChatList", "Компактный список чатов");
        ru.put("LuminaPrivacyTitle", "Конфиденциальность и скрытность");
        ru.put("LuminaPrivacyGhostHeader", "Режим невидимки");
        ru.put("LuminaPrivacySendReadReceipts", "Отправлять отчёты о прочтении");
        ru.put("LuminaPrivacySendReadReceiptsInfo", "Когда выключено, сообщения отмечаются прочитанными только для вас — собеседники не увидят двойную галочку.");
        ru.put("LuminaPrivacySendTyping", "Отправлять статус набора текста");
        ru.put("LuminaPrivacySendTypingInfo", "Когда выключено, другие не увидят, что вы печатаете, записываете или загружаете.");
        ru.put("LuminaPrivacySendOnline", "Отправлять статус «в сети»");
        ru.put("LuminaPrivacySendOnlineInfo", "Когда выключено, вы всегда отображаетесь офлайн, продолжая пользоваться приложением.");
        ru.put("LuminaPrivacyProfileHeader", "Профиль");
        ru.put("LuminaPrivacyShowRegistrationDate", "Показывать дату регистрации");
        ru.put("LuminaPrivacyShowRegistrationDateInfo", "Показывать примерную дату создания аккаунта в профилях. Дата приблизительна.");
        ru.put("ProfileRegistrationDate", "Дата регистрации");
        ru.put("LuminaTranslateTitle", "Автоперевод");
        ru.put("LuminaTranslateHeader", "Перевод");
        ru.put("LuminaTranslateTo", "Переводить на");
        ru.put("LuminaChatSettings", "Чаты и медиа");
        ru.put("LuminaMessageActions", "Действия с сообщениями");
        ru.put("LuminaForwardNoAuthorTitle", "Пересылать без автора");
        ru.put("LuminaForwardNoCaptionTitle", "Пересылать без подписи");
        ru.put("LuminaSaveToCloudTitle", "Сохранить в «Избранное»");
        ru.put("LuminaSelectFromAuthorTitle", "Выбрать все сообщения автора");
        ru.put("LuminaMediaSaving", "Медиа");
        ru.put("LuminaSaveStickers", "Сохранять стикеры");
        ru.put("LuminaForwardNoAuthor", "Пересылать без автора");
        ru.put("LuminaForwardNoCaption", "Пересылать без подписи");
        ru.put("LuminaSaveToCloud", "Сохранить в «Избранное»");
        ru.put("LuminaSelectFromAuthor", "Выбрать все сообщения автора");
        ru.put("LuminaCheckUpdate", "Проверить обновления");
        ru.put("LuminaUpdateAvailable", "Доступно обновление");
        ru.put("LuminaUpdateNewVersion", "Новая версия");
        ru.put("LuminaUpdateNow", "Обновить");
        ru.put("LuminaUpdateDownloading", "Загрузка обновления…");
        ru.put("LuminaUpdateFailed", "Не удалось загрузить обновление");
        ru.put("ShowTranslateButton", "Показывать кнопку перевода");
        ru.put("LuminaTranslateBeforeSend", "Переводить перед отправкой");
        ru.put("LuminaTranslateBeforeSendInfo", "Когда включено, ваше сообщение переводится на выбранный язык. Перевод можно посмотреть перед отправкой.");
        ru.put("LuminaSendTranslation", "Отправить перевод");
        ru.put("LuminaSendOriginal", "Отправить оригинал");
        ru.put("LuminaTranslateOriginalLabel", "Оригинал");
        ru.put("LuminaTranslatePreviewTranslating", "Перевод…");
        ru.put("LuminaTranslateBeforeSendConfirm", "Подтверждать перед отправкой");
        ru.put("LuminaTranslateBeforeSendConfirmInfo", "Когда включено, перед отправкой показывается предпросмотр перевода, чтобы вы могли отправить перевод или оригинал. Когда выключено, перевод отправляется сразу одним касанием.");
        ru.put("LuminaTranslateBeforeSendEnabled", "Перевод перед отправкой: вкл.");
        ru.put("LuminaTranslateBeforeSendDisabled", "Перевод перед отправкой: выкл.");
        ru.put("LuminaPrivacySecurityHeader", "Безопасность");
        ru.put("LuminaPrivacySecureScreen", "Размывать приложение в недавних");
        ru.put("LuminaPrivacySecureScreenInfo", "Скрывает содержимое приложения в списке недавних и блокирует скриншоты в LuminaGram (применяет FLAG_SECURE ко всему приложению).");
        ru.put("LuminaPrivacyDisableLinkPreview", "Отключать предпросмотр ссылок по умолчанию");
        ru.put("LuminaPrivacyDisableLinkPreviewInfo", "Новые сообщения по умолчанию отправляются без предпросмотра ссылок, поэтому серверы не запрашивают вставленные ссылки.");
        ru.put("LuminaPrivacyStripMetadata", "Удалять геоданные и метаданные фото");
        ru.put("LuminaPrivacyStripMetadataInfo", "Удаляет GPS и другие метаданные EXIF из фото, отправляемых как файлы. Сжатые фото и так не содержат этих данных.");
        ru.put("LuminaInfoDensity", "Плотность информации");
        ru.put("LuminaShowDcId", "Показывать ID дата-центра");
        ru.put("LuminaShowDcIdInfo", "Показывать дата-центр, где хранится фото профиля, в профилях пользователей, групп и каналов. Также добавляет действие «Подробности» в меню сообщения.");
        ru.put("LuminaShowChatDate", "Показывать дату создания / вступления");
        ru.put("LuminaShowMessageDetails", "Меню «Подробности» сообщения");
        ru.put("ProfileDcId", "Дата-центр");
        ru.put("ProfileChatCreated", "Создан");
        ru.put("ProfileChatJoined", "Вы вступили");
        ru.put("LuminaMessageDetails", "Подробности");
        ru.put("LuminaDetailsDate", "Дата");
        ru.put("LuminaDetailsMessageId", "ID сообщения");
        ru.put("LuminaDetailsForwardedFrom", "Переслано от");
        ru.put("LuminaDetailsOriginalDate", "Исходная дата");
        ru.put("LuminaSecurityTitle", "Безопасность");
        ru.put("LuminaSecurityPanicHeader", "Экстренная очистка");
        ru.put("LuminaSecurityPanicWipe", "Экстренная очистка (Kaboom)");
        ru.put("LuminaSecurityPanicWipeInfo", "Мгновенно выходит из всех аккаунтов и стирает локальные чаты и кэш с этого устройства, затем возвращает на экран входа. Ваши аккаунты остаются на серверах Telegram. Это действие нельзя отменить.");
        ru.put("LuminaSecurityPanicConfirmTitle", "Экстренная очистка?");
        ru.put("LuminaSecurityPanicConfirmMessage", "Все аккаунты будут выведены из системы, а все локальные чаты и кэш на этом устройстве будут стёрты. Это действие нельзя отменить. Продолжить?");
        ru.put("LuminaSecurityPanicConfirmButton", "Стереть сейчас");
        ru.put("LuminaSecurityDisguiseHeader", "Маскировка");
        ru.put("LuminaSecurityDisguise", "Маскировать под Calculator");
        ru.put("LuminaSecurityDisguiseInfo", "Заменить значок и название приложения на обычный Calculator. Выключите, чтобы вернуть обычный значок LuminaGram.");
        T.put("ru", ru);

        // ---- Persian / Farsi (RTL) ----
        Map<String, String> fa = new HashMap<>();
        fa.put("LuminaGramSettings", "تنظیمات LuminaGram");
        fa.put("LuminaGramSettingsInfo", "امکانات ویژه");
        fa.put("LuminaGramChatList", "فهرست گفتگوها");
        fa.put("LuminaHideTabs", "پنهان کردن زبانه‌های پوشه");
        fa.put("LuminaHideStories", "پنهان کردن استوری‌ها");
        fa.put("LuminaCompactChatList", "فهرست گفتگوی فشرده");
        fa.put("LuminaPrivacyTitle", "حریم خصوصی و ناپیدایی");
        fa.put("LuminaPrivacyGhostHeader", "حالت شبح");
        fa.put("LuminaPrivacySendReadReceipts", "ارسال رسید خواندن");
        fa.put("LuminaPrivacySendReadReceiptsInfo", "وقتی خاموش باشد، پیام‌ها فقط برای شما خوانده‌شده علامت می‌خورند — دیگران تیک دوم را نمی‌بینند.");
        fa.put("LuminaPrivacySendTyping", "ارسال وضعیت نوشتن");
        fa.put("LuminaPrivacySendTypingInfo", "وقتی خاموش باشد، دیگران وضعیت نوشتن، ضبط یا بارگذاری شما را نمی‌بینند.");
        fa.put("LuminaPrivacySendOnline", "ارسال وضعیت آنلاین");
        fa.put("LuminaPrivacySendOnlineInfo", "وقتی خاموش باشد، هنگام استفاده از برنامه همیشه آفلاین نمایش داده می‌شوید.");
        fa.put("LuminaPrivacyProfileHeader", "نمایه");
        fa.put("LuminaPrivacyShowRegistrationDate", "نمایش تاریخ ثبت‌نام");
        fa.put("LuminaPrivacyShowRegistrationDateInfo", "نمایش تاریخ تقریبی ساخت حساب در نمایه کاربران. این تاریخ تقریبی است.");
        fa.put("ProfileRegistrationDate", "تاریخ ثبت‌نام");
        fa.put("LuminaTranslateTitle", "ترجمه خودکار");
        fa.put("LuminaTranslateHeader", "ترجمه");
        fa.put("LuminaTranslateTo", "ترجمه به");
        fa.put("LuminaChatSettings", "گفتگو و رسانه");
        fa.put("LuminaMessageActions", "کنش‌های پیام");
        fa.put("LuminaForwardNoAuthorTitle", "هدایت بدون نویسنده");
        fa.put("LuminaForwardNoCaptionTitle", "هدایت بدون شرح");
        fa.put("LuminaSaveToCloudTitle", "ذخیره در پیام‌های ذخیره‌شده");
        fa.put("LuminaSelectFromAuthorTitle", "انتخاب همه پیام‌های این نویسنده");
        fa.put("LuminaMediaSaving", "رسانه");
        fa.put("LuminaSaveStickers", "ذخیره برچسب‌ها");
        fa.put("LuminaForwardNoAuthor", "هدایت بدون نویسنده");
        fa.put("LuminaForwardNoCaption", "هدایت بدون شرح");
        fa.put("LuminaSaveToCloud", "ذخیره در پیام‌های ذخیره‌شده");
        fa.put("LuminaSelectFromAuthor", "انتخاب همه پیام‌های این نویسنده");
        fa.put("LuminaCheckUpdate", "بررسی به‌روزرسانی‌ها");
        fa.put("LuminaUpdateAvailable", "به‌روزرسانی موجود است");
        fa.put("LuminaUpdateNewVersion", "نسخه جدید");
        fa.put("LuminaUpdateNow", "به‌روزرسانی");
        fa.put("LuminaUpdateDownloading", "در حال دانلود به‌روزرسانی…");
        fa.put("LuminaUpdateFailed", "دانلود به‌روزرسانی ناموفق بود");
        fa.put("ShowTranslateButton", "نمایش دکمه ترجمه");
        fa.put("LuminaTranslateBeforeSend", "ترجمه پیش از ارسال");
        fa.put("LuminaTranslateBeforeSendInfo", "وقتی روشن باشد، پیام نوشته‌شده شما به زبان مقصد ترجمه می‌شود. می‌توانید ترجمه را پیش از ارسال ببینید.");
        fa.put("LuminaSendTranslation", "ارسال ترجمه");
        fa.put("LuminaSendOriginal", "ارسال متن اصلی");
        fa.put("LuminaTranslateOriginalLabel", "متن اصلی");
        fa.put("LuminaTranslatePreviewTranslating", "در حال ترجمه…");
        fa.put("LuminaTranslateBeforeSendConfirm", "تأیید پیش از ارسال");
        fa.put("LuminaTranslateBeforeSendConfirmInfo", "وقتی روشن باشد، پیش از ارسال پیش‌نمایش ترجمه نمایش داده می‌شود تا بتوانید ترجمه یا متن اصلی را بفرستید. وقتی خاموش باشد، ترجمه با یک ضربه مستقیم ارسال می‌شود.");
        fa.put("LuminaTranslateBeforeSendEnabled", "ترجمه پیش از ارسال: روشن");
        fa.put("LuminaTranslateBeforeSendDisabled", "ترجمه پیش از ارسال: خاموش");
        fa.put("LuminaPrivacySecurityHeader", "امنیت");
        fa.put("LuminaPrivacySecureScreen", "محو برنامه در فهرست اخیر");
        fa.put("LuminaPrivacySecureScreenInfo", "محتوای برنامه را در جابه‌جاگر برنامه‌ها پنهان می‌کند و از گرفتن اسکرین‌شات در LuminaGram جلوگیری می‌کند (FLAG_SECURE برای کل برنامه اعمال می‌شود).");
        fa.put("LuminaPrivacyDisableLinkPreview", "غیرفعال‌کردن پیش‌نمایش پیوند به‌صورت پیش‌فرض");
        fa.put("LuminaPrivacyDisableLinkPreviewInfo", "پیام‌های جدیدی که می‌نویسید به‌صورت پیش‌فرض بدون پیش‌نمایش پیوند ارسال می‌شوند تا سرورها پیوندهای چسبانده‌شده را باز نکنند.");
        fa.put("LuminaPrivacyStripMetadata", "حذف موقعیت و فراداده عکس");
        fa.put("LuminaPrivacyStripMetadataInfo", "داده‌های GPS و دیگر فرادادهٔ EXIF را از عکس‌هایی که به‌صورت فایل ارسال می‌شوند حذف می‌کند. عکس‌های فشرده اصولاً این داده‌ها را ندارند.");
        fa.put("LuminaInfoDensity", "چگالی اطلاعات");
        fa.put("LuminaShowDcId", "نمایش شناسه مرکز داده");
        fa.put("LuminaShowDcIdInfo", "نمایش مرکز داده‌ای که عکس نمایه را ذخیره می‌کند در نمایه کاربران، گروه‌ها و کانال‌ها. همچنین کنش «جزئیات» را به منوی پیام می‌افزاید.");
        fa.put("LuminaShowChatDate", "نمایش تاریخ ساخت / پیوستن");
        fa.put("LuminaShowMessageDetails", "منوی جزئیات پیام");
        fa.put("ProfileDcId", "مرکز داده");
        fa.put("ProfileChatCreated", "ساخته‌شده در");
        fa.put("ProfileChatJoined", "پیوستید در");
        fa.put("LuminaMessageDetails", "جزئیات");
        fa.put("LuminaDetailsDate", "تاریخ");
        fa.put("LuminaDetailsMessageId", "شناسه پیام");
        fa.put("LuminaDetailsForwardedFrom", "هدایت‌شده از");
        fa.put("LuminaDetailsOriginalDate", "تاریخ اصلی");
        fa.put("LuminaSecurityTitle", "امنیت");
        fa.put("LuminaSecurityPanicHeader", "پاک‌سازی اضطراری");
        fa.put("LuminaSecurityPanicWipe", "پاک‌سازی اضطراری (Kaboom)");
        fa.put("LuminaSecurityPanicWipeInfo", "بلافاصله از همه حساب‌ها خارج می‌شود و گفتگوهای محلی و فایل‌های موقت را از این دستگاه پاک می‌کند، سپس به صفحه ورود بازمی‌گردد. حساب‌های شما روی سرورهای Telegram باقی می‌مانند. این عمل قابل بازگشت نیست.");
        fa.put("LuminaSecurityPanicConfirmTitle", "پاک‌سازی اضطراری؟");
        fa.put("LuminaSecurityPanicConfirmMessage", "از همه حساب‌ها خارج می‌شوید و همه گفتگوهای محلی و فایل‌های موقت روی این دستگاه پاک می‌شوند. این عمل قابل بازگشت نیست. ادامه می‌دهید؟");
        fa.put("LuminaSecurityPanicConfirmButton", "اکنون پاک کن");
        fa.put("LuminaSecurityDisguiseHeader", "استتار");
        fa.put("LuminaSecurityDisguise", "استتار به‌صورت Calculator");
        fa.put("LuminaSecurityDisguiseInfo", "نماد و نام برنامه را با یک Calculator ساده جایگزین کنید. برای بازگرداندن نماد عادی LuminaGram خاموش کنید.");
        T.put("fa", fa);

        // ---- Turkish ----
        Map<String, String> tr = new HashMap<>();
        tr.put("LuminaGramSettings", "LuminaGram Ayarları");
        tr.put("LuminaGramSettingsInfo", "Özel özellikler");
        tr.put("LuminaGramChatList", "Sohbet listesi");
        tr.put("LuminaHideTabs", "Klasör sekmelerini gizle");
        tr.put("LuminaHideStories", "Hikâyeleri gizle");
        tr.put("LuminaCompactChatList", "Sıkışık sohbet listesi");
        tr.put("LuminaPrivacyTitle", "Gizlilik ve Görünmezlik");
        tr.put("LuminaPrivacyGhostHeader", "Hayalet modu");
        tr.put("LuminaPrivacySendReadReceipts", "Okundu bilgisi gönder");
        tr.put("LuminaPrivacySendReadReceiptsInfo", "Kapalıyken, mesajlar yalnızca sizin için okundu olarak işaretlenir — karşı taraf çift tiki görmez.");
        tr.put("LuminaPrivacySendTyping", "Yazıyor durumu gönder");
        tr.put("LuminaPrivacySendTypingInfo", "Kapalıyken, başkaları yazma, kaydetme veya yükleme durumunuzu göremez.");
        tr.put("LuminaPrivacySendOnline", "Çevrimiçi durumu gönder");
        tr.put("LuminaPrivacySendOnlineInfo", "Kapalıyken, uygulamayı kullanırken bile her zaman çevrimdışı görünürsünüz.");
        tr.put("LuminaPrivacyProfileHeader", "Profil");
        tr.put("LuminaPrivacyShowRegistrationDate", "Kayıt tarihini göster");
        tr.put("LuminaPrivacyShowRegistrationDateInfo", "Kullanıcı profillerinde tahmini hesap oluşturma tarihini gösterir. Tarih yaklaşıktır.");
        tr.put("ProfileRegistrationDate", "Kayıt tarihi");
        tr.put("LuminaTranslateTitle", "Otomatik çeviri");
        tr.put("LuminaTranslateHeader", "Çeviri");
        tr.put("LuminaTranslateTo", "Şu dile çevir");
        tr.put("LuminaChatSettings", "Sohbet ve medya");
        tr.put("LuminaMessageActions", "Mesaj işlemleri");
        tr.put("LuminaForwardNoAuthorTitle", "Yazarı olmadan ilet");
        tr.put("LuminaForwardNoCaptionTitle", "Açıklaması olmadan ilet");
        tr.put("LuminaSaveToCloudTitle", "Kayıtlı Mesajlar'a kaydet");
        tr.put("LuminaSelectFromAuthorTitle", "Yazarın tüm mesajlarını seç");
        tr.put("LuminaMediaSaving", "Medya");
        tr.put("LuminaSaveStickers", "Çıkartmaları kaydet");
        tr.put("LuminaForwardNoAuthor", "Yazarı olmadan ilet");
        tr.put("LuminaForwardNoCaption", "Açıklaması olmadan ilet");
        tr.put("LuminaSaveToCloud", "Kayıtlı Mesajlar'a kaydet");
        tr.put("LuminaSelectFromAuthor", "Yazarın tüm mesajlarını seç");
        tr.put("LuminaCheckUpdate", "Güncellemeleri denetle");
        tr.put("LuminaUpdateAvailable", "Güncelleme mevcut");
        tr.put("LuminaUpdateNewVersion", "Yeni sürüm");
        tr.put("LuminaUpdateNow", "Güncelle");
        tr.put("LuminaUpdateDownloading", "Güncelleme indiriliyor…");
        tr.put("LuminaUpdateFailed", "Güncelleme indirilemedi");
        tr.put("ShowTranslateButton", "Çeviri düğmesini göster");
        tr.put("LuminaTranslateBeforeSend", "Göndermeden önce çevir");
        tr.put("LuminaTranslateBeforeSendInfo", "Açıkken, yazdığınız mesaj hedef dile çevrilir. Çeviriyi göndermeden önce önizleyebilirsiniz.");
        tr.put("LuminaSendTranslation", "Çeviriyi gönder");
        tr.put("LuminaSendOriginal", "Orijinali gönder");
        tr.put("LuminaTranslateOriginalLabel", "Orijinal");
        tr.put("LuminaTranslatePreviewTranslating", "Çevriliyor…");
        tr.put("LuminaTranslateBeforeSendConfirm", "Göndermeden önce onayla");
        tr.put("LuminaTranslateBeforeSendConfirmInfo", "Açıkken, göndermeden önce çevirinin bir önizlemesi görünür; çeviriyi ya da orijinali gönderebilirsiniz. Kapalıyken çeviri tek dokunuşla doğrudan gönderilir.");
        tr.put("LuminaTranslateBeforeSendEnabled", "Göndermeden önce çevir: açık");
        tr.put("LuminaTranslateBeforeSendDisabled", "Göndermeden önce çevir: kapalı");
        tr.put("LuminaPrivacySecurityHeader", "Güvenlik");
        tr.put("LuminaPrivacySecureScreen", "Son uygulamalarda bulanıklaştır");
        tr.put("LuminaPrivacySecureScreenInfo", "Uygulama içeriğini görev değiştiricide gizler ve LuminaGram içinde ekran görüntüsü alınmasını engeller (FLAG_SECURE tüm uygulamaya uygulanır).");
        tr.put("LuminaPrivacyDisableLinkPreview", "Bağlantı önizlemesini varsayılan olarak kapat");
        tr.put("LuminaPrivacyDisableLinkPreviewInfo", "Yazdığınız yeni mesajlar varsayılan olarak bağlantı önizlemesi olmadan gönderilir; böylece sunuculardan yapıştırılan bağlantıları çözmesi istenmez.");
        tr.put("LuminaPrivacyStripMetadata", "Fotoğraf konumunu ve meta verilerini kaldır");
        tr.put("LuminaPrivacyStripMetadataInfo", "Dosya olarak gönderilen fotoğraflardan GPS ve diğer EXIF meta verilerini kaldırır. Sıkıştırılmış fotoğraflar bu verileri zaten içermez.");
        tr.put("LuminaInfoDensity", "Bilgi yoğunluğu");
        tr.put("LuminaShowDcId", "Veri merkezi kimliğini göster");
        tr.put("LuminaShowDcIdInfo", "Kullanıcı, grup ve kanal profillerinde profil fotoğrafını saklayan veri merkezini gösterir. Ayrıca mesaj menüsüne bir Ayrıntılar eylemi ekler.");
        tr.put("LuminaShowChatDate", "Oluşturma / katılma tarihini göster");
        tr.put("LuminaShowMessageDetails", "Mesaj ayrıntıları menüsü");
        tr.put("ProfileDcId", "Veri merkezi");
        tr.put("ProfileChatCreated", "Oluşturuldu");
        tr.put("ProfileChatJoined", "Katıldınız");
        tr.put("LuminaMessageDetails", "Ayrıntılar");
        tr.put("LuminaDetailsDate", "Tarih");
        tr.put("LuminaDetailsMessageId", "Mesaj kimliği");
        tr.put("LuminaDetailsForwardedFrom", "Şuradan iletildi");
        tr.put("LuminaDetailsOriginalDate", "Özgün tarih");
        tr.put("LuminaSecurityTitle", "Güvenlik");
        tr.put("LuminaSecurityPanicHeader", "Acil silme");
        tr.put("LuminaSecurityPanicWipe", "Acil silme (Kaboom)");
        tr.put("LuminaSecurityPanicWipeInfo", "Tüm hesaplardan anında çıkış yapar, bu cihazdaki yerel sohbetleri ve önbelleğe alınmış dosyaları siler, ardından giriş ekranına döner. Hesaplarınız Telegram sunucularında kalır. Bu işlem geri alınamaz.");
        tr.put("LuminaSecurityPanicConfirmTitle", "Acil silme?");
        tr.put("LuminaSecurityPanicConfirmMessage", "Tüm hesaplardan çıkış yapılacak ve bu cihazdaki tüm yerel sohbetler ile önbellek dosyaları silinecek. Bu işlem geri alınamaz. Devam edilsin mi?");
        tr.put("LuminaSecurityPanicConfirmButton", "Şimdi sil");
        tr.put("LuminaSecurityDisguiseHeader", "Kılık değiştirme");
        tr.put("LuminaSecurityDisguise", "Calculator olarak gizle");
        tr.put("LuminaSecurityDisguiseInfo", "Uygulamanın başlatıcı simgesini ve adını sade bir Calculator ile değiştirir. Normal LuminaGram simgesini geri getirmek için kapatın.");
        T.put("tr", tr);

        // ---- Spanish ----
        Map<String, String> es = new HashMap<>();
        es.put("LuminaGramSettings", "Ajustes de LuminaGram");
        es.put("LuminaGramSettingsInfo", "Funciones exclusivas");
        es.put("LuminaGramChatList", "Lista de chats");
        es.put("LuminaHideTabs", "Ocultar pestañas de carpetas");
        es.put("LuminaHideStories", "Ocultar historias");
        es.put("LuminaCompactChatList", "Lista de chats compacta");
        es.put("LuminaPrivacyTitle", "Privacidad y sigilo");
        es.put("LuminaPrivacyGhostHeader", "Modo fantasma");
        es.put("LuminaPrivacySendReadReceipts", "Enviar confirmaciones de lectura");
        es.put("LuminaPrivacySendReadReceiptsInfo", "Cuando está desactivado, los mensajes se marcan como leídos solo para ti; los demás nunca ven el doble check.");
        es.put("LuminaPrivacySendTyping", "Enviar estado de escritura");
        es.put("LuminaPrivacySendTypingInfo", "Cuando está desactivado, los demás nunca ven si estás escribiendo, grabando o subiendo.");
        es.put("LuminaPrivacySendOnline", "Enviar estado en línea");
        es.put("LuminaPrivacySendOnlineInfo", "Cuando está desactivado, siempre apareces desconectado aunque estés usando la app.");
        es.put("LuminaPrivacyProfileHeader", "Perfil");
        es.put("LuminaPrivacyShowRegistrationDate", "Mostrar fecha de registro");
        es.put("LuminaPrivacyShowRegistrationDateInfo", "Muestra una fecha estimada de creación de la cuenta en los perfiles. La fecha es aproximada.");
        es.put("ProfileRegistrationDate", "Fecha de registro");
        es.put("LuminaTranslateTitle", "Autotraducción");
        es.put("LuminaTranslateHeader", "Traducción");
        es.put("LuminaTranslateTo", "Traducir a");
        es.put("LuminaChatSettings", "Chat y multimedia");
        es.put("LuminaMessageActions", "Acciones de mensajes");
        es.put("LuminaForwardNoAuthorTitle", "Reenviar sin autor");
        es.put("LuminaForwardNoCaptionTitle", "Reenviar sin descripción");
        es.put("LuminaSaveToCloudTitle", "Guardar en Mensajes Guardados");
        es.put("LuminaSelectFromAuthorTitle", "Seleccionar todo del autor");
        es.put("LuminaMediaSaving", "Multimedia");
        es.put("LuminaSaveStickers", "Guardar stickers");
        es.put("LuminaForwardNoAuthor", "Reenviar sin autor");
        es.put("LuminaForwardNoCaption", "Reenviar sin descripción");
        es.put("LuminaSaveToCloud", "Guardar en Mensajes Guardados");
        es.put("LuminaSelectFromAuthor", "Seleccionar todo del autor");
        es.put("LuminaCheckUpdate", "Buscar actualizaciones");
        es.put("LuminaUpdateAvailable", "Actualización disponible");
        es.put("LuminaUpdateNewVersion", "Nueva versión");
        es.put("LuminaUpdateNow", "Actualizar");
        es.put("LuminaUpdateDownloading", "Descargando actualización…");
        es.put("LuminaUpdateFailed", "Error al descargar la actualización");
        es.put("ShowTranslateButton", "Mostrar botón de traducción");
        es.put("LuminaTranslateBeforeSend", "Traducir antes de enviar");
        es.put("LuminaTranslateBeforeSendInfo", "Cuando está activado, tu mensaje se traduce al idioma de destino. Puedes ver la traducción antes de enviarla.");
        es.put("LuminaSendTranslation", "Enviar traducción");
        es.put("LuminaSendOriginal", "Enviar original");
        es.put("LuminaTranslateOriginalLabel", "Original");
        es.put("LuminaTranslatePreviewTranslating", "Traduciendo…");
        es.put("LuminaTranslateBeforeSendConfirm", "Confirmar antes de enviar");
        es.put("LuminaTranslateBeforeSendConfirmInfo", "Cuando está activado, aparece una vista previa de la traducción antes de enviar, para que elijas enviar la traducción o el original. Cuando está desactivado, la traducción se envía directamente con un solo toque.");
        es.put("LuminaTranslateBeforeSendEnabled", "Traducir antes de enviar: activado");
        es.put("LuminaTranslateBeforeSendDisabled", "Traducir antes de enviar: desactivado");
        es.put("LuminaPrivacySecurityHeader", "Seguridad");
        es.put("LuminaPrivacySecureScreen", "Difuminar la app en recientes");
        es.put("LuminaPrivacySecureScreenInfo", "Oculta el contenido de la app en el selector de tareas y bloquea las capturas de pantalla dentro de LuminaGram (aplica FLAG_SECURE en toda la app).");
        es.put("LuminaPrivacyDisableLinkPreview", "Desactivar la vista previa de enlaces por defecto");
        es.put("LuminaPrivacyDisableLinkPreviewInfo", "Los mensajes nuevos que redactes se envían por defecto sin vista previa de enlaces, así los servidores no resuelven los enlaces pegados.");
        es.put("LuminaPrivacyStripMetadata", "Eliminar ubicación y metadatos de las fotos");
        es.put("LuminaPrivacyStripMetadataInfo", "Elimina el GPS y otros metadatos EXIF de las fotos enviadas como archivos. Las fotos comprimidas no incluyen estos datos.");
        es.put("LuminaInfoDensity", "Densidad de información");
        es.put("LuminaShowDcId", "Mostrar ID del centro de datos");
        es.put("LuminaShowDcIdInfo", "Muestra el centro de datos que almacena la foto de perfil en los perfiles de usuarios, grupos y canales. También añade una acción Detalles al menú del mensaje.");
        es.put("LuminaShowChatDate", "Mostrar fecha de creación / ingreso");
        es.put("LuminaShowMessageDetails", "Menú de detalles del mensaje");
        es.put("ProfileDcId", "Centro de datos");
        es.put("ProfileChatCreated", "Creado");
        es.put("ProfileChatJoined", "Te uniste");
        es.put("LuminaMessageDetails", "Detalles");
        es.put("LuminaDetailsDate", "Fecha");
        es.put("LuminaDetailsMessageId", "ID del mensaje");
        es.put("LuminaDetailsForwardedFrom", "Reenviado de");
        es.put("LuminaDetailsOriginalDate", "Fecha original");
        es.put("LuminaSecurityTitle", "Seguridad");
        es.put("LuminaSecurityPanicHeader", "Borrado de emergencia");
        es.put("LuminaSecurityPanicWipe", "Borrado de emergencia (Kaboom)");
        es.put("LuminaSecurityPanicWipeInfo", "Cierra sesión al instante en todas las cuentas y borra los chats locales y los archivos en caché de este dispositivo; luego vuelve a la pantalla de inicio de sesión. Tus cuentas permanecen en los servidores de Telegram. Esto no se puede deshacer.");
        es.put("LuminaSecurityPanicConfirmTitle", "¿Borrado de emergencia?");
        es.put("LuminaSecurityPanicConfirmMessage", "Se cerrará sesión en todas las cuentas y se borrarán todos los chats locales y archivos en caché de este dispositivo. Esto no se puede deshacer. ¿Continuar?");
        es.put("LuminaSecurityPanicConfirmButton", "Borrar ahora");
        es.put("LuminaSecurityDisguiseHeader", "Disfraz");
        es.put("LuminaSecurityDisguise", "Disfrazar como Calculator");
        es.put("LuminaSecurityDisguiseInfo", "Reemplaza el icono y el nombre de la app por una simple Calculator. Desactívalo para restaurar el icono normal de LuminaGram.");
        T.put("es", es);

        // ---- Portuguese (Brazil) — key is pt-br ----
        Map<String, String> ptBr = new HashMap<>();
        ptBr.put("LuminaGramSettings", "Configurações do LuminaGram");
        ptBr.put("LuminaGramSettingsInfo", "Recursos exclusivos");
        ptBr.put("LuminaGramChatList", "Lista de conversas");
        ptBr.put("LuminaHideTabs", "Ocultar abas de pastas");
        ptBr.put("LuminaHideStories", "Ocultar stories");
        ptBr.put("LuminaCompactChatList", "Lista de conversas compacta");
        ptBr.put("LuminaPrivacyTitle", "Privacidade e discrição");
        ptBr.put("LuminaPrivacyGhostHeader", "Modo fantasma");
        ptBr.put("LuminaPrivacySendReadReceipts", "Enviar confirmações de leitura");
        ptBr.put("LuminaPrivacySendReadReceiptsInfo", "Quando desativado, as mensagens são marcadas como lidas só para você — os outros nunca veem o check duplo.");
        ptBr.put("LuminaPrivacySendTyping", "Enviar status de digitação");
        ptBr.put("LuminaPrivacySendTypingInfo", "Quando desativado, os outros nunca veem você digitando, gravando ou enviando.");
        ptBr.put("LuminaPrivacySendOnline", "Enviar status on-line");
        ptBr.put("LuminaPrivacySendOnlineInfo", "Quando desativado, você sempre aparece off-line mesmo usando o app.");
        ptBr.put("LuminaPrivacyProfileHeader", "Perfil");
        ptBr.put("LuminaPrivacyShowRegistrationDate", "Mostrar data de registro");
        ptBr.put("LuminaPrivacyShowRegistrationDateInfo", "Mostra uma data estimada de criação da conta nos perfis. A data é aproximada.");
        ptBr.put("ProfileRegistrationDate", "Data de registro");
        ptBr.put("LuminaTranslateTitle", "Tradução automática");
        ptBr.put("LuminaTranslateHeader", "Tradução");
        ptBr.put("LuminaTranslateTo", "Traduzir para");
        ptBr.put("LuminaChatSettings", "Conversa e mídia");
        ptBr.put("LuminaMessageActions", "Ações de mensagens");
        ptBr.put("LuminaForwardNoAuthorTitle", "Encaminhar sem autor");
        ptBr.put("LuminaForwardNoCaptionTitle", "Encaminhar sem legenda");
        ptBr.put("LuminaSaveToCloudTitle", "Salvar em Mensagens Salvas");
        ptBr.put("LuminaSelectFromAuthorTitle", "Selecionar tudo do autor");
        ptBr.put("LuminaMediaSaving", "Mídia");
        ptBr.put("LuminaSaveStickers", "Salvar figurinhas");
        ptBr.put("LuminaForwardNoAuthor", "Encaminhar sem autor");
        ptBr.put("LuminaForwardNoCaption", "Encaminhar sem legenda");
        ptBr.put("LuminaSaveToCloud", "Salvar em Mensagens Salvas");
        ptBr.put("LuminaSelectFromAuthor", "Selecionar tudo do autor");
        ptBr.put("LuminaCheckUpdate", "Verificar atualizações");
        ptBr.put("LuminaUpdateAvailable", "Atualização disponível");
        ptBr.put("LuminaUpdateNewVersion", "Nova versão");
        ptBr.put("LuminaUpdateNow", "Atualizar");
        ptBr.put("LuminaUpdateDownloading", "Baixando atualização…");
        ptBr.put("LuminaUpdateFailed", "Falha ao baixar a atualização");
        ptBr.put("ShowTranslateButton", "Mostrar botão de tradução");
        ptBr.put("LuminaTranslateBeforeSend", "Traduzir antes de enviar");
        ptBr.put("LuminaTranslateBeforeSendInfo", "Quando ativado, sua mensagem é traduzida para o idioma de destino. Você pode ver a tradução antes de enviá-la.");
        ptBr.put("LuminaSendTranslation", "Enviar tradução");
        ptBr.put("LuminaSendOriginal", "Enviar original");
        ptBr.put("LuminaTranslateOriginalLabel", "Original");
        ptBr.put("LuminaTranslatePreviewTranslating", "Traduzindo…");
        ptBr.put("LuminaTranslateBeforeSendConfirm", "Confirmar antes de enviar");
        ptBr.put("LuminaTranslateBeforeSendConfirmInfo", "Quando ativado, uma prévia da tradução aparece antes de enviar, para você escolher enviar a tradução ou o original. Quando desativado, a tradução é enviada diretamente com um único toque.");
        ptBr.put("LuminaTranslateBeforeSendEnabled", "Traduzir antes de enviar: ativado");
        ptBr.put("LuminaTranslateBeforeSendDisabled", "Traduzir antes de enviar: desativado");
        ptBr.put("LuminaPrivacySecurityHeader", "Segurança");
        ptBr.put("LuminaPrivacySecureScreen", "Desfocar o app em recentes");
        ptBr.put("LuminaPrivacySecureScreenInfo", "Oculta o conteúdo do app no alternador de tarefas e bloqueia capturas de tela dentro do LuminaGram (aplica FLAG_SECURE em todo o app).");
        ptBr.put("LuminaPrivacyDisableLinkPreview", "Desativar a prévia de links por padrão");
        ptBr.put("LuminaPrivacyDisableLinkPreviewInfo", "As novas mensagens que você escreve são enviadas por padrão sem prévia de link, então os servidores não resolvem os links colados.");
        ptBr.put("LuminaPrivacyStripMetadata", "Remover localização e metadados das fotos");
        ptBr.put("LuminaPrivacyStripMetadataInfo", "Remove GPS e outros metadados EXIF das fotos enviadas como arquivos. Fotos comprimidas não incluem esses dados.");
        ptBr.put("LuminaInfoDensity", "Densidade de informações");
        ptBr.put("LuminaShowDcId", "Mostrar ID do datacenter");
        ptBr.put("LuminaShowDcIdInfo", "Mostra o datacenter que armazena a foto do perfil nos perfis de usuários, grupos e canais. Também adiciona uma ação Detalhes ao menu da mensagem.");
        ptBr.put("LuminaShowChatDate", "Mostrar data de criação / entrada");
        ptBr.put("LuminaShowMessageDetails", "Menu de detalhes da mensagem");
        ptBr.put("ProfileDcId", "Datacenter");
        ptBr.put("ProfileChatCreated", "Criado");
        ptBr.put("ProfileChatJoined", "Você entrou");
        ptBr.put("LuminaMessageDetails", "Detalhes");
        ptBr.put("LuminaDetailsDate", "Data");
        ptBr.put("LuminaDetailsMessageId", "ID da mensagem");
        ptBr.put("LuminaDetailsForwardedFrom", "Encaminhado de");
        ptBr.put("LuminaDetailsOriginalDate", "Data original");
        ptBr.put("LuminaSecurityTitle", "Segurança");
        ptBr.put("LuminaSecurityPanicHeader", "Limpeza de emergência");
        ptBr.put("LuminaSecurityPanicWipe", "Limpeza de emergência (Kaboom)");
        ptBr.put("LuminaSecurityPanicWipeInfo", "Encerra a sessão de todas as contas na hora e apaga as conversas locais e os arquivos em cache deste dispositivo, voltando à tela de login. Suas contas permanecem nos servidores do Telegram. Isso não pode ser desfeito.");
        ptBr.put("LuminaSecurityPanicConfirmTitle", "Limpeza de emergência?");
        ptBr.put("LuminaSecurityPanicConfirmMessage", "Todas as contas serão desconectadas e todas as conversas locais e arquivos em cache deste dispositivo serão apagados. Isso não pode ser desfeito. Continuar?");
        ptBr.put("LuminaSecurityPanicConfirmButton", "Apagar agora");
        ptBr.put("LuminaSecurityDisguiseHeader", "Disfarce");
        ptBr.put("LuminaSecurityDisguise", "Disfarçar como Calculator");
        ptBr.put("LuminaSecurityDisguiseInfo", "Substitui o ícone e o nome do app por uma Calculator comum. Desative para restaurar o ícone normal do LuminaGram.");
        T.put("pt-br", ptBr);

        // ---- Indonesian ----
        Map<String, String> id = new HashMap<>();
        id.put("LuminaGramSettings", "Pengaturan LuminaGram");
        id.put("LuminaGramSettingsInfo", "Fitur eksklusif");
        id.put("LuminaGramChatList", "Daftar obrolan");
        id.put("LuminaHideTabs", "Sembunyikan tab folder");
        id.put("LuminaHideStories", "Sembunyikan cerita");
        id.put("LuminaCompactChatList", "Daftar obrolan ringkas");
        id.put("LuminaPrivacyTitle", "Privasi & Siluman");
        id.put("LuminaPrivacyGhostHeader", "Mode hantu");
        id.put("LuminaPrivacySendReadReceipts", "Kirim tanda telah dibaca");
        id.put("LuminaPrivacySendReadReceiptsInfo", "Saat nonaktif, pesan ditandai telah dibaca hanya untuk Anda — orang lain tidak melihat centang ganda.");
        id.put("LuminaPrivacySendTyping", "Kirim status mengetik");
        id.put("LuminaPrivacySendTypingInfo", "Saat nonaktif, orang lain tidak melihat status mengetik, merekam, atau mengunggah Anda.");
        id.put("LuminaPrivacySendOnline", "Kirim status daring");
        id.put("LuminaPrivacySendOnlineInfo", "Saat nonaktif, Anda selalu tampak luring meski sedang memakai aplikasi.");
        id.put("LuminaPrivacyProfileHeader", "Profil");
        id.put("LuminaPrivacyShowRegistrationDate", "Tampilkan tanggal registrasi");
        id.put("LuminaPrivacyShowRegistrationDateInfo", "Tampilkan perkiraan tanggal pembuatan akun di profil pengguna. Tanggal ini hanya perkiraan.");
        id.put("ProfileRegistrationDate", "Tanggal registrasi");
        id.put("LuminaTranslateTitle", "Terjemah otomatis");
        id.put("LuminaTranslateHeader", "Terjemahan");
        id.put("LuminaTranslateTo", "Terjemahkan ke");
        id.put("LuminaChatSettings", "Obrolan & media");
        id.put("LuminaMessageActions", "Tindakan pesan");
        id.put("LuminaForwardNoAuthorTitle", "Teruskan tanpa penulis");
        id.put("LuminaForwardNoCaptionTitle", "Teruskan tanpa keterangan");
        id.put("LuminaSaveToCloudTitle", "Simpan ke Pesan Tersimpan");
        id.put("LuminaSelectFromAuthorTitle", "Pilih semua dari penulis");
        id.put("LuminaMediaSaving", "Media");
        id.put("LuminaSaveStickers", "Simpan stiker");
        id.put("LuminaForwardNoAuthor", "Teruskan tanpa penulis");
        id.put("LuminaForwardNoCaption", "Teruskan tanpa keterangan");
        id.put("LuminaSaveToCloud", "Simpan ke Pesan Tersimpan");
        id.put("LuminaSelectFromAuthor", "Pilih semua dari penulis");
        id.put("LuminaCheckUpdate", "Periksa pembaruan");
        id.put("LuminaUpdateAvailable", "Pembaruan tersedia");
        id.put("LuminaUpdateNewVersion", "Versi baru");
        id.put("LuminaUpdateNow", "Perbarui");
        id.put("LuminaUpdateDownloading", "Mengunduh pembaruan…");
        id.put("LuminaUpdateFailed", "Gagal mengunduh pembaruan");
        id.put("ShowTranslateButton", "Tampilkan tombol terjemahan");
        id.put("LuminaTranslateBeforeSend", "Terjemahkan sebelum kirim");
        id.put("LuminaTranslateBeforeSendInfo", "Saat aktif, pesan yang Anda ketik diterjemahkan ke bahasa tujuan. Anda dapat melihat pratinjau terjemahan sebelum dikirim.");
        id.put("LuminaSendTranslation", "Kirim terjemahan");
        id.put("LuminaSendOriginal", "Kirim asli");
        id.put("LuminaTranslateOriginalLabel", "Asli");
        id.put("LuminaTranslatePreviewTranslating", "Menerjemahkan…");
        id.put("LuminaTranslateBeforeSendConfirm", "Konfirmasi sebelum kirim");
        id.put("LuminaTranslateBeforeSendConfirmInfo", "Saat aktif, pratinjau terjemahan muncul sebelum mengirim, sehingga Anda dapat memilih mengirim terjemahan atau teks asli. Saat nonaktif, terjemahan langsung dikirim dengan satu ketukan.");
        id.put("LuminaTranslateBeforeSendEnabled", "Terjemahkan sebelum kirim: aktif");
        id.put("LuminaTranslateBeforeSendDisabled", "Terjemahkan sebelum kirim: nonaktif");
        id.put("LuminaPrivacySecurityHeader", "Keamanan");
        id.put("LuminaPrivacySecureScreen", "Buramkan aplikasi di layar terkini");
        id.put("LuminaPrivacySecureScreenInfo", "Menyembunyikan konten aplikasi di pengalih tugas dan memblokir tangkapan layar di dalam LuminaGram (menerapkan FLAG_SECURE ke seluruh aplikasi).");
        id.put("LuminaPrivacyDisableLinkPreview", "Nonaktifkan pratinjau tautan secara bawaan");
        id.put("LuminaPrivacyDisableLinkPreviewInfo", "Pesan baru yang Anda tulis dikirim secara bawaan tanpa pratinjau tautan, sehingga server tidak diminta membuka tautan yang ditempel.");
        id.put("LuminaPrivacyStripMetadata", "Hapus lokasi & metadata foto");
        id.put("LuminaPrivacyStripMetadataInfo", "Menghapus GPS dan metadata EXIF lainnya dari foto yang dikirim sebagai file. Foto terkompresi tidak menyertakan data ini.");
        id.put("LuminaInfoDensity", "Kepadatan info");
        id.put("LuminaShowDcId", "Tampilkan ID pusat data");
        id.put("LuminaShowDcIdInfo", "Tampilkan pusat data yang menyimpan foto profil di profil pengguna, grup, dan kanal. Juga menambahkan tindakan Detail ke menu pesan.");
        id.put("LuminaShowChatDate", "Tampilkan tanggal dibuat / bergabung");
        id.put("LuminaShowMessageDetails", "Menu detail pesan");
        id.put("ProfileDcId", "Pusat data");
        id.put("ProfileChatCreated", "Dibuat");
        id.put("ProfileChatJoined", "Anda bergabung");
        id.put("LuminaMessageDetails", "Detail");
        id.put("LuminaDetailsDate", "Tanggal");
        id.put("LuminaDetailsMessageId", "ID pesan");
        id.put("LuminaDetailsForwardedFrom", "Diteruskan dari");
        id.put("LuminaDetailsOriginalDate", "Tanggal asli");
        id.put("LuminaSecurityTitle", "Keamanan");
        id.put("LuminaSecurityPanicHeader", "Hapus darurat");
        id.put("LuminaSecurityPanicWipe", "Hapus darurat (Kaboom)");
        id.put("LuminaSecurityPanicWipeInfo", "Segera keluar dari semua akun dan menghapus obrolan lokal serta berkas cache dari perangkat ini, lalu kembali ke layar masuk. Akun Anda tetap ada di server Telegram. Tindakan ini tidak dapat dibatalkan.");
        id.put("LuminaSecurityPanicConfirmTitle", "Hapus darurat?");
        id.put("LuminaSecurityPanicConfirmMessage", "Semua akun akan dikeluarkan dan semua obrolan lokal serta berkas cache di perangkat ini akan dihapus. Tindakan ini tidak dapat dibatalkan. Lanjutkan?");
        id.put("LuminaSecurityPanicConfirmButton", "Hapus sekarang");
        id.put("LuminaSecurityDisguiseHeader", "Penyamaran");
        id.put("LuminaSecurityDisguise", "Menyamar sebagai Calculator");
        id.put("LuminaSecurityDisguiseInfo", "Ganti ikon peluncur dan nama aplikasi dengan Calculator biasa. Matikan untuk memulihkan ikon LuminaGram normal.");
        T.put("id", id);
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
        String lang   = info.getLangCode()     == null ? "" : info.getLangCode().toLowerCase();      // "en","pt-br","zh-hans"
        String base   = info.getBaseLangCode() == null ? "" : info.getBaseLangCode().toLowerCase();  // unofficial pack base
        String plural = info.pluralLangCode    == null ? "" : info.pluralLangCode.toLowerCase();     // "zh" for every Chinese pack

        // --- Chinese: split Traditional vs Simplified across every reporting style ---
        if (lang.contains("zh") || base.contains("zh") || "zh".equals(plural)) {
            String probe = lang + "|" + base;
            boolean hant = probe.contains("hant") || probe.contains("tw")
                    || probe.contains("hk") || probe.contains("mo") || probe.contains("traditional");
            return T.get(hant ? "zh-hant" : "zh-hans");
        }

        // --- Everything else: reduce to our key set ---
        String key;
        if (lang.startsWith("pt")) {
            key = "pt-br";                 // pt / pt-br → our Brazilian map
        } else if (lang.length() >= 2) {
            key = lang.substring(0, 2);    // "ar","ru","fa","tr","es","id","in",...
        } else {
            key = lang;
        }
        if ("in".equals(key)) {            // legacy Android code for Indonesian
            key = "id";
        }
        return T.get(key);                 // null (en + untranslated) → English fallback via getString()
    }
}
