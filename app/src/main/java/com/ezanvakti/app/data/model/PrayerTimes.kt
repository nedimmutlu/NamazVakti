package com.ezanvakti.app.data.model

import java.util.Calendar

enum class PrayerType(val displayName: String) {
    FAJR("İmsak"),
    SUNRISE("Güneş"),
    DHUHR("Öğle"),
    ASR("İkindi"),
    MAGHRIB("Akşam"),
    ISHA("Yatsı")
}

data class PrayerTimes(
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val date: String,
    val fajr: String,     // "05:32"
    val sunrise: String,  // "06:54"
    val dhuhr: String,    // "12:45"
    val asr: String,      // "15:58"
    val maghrib: String,  // "18:25"
    val isha: String      // "19:42"
) {
    /**
     * Belirtilen vaktin String formatından günün milisaniyesine çevrilmesi
     */
    fun getTimeInMillis(type: PrayerType, calendar: Calendar = Calendar.getInstance()): Long {
        val timeStr = when (type) {
            PrayerType.FAJR -> fajr
            PrayerType.SUNRISE -> sunrise
            PrayerType.DHUHR -> dhuhr
            PrayerType.ASR -> asr
            PrayerType.MAGHRIB -> maghrib
            PrayerType.ISHA -> isha
        }
        val parts = timeStr.split(":")
        val hour = parts[0].trim().toIntOrNull() ?: 12
        val minute = parts[1].trim().toIntOrNull() ?: 0

        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
