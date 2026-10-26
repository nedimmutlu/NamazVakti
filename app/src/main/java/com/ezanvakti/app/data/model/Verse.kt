package com.ezanvakti.app.data.model

/**
 * Problem 6 Çözümü:
 * Genişletilmiş Ayet Modeli.
 */
data class Verse(
    override val id: Int,
    val surahName: String,          // Örn: Bakara, İsrâ
    val verseNumber: Int,           // Örn: 238
    override val text: String,      // Meal metni
    override val applicablePrayers: List<String> = listOf("ALL")
) : DailyContent {

    override fun getShortSourceText(): String {
        return "— Kaynak: $surahName Suresi, $verseNumber"
    }

    override fun getFullSourceText(): String {
        return "— Kaynak: Kur'an-ı Kerim, $surahName Suresi, $verseNumber. Ayet"
    }
}
