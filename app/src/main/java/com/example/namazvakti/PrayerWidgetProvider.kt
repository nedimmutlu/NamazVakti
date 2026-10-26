package com.example.namazvakti

import com.firstnamazvakti.R
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Namaz vakti widget'ı (Kompakt ve İnce 4x1 Tasarım).
 *
 * Veri kaynağı: "namaz" SharedPreferences içindeki "cache" (PrayerScheduler yazar).
 * Format: "<epochMs>|<vakitAdı>;" tekrarlı.
 *
 * Geri sayım Chronometer ile yapılır; sistem saniyelerini kendisi günceller,
 * bu yüzden widget'ın her saniye güncellenmesine gerek yoktur.
 * Sıradaki aktif vakit altın sarısı ile vurgulanır ve Chronometer vakit adıyla birlikte sayar.
 */
class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_TICK -> updateAllWidgets(context)
            ACTION_REFRESH -> {
                val pendingResult = goAsync()
                PrayerScheduler.refreshInBackground(context) {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onDisabled(context: Context) {
        cancelTick(context)
    }

    private data class PrayerTime(val timeMs: Long, val name: String)

    companion object {
        const val ACTION_TICK = "com.example.namazvakti.WIDGET_TICK"
        const val ACTION_REFRESH = "com.example.namazvakti.WIDGET_REFRESH"

        private const val TZ_ID = "GMT+3"

        private const val MIN_AUTO_REFRESH_GAP_MS = 10 * 60 * 1000L
        private const val HIJRI_DAY_OFFSET = 0

        private val PRAYER_NAMES = listOf("İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı")

        private val HIJRI_MONTHS = arrayOf(
            "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir",
            "Cemaziyelevvel", "Cemaziyelahir", "Recep", "Şaban",
            "Ramazan", "Şevval", "Zilkade", "Zilhicce"
        )

        private val TIME_IDS = intArrayOf(
            R.id.widget_time_0, R.id.widget_time_1, R.id.widget_time_2,
            R.id.widget_time_3, R.id.widget_time_4, R.id.widget_time_5
        )

        fun triggerUpdate(context: Context) {
            updateAllWidgets(context)
        }

        fun updateAllWidgets(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val component = ComponentName(appContext, PrayerWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) {
                cancelTick(appContext)
                return
            }

            val now = System.currentTimeMillis()
            val all = loadTimes(appContext)
            val todayKey = dayKey(now)
            val today = all.filter { dayKey(it.timeMs) == todayKey }
            val next = all.firstOrNull { it.timeMs > now }

            manager.updateAppWidget(ids, buildViews(appContext, now, today, next))

            val midnight = nextMidnight(now)
            val triggerAt = if (next != null) minOf(next.timeMs + 1000L, midnight) else minOf(now + 15 * 60 * 1000L, midnight)
            scheduleTick(appContext, triggerAt)

            if (today.size < PRAYER_NAMES.size || next == null) {
                maybeRefreshData(appContext, now)
            }
        }

        private fun buildViews(
            context: Context,
            now: Long,
            today: List<PrayerTime>,
            next: PrayerTime?
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_prayer)
            val prefs = context.getSharedPreferences("namaz", Context.MODE_PRIVATE)

            val clock = SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR")).apply {
                timeZone = TimeZone.getTimeZone(TZ_ID)
            }

            // 1. Vakit saatlerini yaz ve sıradaki vakti ALTIN SARISI ile öne çıkar
            for (i in PRAYER_NAMES.indices) {
                val prayerName = PRAYER_NAMES[i]
                val entry = today.firstOrNull { it.name == prayerName }
                val isNext = (next != null && next.name == prayerName)

                // Saati yaz
                views.setTextViewText(TIME_IDS[i], entry?.let { clock.format(Date(it.timeMs)) } ?: "--:--")

                // Vurgu rengi: Sıradaki vakit Altın Sarısı (#FFD54F), diğerleri %70 opak beyaz
                if (isNext) {
                    views.setTextColor(TIME_IDS[i], Color.parseColor("#FFD54F"))
                } else {
                    views.setTextColor(TIME_IDS[i], Color.parseColor("#B3FFFFFF"))
                }
            }

            // 2. Chronometer sayacına vakit adını ekle (Örn: "İmsak: 04:46:46")
            views.setChronometerCountDown(R.id.widget_countdown, true)
            if (next != null) {
                val base = SystemClock.elapsedRealtime() + (next.timeMs - now)
                // format parametresiyle sayacın başına "İmsak:" veya sıradaki vaktin adı dinamik eklenir:
                views.setChronometer(R.id.widget_countdown, base, "${next.name}: %s", true)
            } else {
                views.setChronometer(R.id.widget_countdown, SystemClock.elapsedRealtime(), null, false)
                views.setTextViewText(R.id.widget_countdown, "--:--")
            }

            // 3. Hicri Tarih
            val maghrib = today.firstOrNull { it.name == "Akşam" }
            val afterMaghrib = maghrib != null && now >= maghrib.timeMs
            val shiftDays = (if (afterMaghrib) 1 else 0) + HIJRI_DAY_OFFSET
            views.setTextViewText(R.id.widget_hijri, hijriText(now + shiftDays * 86_400_000L))

            // 4. Şehir / Konum
            views.setTextViewText(R.id.widget_city, prefs.getString("city", "Malatya") ?: "Malatya")

            // 5. Tıklama Eylemleri
            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)

            val refresh = PendingIntent.getBroadcast(
                context,
                1,
                Intent(context, PrayerWidgetProvider::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh, refresh)

            return views
        }

        private fun loadTimes(context: Context): List<PrayerTime> {
            val cache = context.getSharedPreferences("namaz", Context.MODE_PRIVATE)
                .getString("cache", "") ?: ""
            return cache.split(";").mapNotNull { entry ->
                val parts = entry.split("|")
                val time = parts.getOrNull(0)?.toLongOrNull()
                val name = parts.getOrNull(1)
                if (time == null || name.isNullOrEmpty()) null else PrayerTime(time, name)
            }.sortedBy { it.timeMs }
        }

        private fun dayKey(ms: Long): Int {
            val c = Calendar.getInstance(TimeZone.getTimeZone(TZ_ID))
            c.timeInMillis = ms
            return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
        }

        private fun nextMidnight(now: Long): Long {
            val c = Calendar.getInstance(TimeZone.getTimeZone(TZ_ID))
            c.timeInMillis = now
            c.add(Calendar.DAY_OF_YEAR, 1)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 5)
            c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }

        private fun hijriText(ms: Long): String {
            return try {
                val cal = android.icu.util.IslamicCalendar(
                    android.icu.util.TimeZone.getTimeZone(TZ_ID),
                    Locale.forLanguageTag("tr-TR")
                )
                cal.setCalculationType(android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA)
                cal.timeInMillis = ms
                val day = cal.get(android.icu.util.Calendar.DAY_OF_MONTH)
                val month = cal.get(android.icu.util.Calendar.MONTH)
                "$day ${HIJRI_MONTHS[month]}"
            } catch (e: Throwable) {
                ""
            }
        }

        private fun maybeRefreshData(context: Context, now: Long) {
            val prefs = context.getSharedPreferences("namaz", Context.MODE_PRIVATE)
            val last = prefs.getLong("widget_last_refresh", 0L)
            if (now - last < MIN_AUTO_REFRESH_GAP_MS) return
            prefs.edit().putLong("widget_last_refresh", now).apply()
            PrayerScheduler.refreshInBackground(context)
        }

        private fun tickPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, PrayerWidgetProvider::class.java).setAction(ACTION_TICK)
            return PendingIntent.getBroadcast(
                context,
                2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun scheduleTick(context: Context, triggerAt: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = tickPendingIntent(context)
            try {
                val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        alarmManager.canScheduleExactAlarms()
                if (canExact) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        }

        private fun cancelTick(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(tickPendingIntent(context))
        }
    }
}