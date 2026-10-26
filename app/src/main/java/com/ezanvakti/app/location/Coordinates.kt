package com.ezanvakti.app.location

/**
 * Dinamik GPS koordinatları ve şehir bilgisi.
 */
data class Coordinates(
    val latitude: Double,
    val longitude: Double,
    val cityName: String = "Bilinmeyen Konum"
) {
    companion object {
        // Güvenli yedek — cihazın ilk kurulumundaki varsayılanı.
        // Egzersiz/örnek amaçlı, gerçek konum her zaman GPS ile alınır.
        val DEFAULT_MALATYA = Coordinates(
            latitude = 38.3552,
            longitude = 38.3095,
            cityName = "Malatya"
        )

        // Hiç konum kaydedilmemeseyse fallback (şehir stringi boş gelir)
        val DEFAULT_UNKNOWN = Coordinates(
            latitude = 0.0,
            longitude = 0.0,
            cityName = "Konum Alınıyor"
        )
    }
}
