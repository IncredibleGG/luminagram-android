package org.telegram.messenger;

import java.util.HashMap;
import java.util.Map;

/**
 * LuminaChangelogText — translations for the What's New entries.
 *
 * The canonical changelog lives in {@code LuminaChangelogActivity.CHANGELOG}: version
 * name, build number and the English bullet lines. That array stays there because
 * release/publish.sh greps it for the newest build number and refuses to publish when
 * it does not match the release being cut — do not move it.
 *
 * This file holds only the translations, keyed by version name. Adding a release is a
 * single {@link #put} call at the top of the static block with the nine languages side
 * by side, instead of nine edits scattered through LuminaLocale. A version with no
 * entry here (or a language we do not translate) simply falls back to the English
 * lines from the activity, so a forgotten translation degrades instead of breaking.
 */
public final class LuminaChangelogText {

    private LuminaChangelogText() {}

    /** langKey (as used by LuminaLocale) -> versionName -> localized bullet lines. */
    private static final Map<String, Map<String, String[]>> T = new HashMap<>();

    /**
     * Bullet lines for {@code version} in the current in-app language, or
     * {@code english} when that language or that version is not translated.
     */
    public static String[] get(String version, String[] english) {
        String lang = LuminaLocale.currentLangKey();
        if (lang == null) {
            return english;
        }
        Map<String, String[]> byVersion = T.get(lang);
        if (byVersion == null) {
            return english;
        }
        String[] localized = byVersion.get(version);
        return localized != null ? localized : english;
    }

    /** One call per release. The nine arrays are always in this order. */
    private static void put(String version,
                            String[] zhHans, String[] zhHant, String[] ar,
                            String[] ru, String[] fa, String[] tr,
                            String[] es, String[] ptBr, String[] id) {
        add("zh-hans", version, zhHans);
        add("zh-hant", version, zhHant);
        add("ar", version, ar);
        add("ru", version, ru);
        add("fa", version, fa);
        add("tr", version, tr);
        add("es", version, es);
        add("pt-br", version, ptBr);
        add("id", version, id);
    }

    private static void add(String lang, String version, String[] lines) {
        Map<String, String[]> byVersion = T.get(lang);
        if (byVersion == null) {
            byVersion = new HashMap<>();
            T.put(lang, byVersion);
        }
        byVersion.put(version, lines);
    }

