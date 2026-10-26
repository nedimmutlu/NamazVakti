package com.ezanvakti.app.data.model

/**
 * Genişletilmiş Hadis Modeli.
 */
data class Hadith(
    override val id: Int,
    val source: String,             // Örn: Buhari, Müslim, Tirmizi
    val bookName: String,           // Örn: Salat, İman
    val hadithNumber: Int,          // Örn: 45
    val narrator: String? = null,   // Örn: Ebû Hüreyre (r.a.)
    override val text: String,
    override val applicablePrayers: List<String> = listOf("ALL")
) : DailyContent {

    override fun getShortSourceText(): String {
        return "— Kaynak: $source, $bookName, $hadithNumber"
    }

    override fun getFullSourceText(): String {
        val narratorText = narrator?.let { " ($it)" } ?: ""
        return "— Kaynak: $source, $bookName, $hadithNumber$narratorText"
    }
}
