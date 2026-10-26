package com.ezanvakti.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.firstnamazvakti.R
import com.ezanvakti.app.alarm.KerahatType
import com.ezanvakti.app.data.model.DailyContent
import com.ezanvakti.app.data.model.PrayerType
import com.ezanvakti.app.ui.MainActivity
import com.ezanvakti.app.alarm.AlarmScheduler.NotificationType

/**
 * Gelişmiş bildirim yardımcısı.
 *
 * Özellikler:
 * - Çoklu bildirim kanalları (tam vakit, öncesi, kerahat)
 * - Ezan sesi desteği
 * - Kerahat uyarıları
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        // Kanal ID'leri
        const val CHANNEL_ID_EXACT = "prayer_times_channel_exact"
        const val CHANNEL_ID_PRE = "prayer_times_channel_pre"
        const val CHANNEL_ID_KERAHAT = "prayer_times_channel_kerahat"

        // Bildirim ID'leri
        const val NOTIFICATION_ID_BASE = 1000
        const val NOTIFICATION_ID_PRE = 2000
        const val NOTIFICATION_ID_KERAHAT = 3000
    }

    init {
        createAllNotificationChannels()
    }

    /**
     * Tüm bildirim kanallarını oluşturur.
     */
    private fun createAllNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 1. Tam Vakit Kanalı - Yüksek öncelik, ezan sesi
            val exactChannel = NotificationChannel(
                CHANNEL_ID_EXACT,
                "Ezan Vakti Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Namaz vakitlerinde ezan ve günün hadisi/ayeti bildirimi"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 2. Vakit Öncesi Kanalı - Orta öncelik, sessiz/ton
            val preChannel = NotificationChannel(
                CHANNEL_ID_PRE,
                "Vakit Öncesi Hatırlatmalar",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Namaz vaktine dakikalar kalan hatırlatmalar"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200)
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 3. Kerahat Kanalı - Orta öncelik
            val kerahatChannel = NotificationChannel(
                CHANNEL_ID_KERAHAT,
                "Kerahat Vakti Uyarıları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kerahat vakitlerine girmeden önceki uyarılar"
                enableLights(true)
                lightColor = Color.parseColor("#F59E0B") // Turuncu
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 100, 300)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannels(listOf(exactChannel, preChannel, kerahatChannel))
        }
    }

    /**
     * Tam vakit bildirimini gösterir (ezan sesi ile).
     */
    fun showPrayerNotification(prayerType: PrayerType, content: DailyContent) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayerType.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = getSoundUri("namaz_vakti")
        val sourceFormatted = content.getFullSourceText()
        val bigMessage = "${content.text}\n\n— Kaynak: ${content.getFullSourceText()}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_EXACT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🕌 Vakit Girdi: ${prayerType.displayName}")
            .setContentText(content.text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigMessage)
                    .setSummaryText(prayerType.displayName)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(NOTIFICATION_ID_BASE + prayerType.ordinal, builder.build())
    }

    /**
     * Vakit öncesi bildirimini gösterir.
     */
    fun showPreNotification(prayerType: PrayerType, minutesBefore: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayerType.ordinal + 100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = getSoundUri("dakika_10")

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_PRE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ ${prayerType.displayName} Vaktine $minutesBefore Dakika Kaldı")
            .setContentText("${prayerType.displayName} namazı vaktine yaklaşılıyor.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(NOTIFICATION_ID_PRE + prayerType.ordinal, builder.build())
    }

    /**
     * Kerahat uyarı bildirimini gösterir.
     */
    fun showKerahatNotification(kerahatType: KerahatType, minutesBefore: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            kerahatType.ordinal + 200,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = getSoundUri("kerahat")

        val warningMessage = when (kerahatType) {
            KerahatType.MORNING_SUNRISE -> "Güneş doğduktan sonraki kerahat vaktine $minutesBefore dakika kaldı. Bu vakitte nafile namaz kılınmaz."
            KerahatType.BEFORE_DHUHR -> "Öğle vakti öncesi kerahat vaktine $minutesBefore dakika kaldı. Güneş tepeye yaklaşırken nafile namaz kılınmaz."
            KerahatType.BEFORE_MAGHRIB -> "Akşam vakti öncesi kerahat vaktine $minutesBefore dakika kaldı. Güneş batmadan önce nafile namaz kılınmaz."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_KERAHAT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⚠️ Kerahat Vakti Uyarısı: ${kerahatType.displayName}")
            .setContentText(warningMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(warningMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#F59E0B"))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(NOTIFICATION_ID_KERAHAT + kerahatType.ordinal, builder.build())
    }

    /**
     * Ses URI'sini döner.
     */
    private fun getSoundUri(soundKey: String): Uri? {
        return try {
            when (soundKey) {
                "namaz_vakti" -> {
                    // Özel ezan sesi (raw klasöründen)
                    val resourceId = context.resources.getIdentifier(
                        "ezan_sesi",
                        "raw",
                        context.packageName
                    )
                    if (resourceId != 0) {
                        Uri.parse("android.resource://${context.packageName}/$resourceId")
                    } else {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    }
                }
                "kerahat" -> {
                    val resourceId = context.resources.getIdentifier(
                        "kerahat_uyarisi",
                        "raw",
                        context.packageName
                    )
                    if (resourceId != 0) {
                        Uri.parse("android.resource://${context.packageName}/$resourceId")
                    } else {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }
                }
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
        } catch (e: Exception) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }
}
