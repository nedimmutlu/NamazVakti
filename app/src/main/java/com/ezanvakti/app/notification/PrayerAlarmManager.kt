package com.ezanvakti.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ezanvakti.app.data.model.PrayerTimes

/**
 * AlarmManager ile namaz vakitlerini planlar.
 */
class PrayerAlarmManager(private val context: Context) {

    fun scheduleAllPrayers(times: PrayerTimes) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Basit örnek: Her vakit için alarm planla
        val prayerTypes = listOf(
            "FAJR" to times.fajr,
            "DHUHR" to times.dhuhr,
            "ASR" to times.asr,
            "MAGHRIB" to times.maghrib,
            "ISHA" to times.isha
        )

        for ((name, timeStr) in prayerTypes) {
            val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
                putExtra("EXTRA_PRAYER_TYPE", name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Basit zaman ayarı (gerçek uygulamada zaman parse edilip kullanılmalı)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + 3600000,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 3600000,
                    pendingIntent
                )
            }
        }
    }
}
