package com.example.namazvakti

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object PrayerScheduler {

    private const val CHANNEL_EXACT_ID = "namaz_vakti_channel_exact"
    private const val CHANNEL_KERAHAT_ID = "namaz_vakti_channel_kerahat"

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = CHANNEL_EXACT_ID
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Namaz ve Kerahat Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                description = "Vakit girişleri ve öncesi hatırlatıcılar"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }

    fun refreshInBackground(context: Context, onComplete: (() -> Unit)? = null) {
        Thread {
            try {
                val prefs = context.getSharedPreferences("namaz", Context.MODE_PRIVATE)
                val lat = prefs.getFloat("lat", 38.3552f).toDouble()
                val lng = prefs.getFloat("lng", 38.3093f).toDouble()

                val cal = Calendar.getInstance()
                val months = mutableListOf(cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1))
                if (cal.get(Calendar.DAY_OF_MONTH) >= cal.getActualMaximum(Calendar.DAY_OF_MONTH) - 2) {
                    val nextMonth = (cal.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        add(Calendar.MONTH, 1)
                    }
                    months.add(nextMonth.get(Calendar.YEAR) to (nextMonth.get(Calendar.MONTH) + 1))
                }

                val cacheBuilder = StringBuilder()
                val sdfInput = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("GMT+3")
                }

                for ((index, ym) in months.withIndex()) {
                    val (year, month) = ym
                    try {
                        val urlStr = "https://api.aladhan.com/v1/calendar/$year/$month?latitude=$lat&longitude=$lng&method=13"
                        val response = URL(urlStr).readText()
                        val json = JSONObject(response)
                        val dataArray = json.getJSONArray("data")

                        for (i in 0 until dataArray.length()) {
                            val dayObj = dataArray.getJSONObject(i)
                            val dateObj = dayObj.getJSONObject("date")
                            val gregorian = dateObj.getJSONObject("gregorian")
                            val dateStr = gregorian.getString("date")

                            val timings = dayObj.getJSONObject("timings")
                            val pNames = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
                            val trNames = listOf("İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı")

                            for (j in pNames.indices) {
                                val rawTime = timings.getString(pNames[j]).split(" ")[0]
                                val fullStr = "$dateStr $rawTime"
                                val parsedDate = sdfInput.parse(fullStr)
                                if (parsedDate != null) {
                                    cacheBuilder.append("${parsedDate.time}|${trNames[j]};")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (index == 0) throw e
                    }
                }

                prefs.edit().putString("cache", cacheBuilder.toString()).apply()

                resolveCityName(context, lat, lng)?.let {
                    prefs.edit().putString("city", it).apply()
                }

                scheduleFromCache(context)
                PrayerWidgetProvider.triggerUpdate(context)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onComplete?.invoke()
            }
        }.start()
    }

    /**
     * Kullanıcının SettingsActivity'de belirlediği ayarlarla alarmları kurar.
     * Eski tüm alarmları iptal ederek çift alarm çalmasını önler.
     */
    fun scheduleFromCache(context: Context) {
        val prefs = context.getSharedPreferences("namaz", Context.MODE_PRIVATE)
        val settingsPrefs = context.getSharedPreferences("alarm_settings_prefs", Context.MODE_PRIVATE)
        val cache = prefs.getString("cache", "") ?: return
        val now = System.currentTimeMillis()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // ÖNCEKİ TÜM ALARMLARI İPTAL ET (15 dk ve 10 dk çakışmasını önleyen kritik temizlik)
        for (idx in 0 until 40) {
            cancelAlarm(context, alarmManager, 1000 + idx) // Exact
            cancelAlarm(context, alarmManager, 2000 + idx) // Pre
            cancelAlarm(context, alarmManager, 4000 + idx) // Kerahat
        }

        val allTimes = cache.split(";").filter { it.contains("|") }.mapNotNull {
            val parts = it.split("|")
            val t = parts[0].toLongOrNull()
            if (t == null) null else t to parts[1]
        }.filter { it.first > now }

        val enableExact = settingsPrefs.getBoolean("ezan_notify_enabled", true)
        val enableKerahat = settingsPrefs.getBoolean("kerahat_notify_enabled", true)
        val kerahatMins = settingsPrefs.getInt("kerahat_minutes", 20)

        // SettingsActivity ile birebir aynı SharedPreferences anahtarları
        val prefKeyPrefixMap = mapOf(
            "İmsak" to "imsak",
            "Güneş" to "gunes",
            "Öğle" to "ogle",
            "İkindi" to "ikindi",
            "Akşam" to "aksam",
            "Yatsı" to "yatsi"
        )

        allTimes.take(24).forEachIndexed { index, (timeMs, label) ->
            val key = prefKeyPrefixMap[label] ?: "ogle"
            val isPreEnabled = settingsPrefs.getBoolean("${key}_pre_enabled", true)
            val preMinutes = settingsPrefs.getInt("${key}_pre", 15)

            // 1. Tam Vakit Alarmı
            if (enableExact && label != "Güneş") {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("label", label)
                    putExtra("time", timeMs)
                    putExtra("type", "EXACT")
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    1000 + index,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setExactAlarmCompat(alarmManager, timeMs, pi)
            }

            // 2. Kullanıcının Seçtiği Dakika Öncesi Bildirimi (Yalnızca etkinse kurulur!)
            if (isPreEnabled) {
                val preTriggerMs = timeMs - (preMinutes * 60 * 1000L)
                if (preTriggerMs > now) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("label", label)
                        putExtra("time", timeMs)
                        putExtra("type", "PRE_$preMinutes")
                        putExtra("preMinutes", preMinutes)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context,
                        2000 + index,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setExactAlarmCompat(alarmManager, preTriggerMs, pi)
                }
            }

            // 3. Kerahat Uyarısı (Güneş Doğuşu, Öğle İstivası ve Akşam Gurubu Öncesi)
            if (enableKerahat && (label == "Güneş" || label == "Öğle" || label == "Akşam")) {
                val kerahatTriggerMs = timeMs - (kerahatMins * 60 * 1000L)
                if (kerahatTriggerMs > now) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("label", label)
                        putExtra("time", timeMs)
                        putExtra("type", "KERAHAT")
                        putExtra("preMinutes", kerahatMins)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context,
                        4000 + index,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setExactAlarmCompat(alarmManager, kerahatTriggerMs, pi)
                }
            }
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun setExactAlarmCompat(alarmManager: AlarmManager, triggerAtMs: Long, pi: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        }
    }

    private fun resolveCityName(context: Context, lat: Double, lng: Double): String? {
        return try {
            if (!Geocoder.isPresent()) return null
            @Suppress("DEPRECATION")
            val results = Geocoder(context, Locale.forLanguageTag("tr-TR")).getFromLocation(lat, lng, 1)
            val address = results?.firstOrNull() ?: return null
            address.subLocality ?: address.locality ?: address.subAdminArea ?: address.adminArea
        } catch (e: Exception) {
            null
        }
    }
}