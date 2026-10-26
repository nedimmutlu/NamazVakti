package com.ezanvakti.app.data.model

/**
 * Günün Ayeti ve Hadisi için ortak arayüz.
 * Problem 6: Kaynak detaylarını standart formatta sunmayı garanti eder.
 */
interface DailyContent {
    val id: Int
    val text: String
    val applicablePrayers: List<String>
    
    /**
     * Tam kaynak gösterimi:
     * Örneğin Hadis için: "— Kaynak: Tirmîzî, Salat, 45 (Râvi: Ebû Hüreyre (r.a.))"
     * Örneğin Ayet için: "— Kaynak: Kur'ân-ı Kerîm, Bakara Suresi, 238"
     */
    fun getFullSourceText(): String
    fun getShortSourceText(): String
}
