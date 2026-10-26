package com.example.namazvakti

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Bildirimlerde gösterilecek Ayet ve Sahih Hadis havuzu
data class NotificationInspiration(
    val text: String,
    val source: String,
    val isHadith: Boolean = false
)

val SPIRITUAL_NOTIFICATIONS_LIST = listOf(
    NotificationInspiration(
        text = "Şüphesiz namaz, müminler üzerine vakitleri belirlenmiş bir farzdır.",
        source = "Nisâ Suresi, 103. Ayet"
    ),
    NotificationInspiration(
        text = "Namazı dosdoğru kılın, zekâtı verin ve rükû edenlerle birlikte siz de rükû edin.",
        source = "Bakara Suresi, 43. Ayet"
    ),
    NotificationInspiration(
        text = "Şüphesiz namaz, insanı her türlü hayasızlıktan ve kötülükten alıkoyar.",
        source = "Ankebût Suresi, 45. Ayet"
    ),
    NotificationInspiration(
        text = "Beni anın ki ben de sizi anayım. Bana şükredin, nankörlük etmeyin.",
        source = "Bakara Suresi, 152. Ayet"
    ),
    NotificationInspiration(
        text = "Sabır ve namazla Allah'tan yardım dileyin.",
        source = "Bakara Suresi, 45. Ayet"
    ),
    NotificationInspiration(
        text = "Gündüzün iki tarafında ve gecenin gündüze yakın saatlerinde namaz kıl. İyilikler kötülükleri giderir.",
        source = "Hûd Suresi, 114. Ayet"
    ),
    NotificationInspiration(
        text = "Kalpler ancak Allah'ı anmakla huzur ve sükûna kavuşur.",
        source = "Ra'd Suresi, 28. Ayet"
    ),
    NotificationInspiration(
        text = "Namaz dinin direğidir. Onu ikame eden dinini ayakta tutmuş olur.",
        source = "Hadis-i Şerif (Beyhaki)",
        isHadith = true
    ),
    NotificationInspiration(
        text = "Kıyamet gününde kulun ilk hesaba çekileceği amel namazdır.",
        source = "Hadis-i Şerif (Tirmizî)",
        isHadith = true
    ),
    NotificationInspiration(
        text = "Kulun Rabbine en yakın olduğu an, secde anıdır. Secdede çokça dua ediniz.",
        source = "Hadis-i Şerif (Müslim)",
        isHadith = true
    ),
    NotificationInspiration(
        text = "İki serinlik namazını (Sabah ve İkindi) vaktinde kılan kimse cennete girer.",
        source = "Hadis-i Şerif (Buhârî)",
        isHadith = true
    ),
    NotificationInspiration(
        text = "Yatsı namazını cemaatle kılan gecenin yarısını; sabah namazını cemaatle kılan ise gecenin tamamını ihya etmiş sayılır.",
        source = "Hadis-i Şerif (Müslim)",
        isHadith = true
    )
)

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra("label") ?: "Namaz Vakti"
        val time = intent.getLongExtra("time", 0L)
        val type = intent.getStringExtra("type") ?: "EXACT"
        val preMinutes = intent.getIntExtra("preMinutes", 15)
        val clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))

        // Rastgele ayet veya hadis seçimi
        val quote = SPIRITUAL_NOTIFICATIONS_LIST.random()
        val quotePrefix = if (quote.isHadith) "✨ Hadis-i Şerif:" else "📖 Ayet-i Kerime:"

        val title: String
        val body: String

        when {
            type == "EXACT" -> {
                title = "🕌 Vakit Girdi: $label"
                body = "$label namazı vakti girdi ($clock).\n\n$quotePrefix \"${quote.text}\"\n— ${quote.source}"
            }
            type.startsWith("PRE_") -> {
                title = "⏰ $label Vaktine $preMinutes Dk Kaldı"
                body = "Ezan saati: $clock. Abdest ve namaz için hazırlık vakti.\n\n$quotePrefix \"${quote.text}\"\n— ${quote.source}"
            }
            type == "KERAHAT" -> {
                when (label) {
                    "Öğle" -> {
                        title = "⚠️ Kerahat Vakti (İstiva / Zeval)"
                        body = "Öğle ezanına $preMinutes dakika kaldı ($clock). Güneş tam tepe noktasında olduğundan nafile namaz kılınması mekruhtur."
                    }
                    "Akşam" -> {
                        title = "⚠️ Kerahat Vakti (Güneş Batışı)"
                        body = "Akşam ezanına $preMinutes dakika kaldı ($clock). Güneş sararıp batarken ikindi namazının farzı hariç namaz kılınmaz."
                    }
                    else -> { // Güneş / Sabah
                        title = "⚠️ Kerahat Uyarısı: Güneş Doğuşu"
                        body = "Güneş doğmak üzere ($clock). Sabah namazınızı güneş doğmadan tamamlayınız."
                    }
                }
            }
            else -> {
                title = "$label Hatırlatması"
                body = "Saat: $clock\n\n$quotePrefix \"${quote.text}\"\n— ${quote.source}"
            }
        }

        val channelId = if (type == "KERAHAT") "namaz_vakti_channel_kerahat" else "namaz_vakti_channel_exact"
        PrayerScheduler.showNotification(context, title, body, channelId)

        // Bir sonraki vakitlerin alarmlarını arka planda tazele
        val pendingResult = goAsync()
        PrayerScheduler.refreshInBackground(context) {
            pendingResult.finish()
        }
    }
}