package com.ezanvakti.app.alarm

import com.ezanvakti.app.data.model.PrayerType
import com.ezanvakti.app.data.model.PrayerTimes
import java.util.Calendar

/**
 * Fıkhî Kerahat Vakitleri Hesaplayıcısı:
 * 1. Sabah Kerahati: Güneş doğduktan sonraki ~45 dakika.
 * 2. İstiva / Zeval Kerahati: Öğle namazından önceki ~45 dakika (güneş tam tepe noktasındayken).
 * 3. Akşam / Gurub Kerahati: Akşam namazından önceki ~45 dakika (güneş batarken).
 */
object KerahatCalculator {

    private const val KERAHAT_DURATION_MS = 45 * 60 * 1000L

    fun calculateKerahatWarnings(
        times: PrayerTimes,
        warningMinutesBefore: Int = 15
    ): List<KerahatWarning> {
        val warnings = mutableListOf<KerahatWarning>()
        val warningMs = warningMinutesBefore * 60 * 1000L
        val calendar = Calendar.getInstance()

        // 1. Sabah Kerahati (Güneş Doğuşu)
        val sunriseTime = times.getTimeInMillis(PrayerType.SUNRISE, calendar)
        val morningKerahatStart = sunriseTime
        val morningKerahatWarning = morningKerahatStart - warningMs

        if (morningKerahatWarning > System.currentTimeMillis()) {
            warnings.add(
                KerahatWarning(
                    type = KerahatType.MORNING_SUNRISE,
                    warningTimeMs = morningKerahatWarning,
                    kerahatStartTimeMs = morningKerahatStart,
                    kerahatEndTimeMs = morningKerahatStart + KERAHAT_DURATION_MS,
                    prayerType = PrayerType.SUNRISE
                )
            )
        }

        // 2. Öğle Öncesi Kerahat (İstiva / Zeval Vakti)
        val dhuhrTime = times.getTimeInMillis(PrayerType.DHUHR, calendar)
        val dhuhrKerahatStart = dhuhrTime - KERAHAT_DURATION_MS
        val dhuhrKerahatWarning = dhuhrKerahatStart - warningMs

        if (dhuhrKerahatWarning > System.currentTimeMillis()) {
            warnings.add(
                KerahatWarning(
                    type = KerahatType.BEFORE_DHUHR,
                    warningTimeMs = dhuhrKerahatWarning,
                    kerahatStartTimeMs = dhuhrKerahatStart,
                    kerahatEndTimeMs = dhuhrTime,
                    prayerType = PrayerType.DHUHR
                )
            )
        }

        // 3. Akşam Öncesi Kerahat (Güneş Batışı / Gurub)
        val maghribTime = times.getTimeInMillis(PrayerType.MAGHRIB, calendar)
        val maghribKerahatStart = maghribTime - KERAHAT_DURATION_MS
        val maghribKerahatWarning = maghribKerahatStart - warningMs

        if (maghribKerahatWarning > System.currentTimeMillis()) {
            warnings.add(
                KerahatWarning(
                    type = KerahatType.BEFORE_MAGHRIB,
                    warningTimeMs = maghribKerahatWarning,
                    kerahatStartTimeMs = maghribKerahatStart,
                    kerahatEndTimeMs = maghribTime,
                    prayerType = PrayerType.MAGHRIB
                )
            )
        }

        return warnings
    }

    fun isCurrentlyInKerahat(times: PrayerTimes): KerahatType? {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val sunriseTime = times.getTimeInMillis(PrayerType.SUNRISE, calendar)
        val dhuhrTime = times.getTimeInMillis(PrayerType.DHUHR, calendar)
        val maghribTime = times.getTimeInMillis(PrayerType.MAGHRIB, calendar)

        return when {
            now >= sunriseTime && now <= sunriseTime + KERAHAT_DURATION_MS -> KerahatType.MORNING_SUNRISE
            now >= dhuhrTime - KERAHAT_DURATION_MS && now <= dhuhrTime -> KerahatType.BEFORE_DHUHR
            now >= maghribTime - KERAHAT_DURATION_MS && now <= maghribTime -> KerahatType.BEFORE_MAGHRIB
            else -> null
        }
    }
}

enum class KerahatType(val displayName: String, val description: String) {
    MORNING_SUNRISE("Güneş Doğuşu Kerahati", "Güneş doğduktan sonraki 45 dakika"),
    BEFORE_DHUHR("İstiva Kerahati (Öğle Öncesi)", "Güneş tam tepedeyken, öğle ezanından önceki 45 dakika"),
    BEFORE_MAGHRIB("Gurub Kerahati (Akşam Öncesi)", "Güneş batarken, akşam ezanından önceki 45 dakika")
}

data class KerahatWarning(
    val type: KerahatType,
    val warningTimeMs: Long,
    val kerahatStartTimeMs: Long,
    val kerahatEndTimeMs: Long,
    val prayerType: PrayerType
)