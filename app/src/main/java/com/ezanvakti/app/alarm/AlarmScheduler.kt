package com.ezanvakti.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ezanvakti.app.data.model.PrayerTimes
import com.ezanvakti.app.data.model.PrayerType
import com.ezanvakti.app.data.room.NotificationSettings
import com.ezanvakti.app.notification.PrayerNotificationReceiver

/**
 * Gelişmiş alarm zamanlayıcı.
 *
 * Özellikler:
 * - Android 12+ SCHEDULE_EXACT_ALARM uyumluluğu
 * - Doze modunda bile çalışan alarmlar
 * - Çakışma kontrolü
 * - Kerahat uyarıları
 * - Vakit öncesi bildirimler
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"

        // Request code base values
        private const val REQUEST_CODE_EXACT_BASE = 1000
        private const val REQUEST_CODE_PRE_BASE = 2000
        private const val REQUEST_CODE_KERAHAT_BASE = 3000

        // Intent extras
        const val EXTRA_PRAYER_TYPE = "EXTRA_PRAYER_TYPE"
        const val EXTRA_NOTIFICATION_TYPE = "EXTRA_NOTIFICATION_TYPE"
        const val EXTRA_MINUTES_BEFORE = "EXTRA_MINUTES_BEFORE"
        const val EXTRA_KERAHAT_TYPE = "EXTRA_KERAHAT_TYPE"
        const val EXTRA_WARNING_TIME = "EXTRA_WARNING_TIME"
    }

    enum class NotificationType {
        EXACT_TIME,      // Tam vakit bildirimi
        PRE_NOTIFICATION, // Vakit öncesi bildirim
        KERAHAT_WARNING  // Kerahat uyarısı
    }

    /**
     * Tüm namaz vakitleri için alarmları planlar.
     *
     * @param times Günün namaz vakitleri
     * @param settingsList Her vakit için bildirim ayarları
     * @param kerahatEnabled Kerahat uyarıları aktif mi?
     * @param kerahatMinutes Kerahat uyarısı kaç dakika önceden
     * @return Çakışma uyarısı (varsa)
     */
    fun scheduleAllAlarms(
        times: PrayerTimes,
        settingsList: List<NotificationSettings>,
        kerahatEnabled: Boolean = false,
        kerahatMinutes: Int = 15
    ): ClashWarning? {
        var clashWarning: ClashWarning? = null

        // Her vakit için alarmları planla
        for (prayerType in PrayerType.values()) {
            val settings = settingsList.find { it.prayerType == prayerType.name }
                ?: getDefaultSettings(prayerType)

            // Tam vakit bildirimi
            scheduleExactTimeAlarm(times, prayerType)

            // Vakit öncesi bildirim
            if (settings.isPreNotificationEnabled) {
                val clash = schedulePreNotificationAlarm(
                    times,
                    prayerType,
                    settings.preNotificationMinutes
                )
                if (clash != null) {
                    clashWarning = clash
                }
            }
        }

        // Kerahat uyarıları
        if (kerahatEnabled) {
            scheduleKerahatAlarms(times, kerahatMinutes)
        }

        return clashWarning
    }

    /**
     * Tam vakit bildirimi için alarm planlar.
     */
    fun scheduleExactTimeAlarm(times: PrayerTimes, prayerType: PrayerType) {
        val triggerTime = times.getTimeInMillis(prayerType)

        if (triggerTime <= System.currentTimeMillis()) {
            Log.d(TAG, "${prayerType.displayName} için zaman geçmiş, atlanıyor")
            return
        }

        val intent = createNotificationIntent(
            prayerType = prayerType,
            notificationType = NotificationType.EXACT_TIME
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_EXACT_BASE + prayerType.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(triggerTime, pendingIntent, "${prayerType.displayName} tam vakit")
    }

    /**
     * Vakit öncesi bildirim için alarm planlar.
     *
     * @return Çakışma uyarısı (varsa)
     */
    fun schedulePreNotificationAlarm(
        times: PrayerTimes,
        prayerType: PrayerType,
        minutesBefore: Int
    ): ClashWarning? {
        val prayerTime = times.getTimeInMillis(prayerType)
        val triggerTime = prayerTime - (minutesBefore * 60 * 1000L)

        if (triggerTime <= System.currentTimeMillis()) {
            Log.d(TAG, "${prayerType.displayName} için öncesi bildirim zamanı geçmiş")
            return null
        }

        // Çakışma kontrolü
        val clashWarning = checkForClash(times, prayerType, minutesBefore)
        if (clashWarning != null) {
            Log.w(TAG, "Çakışma tespit edildi: ${clashWarning.message}")
        }

        val intent = createNotificationIntent(
            prayerType = prayerType,
            notificationType = NotificationType.PRE_NOTIFICATION,
            minutesBefore = minutesBefore
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_PRE_BASE + prayerType.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(triggerTime, pendingIntent, "${prayerType.displayName} $minutesBefore dk öncesi")

        return clashWarning
    }

    /**
     * Kerahat uyarıları için alarmları planlar.
     */
    fun scheduleKerahatAlarms(times: PrayerTimes, warningMinutesBefore: Int) {
        val warnings = KerahatCalculator.calculateKerahatWarnings(times, warningMinutesBefore)

        for (warning in warnings) {
            val intent = createKerahatIntent(warning)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_KERAHAT_BASE + warning.type.ordinal,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleExactAlarm(
                warning.warningTimeMs,
                pendingIntent,
                "Kerahat: ${warning.type.displayName}"
            )
        }
    }

    /**
     * Exact alarm planlar - Android 12+ uyumlu.
     */
    private fun scheduleExactAlarm(triggerTime: Long, pendingIntent: PendingIntent, label: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    // Android 12+: Exact alarm izni varsa
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.d(TAG, "Exact alarm planlandı: $label")
                } else {
                    // Exact alarm izni yoksa, inexact kullan
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.w(TAG, "Exact alarm izni yok, inexact kullanılıyor: $label")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6-11: Doze mode uyumlu
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Exact alarm planlandı (M+): $label")
            } else {
                // Eski Android sürümleri
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Exact alarm planlandı (eski): $label")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Alarm planlanırken güvenlik hatası: ${e.message}")
            // Fallback: set() kullan
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    /**
     * Çakışma kontrolü yapar.
     *
     * Seçilen öncesi bildirim süresi, bir sonraki vakte kalan süreden fazlaysa
     * uyarı döner.
     */
    private fun checkForClash(
        times: PrayerTimes,
        prayerType: PrayerType,
        minutesBefore: Int
    ): ClashWarning? {
        val prayerTime = times.getTimeInMillis(prayerType)
        val notificationTime = prayerTime - (minutesBefore * 60 * 1000L)
        val now = System.currentTimeMillis()

        if (notificationTime <= now) return null

        // Bir önceki vakti bul
        val allTimes = listOf(
            Pair(PrayerType.FAJR, times.getTimeInMillis(PrayerType.FAJR)),
            Pair(PrayerType.SUNRISE, times.getTimeInMillis(PrayerType.SUNRISE)),
            Pair(PrayerType.DHUHR, times.getTimeInMillis(PrayerType.DHUHR)),
            Pair(PrayerType.ASR, times.getTimeInMillis(PrayerType.ASR)),
            Pair(PrayerType.MAGHRIB, times.getTimeInMillis(PrayerType.MAGHRIB)),
            Pair(PrayerType.ISHA, times.getTimeInMillis(PrayerType.ISHA))
        ).sortedBy { it.second }

        val currentIndex = allTimes.indexOfFirst { it.first == prayerType }
        if (currentIndex <= 0) return null

        val previousPrayer = allTimes[currentIndex - 1]
        val previousPrayerTime = previousPrayer.second

        // Bildirim zamanı, önceki vakitten önceyse çakışma var
        if (notificationTime < previousPrayerTime) {
            val clashMinutes = ((previousPrayerTime - notificationTime) / (60 * 1000)).toInt()
            return ClashWarning(
                prayerType = prayerType,
                previousPrayerType = previousPrayer.first,
                requestedMinutes = minutesBefore,
                clashMinutes = clashMinutes,
                message = "${prayerType.displayName} için $minutesBefore dk öncesi bildirim, ${previousPrayer.first.displayName} vaktiyle çakışıyor ($clashMinutes dk fark)"
            )
        }

        return null
    }

    /**
     * Bildirim Intent'i oluşturur.
     */
    private fun createNotificationIntent(
        prayerType: PrayerType,
        notificationType: NotificationType,
        minutesBefore: Int = 0
    ): Intent {
        return Intent(context, PrayerNotificationReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER_TYPE, prayerType.name)
            putExtra(EXTRA_NOTIFICATION_TYPE, notificationType.name)
            putExtra(EXTRA_MINUTES_BEFORE, minutesBefore)
        }
    }

    /**
     * Kerahat Intent'i oluşturur.
     */
    private fun createKerahatIntent(warning: KerahatWarning): Intent {
        return Intent(context, PrayerNotificationReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_TYPE, NotificationType.KERAHAT_WARNING.name)
            putExtra(EXTRA_KERAHAT_TYPE, warning.type.name)
            putExtra(EXTRA_PRAYER_TYPE, warning.prayerType.name)
            putExtra(EXTRA_WARNING_TIME, warning.kerahatStartTimeMs)
        }
    }

    /**
     * Tüm alarmları iptal eder.
     */
    fun cancelAllAlarms() {
        for (prayerType in PrayerType.values()) {
            cancelAlarm(REQUEST_CODE_EXACT_BASE + prayerType.ordinal)
            cancelAlarm(REQUEST_CODE_PRE_BASE + prayerType.ordinal)
        }
        for (kerahatType in KerahatType.values()) {
            cancelAlarm(REQUEST_CODE_KERAHAT_BASE + kerahatType.ordinal)
        }
        Log.d(TAG, "Tüm alarmlar iptal edildi")
    }

    private fun cancelAlarm(requestCode: Int) {
        val intent = Intent(context, PrayerNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Varsayılan ayarları döner.
     */
    private fun getDefaultSettings(prayerType: PrayerType): NotificationSettings {
        return NotificationSettings(
            prayerType = prayerType.name,
            preNotificationMinutes = 10,
            isPreNotificationEnabled = true,
            preNotificationSoundKey = "dakika_10",
            kerahatWarningEnabled = false,
            kerahatPreMinutes = 15,
            kerahatSoundKey = "kerahat"
        )
    }
}

/**
 * Çakışma uyarısı data class.
 */
data class ClashWarning(
    val prayerType: PrayerType,
    val previousPrayerType: PrayerType,
    val requestedMinutes: Int,
    val clashMinutes: Int,
    val message: String
)
