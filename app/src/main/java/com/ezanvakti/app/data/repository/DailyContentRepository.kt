package com.ezanvakti.app.data.repository

import android.content.Context
import com.ezanvakti.app.data.model.DailyContent
import com.ezanvakti.app.data.model.PrayerType
import com.ezanvakti.app.data.model.Verse
import kotlin.random.Random

/**
 * Günün ayet ve hadis havuzunu yönetir.
 * Her namaz vakti için uygun içerik seçer.
 */
class DailyContentRepository private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: DailyContentRepository? = null

        fun getInstance(context: Context): DailyContentRepository {
            return instance ?: synchronized(this) {
                instance ?: DailyContentRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    // Ayet havuzu
    private val verses = listOf(
        Verse(1, "Bakara", 238, "Namazlara ve orta namaza dikkat edin. Allah'a gönülden boyun eğerek namaz kılın."),
        Verse(2, "Nisa", 103, "Namazlara ve orta namaza dikkat edin. Allah'a gönülden boyun eğerek namaz kılın."),
        Verse(3, "Müzzemmil", 20, "Şüphesiz Rabbin bilmektedir ki, sen gecenin üçte ikisine yakın kısmını, bazen yarısını, bazen de üçte birini namaz kılmakla geçiriyorsun."),
        Verse(4, "İsra", 78, "Güneşin kaymasından gecenin kararmasına kadar namaz kıl. Sabah namazını da kıl. Çünkü sabah namazı şahitlendirilmiştir."),
        Verse(5, "Hud", 114, "Gündüzün iki ucunda ve gecenin gündüze yakın saatlerinde namaz kıl. Çünkü iyilikler kötülükleri giderir."),
        Verse(6, "Bakara", 45, "Sabır ve namazla Allah'tan yardım isteyin. Bu, huşu duyanlardan başkasına elbette ağır gelir."),
        Verse(7, "Ankebut", 45, "Kitaptan sana vahyedilenleri oku, namazı kıl. Muhakkak ki namaz hayasızlıktan ve kötülükten alıkoyar.")
    )

    /**
     * Her çağrıda rastgele bir ayet seçer
     */
    fun getRandomContentForPrayer(prayerType: PrayerType?): DailyContent {
        val random = Random(System.currentTimeMillis())
        return verses.random(random)
    }
}
