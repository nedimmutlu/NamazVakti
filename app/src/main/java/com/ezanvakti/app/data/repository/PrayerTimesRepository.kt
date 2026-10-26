package com.ezanvakti.app.data.repository

import com.ezanvakti.app.data.model.PrayerTimes
import com.ezanvakti.app.location.Coordinates

/**
 * Namaz vakitlerini yöneten repository.
 * API'den veya yerel önbellekten vakitleri çeker.
 */
class PrayerTimesRepository {

    /**
     * Verilen koordinatlar için namaz vakitlerini döndürür.
     * Şimdilik örnek veri döndürüyor, gerçek uygulamada API'den çekmeli.
     */
    fun getPrayerTimesForCoordinates(coords: Coordinates?): PrayerTimes {
        // Gerçek uygulamada buradan bir API (örneğin Aladhan, MuslimPro vb.) çağrılmalı.
        // Şimdilik koordinatlara dayalı örnek vakitler üretiliyor (gerçek değil).
        val lat = coords?.latitude ?: 0.0
        val lng = coords?.longitude ?: 0.0
        // Basit bir örnek: enlemi göre değişenvakitler (sadece demonstrasyon)
        val offset = (lat * 4).toInt() // sadece örnek
        val fajr = "0${5 + offset / 60}:${(offset % 60).toString().padStart(2, '0')}"
        val sunrise = "0${6 + offset / 60}:${((offset + 30) % 60).toString().padStart(2, '0')}"
        val dhuhr = "1${2 + offset / 60}:${((offset + 60) % 60).toString().padStart(2, '0')}"
        val asr = "1${5 + offset / 60}:${((offset + 90) % 60).toString().padStart(2, '0')}"
        val maghrib = "1${7 + offset / 60}:${((offset + 120) % 60).toString().padStart(2, '0')}"
        val isha = "1${9 + offset / 60}:${((offset + 150) % 60).toString().padStart(2, '0')}"
        return PrayerTimes(
            cityName = coords?.cityName ?: "Bilinmeyen Konum",
            latitude = lat,
            longitude = lng,
            date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
            fajr = fajr,
            sunrise = sunrise,
            dhuhr = dhuhr,
            asr = asr,
            maghrib = maghrib,
            isha = isha
        )
    }
}
