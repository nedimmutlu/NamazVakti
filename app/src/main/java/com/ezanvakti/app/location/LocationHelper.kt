package com.ezanvakti.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Konum yardımcısı - FusedLocationProviderClient ile gerçek GPS konumu alır,
 * koordinatları kaydeder, şehir adını ters geocoding ile çözümler.
 */
class LocationHelper(private val context: Context) {

    private val prefs = context.getSharedPreferences(
        "namaz",
        Context.MODE_PRIVATE
    )

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val FALLBACK_CITY = "Konum Alınıyor"

    fun saveLastSavedCoordinates(coords: Coordinates) {
        prefs.edit().apply {
            putString("last_city", coords.cityName)
            putFloat("last_lat", coords.latitude.toFloat())
            putFloat("last_lng", coords.longitude.toFloat())
            putString("city", coords.cityName)
            putFloat("lat", coords.latitude.toFloat())
            putFloat("lng", coords.longitude.toFloat())
            apply()
        }
    }

    /**
     * Kaydedilmiş son koordinatları döndürür. Eğer hiç kayıt yoksa
     * FALLBACK_CITY ("Konum Alınıyor") varsayılan olarak kullanılır;
     * böylece gerçek GPS'in alınması beklenir.
     */
    fun getLastSavedCoordinates(): Coordinates {
        val city = prefs.getString("city", prefs.getString("last_city", FALLBACK_CITY)) ?: FALLBACK_CITY
        val lat = prefs.getFloat("lat", prefs.getFloat("last_lat", 0.0f)).toDouble()
        val lng = prefs.getFloat("lng", prefs.getFloat("last_lng", 0.0f)).toDouble()
        return Coordinates(latitude = lat, longitude = lng, cityName = city)
    }

    /**
     * Gerçek GPS konumunu alır; Geocoder başarısız olursa açık kaynaklı
     * OpenStreetMap Nominatim API'sini arka planda (thread içinde, try-catch ile)
     * çevrimiçi bir fallback olarak kullanır.
     */
    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(callback: (Coordinates) -> Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            callback(getLastSavedCoordinates())
            return
        }

