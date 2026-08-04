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
