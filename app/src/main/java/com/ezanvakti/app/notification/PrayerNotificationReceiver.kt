package com.ezanvakti.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

// Ayet ve Hadis Modeli
data class SpiritualQuote(
    val text: String,
    val source: String,
    val isHadith: Boolean = false
)

// Zengin Ayet ve Sahih Hadis Havuzu
val SPIRITUAL_QUOTES = listOf(
    SpiritualQuote(
        text = "Şüphesiz namaz, müminler üzerine belirli vakitlerde farz kılınmıştır.",
        source = "Nisâ Suresi, 103. Ayet"
    ),
    SpiritualQuote(
        text = "Namazı dosdoğru kılın, zekâtı verin ve rükû edenlerle birlikte siz de rükû edin.",
        source = "Bakara Suresi, 43. Ayet"
    ),
    SpiritualQuote(
        text = "Şüphesiz namaz, insanı her türlü hayasızlıktan ve kötülükten alıkoyar.",
        source = "Ankebût Suresi, 45. Ayet"
    ),
    SpiritualQuote(
        text = "Beni anın ki ben de sizi anayım. Bana şükredin, nankörlük etmeyin.",
        source = "Bakara Suresi, 152. Ayet"
    ),
    SpiritualQuote(
        text = "Sabır ve namazla Allah'tan yardım dileyin.",
        source = "Bakara Suresi, 45. Ayet"
    ),
    SpiritualQuote(
        text = "Gündüzün iki tarafında ve gecenin gündüze yakın saatlerinde namaz kıl. Şüphesiz iyilikler kötülükleri giderir.",
        source = "Hûd Suresi, 114. Ayet"
    ),
    SpiritualQuote(
        text = "Kalpler ancak Allah'ı anmakla huzur ve sükûna kavuşur.",
        source = "Ra'd Suresi, 28. Ayet"
    ),
    SpiritualQuote(
        text = "Namaz dinin direğidir. Onu ikame eden dinini ikame etmiş olur.",
        source = "Hadis-i Şerif (Beyhaki)",
        isHadith = true
    ),
    SpiritualQuote(
        text = "Kıyamet gününde kulun ilk hesaba çekileceği amel namazdır.",
        source = "Hadis-i Şerif (Tirmizî)",
        isHadith = true
    ),
    SpiritualQuote(
        text = "Kulun Rabbine en yakın olduğu an, secde anıdır. Öyleyse secdede çokça dua ediniz.",
        source = "Hadis-i Şerif (Müslim)",
        isHadith = true
    ),
    SpiritualQuote(
        text = "İki serinlik namazını (Sabah ve İkindi) kılan kimse cennete girer.",
        source = "Hadis-i Şerif (Buhârî)",
        isHadith = true
    ),
    SpiritualQuote(
        text = "Müminin nurlu nişanı, aldığı abdest ve kıldığı namazın izleridir.",
        source = "Hadis-i Şerif (Müslim)",
        isHadith = true
    ),
    SpiritualQuote(
        text = "Bana dünyanızdan üç şey sevdirildi: Güzel koku, saliha kadın ve gözümün nuru namaz.",
        source = "Hadis-i Şerif (Nesâî)",
        isHadith = true
    )
)

class PrayerNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Namaz"
        val isBeforeReminder = intent.getBooleanExtra("IS_BEFORE", false)
        val minutesBefore = intent.getIntExtra("MINUTES_BEFORE", 15)

        sendPrayerWithInspirationNotification(context, prayerName, isBeforeReminder, minutesBefore)
    }

    private fun sendPrayerWithInspirationNotification(
        context: Context,
        prayerName: String,
        isBefore: Boolean,
        minutesBefore: Int
    ) {
        val channelId = "prayer_times_inspiration_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0+ Bildirim Kanalı
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Namaz Vakitleri & Vaktin Ayeti",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Namaz vakit hatırlatmaları ile ayet ve hadis paylaşımları"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tıklanınca uygulamayı aç
        val appIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Havuzdan Rastgele Bir Ayet veya Hadis Seç
        val quote = SPIRITUAL_QUOTES.random()
        val quotePrefix = if (quote.isHadith) "✨ Vaktin Hadisi:" else "📖 Vaktin Ayeti:"

        // Bildirim Başlığı ve Kısa Metni
        val title = if (isBefore) {
            "🕌 $prayerName Vaktine $minutesBefore Dakika Kaldı"
        } else {
            "🕌 $prayerName Vakti Girdi"
        }

        val shortSummary = if (isBefore) {
            "Vakit yaklaşıyor. Abdest ve hazırlık vakti."
        } else {
            "Haydin namaza, haydin kurtuluşa!"
        }

        // Genişletilebilir Detaylı Metin (BigTextStyle)
        val expandedBigText = buildString {
            append(shortSummary)
            append("\n\n")
            append(quotePrefix)
            append(" \"")
            append(quote.text)
            append("\"\n— ")
            append(quote.source)
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Varsa projenizdeki R.drawable.ic_notification
            .setContentTitle(title)
            .setContentText(shortSummary)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedBigText)
                    .setBigContentTitle(title)
                    .setSummaryText(if (quote.isHadith) "Günün Hadis-i Şerifi" else "Günün Ayet-i Kerimesi")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setColor(0xFF0F3E2C.toInt()) // Zümrüt Yeşili
            .build()

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }
}