    static {
        put("1.3.4",
            /* zh-hans */ new String[]{
                "发送前翻译改为按对话:全局开关只是启用功能,要在送出键选单里为每个联系人或群组分别打开",
                "长的双语讯息会把原文折成一行、点击展开,译文永远完整显示",
            },
            /* zh-hant */ new String[]{
                "發送前翻譯改為按對話:全局開關只是啟用功能,要在送出鍵選單裡為每個聯絡人或群組分別打開",
                "長的雙語訊息會把原文折成一行、點擊展開,譯文永遠完整顯示",
            },
            /* ar      */ new String[]{
                "الترجمة قبل الإرسال أصبحت لكل محادثة: المفتاح العام يفعّل الميزة فقط، وتشغّلها لكل جهة اتصال أو مجموعة من قائمة زر الإرسال",
                "الرسائل ثنائية اللغة الطويلة تطوي النص الأصلي إلى سطر واحد مع لمسة للتوسيع، وتبقى الترجمة كاملة دائمًا",
            },
            /* ru      */ new String[]{
                "Перевод перед отправкой теперь для каждого чата: общий переключатель лишь включает функцию, а в каждом чате вы включаете её из меню кнопки отправки",
                "Длинные двуязычные сообщения сворачивают оригинал в одну строку с разворотом по нажатию; перевод всегда показан полностью",
            },
            /* fa      */ new String[]{
                "ترجمه پیش از ارسال اکنون برای هر گفتگو است: کلید سراسری فقط ویژگی را فعال می‌کند و برای هر مخاطب یا گروه از منوی دکمه ارسال روشنش می‌کنید",
                "پیام‌های دوزبانه بلند، متن اصلی را در یک خط جمع می‌کنند و با یک ضربه باز می‌شوند؛ ترجمه همیشه کامل می‌ماند",
            },
            /* tr      */ new String[]{
                "Göndermeden önce çeviri artık sohbet başına: genel anahtar yalnızca özelliği etkinleştirir, her kişi veya grup için gönder düğmesi menüsünden açarsınız",
                "Uzun iki dilli mesajlar orijinali tek satıra katlar, dokununca açılır; çeviri her zaman tam kalır",
            },
            /* es      */ new String[]{
                "La traducción al enviar ahora es por chat: el interruptor global solo activa la función, y la enciendes para cada contacto o grupo desde el menú del botón de enviar",
                "Los mensajes bilingües largos pliegan el original a una línea con toque para expandir; la traducción siempre se muestra completa",
            },
            /* pt-br   */ new String[]{
                "A tradução ao enviar agora é por conversa: o interruptor global apenas ativa o recurso, e você o liga para cada contato ou grupo pelo menu do botão de enviar",
                "Mensagens bilíngues longas dobram o original em uma linha com toque para expandir; a tradução sempre aparece completa",
            },
            /* id      */ new String[]{
                "Terjemah sebelum kirim kini per obrolan: sakelar global hanya mengaktifkan fitur, dan Anda menyalakannya untuk tiap kontak atau grup dari menu tombol kirim",
                "Pesan dwibahasa panjang melipat teks asli jadi satu baris dengan ketuk untuk membuka; terjemahan selalu tampil penuh",
            });
        put("1.3.3",
            /* zh-hans */ new String[]{
                "下载好的更新可能会被卡住:第一次 Android 询问安装权限时，提示被当成已送达，之后再也不出现",
                "手动检查更新一定会有回应，不会再对已经看过的版本毫无反应",
            },
            /* zh-hant */ new String[]{
                "下載好的更新可能會被卡住:第一次 Android 詢問安裝權限時,提示被當成已送達,之後再也不出現",
                "手動檢查更新一定會有回應,不會再對已經看過的版本毫無反應",
            },
            /* ar      */ new String[]{
                "كان التحديث المُنزَّل قد يعلق: عند أول مرة يطلب فيها أندرويد إذن التثبيت، كان العرض يُحسب مُسلَّمًا ولا يعود",
                "التحقق اليدوي من التحديثات يستجيب دائمًا الآن بدل أن يصمت أمام إصدار سبق أن رآه",
            },
            /* ru      */ new String[]{
                "Загруженное обновление могло застрять: при первом запросе Android на разрешение установки предложение считалось доставленным и больше не появлялось",
                "Ручная проверка обновлений теперь всегда отвечает, а не молчит о версии, которую уже видела",
            },
            /* fa      */ new String[]{
                "به‌روزرسانی دانلودشده ممکن بود گیر کند: نخستین باری که اندروید اجازه نصب می‌خواست، پیشنهاد تحویل‌شده حساب می‌شد و دیگر برنمی‌گشت",
                "بررسی دستی به‌روزرسانی همیشه پاسخ می‌دهد، نه اینکه درباره نسخه‌ای که قبلاً دیده سکوت کند",
            },
            /* tr      */ new String[]{
                "İndirilen güncelleme takılabiliyordu: Android ilk kez kurulum izni istediğinde teklif iletilmiş sayılıp bir daha gelmiyordu",
                "Elle güncelleme kontrolü artık her zaman yanıt veriyor; daha önce gördüğü bir sürüm için sessiz kalmıyor",
            },
            /* es      */ new String[]{
                "Una actualización descargada podía quedarse varada: la primera vez que Android pedía permiso para instalar, el aviso se daba por entregado y no volvía",
                "Buscar actualizaciones a mano siempre responde ahora, en vez de callar ante una versión que ya había visto",
            },
            /* pt-br   */ new String[]{
                "Uma atualização baixada podia ficar presa: na primeira vez que o Android pedia permissão para instalar, o aviso era dado como entregue e não voltava",
                "Procurar atualizações manualmente agora sempre responde, em vez de ficar em silêncio sobre uma versão que já tinha visto",
            },
            /* id      */ new String[]{
                "Pembaruan yang sudah diunduh bisa tersangkut: saat Android pertama kali meminta izin memasang, tawaran itu dianggap tersampaikan dan tidak muncul lagi",
                "Memeriksa pembaruan secara manual kini selalu menjawab, bukan diam soal versi yang sudah pernah dilihatnya",
            });
        put("1.3.2",
            /* zh-hans */ new String[]{
                "加密货币地址警告现在会把要你核对的地址显示出来",
                "关掉应用也收得到通知；被 Android 静音时，设置页会明说",
                "保险箱设置恢复正常显示，并标出当前的模式与伪装外观",
                "开启保险箱时，桌面图标会自动换成伪装应用的图标",
                "语音模型只列 Vosk 真正提供的语言，标明大小，也可以删除",
                "保存的照片和文件放进 LuminaGram 文件夹；之前存的仍留在原处",
                "「新功能」已翻译成应用支持的所有语言",
            },
            /* zh-hant */ new String[]{
                "加密貨幣地址警告現在會把要你核對的地址顯示出來",
                "關掉 App 也收得到通知；被 Android 靜音時,設定頁會明說",
                "保險箱設定恢復正常顯示,並標出目前的模式與偽裝外觀",
                "開啟保險箱時,桌面圖示會自動換成偽裝 App 的圖示",
                "語音模型只列 Vosk 真正提供的語言,標明大小,也可以刪除",
                "儲存的照片和檔案放進 LuminaGram 資料夾;先前存的仍留在原處",
                "「新功能」已翻譯成 App 支援的所有語言",
            },
            /* ar      */ new String[]{
                "تحذير عنوان العملات الرقمية يعرض الآن العنوان الذي يطلب منك التحقق منه",
                "تصل الإشعارات والتطبيق مغلق، وتوضح الإعدادات ذلك عندما يكتم أندرويد صوتنا",
                "تُقرأ إعدادات الخزنة بشكل صحيح مجددًا، وتُبيّن الوضع ونمط التمويه الفعّال",
                "تشغيل الخزنة يبدّل أيقونة الشاشة الرئيسية لتطابق تطبيق التمويه",
                "نماذج الصوت: اللغات التي يوفرها Vosk فعليًا فقط، مع الأحجام، ويمكن حذفها",
                "تُحفظ الصور والملفات في مجلد LuminaGram؛ وما حُفظ سابقًا يبقى مكانه",
                "تُرجمت «ما الجديد» إلى كل لغة يدعمها التطبيق",
            },
            /* ru      */ new String[]{
                "Предупреждение о криптоадресе теперь показывает адрес, который просит проверить",
                "Уведомления приходят и при закрытом приложении, а настройки предупреждают, если Android нас заглушает",
                "Настройки хранилища снова читаются нормально и отмечают активный режим и вид маскировки",
                "Включение хранилища меняет значок на рабочем столе на значок приложения-обманки",
                "Голосовые модели: только языки, которые Vosk действительно выпускает, с размерами и возможностью удаления",
                "Сохранённые фото и файлы попадают в папку LuminaGram; сохранённое раньше остаётся на месте",
                "«Что нового» переведено на все поддерживаемые языки",
            },
            /* fa      */ new String[]{
                "هشدار نشانی رمزارز اکنون نشانی‌ای را که می‌خواهد بررسی کنید نمایش می‌دهد",
                "اعلان‌ها با بسته بودن برنامه هم می‌رسند و اگر اندروید ما را ساکت کند، تنظیمات آن را می‌گوید",
                "تنظیمات گاوصندوق دوباره درست خوانده می‌شود و حالت و ظاهر استتار فعال را نشان می‌دهد",
                "روشن کردن گاوصندوق نماد صفحه اصلی را با برنامه استتار هماهنگ می‌کند",
                "مدل‌های صوتی: فقط زبان‌هایی که Vosk واقعاً منتشر می‌کند، همراه با اندازه، و قابل حذف",
                "عکس‌ها و فایل‌های ذخیره‌شده به پوشه LuminaGram می‌روند؛ موارد قبلی سر جای خود می‌مانند",
                "«تازه‌ها» به همه زبان‌های پشتیبانی‌شده ترجمه شد",
            },
            /* tr      */ new String[]{
                "Kripto adres uyarısı artık kontrol etmenizi istediği adresi gösteriyor",
                "Uygulama kapalıyken de bildirimler geliyor; Android bizi susturuyorsa ayarlar bunu söylüyor",
                "Kasa ayarları yeniden düzgün okunuyor ve hangi modun ve sahte uygulama görünümünün etkin olduğunu belirtiyor",
                "Kasa açıldığında ana ekran simgesi sahte uygulamaya uyacak şekilde değişiyor",
                "Ses modelleri: yalnızca Vosk'un gerçekten yayınladığı diller, boyutlarıyla birlikte, ve silinebiliyorlar",
                "Kaydedilen fotoğraf ve dosyalar LuminaGram klasörüne gidiyor; daha önce kaydedilenler yerinde kalıyor",
                "Yenilikler artık uygulamanın desteklediği her dile çevrildi",
            },
            /* es      */ new String[]{
                "El aviso de dirección de criptomonedas ahora muestra la dirección que te pide comprobar",
                "Las notificaciones siguen llegando con la app cerrada, y los ajustes lo indican si Android nos silencia",
                "Los ajustes de la caja fuerte vuelven a leerse bien y marcan el modo y el estilo de señuelo activos",
                "Al activar la caja fuerte, el icono de la pantalla de inicio cambia para coincidir con la app señuelo",
                "Modelos de voz: solo los idiomas que Vosk publica realmente, con su tamaño, y se pueden borrar",
                "Las fotos y archivos guardados van a una carpeta LuminaGram; lo guardado antes se queda donde está",
                "Novedades ya está traducido a todos los idiomas que admite la app",
            },
            /* pt-br   */ new String[]{
                "O aviso de endereço de criptomoeda agora mostra o endereço que pede para você conferir",
                "As notificações continuam chegando com o app fechado, e os ajustes avisam quando o Android nos silencia",
                "Os ajustes do cofre voltam a ser legíveis e indicam o modo e o estilo de disfarce ativos",
                "Ao ligar o cofre, o ícone da tela inicial muda para combinar com o app disfarce",
                "Modelos de voz: só os idiomas que o Vosk realmente publica, com tamanhos, e podem ser excluídos",
                "Fotos e arquivos salvos vão para uma pasta LuminaGram; o que já foi salvo continua onde está",
                "Novidades agora está traduzido para todos os idiomas que o app suporta",
            },
            /* id      */ new String[]{
                "Peringatan alamat kripto kini menampilkan alamat yang diminta untuk Anda periksa",
                "Notifikasi tetap masuk saat aplikasi tertutup, dan pengaturan memberi tahu bila Android membisukan kami",
                "Pengaturan brankas kembali terbaca dengan benar dan menandai mode serta gaya samaran yang aktif",
                "Menyalakan brankas mengubah ikon layar utama agar cocok dengan aplikasi samaran",
                "Model suara: hanya bahasa yang benar-benar disediakan Vosk, lengkap dengan ukuran, dan bisa dihapus",
                "Foto dan berkas yang disimpan masuk ke folder LuminaGram; yang tersimpan sebelumnya tetap di tempatnya",
                "Yang Baru kini diterjemahkan ke semua bahasa yang didukung aplikasi",
            });
        put("1.3.1",
            /* zh-hans */ new String[]{
                "误触不会再把正在下载的更新丢掉",
                "下载会在后台继续，进度显示在通知栏",
                "下载完成后会主动提示安装，可以立刻装，也可以回来再装",
                "「新功能」之前停在 1.2.8，缺的版本已经补齐",
            },
            /* zh-hant */ new String[]{
                "誤觸不會再把正在下載的更新丟掉",
                "下載會在背景繼續，進度顯示在通知列",
                "下載完成後會主動提示安裝，可以立刻裝，也可以回來再裝",
                "「新功能」之前停在 1.2.8，缺的版本已經補齊",
            },
            /* ar */ new String[]{
                "لمسة عابرة لم تعد تلغي تنزيل التحديث",
                "يستمر التنزيل في الخلفية، مع عرض التقدم في شريط الإشعارات",
                "عند اكتمال التنزيل يعرض التطبيق تثبيته فورًا أو عند عودتك",
                "كانت صفحة \"ما الجديد\" متوقفة عند 1.2.8، وقد أُضيفت الإصدارات الناقصة",
            },
            /* ru */ new String[]{
                "Случайное касание больше не отменяет загрузку обновления",
                "Загрузка продолжается в фоне, прогресс виден в шторке уведомлений",
                "По завершении загрузки предлагается установка — сразу или когда вернётесь",
                "Раздел «Что нового» останавливался на 1.2.8, пропущенные версии дописаны",
            },
            /* fa */ new String[]{
                "لمس اتفاقی دیگر دانلود به‌روزرسانی را دور نمی‌اندازد",
                "دانلود در پس‌زمینه ادامه می‌یابد و پیشرفت آن در نوار اعلان دیده می‌شود",
                "پس از پایان دانلود، نصب پیشنهاد می‌شود؛ همان لحظه یا وقتی برگشتید",
                "صفحهٔ «تازه‌ها» تا ۱.۲.۸ متوقف مانده بود؛ نسخه‌های جاافتاده نوشته شد",
            },
            /* tr */ new String[]{
                "Yanlışlıkla dokunmak artık indirilen güncellemeyi iptal etmiyor",
                "İndirme arka planda sürüyor, ilerleme bildirim panelinde görünüyor",
                "İndirme bitince kurulum öneriliyor: hemen ya da geri döndüğünüzde",
                "Yenilikler sayfası 1.2.8'de kalmıştı; eksik sürümler yazıldı",
            },
            /* es */ new String[]{
                "Un toque accidental ya no descarta la descarga de la actualización",
                "La descarga continúa en segundo plano, con el progreso en la barra de notificaciones",
                "Al terminar la descarga se ofrece instalarla, en el momento o cuando vuelvas",
                "Novedades se había quedado en 1.2.8; las versiones que faltaban ya están escritas",
            },
            /* pt-br */ new String[]{
                "Um toque acidental não descarta mais o download da atualização",
                "O download continua em segundo plano, com o progresso na barra de notificações",
                "Quando o download termina, o app oferece instalar na hora ou quando você voltar",
                "Novidades tinha parado na 1.2.8; as versões que faltavam foram escritas",
            },
            /* id */ new String[]{
                "Ketukan tak sengaja tidak lagi membuang unduhan pembaruan",
                "Unduhan berlanjut di latar belakang, dengan progres di panel notifikasi",
                "Setelah unduhan selesai, aplikasi menawarkan pemasangan sekarang atau saat Anda kembali",
                "Halaman Yang Baru berhenti di 1.2.8; versi yang terlewat sudah ditulis",
            }
        );

        put("1.3.0",
            /* zh-hans */ new String[]{
                "登录码泄露防护：把 Telegram 登录码发给别人之前会先警告",
                "新登录提醒：有新登录会提示，扫码授权前会再确认一次",
                "限时动态可以完全关闭，包括头像圆圈和动态通知",
                "语音转文字之后会接着翻译，而不是停在转写文字",
                "每个对话可以设定语气：客户、同事、朋友、家人、长辈，或你自己的描述",
            },
            /* zh-hant */ new String[]{
                "登入碼外洩防護：把 Telegram 登入碼傳給別人之前會先警告",
                "新登入警示：有新登入會提示，掃碼授權前會再確認一次",
                "限時動態可以完全關閉，包括頭像圓圈和動態通知",
                "語音轉文字之後會接著翻譯，而不是停在轉寫文字",
                "每個對話可以設定語氣：客戶、同事、朋友、家人、長輩，或你自己的描述",
            },
            /* ar */ new String[]{
                "حماية رمز تسجيل الدخول: تحذير قبل إرسال رمز دخول تيليجرام إلى أي شخص",
                "تنبيهات تسجيل الدخول الجديد: إشعار عند كل دخول جديد، وتأكيد قبل التفويض برمز QR",
                "يمكن الآن إيقاف القصص تمامًا، بما في ذلك الحلقات حول الصور والإشعارات",
                "تحويل الصوت إلى نص يتابع إلى الترجمة بدل التوقف عند النص المكتوب",
                "نبرة لكل محادثة: عميل، زميل، صديق، عائلة، شخص أكبر سنًا، أو صياغتك أنت",
            },
            /* ru */ new String[]{
                "Защита кода входа: предупреждение перед отправкой кода входа Telegram кому-либо",
                "Оповещения о новых входах: уведомление о новом входе и подтверждение перед авторизацией по QR",
                "Истории теперь можно отключить полностью — вместе с кольцами на аватарах и уведомлениями",
                "Голос в текст продолжается переводом, а не останавливается на расшифровке",
                "Тон для каждого чата: клиент, коллега, друг, семья, старший или ваша формулировка",
            },
            /* fa */ new String[]{
                "محافظت از کد ورود: پیش از فرستادن کد ورود تلگرام برای دیگران هشدار می‌دهد",
                "هشدار ورود جدید: هر ورود تازه اطلاع داده می‌شود و پیش از تأیید با کد QR دوباره می‌پرسد",
                "استوری‌ها را می‌توان کاملاً خاموش کرد، شامل حلقه‌های دور عکس و اعلان‌ها",
                "تبدیل گفتار به متن تا ترجمه ادامه می‌یابد و روی متن پیاده‌شده متوقف نمی‌شود",
                "لحن هر گفت‌وگو: مشتری، همکار، دوست، خانواده، بزرگ‌تر، یا توصیف خودتان",
            },
            /* tr */ new String[]{
                "Giriş kodu koruması: Telegram giriş kodunuzu birine göndermeden önce uyarır",
                "Yeni giriş uyarıları: yeni girişleri bildirir, QR ile yetkilendirmeden önce onay ister",
                "Hikâyeler artık tamamen kapatılabiliyor; profil halkaları ve bildirimler dâhil",
                "Sesi yazıya dönüştürme, deşifreyle durmayıp çeviriye devam ediyor",
                "Sohbet başına üslup: müşteri, iş arkadaşı, arkadaş, aile, büyük veya kendi ifadeniz",
            },
            /* es */ new String[]{
                "Protección del código de acceso: avisa antes de que envíes a alguien tu código de acceso de Telegram",
                "Avisos de nuevos inicios de sesión: notifica los inicios nuevos y pide confirmación antes de autorizar por QR",
                "Las historias ya se pueden desactivar por completo, incluidos los anillos y las notificaciones",
                "Voz a texto sigue hasta la traducción en vez de detenerse en la transcripción",
                "Tono por chat: cliente, colega, amistad, familia, persona mayor o tus propias palabras",
            },
            /* pt-br */ new String[]{
                "Proteção do código de login: avisa antes de você enviar seu código de login do Telegram a alguém",
                "Alertas de novos logins: avisa sobre logins novos e confirma antes de autorizar por QR",
                "Os stories agora podem ser desativados completamente, incluindo os anéis e as notificações",
                "Voz para texto segue até a tradução em vez de parar na transcrição",
                "Tom por conversa: cliente, colega, amigo, família, pessoa mais velha ou suas próprias palavras",
            },
            /* id */ new String[]{
                "Perlindungan kode masuk: memperingatkan sebelum Anda mengirim kode masuk Telegram kepada siapa pun",
                "Peringatan login baru: memberi tahu setiap login baru dan meminta konfirmasi sebelum otorisasi QR",
                "Cerita kini bisa dimatikan sepenuhnya, termasuk cincin di foto profil dan notifikasinya",
                "Suara ke teks berlanjut ke terjemahan, tidak berhenti di transkrip",
                "Nada per obrolan: klien, rekan kerja, teman, keluarga, orang yang lebih tua, atau kata-kata Anda sendiri",
            }
        );

        put("1.2.9",
            /* zh-hans */ new String[]{
                "对话标题栏的翻译图标带出语言选单：分别设定对方和己方",
                "检查更新会显示进度，不再毫无反应",
                "个人资料照片的上传日期",
            },
            /* zh-hant */ new String[]{
                "對話標題列的翻譯圖示帶出語言選單：分別設定對方和己方",
                "檢查更新會顯示進度，不再毫無反應",
                "個人資料照片的上傳日期",
            },
            /* ar */ new String[]{
                "قائمة لغات على أيقونة الترجمة في شريط المحادثة: اضبط جهتهم وجهتك",
                "فحص التحديث صار يعرض تقدّمه بدل أن يبقى صامتًا",
                "تاريخ رفع صورة الملف الشخصي",
            },
            /* ru */ new String[]{
                "Меню языков на значке перевода в шапке чата: отдельно для их и вашей стороны",
                "Проверка обновлений показывает ход выполнения, а не молчит",
                "Дата загрузки фото профиля",
            },
            /* fa */ new String[]{
                "منوی زبان روی نماد ترجمه در نوار بالای گفت‌وگو: سمت آن‌ها و سمت خودتان",
                "بررسی به‌روزرسانی به‌جای سکوت، پیشرفت را نشان می‌دهد",
                "تاریخ بارگذاری عکس نمایه",
            },
            /* tr */ new String[]{
                "Sohbet başlığındaki çeviri simgesinde dil menüsü: karşı taraf ve kendi tarafınız",
                "Güncelleme denetimi sessiz kalmak yerine ilerlemeyi bildiriyor",
                "Profil fotoğrafının yüklenme tarihi",
            },
            /* es */ new String[]{
                "Menú de idiomas en el icono de traducción de la cabecera del chat: su lado y el tuyo",
                "La comprobación de actualizaciones informa del progreso en vez de quedarse en silencio",
                "Fecha de subida de la foto de perfil",
            },
            /* pt-br */ new String[]{
                "Menu de idiomas no ícone de tradução do cabeçalho da conversa: o lado dele e o seu",
                "A verificação de atualizações mostra o progresso em vez de ficar em silêncio",
                "Data de envio da foto de perfil",
            },
            /* id */ new String[]{
                "Menu bahasa pada ikon terjemahan di kepala obrolan: sisi mereka dan sisi Anda",
                "Pemeriksaan pembaruan kini menunjukkan progres, tidak lagi diam saja",
                "Tanggal unggah foto profil",
            }
        );

        put("1.2.8",
            /* zh-hans */ new String[]{
                "修正：开启发送前翻译时，标题栏的翻译图标一定会显示",
                "修正：送出的消息一定会同时显示原文和译文，不会再丢失原文",
            },
            /* zh-hant */ new String[]{
                "修正：開啟發送前翻譯時，標題列的翻譯圖示一定會顯示",
                "修正：送出的訊息一定會同時顯示原文和譯文，不會再遺失原文",
            },
            /* ar */ new String[]{
                "إصلاح: أيقونة الترجمة في شريط المحادثة تظهر دائمًا عند تفعيل الترجمة قبل الإرسال",
                "إصلاح: الرسائل المُرسلة تعرض دائمًا النص الأصلي مع الترجمة، ولم يعد الأصل يضيع",
            },
            /* ru */ new String[]{
                "Исправлено: значок перевода в шапке чата всегда виден при включённом переводе перед отправкой",
                "Исправлено: отправленные сообщения всегда показывают оригинал и перевод — оригиналы больше не теряются",
            },
            /* fa */ new String[]{
                "رفع اشکال: وقتی ترجمه پیش از ارسال روشن است، نماد ترجمه در نوار بالا همیشه دیده می‌شود",
                "رفع اشکال: پیام‌های فرستاده‌شده همیشه متن اصلی و ترجمه را نشان می‌دهند و متن اصلی گم نمی‌شود",
            },
            /* tr */ new String[]{
                "Düzeltme: göndermeden önce çeviri açıkken başlıktaki çeviri simgesi her zaman görünüyor",
                "Düzeltme: gönderilen iletiler her zaman özgün metni ve çeviriyi birlikte gösteriyor, özgün metin kaybolmuyor",
            },
            /* es */ new String[]{
                "Corrección: el icono de traducción de la cabecera se ve siempre cuando la traducción antes de enviar está activada",
                "Corrección: los mensajes enviados muestran siempre el original y la traducción; ya no se pierden los originales",
            },
            /* pt-br */ new String[]{
                "Correção: o ícone de tradução do cabeçalho fica sempre visível quando a tradução antes de enviar está ativa",
                "Correção: as mensagens enviadas sempre mostram o original e a tradução; os originais não se perdem mais",
            },
            /* id */ new String[]{
                "Perbaikan: ikon terjemahan di kepala obrolan selalu terlihat saat terjemahan sebelum kirim aktif",
                "Perbaikan: pesan terkirim selalu menampilkan teks asli dan terjemahannya; teks asli tidak hilang lagi",
            }
        );

        put("1.2.7",
            /* zh-hans */ new String[]{
                "发送前可以拖拽调整照片和文件的顺序",
                "转发的消息上会显示来源警示标签",
                "一段时间没用之后回来，会看到未读摘要横幅",
            },
            /* zh-hant */ new String[]{
                "發送前可以拖曳調整照片和檔案的順序",
                "轉發的訊息上會顯示來源警示標籤",
                "一段時間沒用之後回來，會看到未讀摘要橫幅",
            },
            /* ar */ new String[]{
                "سحب الصور والملفات لإعادة ترتيبها قبل الإرسال",
                "ملصق تحذير بمصدر التحويل على الرسائل المُحوَّلة",
                "شريط ملخص غير المقروء عند العودة بعد غياب",
            },
            /* ru */ new String[]{
                "Перетаскивание фото и файлов для сортировки перед отправкой",
                "Метка-предупреждение об источнике на пересланных сообщениях",
                "Баннер со сводкой непрочитанных при возвращении после перерыва",
            },
            /* fa */ new String[]{
                "کشیدن عکس‌ها و فایل‌ها برای مرتب‌سازی پیش از ارسال",
                "برچسب هشدار منبع روی پیام‌های فوروارد شده",
                "نوار خلاصهٔ خوانده‌نشده‌ها هنگام بازگشت پس از مدتی غیبت",
            },
            /* tr */ new String[]{
                "Göndermeden önce fotoğrafları ve dosyaları sürükleyip yeniden sıralama",
                "İletilen iletilerde iletme kaynağı uyarı etiketi",
                "Bir süre uzak kaldıktan sonra dönüşte okunmamış özeti şeridi",
            },
            /* es */ new String[]{
                "Arrastra para reordenar fotos y archivos antes de enviarlos",
                "Etiqueta de aviso de origen en los mensajes reenviados",
                "Banner con el resumen de no leídos al volver tras una ausencia",
            },
            /* pt-br */ new String[]{
                "Arraste para reordenar fotos e arquivos antes de enviar",
                "Etiqueta de aviso de origem nas mensagens encaminhadas",
                "Faixa com o resumo de não lidas ao voltar depois de um tempo",
            },
            /* id */ new String[]{
                "Seret untuk mengurutkan ulang foto dan berkas sebelum mengirim",
                "Label peringatan asal pada pesan yang diteruskan",
                "Spanduk ringkasan belum dibaca saat kembali setelah lama tidak aktif",
            }
        );

        put("1.2.6",
            /* zh-hans */ new String[]{
                "收到的媒体自动模糊，点一下才显示",
                "私聊中的截屏检测提醒",
                "「新功能」更新日志页面",
            },
            /* zh-hant */ new String[]{
                "收到的媒體自動模糊，點一下才顯示",
                "私訊中的截圖偵測提醒",
                "「新功能」更新日誌頁面",
            },
            /* ar */ new String[]{
                "طمس الوسائط الواردة تلقائيًا حتى تنقر لإظهارها",
                "تنبيهات اكتشاف لقطات الشاشة في المحادثات الخاصة",
                "صفحة \"ما الجديد\" لسجل التغييرات",
            },
            /* ru */ new String[]{
                "Автоматическое размытие входящих медиа, пока вы не нажмёте",
                "Оповещения об обнаружении скриншотов в личных чатах",
                "Экран «Что нового» с историей изменений",
            },
            /* fa */ new String[]{
                "تار کردن خودکار رسانه‌های دریافتی تا وقتی برای دیدنشان ضربه بزنید",
                "هشدار شناسایی اسکرین‌شات در گفت‌وگوهای خصوصی",
                "صفحهٔ «تازه‌ها» با تاریخچهٔ تغییرات",
            },
            /* tr */ new String[]{
                "Gelen medyayı, dokunup gösterene kadar otomatik bulanıklaştırma",
                "Özel sohbetlerde ekran görüntüsü algılama uyarıları",
                "Değişiklik geçmişini gösteren Yenilikler ekranı",
            },
            /* es */ new String[]{
                "Desenfoque automático de los medios recibidos hasta que tocas para verlos",
                "Avisos de detección de capturas de pantalla en chats privados",
                "Pantalla de Novedades con el historial de cambios",
            },
            /* pt-br */ new String[]{
                "Desfoque automático da mídia recebida até você tocar para ver",
                "Alertas de detecção de captura de tela em conversas privadas",
                "Tela de Novidades com o histórico de mudanças",
            },
            /* id */ new String[]{
                "Media masuk diburamkan otomatis sampai Anda ketuk untuk melihat",
                "Peringatan deteksi tangkapan layar di obrolan pribadi",
                "Halaman Yang Baru berisi riwayat perubahan",
            }
        );

        put("1.2.5",
            /* zh-hans */ new String[]{
                "语音转文字（Vosk 离线，或用你自己的云端 API 密钥）",
                "语音转文字引擎与语音模型下载的设置页面",
            },
            /* zh-hant */ new String[]{
                "語音轉文字（Vosk 離線，或用你自己的雲端 API 金鑰）",
                "語音轉文字引擎與語音模型下載的設定頁面",
            },
            /* ar */ new String[]{
                "تحويل الصوت إلى نص (Vosk دون اتصال، أو بمفتاح السحابة الخاص بك)",
                "صفحة إعدادات لمحركات تحويل الصوت إلى نص وتنزيل النماذج",
            },
            /* ru */ new String[]{
                "Расшифровка голоса в текст (Vosk офлайн или ваш собственный облачный ключ)",
                "Страница настроек для движков распознавания речи и загрузки моделей",
            },
            /* fa */ new String[]{
                "تبدیل گفتار به متن (Vosk به‌صورت آفلاین یا با کلید ابری خودتان)",
                "صفحهٔ تنظیمات برای موتورهای تبدیل گفتار به متن و دانلود مدل‌ها",
            },
            /* tr */ new String[]{
                "Sesi yazıya dönüştürme (çevrimdışı Vosk veya kendi bulut anahtarınız)",
                "Konuşma tanıma motorları ve model indirmeleri için ayarlar sayfası",
            },
            /* es */ new String[]{
                "Transcripción de voz a texto (Vosk sin conexión o tu propia clave en la nube)",
                "Página de ajustes para los motores de voz a texto y la descarga de modelos",
            },
            /* pt-br */ new String[]{
                "Transcrição de voz para texto (Vosk offline ou sua própria chave na nuvem)",
                "Página de configurações para os motores de voz para texto e o download de modelos",
            },
            /* id */ new String[]{
                "Transkripsi suara ke teks (Vosk luring atau kunci awan milik Anda sendiri)",
                "Halaman pengaturan untuk mesin suara ke teks dan unduhan model",
            }
        );

        put("1.2.4",
            /* zh-hans */ new String[]{
                "统一的伪装保险箱：密码门模式与诱饵应用模式",
                "可以真的用的笔记本诱饵界面",
                "保险箱设置改版（模式／外观／密码）",
            },
            /* zh-hant */ new String[]{
                "統一的偽裝保險箱：密碼門模式與誘餌應用模式",
                "可以真的用的筆記本誘餌介面",
                "保險箱設定改版（模式／外觀／密碼）",
            },
            /* ar */ new String[]{
                "خزنة تمويه موحّدة: وضع باب كلمة المرور ووضع التطبيق البديل",
                "واجهة ملاحظات بديلة تعمل فعلًا",
                "إعادة تصميم إعدادات الخزنة (الوضع، المظهر، الرمز)",
            },
            /* ru */ new String[]{
                "Единый сейф маскировки: режим двери с паролем и режим приложения-обманки",
                "Работающая заметочная обманка, а не пустышка",
                "Переработанные настройки сейфа (режим, оформление, код)",
            },
            /* fa */ new String[]{
                "گاوصندوق استتار یکپارچه: حالت درِ رمزدار و حالت برنامهٔ ساختگی",
                "یادداشت ساختگی که واقعاً کار می‌کند",
                "بازطراحی تنظیمات گاوصندوق (حالت، ظاهر، رمز)",
            },
            /* tr */ new String[]{
                "Birleşik gizlenme kasası: parola kapısı modu ve sahte uygulama modu",
                "Gerçekten çalışan not defteri sahte ekranı",
                "Kasa ayarlarının yeniden tasarımı (mod, görünüm, kod)",
            },
            /* es */ new String[]{
                "Caja fuerte de disfraz unificada: modo puerta con contraseña y modo app señuelo",
                "Bloc de notas señuelo que funciona de verdad",
                "Rediseño de los ajustes de la caja fuerte (modo, apariencia y código)",
            },
            /* pt-br */ new String[]{
                "Cofre de disfarce unificado: modo porta com senha e modo app isca",
                "Bloco de notas isca que funciona de verdade",
                "Redesenho das configurações do cofre (modo, aparência e código)",
            },
            /* id */ new String[]{
                "Brankas penyamaran terpadu: mode pintu kata sandi dan mode aplikasi umpan",
                "Catatan umpan yang benar-benar berfungsi",
                "Perombakan pengaturan brankas (mode, tampilan, kode)",
            }
        );

        put("1.2.3",
            /* zh-hans */ new String[]{
                "七组安全审查，问题全部修掉",
                "修正翻译模式的判断（回到前台时会绕过手动模式）",
                "发送前翻译加了 20 秒看门狗，避免卡住送不出去",
                "预览语言变更后会清掉旧的缓存",
                "撤销发送：空内容防护，并在重启后保留",
                "同语言不再重复翻译",
                "紧急清除：先登出，再在后台抹除资料",
                "假崩溃密码会检查是否与真正的锁定密码重复",
                "伪装图标不再出现在图标选择器和会员预览里",
            },
            /* zh-hant */ new String[]{
                "七組安全審查，問題全部修掉",
                "修正翻譯模式的判斷（回到前景時會繞過手動模式）",
                "發送前翻譯加了 20 秒看門狗，避免卡住送不出去",
                "預覽語言變更後會清掉舊的快取",
                "復原發送：空內容防護，並在重啟後保留",
                "同語言不再重複翻譯",
                "緊急清除：先登出，再在背景抹除資料",
                "假當機密碼會檢查是否與真正的鎖定密碼重複",
                "偽裝圖示不再出現在圖示選擇器和會員預覽裡",
            },
            /* ar */ new String[]{
                "تدقيق أمني من سبع مجموعات، مع إصلاح كل ما ظهر",
                "إصلاح فحص وضع الترجمة (تجاوز الوضع اليدوي عند العودة إلى التطبيق)",
                "مؤقّت حماية 20 ثانية للترجمة قبل الإرسال يمنع تعليق الرسائل",
                "مسح ذاكرة المعاينة عند تغيير لغتها",
                "التراجع عن الإرسال: حماية من النص الفارغ مع حفظه بعد إعادة التشغيل",
                "منع إعادة الترجمة إلى اللغة نفسها",
                "المسح الطارئ: تسجيل الخروج أولًا ثم المحو في الخلفية",
                "التحقق من عدم تطابق رمز التعطل الوهمي مع رمز القفل الحقيقي",
                "إخفاء أيقونات التمويه من مُنتقي الأيقونات ومن معاينة بريميوم",
            },
            /* ru */ new String[]{
                "Аудит безопасности семью группами, все найденные проблемы исправлены",
                "Исправлена проверка режима перевода (обход ручного режима при возврате в приложение)",
                "20-секундный сторож для перевода перед отправкой — сообщения больше не зависают",
                "Сброс кэша предпросмотра при смене его языка",
                "Отмена отправки: защита от пустого текста и сохранение после перезапуска",
                "Повторный перевод на тот же язык больше не выполняется",
                "Экстренная очистка: сначала выход из аккаунта, затем стирание в фоне",
                "Проверка, что код ложного сбоя не совпадает с настоящим кодом-паролем",
                "Значки маскировки убраны из выбора значков и из премиум-предпросмотра",
            },
            /* fa */ new String[]{
                "بازبینی امنیتی با هفت گروه و رفع همهٔ موارد یافته‌شده",
                "اصلاح بررسی حالت ترجمه (دور زدن حالت دستی هنگام بازگشت به برنامه)",
                "نگهبان ۲۰ ثانیه‌ای برای ترجمه پیش از ارسال تا پیام‌ها گیر نکنند",
                "پاک شدن حافظهٔ پیش‌نمایش هنگام تغییر زبان آن",
                "لغو ارسال: محافظت در برابر متن خالی و ماندگاری پس از راه‌اندازی دوباره",
                "جلوگیری از ترجمهٔ دوباره به همان زبان",
                "پاک‌سازی اضطراری: نخست خروج از حساب، سپس پاک کردن در پس‌زمینه",
                "بررسی اینکه رمز خرابی جعلی با رمز قفل واقعی یکی نباشد",
                "نمادهای استتار از انتخابگر نماد و پیش‌نمایش پریمیوم برداشته شد",
            },
            /* tr */ new String[]{
                "Yedi ekiple güvenlik denetimi ve bulunan her sorunun giderilmesi",
                "Çeviri modu denetimi düzeltildi (uygulamaya dönüşte elle modun atlanması)",
                "Göndermeden önce çeviri için 20 saniyelik bekçi: iletiler artık takılmıyor",
                "Önizleme dili değişince önbelleğin temizlenmesi",
                "Göndermeyi geri alma: boş metne karşı koruma ve yeniden başlatmadan sonra kalıcılık",
                "Aynı dile yeniden çeviri yapılmasının önlenmesi",
                "Acil silme: önce oturumu kapatma, ardından arka planda silme",
                "Sahte çökme kodunun gerçek kilit koduyla çakışıp çakışmadığının denetimi",
                "Gizlenme simgeleri simge seçicisinden ve premium önizlemesinden kaldırıldı",
            },
            /* es */ new String[]{
                "Auditoría de seguridad con siete equipos y todas las correcciones aplicadas",
                "Corregida la comprobación del modo de traducción (se saltaba el modo manual al volver a la app)",
                "Vigilante de 20 s en la traducción antes de enviar: los mensajes ya no se quedan atascados",
                "Se limpia la caché de la vista previa al cambiar su idioma",
                "Deshacer envío: protección contra texto vacío y persistencia tras reiniciar",
                "Ya no se retraduce a un idioma que ya es el mismo",
                "Borrado de emergencia: primero cerrar sesión, después borrar en segundo plano",
                "Se comprueba que el código de fallo falso no coincida con el código de bloqueo real",
                "Los iconos de disfraz ya no aparecen en el selector de iconos ni en la vista previa de Premium",
            },
            /* pt-br */ new String[]{
                "Auditoria de segurança com sete equipes e todas as correções aplicadas",
                "Corrigida a checagem do modo de tradução (o modo manual era ignorado ao voltar ao app)",
                "Vigia de 20 s na tradução antes de enviar: as mensagens não travam mais",
                "O cache da pré-visualização é limpo quando o idioma dela muda",
                "Desfazer envio: proteção contra texto vazio e persistência depois de reiniciar",
                "Não há mais retradução para um idioma que já é o mesmo",
                "Apagamento de emergência: primeiro sair da conta, depois apagar em segundo plano",
                "Verificação de que o código de falha falsa não coincide com o código de bloqueio real",
                "Os ícones de disfarce saíram do seletor de ícones e da pré-visualização do Premium",
            },
            /* id */ new String[]{
                "Audit keamanan oleh tujuh tim, semua temuan diperbaiki",
                "Perbaikan pemeriksaan mode terjemahan (mode manual terlewati saat kembali ke aplikasi)",
                "Penjaga 20 detik untuk terjemahan sebelum kirim agar pesan tidak tersangkut",
                "Cache pratinjau dibersihkan saat bahasanya diubah",
                "Urungkan kirim: perlindungan terhadap teks kosong dan tetap ada setelah mulai ulang",
                "Tidak lagi menerjemahkan ulang ke bahasa yang sudah sama",
                "Penghapusan darurat: keluar akun dulu, lalu hapus di latar belakang",
                "Pemeriksaan agar kode crash palsu tidak sama dengan kode kunci asli",
                "Ikon penyamaran disembunyikan dari pemilih ikon dan pratinjau Premium",
            }
        );

        put("1.2.2",
            /* zh-hans */ new String[]{
                "对话标题栏加上翻译开关图标",
                "翻译模式：全部（自动）或手动（每个对话自己开）",
                "修正双语显示时字幕时间重叠的问题",
            },
            /* zh-hant */ new String[]{
                "對話標題列加上翻譯開關圖示",
                "翻譯模式：全部（自動）或手動（每個對話自己開）",
                "修正雙語顯示時字幕時間重疊的問題",
            },
            /* ar */ new String[]{
                "أيقونة تبديل الترجمة في شريط عنوان المحادثة",
                "وضع الترجمة: الكل (تلقائي) أو يدوي (تفعيل لكل محادثة)",
                "إصلاح تداخل التوقيت في العرض بلغتين",
            },
            /* ru */ new String[]{
                "Значок включения перевода в шапке чата",
                "Режим перевода: все чаты (автоматически) или вручную для каждого чата",
                "Исправлено наложение времени в двуязычном отображении",
            },
            /* fa */ new String[]{
                "نماد روشن و خاموش کردن ترجمه در نوار عنوان گفت‌وگو",
                "حالت ترجمه: همه (خودکار) یا دستی (برای هر گفت‌وگو جداگانه)",
                "رفع هم‌پوشانی زمان در نمایش دوزبانه",
            },
            /* tr */ new String[]{
                "Sohbet başlık çubuğunda çeviri açma ve kapama simgesi",
                "Çeviri modu: Tümü (otomatik) veya Elle (her sohbet için ayrı)",
                "İki dilli gösterimde zaman çakışması düzeltildi",
            },
            /* es */ new String[]{
                "Icono para activar la traducción en la barra de título del chat",
                "Modo de traducción: Todos (automático) o Manual (activándolo en cada chat)",
                "Corregido el solapamiento de la hora en la vista bilingüe",
            },
            /* pt-br */ new String[]{
                "Ícone para ativar a tradução na barra de título da conversa",
                "Modo de tradução: Todas (automático) ou Manual (ativando em cada conversa)",
                "Corrigida a sobreposição do horário na exibição bilíngue",
            },
            /* id */ new String[]{
                "Ikon pengalih terjemahan di bilah judul obrolan",
                "Mode terjemahan: Semua (otomatis) atau Manual (diaktifkan per obrolan)",
                "Perbaikan tumpang tindih waktu pada tampilan dwibahasa",
            }
        );

        put("1.2.1",
            /* zh-hans */ new String[]{
                "双向翻译：送出语言和阅读语言分开设定",
                "送出翻译时自动侦测对方的语言",
                "翻译设置里加上送出语言与阅读语言的选择",
                "可以分别决定单聊和群组要不要翻译",
            },
            /* zh-hant */ new String[]{
                "雙向翻譯：送出語言和閱讀語言分開設定",
                "送出翻譯時自動偵測對方的語言",
                "翻譯設定裡加上送出語言與閱讀語言的選擇",
                "可以分別決定單聊和群組要不要翻譯",
            },
            /* ar */ new String[]{
                "ترجمة ثنائية الاتجاه: لغة إرسال ولغة قراءة منفصلتان",
                "اكتشاف لغة المستلم تلقائيًا للترجمة عند الإرسال",
                "مُحدِّدات لغة الإرسال والقراءة في إعدادات الترجمة",
                "مفاتيح لتحديد النطاق: المحادثات الخاصة والمجموعات",
            },
            /* ru */ new String[]{
                "Двусторонний перевод: раздельные языки отправки и чтения",
                "Автоопределение языка собеседника для перевода исходящих",
                "Выбор языков отправки и чтения в настройках перевода",
                "Переключатели области: личные чаты и группы",
            },
            /* fa */ new String[]{
                "ترجمهٔ دوطرفه: زبان ارسال و زبان خواندن جداگانه",
                "تشخیص خودکار زبان مخاطب برای ترجمهٔ پیام‌های ارسالی",
                "انتخاب زبان ارسال و زبان خواندن در تنظیمات ترجمه",
                "کلیدهای دامنه: گفت‌وگوهای خصوصی و گروه‌ها",
            },
            /* tr */ new String[]{
                "Çift yönlü çeviri: gönderme ve okuma dilleri ayrı ayrı",
                "Giden çeviri için alıcının dilini otomatik algılama",
                "Çeviri ayarlarında gönderme ve okuma dili seçicileri",
                "Kapsam anahtarları: özel sohbetler ve gruplar",
            },
            /* es */ new String[]{
                "Traducción bidireccional: idiomas de envío y de lectura por separado",
                "Detección automática del idioma del destinatario al traducir lo que envías",
                "Selectores de idioma de envío y de lectura en los ajustes de traducción",
                "Interruptores de alcance: chats privados y grupos",
            },
            /* pt-br */ new String[]{
                "Tradução bidirecional: idiomas de envio e de leitura separados",
                "Detecção automática do idioma do destinatário ao traduzir o que você envia",
                "Seletores de idioma de envio e de leitura nas configurações de tradução",
                "Chaves de alcance: conversas privadas e grupos",
            },
            /* id */ new String[]{
                "Terjemahan dua arah: bahasa pengiriman dan bahasa bacaan terpisah",
                "Deteksi otomatis bahasa penerima untuk terjemahan pesan keluar",
                "Pemilih bahasa pengiriman dan bacaan di pengaturan terjemahan",
                "Sakelar cakupan: obrolan pribadi dan grup",
            }
        );

        put("1.2.0",
            /* zh-hans */ new String[]{
                "品牌保护：云端语言包不会再覆盖掉 LuminaGram 的名称",
                "LocaleController 会动态替换品牌名称",
            },
            /* zh-hant */ new String[]{
                "品牌保護：雲端語言包不會再覆蓋掉 LuminaGram 的名稱",
                "LocaleController 會動態替換品牌名稱",
            },
            /* ar */ new String[]{
                "حماية العلامة: حزمة اللغة السحابية لم تعد تستبدل اسم LuminaGram",
                "استبدال ديناميكي لاسم العلامة في LocaleController",
            },
            /* ru */ new String[]{
                "Защита названия: облачный языковой пакет больше не затирает название LuminaGram",
                "Динамическая замена названия в LocaleController",
            },
            /* fa */ new String[]{
                "محافظت از نام برند: بستهٔ زبان ابری دیگر نام LuminaGram را بازنویسی نمی‌کند",
                "جایگزینی پویای نام برند در LocaleController",
            },
            /* tr */ new String[]{
                "Marka koruması: bulut dil paketi artık LuminaGram adını ezmiyor",
                "LocaleController içinde dinamik marka adı değişimi",
            },
            /* es */ new String[]{
                "Protección de marca: el paquete de idioma en la nube ya no sobrescribe el nombre LuminaGram",
                "Sustitución dinámica del nombre de marca en LocaleController",
            },
            /* pt-br */ new String[]{
                "Proteção da marca: o pacote de idioma na nuvem não sobrescreve mais o nome LuminaGram",
                "Substituição dinâmica do nome da marca no LocaleController",
            },
            /* id */ new String[]{
                "Perlindungan merek: paket bahasa awan tidak lagi menimpa nama LuminaGram",
                "Penggantian nama merek secara dinamis di LocaleController",
            }
        );

        put("1.1.9",
            /* zh-hans */ new String[]{
                "稳定性大修：修正诱饵保险箱的崩溃",
                "避免更换启动图标后 App 打不开",
                "收讯翻译：非 Telegram 引擎不再被会员限制挡住",
                "送出的原文会以对话与消息编号存起来，不会遗失",
                "翻译进行中会上锁，避免重复送出",
                "灰色地带的隐蔽功能暂时收起，留到 Safe 里程碑再谈",
            },
            /* zh-hant */ new String[]{
                "穩定性大修：修正誘餌保險箱的當機",
                "避免更換啟動圖示後 App 打不開",
                "收訊翻譯：非 Telegram 引擎不再被會員限制擋住",
                "送出的原文會以對話與訊息編號存起來，不會遺失",
                "翻譯進行中會上鎖，避免重複送出",
                "灰色地帶的隱蔽功能暫時收起，留到 Safe 里程碑再談",
            },
            /* ar */ new String[]{
                "إصدار استقرار كبير: إصلاح تعطّل خزنة التمويه",
                "منع تعطّل التطبيق بعد تبديل أيقونة المشغّل",
                "ترجمة الوارد: محركات غير تيليجرام لم تعد محجوبة خلف بريميوم",
                "حفظ النص الأصلي المُرسل حسب رقم المحادثة والرسالة",
                "قفل أثناء الترجمة يمنع الإرسال المكرر",
                "تأجيل الميزات الخفية غير الواضحة إلى مرحلة Safe",
            },
            /* ru */ new String[]{
                "Крупный релиз стабильности: исправлен сбой сейфа-обманки",
                "Защита от того, чтобы приложение переставало открываться после смены значка",
                "Перевод входящих: движки, кроме телеграмного, больше не блокируются премиумом",
                "Отправленный оригинал сохраняется по номеру чата и сообщения",
                "Блокировка на время перевода — сообщение не уходит дважды",
                "Спорные скрытные функции отложены до этапа Safe",
            },
            /* fa */ new String[]{
                "نسخهٔ بزرگ پایداری: رفع خرابی گاوصندوق ساختگی",
                "جلوگیری از باز نشدن برنامه پس از تعویض نماد اجرا",
                "ترجمهٔ دریافتی: موتورهای غیر تلگرامی دیگر پشت پریمیوم قفل نیستند",
                "متن اصلی ارسالی بر پایهٔ شمارهٔ گفت‌وگو و پیام ذخیره می‌شود",
                "قفل هنگام ترجمه تا پیام دوبار فرستاده نشود",
                "ویژگی‌های پنهان‌کارانهٔ مرزی تا مرحلهٔ Safe کنار گذاشته شد",
            },
            /* tr */ new String[]{
                "Büyük kararlılık sürümü: sahte kasa çökmesi giderildi",
                "Başlatıcı simgesi değiştikten sonra uygulamanın açılmaz hâle gelmesinin önlenmesi",
                "Gelen çeviri: Telegram dışı motorlar artık premium duvarına takılmıyor",
                "Gönderilen özgün metin, sohbet ve ileti numarasına göre saklanıyor",
                "Çeviri sürerken kilit: ileti iki kez gönderilmiyor",
                "Sınırdaki gizlilik özellikleri Safe kilometre taşına bırakıldı",
            },
            /* es */ new String[]{
                "Versión centrada en estabilidad: corregido el fallo de la caja fuerte señuelo",
                "Prevención de que la app deje de abrirse tras cambiar el icono del lanzador",
                "Traducción de lo recibido: los motores distintos al de Telegram ya no exigen Premium",
                "El texto original enviado se guarda por número de chat y de mensaje",
                "Bloqueo mientras se traduce: el mensaje no se envía dos veces",
                "Las funciones sigilosas de zona gris quedan aparcadas hasta el hito Safe",
            },
            /* pt-br */ new String[]{
                "Versão focada em estabilidade: corrigida a falha do cofre isca",
                "Prevenção de o app parar de abrir depois de trocar o ícone do lançador",
                "Tradução do que chega: os motores fora do Telegram não exigem mais Premium",
                "O texto original enviado é guardado por número de conversa e de mensagem",
                "Trava durante a tradução: a mensagem não é enviada duas vezes",
                "Os recursos furtivos de zona cinzenta ficam parados até o marco Safe",
            },
            /* id */ new String[]{
                "Rilis besar untuk kestabilan: perbaikan crash pada brankas umpan",
                "Pencegahan aplikasi tidak bisa dibuka setelah ikon peluncur diganti",
                "Terjemahan pesan masuk: mesin selain Telegram tidak lagi terhalang Premium",
                "Teks asli yang dikirim disimpan berdasarkan nomor obrolan dan pesan",
                "Kunci selama penerjemahan agar pesan tidak terkirim dua kali",
                "Fitur senyap di area abu-abu ditunda sampai tonggak Safe",
            }
        );

        put("1.1.2",
            /* zh-hans */ new String[]{
                "链接安全检查（打开前先看是不是钓鱼网址）",
                "假崩溃解锁（被强迫开锁时用）",
                "跨语言搜索（NFKD 转写，忽略变音符号）",
                "联系人的私密备注与标签",
                "在对话里滚动就会收起键盘",
                "贴纸大小可以自己调",
            },
            /* zh-hant */ new String[]{
                "連結安全檢查（開啟前先看是不是釣魚網址）",
                "假當機解鎖（被強迫開鎖時用）",
                "跨語言搜尋（NFKD 轉寫，忽略變音符號）",
                "聯絡人的私密備註與標籤",
                "在對話裡捲動就會收起鍵盤",
                "貼圖大小可以自己調",
            },
            /* ar */ new String[]{
                "فاحص أمان الروابط (تحقق من الروابط لكشف التصيّد)",
                "فتح بإيهام التعطل (للحماية عند الإكراه)",
                "بحث عابر للغات (نقل حرفي NFKD يتجاهل الحركات)",
                "ملاحظات ووسوم خاصة لجهات الاتصال",
                "إخفاء لوحة المفاتيح عند تمرير المحادثة",
                "شريط تمرير لضبط حجم الملصقات",
            },
            /* ru */ new String[]{
                "Проверка безопасности ссылок (распознавание фишинговых адресов)",
                "Разблокировка ложным сбоем (на случай принуждения)",
                "Межъязыковой поиск (транслитерация NFKD, без диакритики)",
                "Личные заметки и метки к контактам",
                "Клавиатура убирается при прокрутке чата",
                "Ползунок размера стикеров",
            },
            /* fa */ new String[]{
                "بررسی امنیت پیوند (تشخیص نشانی‌های فیشینگ)",
                "بازگشایی با خرابی جعلی (برای مواقع اجبار)",
                "جست‌وجوی بین‌زبانی (نویسه‌گردانی NFKD، بدون اعراب)",
                "یادداشت‌ها و برچسب‌های خصوصی برای مخاطبان",
                "بسته شدن صفحه‌کلید هنگام پیمایش گفت‌وگو",
                "لغزندهٔ اندازهٔ استیکر",
            },
            /* tr */ new String[]{
                "Bağlantı güvenlik denetimi (kimlik avı adreslerini yakalar)",
                "Sahte çökme kilidi (zorlama anları için)",
                "Diller arası arama (NFKD çevriyazısı, aksan işaretlerini yok sayar)",
                "Kişilere özel notlar ve etiketler",
                "Sohbette kaydırınca klavyenin kapanması",
                "Çıkartma boyutu için kaydırma çubuğu",
            },
            /* es */ new String[]{
                "Inspector de seguridad de enlaces (detecta direcciones de phishing)",
                "Desbloqueo con fallo falso (protección bajo coacción)",
                "Búsqueda entre idiomas (transliteración NFKD, sin acentos)",
                "Notas y etiquetas privadas en los contactos",
                "El teclado se cierra al desplazar el chat",
                "Control deslizante para el tamaño de los stickers",
            },
            /* pt-br */ new String[]{
                "Inspetor de segurança de links (detecta endereços de phishing)",
                "Desbloqueio com falha falsa (proteção sob coação)",
                "Busca entre idiomas (transliteração NFKD, sem acentos)",
                "Notas e etiquetas privadas nos contatos",
                "O teclado se fecha ao rolar a conversa",
                "Controle deslizante para o tamanho das figurinhas",
            },
            /* id */ new String[]{
                "Pemeriksa keamanan tautan (mendeteksi alamat phishing)",
                "Buka kunci crash palsu (perlindungan saat dipaksa)",
                "Pencarian lintas bahasa (alih aksara NFKD, mengabaikan tanda diakritik)",
                "Catatan dan tag pribadi untuk kontak",
                "Papan ketik tertutup saat obrolan digulir",
                "Penggeser untuk mengatur ukuran stiker",
            }
        );

        put("1.0.8",
            /* zh-hans */ new String[]{
                "可以改用系统内建表情符号",
                "送出语音／视频消息前先确认",
                "关闭数字四舍五入",
                "一键全部解除拉黑",
                "界面设置页面",
            },
            /* zh-hant */ new String[]{
                "可以改用系統內建表情符號",
                "送出語音／視訊訊息前先確認",
                "關閉數字四捨五入",
                "一鍵全部解除封鎖",
                "介面設定頁面",
            },
            /* ar */ new String[]{
                "خيار استخدام إيموجي النظام",
                "تأكيد قبل إرسال الرسائل الصوتية والمرئية",
                "إيقاف تقريب الأرقام",
                "إلغاء حظر الكل بنقرة واحدة",
                "صفحة إعدادات الواجهة",
            },
            /* ru */ new String[]{
                "Возможность использовать системные эмодзи",
                "Подтверждение перед отправкой голосовых и видеосообщений",
                "Отключение округления чисел",
                "Разблокировать всех одним нажатием",
                "Страница настроек интерфейса",
            },
            /* fa */ new String[]{
                "گزینهٔ استفاده از ایموجی سیستم",
                "تأیید پیش از فرستادن پیام صوتی و تصویری",
                "خاموش کردن گرد کردن اعداد",
                "رفع مسدودیت همه با یک ضربه",
                "صفحهٔ تنظیمات رابط کاربری",
            },
            /* tr */ new String[]{
                "Sistem emojilerini kullanma seçeneği",
                "Sesli ve görüntülü ileti göndermeden önce onay",
                "Sayı yuvarlamayı kapatma",
                "Tek dokunuşla tümünün engelini kaldırma",
                "Arayüz ayarları sayfası",
            },
            /* es */ new String[]{
                "Opción de usar los emojis del sistema",
                "Confirmación antes de enviar mensajes de voz y de vídeo",
                "Desactivar el redondeo de números",
                "Desbloquear a todos con un toque",
                "Página de ajustes de interfaz",
            },
            /* pt-br */ new String[]{
                "Opção de usar os emojis do sistema",
                "Confirmação antes de enviar mensagens de voz e de vídeo",
                "Desativar o arredondamento de números",
                "Desbloquear todos com um toque",
                "Página de configurações de interface",
            },
            /* id */ new String[]{
                "Opsi memakai emoji bawaan sistem",
                "Konfirmasi sebelum mengirim pesan suara dan video",
                "Mematikan pembulatan angka",
                "Buka blokir semua dengan satu ketukan",
                "Halaman pengaturan antarmuka",
            }
        );

        put("1.0.5",
            /* zh-hans */ new String[]{
                "快捷回复模板",
                "消息书签",
                "紧凑聊天列表与列表选项",
                "收到的消息可以用多种翻译引擎翻译",
            },
            /* zh-hant */ new String[]{
                "快捷回覆範本",
                "訊息書籤",
                "精簡聊天列表與列表選項",
                "收到的訊息可以用多種翻譯引擎翻譯",
            },
            /* ar */ new String[]{
                "قوالب الردود السريعة",
                "إشارات مرجعية للرسائل",
                "قائمة دردشات مضغوطة وخيارات القائمة",
                "ترجمة الرسائل الواردة بعدة محركات",
            },
            /* ru */ new String[]{
                "Шаблоны быстрых ответов",
                "Закладки на сообщения",
                "Компактный список чатов и настройки списка",
                "Перевод входящих сообщений несколькими движками",
            },
            /* fa */ new String[]{
                "الگوهای پاسخ سریع",
                "نشانک برای پیام‌ها",
                "فهرست گفتگوی فشرده و گزینه‌های فهرست",
                "ترجمهٔ پیام‌های دریافتی با چند موتور",
            },
            /* tr */ new String[]{
                "Hızlı yanıt şablonları",
                "İleti yer imleri",
                "Sıkışık sohbet listesi ve liste seçenekleri",
                "Gelen iletileri birden çok motorla çevirme",
            },
            /* es */ new String[]{
                "Plantillas de respuestas rápidas",
                "Marcadores de mensajes",
                "Lista de chats compacta y opciones de la lista",
                "Traducción de los mensajes recibidos con varios motores",
            },
            /* pt-br */ new String[]{
                "Modelos de respostas rápidas",
                "Marcadores de mensagens",
                "Lista de conversas compacta e opções da lista",
                "Tradução das mensagens recebidas com vários motores",
            },
            /* id */ new String[]{
                "Templat balasan cepat",
                "Penanda pesan",
                "Daftar obrolan ringkas dan opsi daftar",
                "Terjemahan pesan masuk dengan beberapa mesin",
            }
        );

        put("1.0.0",
            /* zh-hans */ new String[]{
                "首个版本：LuminaGram 设置中心",
                "隐藏文件夹标签与隐藏限时动态的开关",
                "从 R2 直接更新 App",
                "十种语言的在地化",
            },
            /* zh-hant */ new String[]{
                "首個版本：LuminaGram 設定中心",
                "隱藏資料夾分頁與隱藏限時動態的開關",
                "從 R2 直接更新 App",
                "十種語言的在地化",
            },
            /* ar */ new String[]{
                "الإصدار الأول: مركز إعدادات LuminaGram",
                "مفاتيح إخفاء علامات تبويب المجلدات وإخفاء القصص",
                "تحديث التطبيق مباشرة من R2",
                "توطين بعشر لغات",
            },
            /* ru */ new String[]{
                "Первый выпуск: центр настроек LuminaGram",
                "Переключатели скрытия вкладок папок и историй",
                "Обновление приложения напрямую из R2",
                "Локализация на десять языков",
            },
            /* fa */ new String[]{
                "نخستین نسخه: مرکز تنظیمات LuminaGram",
                "کلیدهای پنهان کردن زبانه‌های پوشه و استوری‌ها",
                "به‌روزرسانی برنامه مستقیم از R2",
                "بومی‌سازی به ده زبان",
            },
            /* tr */ new String[]{
                "İlk sürüm: LuminaGram ayarlar merkezi",
                "Klasör sekmelerini ve hikâyeleri gizleme anahtarları",
                "Uygulamayı doğrudan R2 üzerinden güncelleme",
                "On dilde yerelleştirme",
            },
            /* es */ new String[]{
                "Primera versión: centro de ajustes de LuminaGram",
                "Interruptores para ocultar las pestañas de carpetas y las historias",
                "Actualización de la app directamente desde R2",
                "Localización en diez idiomas",
            },
            /* pt-br */ new String[]{
                "Primeira versão: central de configurações do LuminaGram",
                "Chaves para ocultar as abas de pastas e os stories",
                "Atualização do app direto do R2",
                "Localização em dez idiomas",
            },
            /* id */ new String[]{
                "Rilis pertama: pusat pengaturan LuminaGram",
                "Sakelar untuk menyembunyikan tab folder dan cerita",
                "Pembaruan aplikasi langsung dari R2",
                "Pelokalan dalam sepuluh bahasa",
            }
        );
    }
}