        // Önce cached konumu kontrol et - hemen sonuç verebilir
        fusedLocationClient.lastLocation.addOnSuccessListener { cachedLocation ->
            cachedLocation?.let { location ->
                val lat = location.latitude
                val lng = location.longitude
                val cityName = reverseGeocodeWithFallback(lat, lng)
                val coords = Coordinates(
                    latitude = lat,
                    longitude = lng,
                    cityName = cityName
                )
                saveLastSavedCoordinates(coords)
                callback(coords)
                return@addOnSuccessListener // Cached konum bulundu, işlem tamam
            } ?: run {
                // Cached konum yoksa, yeni konum iste
                requestFreshLocation(callback)
            }
        }.addOnFailureListener {
            // Hata durumunda yeni konum iste
            requestFreshLocation(callback)
        }
    }

    /**
     * Yeni GPS konumu ister. Önce yüksek doğruluk dener, başarısız olursa
     * düşük doğruluk (network-based) kullanır.
     */
    @SuppressLint("MissingPermission")
    private fun requestFreshLocation(callback: (Coordinates) -> Unit) {
        var callbackCalled = false

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // 10 saniye interval
        ).apply {
            setMinUpdateDistanceMeters(0f) // Minimum mesafe
            setGranularity(Granularity.GRANULARITY_FINE)
            setWaitForAccurateLocation(true) // Doğru konum için bekle
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    if (!callbackCalled) {
                        callbackCalled = true
                        val lat = location.latitude
                        val lng = location.longitude
                        val cityName = reverseGeocodeWithFallback(lat, lng)
                        val coords = Coordinates(
                            latitude = lat,
                            longitude = lng,
                            cityName = cityName
                        )
                        saveLastSavedCoordinates(coords)
                        fusedLocationClient.removeLocationUpdates(this)
                        callback(coords)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // 15 saniye timeout - GPS için daha gerçekçi
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            if (!callbackCalled) {
                callbackCalled = true
                fusedLocationClient.removeLocationUpdates(locationCallback)

                // Kayıtlı konum varsa onu kullan, yoksa fallback
                val savedCoords = getLastSavedCoordinates()
                if (savedCoords.latitude != 0.0 && savedCoords.longitude != 0.0) {
                    callback(savedCoords)
                } else {
                    // GPS alınamadı ve kayıtlı konum yok - varsayılan Malatya
                    val fallbackCoords = Coordinates(
                        latitude = 38.3552,
                        longitude = 38.3095,
                        cityName = "Malatya"
                    )
                    saveLastSavedCoordinates(fallbackCoords)
                    callback(fallbackCoords)
                }
            }
        }, 15000)
    }

    /**
     * Önce yerel Android Geocoder'ı deneyen, başarısız/boş olursa veya "Salköprü" gibi
     * genel/eski bir mahalle adı dönerse açık kaynaklı OpenStreetMap Nominatim API'sini
     * kullanarak doğru yer adını (ilçe/semt) alan çözümleyici.
     */
    fun reverseGeocodeWithFallback(lat: Double, lng: Double): String {
        // Doğrudan çevrimiçi Nominatim servisini veya gelişmiş Geocoder'ı sorgula
        // Böylece fiziksel cihazlarda cache'lenen eski mahalle adları yerine gerçek anlık konum alınır.
        val onlineCity = fetchCityOnlineSafely(lat, lng)
        if (!onlineCity.isBlank()) {
            return onlineCity
        }

        // Fallback olarak Android Geocoder
        val androidCity = try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            addresses?.firstOrNull()?.subAdminArea
                ?: addresses?.firstOrNull()?.locality
                ?: addresses?.firstOrNull()?.adminArea
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        return androidCity?.takeIf { !it.isBlank() } ?: FALLBACK_CITY
    }

    /**
     * Engellenmeyen bir thread'de OpenStreetMap Nominatim API'sini çağırır.
     * JSON parse hataları, zaman aşımı ve ağ hataları try-catch ile yakalanır;
     * herhangi bir hata durumunda null döndürülür ve ana akış yormaz.
     */
    private fun fetchCityOnlineSafely(lat: Double, lng: Double): String {
        var result: String? = null
        val latch = java.util.concurrent.CountDownLatch(1)

        Thread {
            try {
                val urlStr =
                    "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("User-Agent", "NamazVaktiApp/1.0")
                conn.setRequestProperty("Accept-Language", "tr")

                conn.inputStream.bufferedReader().use { reader ->
                    val response = StringBuilder()
                    reader.lineSequence().forEach { response.append(it) }
                    val json = response.toString()

                    // "suburb", "city", "town", "village" veya "state" alanından en spesifik yeri çıkar
                    val cityMatch =
                        "\"suburb\":\"([^\"]+)\"".toRegex().find(json)
                            ?: "\"city\":\"([^\"]+)\"".toRegex().find(json)
                            ?: "\"town\":\"([^\"]+)\"".toRegex().find(json)
                            ?: "\"village\":\"([^\"]+)\"".toRegex().find(json)
                            ?: "\"state\":\"([^\"]+)\"".toRegex().find(json)
                    if (cityMatch != null) {
                        result = cityMatch.groupValues[1]
                    } else {
                        // display_name içindeki ilk virgülden önceki kısım şehir olabilir
                        val displayNameMatch = "\"display_name\":\"([^\"]+)\"".toRegex().find(json)
                        if (displayNameMatch != null) {
                            val name = displayNameMatch.groupValues[1]
                            val parts = name.split(",")
                            if (parts.isNotEmpty()) {
                                result = parts[0].trim()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ağ hatası, JSON parse hatası vb. - sessizce atla
                e.printStackTrace()
            } finally {
                latch.countDown()
            }
        }.start()

        try {
            latch.await()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        return result ?: ""
    }
}