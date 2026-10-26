package com.example.namazvakti

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Esmâü'l-Hüsnâ Modeli
data class EsmaItem(
    val number: Int,
    val arabic: String,
    val nameTurkish: String,
    val meaning: String,
    val ebced: Int,
    val virtue: String // Fazileti ve zikir niyeti
)

val ESMA_LIST = listOf(
    EsmaItem(1, "الرَّحْمَنُ", "Er-Rahmân", "Dünyada bütün mahlûkata merhamet eden, şefkat gösteren.", 298, "Maddi ve manevi rızıkların artması, kalbin nurlanması için."),
    EsmaItem(2, "الرَّحِيمُ", "Er-Rahîm", "Ahirette sadece müminlere sonsuz merhamet ve lütuf eden.", 258, "Huzur, af ve ilahi himaye için."),
    EsmaItem(3, "الْمَلِكُ", "El-Melik", "Mülkün, kâinatın gerçek sahibi ve mutlak hükümdarı.", 90, "Maddi ve manevi güç, itibar ve fakirlikten kurtulmak için."),
    EsmaItem(4, "الْقُدُّوسُ", "El-Kuddûs", "Her türlü eksiklik ve kusurdan münezzeh, tertemiz olan.", 170, "Gönül temizliği ve kötü huylardan arınmak için."),
    EsmaItem(5, "السَّلَامُ", "Es-Selâm", "Kullarını selamete çıkaran, her türlü tehlikeden kurtaran.", 131, "Korkulardan emin olmak ve şifa bulmak için."),
    EsmaItem(6, "الْمُؤْمِنُ", "El-Mü'min", "Gönüllerde iman ışığı yakan, güven veren, emin kılan.", 136, "Düşman şerrinden ve vesveselerden korunmak için."),
    EsmaItem(7, "الْمُهَيْمِنُ", "El-Müheymin", "Her şeyi gözetip koruyan, bütün varlıkları hükmü altında tutan.", 145, "Unutkanlıktan kurtulmak ve hafızanın kuvvetlenmesi için."),
    EsmaItem(8, "الْعَزِيزُ", "El-Azîz", "Mağlup edilmesi imkânsız olan, daima galip ve izzet sahibi.", 94, "İzzet, şeref ve insanların sevgisini kazanmak için."),
    EsmaItem(9, "الْجَبَّارُ", "El-Cebbâr", "Dilediğini zorla yaptıran, kırılanları onaran, eksikleri tamamlayan.", 206, "Zulümden korunmak ve zor işlerin kolaylaşması için."),
    EsmaItem(10, "الْمُتَكَبِّرُ", "El-Mütekebbir", "Büyüklük ve ululukta eşi benzeri olmayan.", 662, "Manevi mertebe ve refah için."),
    EsmaItem(11, "الْخَالِقُ", "El-Hâlık", "Her şeyi yoktan var eden, yaratan.", 731, "İşlerin başarıyla neticelenmesi ve sıkıntıların aşılması için."),
    EsmaItem(12, "الْبَارِئُ", "El-Bâri", "Her şeyi kusursuz ve birbiriyle uyumlu yaratan.", 213, "Zorluklardan ve hastalıklardan kurtulmak için."),
    EsmaItem(13, "الْمُصَوِّرُ", "El-Musavvir", "Varlıklara en güzel suret ve şekli veren.", 336, "Hayırlı evlat ve sanatta muvaffakiyet için."),
    EsmaItem(14, "الْغَفَّارُ", "El-Gaffâr", "Günahları çokça bağışlayan, örten ve affeden.", 1281, "Bağışlanma ve günahlardan arınmak için."),
    EsmaItem(15, "الْقَهَّارُ", "El-Kahhâr", "Her şeye galip gelen, mutlak hâkimiyet sahibi.", 306, "Nefsi ıslah etmek ve kötü alışkanlıkları bırakmak için."),
    EsmaItem(16, "الْوَهَّابُ", "El-Vehhâb", "Karşılıksız ve hesapsız bolca nimet bağışlayan.", 14, "Geçim darlığından kurtulmak ve rızık genişliği için."),
    EsmaItem(17, "الرَّزَّاقُ", "Er-Rezzâk", "Bütün mahlûkatın rızkını veren ve ihtiyacını karşılayan.", 308, "Bereket, bol rızık ve helal kazanç için."),
    EsmaItem(18, "الْفَتَّاحُ", "El-Fettâh", "Her türlü müşkülleri açan, fetih ve ferahlık veren.", 489, "Kapalı kapıların açılması, başarı ve kalbin ferahlaması için."),
    EsmaItem(19, "الْعَلِيمُ", "El-Alîm", "Gizli ve açık her şeyi hakkıyla ve eksiksiz bilen.", 150, "İlim, hikmet ve zihin açıklığı için."),
    EsmaItem(20, "الْقَابِضُ", "El-Kâbıd", "Dilediğine rızkı daraltan, ruhları kabzeden.", 903, "Zalimlerin şerrinden emin olmak için."),
    EsmaItem(21, "الْبَاسِطُ", "El-Bâsıt", "Dilediğine rızkı ve ömrü genişleten, ferahlık veren.", 72, "Kalp darlığını gidermek ve rızık bolluğu için."),
    EsmaItem(22, "الْخَافِضُ", "El-Hâfıd", "Kafirleri ve zalimleri alçaltan, değersiz kılan.", 1481, "Kötü insanların hilesinden korunmak için."),
    EsmaItem(23, "الرَّافِعُ", "Er-Râfi", "Müminleri yücelten, şeref ve mertebelerini artıran.", 351, "Makam, derece ve manevi yükseliş için."),
    EsmaItem(24, "الْمُعِزُّ", "El-Mu'izz", "Dilediğini aziz kılan, izzet ve üstünlük veren.", 117, "İtibar ve izzet kazanmak için."),
    EsmaItem(25, "الْمُذِلُّ", "El-Müzill", "Dilediğini zelil kılan, perişan eden.", 770, "Zalimlerin şerrinden korunmak için."),
    EsmaItem(26, "السَّمِيعُ", "Es-Semî", "Her şeyi, gizli fısıltıları ve duaları işiten.", 180, "Duaların süratle kabul olması için."),
    EsmaItem(27, "الْبَصِيرُ", "El-Basîr", "Her şeyi, karanlıkta olanı dahi hakkıyla gören.", 302, "Basiret ve göz aydınlığı için."),
    EsmaItem(28, "الْحَكَمُ", "El-Hakem", "Mutlak hüküm koyucu, hakkı batıldan ayıran adil yargıç.", 68, "Doğru karar vermek ve hakka ulaşmak için."),
    EsmaItem(29, "الْعَدْلُ", "El-Adl", "Sonsuz adalet sahibi, zulümden uzak olan.", 104, "Adaletin tecellisi ve nefsi terbiye için."),
    EsmaItem(30, "اللَّطِيفُ", "El-Latîf", "Lütuf ve keremi bol olan, en ince sırlara vakıf.", 129, "Dileklerin kabulü, rızık ve sıkıntılardan kurtulmak için."),
    EsmaItem(31, "الْخَبِيرُ", "El-Habîr", "Her şeyin iç yüzünden ve gizlisinden haberdar olan.", 812, "Hafıza ve manevi sırlara agah olmak için."),
    EsmaItem(32, "الْحَلِيمُ", "El-Halîm", "Cezalandırmada acele etmeyen, hilm ve yumuşaklık sahibi.", 88, "Öfkeyi yenmek ve ahlak güzelliği için."),
    EsmaItem(33, "الْعَظِيمُ", "El-Azîm", "Büyüklüğünün ve azametinin sınırı olmayan.", 1020, "Hastalara şifa ve hürmet görmek için."),
    EsmaItem(34, "الْغَفُورُ", "El-Gafûr", "Mağfireti çok bol, affı sınırsız olan.", 1286, "Günahların affı ve gönül huzuru için."),
    EsmaItem(35, "الشَّكُورُ", "Eş-Şekûr", "Az amele çok mükafat veren, şükrü bolca karşılayan.", 526, "Nimetin artması ve bereket için."),
    EsmaItem(36, "الْعَلِيُّ", "El-Aliyy", "İzzet ve şerefte en yüce, eşsiz ve benzersiz.", 110, "İlim ve derecenin yükselmesi için."),
    EsmaItem(37, "الْكَبِيرُ", "El-Kebîr", "Büyüklüğü akıllara sığmayan, ulu.", 232, "Hürmet ve itibar görmek için."),
    EsmaItem(38, "الْحَفِيظُ", "El-Hafîz", "Her şeyi koruyup gözeten, hıfz eden.", 998, "Her türlü tehlike ve kazadan korunmak için."),
    EsmaItem(39, "الْمُقِيتُ", "El-Mukît", "Bütün mahlûkatın azığını veren, muktedir olan.", 550, "Açlık ve muhtaçlıktan korunmak için."),
    EsmaItem(40, "الْحَسِيبُ", "El-Hasîb", "Kullarının hesabını en mükemmel şekilde gören.", 80, "Düşman şerrinden korunmak için."),
    EsmaItem(41, "الْجَلِيلُ", "El-Celîl", "Azamet ve celal sahibi, yüce.", 73, "Saygınlık ve hürmet kazanmak için."),
    EsmaItem(42, "الْكَرِيمُ", "El-Kerîm", "Lütuf ve ihsanı bol, kerem sahibi.", 270, "Bol nimet ve cömertlik için."),
    EsmaItem(43, "الرَّقِيبُ", "Er-Rakîb", "Her şeyi her an görüp gözeten, kontrol eden.", 312, "Emaneti korumak ve gafletten uyanmak için."),
    EsmaItem(44, "الْمُجِيبُ", "El-Mücîb", "Duaları ve niyazları işitip kabul buyuran.", 55, "Duaların tez zamanda kabulü için."),
    EsmaItem(45, "الْوَاسِعُ", "El-Vâsi", "İlmi, rahmeti ve kudreti her şeyi kuşatan.", 137, "Gönül ve rızık genişliği için."),
    EsmaItem(46, "الْحَكِيمُ", "El-Hakîm", "Her emri ve işi hikmetli, faydalı olan.", 78, "İlim, hikmet ve basiret için."),
    EsmaItem(47, "الْوَدُودُ", "El-Vedûd", "Kullarını çok seven, sevilmeye en layık olan.", 20, "Aile huzuru, muhabbet ve sevgi kazanmak için."),
    EsmaItem(48, "الْمَجِيدُ", "El-Mecîd", "Şanı büyük, keremi sonsuz olan.", 57, "İzzet ve şeref kazanmak için."),
    EsmaItem(49, "الْبَاعِثُ", "El-Bâis", "Ölüleri dirilten, peygamberler gönderen.", 573, "Manevi uyanış ve gafletten kurtulmak için."),
    EsmaItem(50, "الشَّهِيدُ", "Eş-Şehîd", "Her şeye şahit olan, hiçbir şeyden gafil olmayan.", 319, "İsyankar nefsi ve çocukları ıslah etmek için."),
    EsmaItem(51, "الْحَقُّ", "El-Hakk", "Varlığı hiç değişmeyen, mutlak gerçek.", 108, "İşlerin sağlamlığı ve batıldan kurtulmak için."),
    EsmaItem(52, "الْوَكِيلُ", "El-Vekîl", "İşleri kendisine havale edenlerin işini en güzel yapan.", 66, "Korkulardan ve belalardan emin olmak için."),
    EsmaItem(53, "الْقَوِيُّ", "El-Kaviyy", "Kudreti sonsuz, her şeye gücü yeten.", 116, "Maddi ve manevi güç, cesaret için."),
    EsmaItem(54, "الْمَتِينُ", "El-Metîn", "Kuvveti çok sağlam, sarsılmaz kudret sahibi.", 500, "Zorluklara karşı dayanıklılık ve sabır için."),
    EsmaItem(55, "الْوَلِيُّ", "El-Veliyy", "Müminlerin dostu, yardımcısı ve hamisi.", 46, "İlahi yardım ve dostluk kazanmak için."),
    EsmaItem(56, "الْحَمِيدُ", "El-Hamîd", "Bütün övgülere ve hamdlere layık olan.", 62, "Ahlak güzelliği ve güzel söz söylemek için."),
    EsmaItem(57, "الْمُحْصِي", "El-Muhsî", "Her şeyin sayısını ve miktarını tek tek bilen.", 148, "Zihin açıklığı ve unutkanlığı yenmek için."),
    EsmaItem(58, "الْمُبْدِئُ", "El-Mübdi", "Mahlûkatı maddesiz ve örneksiz ilk baştan yaratan.", 56, "İşlere hayırla başlamak ve muvaffakiyet için."),
    EsmaItem(59, "الْمُعِيدُ", "El-Muîd", "Öldükten sonra tekrar dirilten, iade eden.", 124, "Kaybedilen şeyin geri gelmesi için."),
    EsmaItem(60, "الْمُحْيِي", "El-Muhyî", "Hayat veren, dirilten, can bağışlayan.", 68, "Şifa ve kalbin dirilmesi için."),
    EsmaItem(61, "الْمُمِيتُ", "El-Mümît", "Eceli gelenlerin canını alan, ölümü yaratan.", 490, "Kötü nefsi terbiye etmek için."),
    EsmaItem(62, "الْحَيُّ", "El-Hayy", "Daima diri, ezeli ve ebedi hayat sahibi.", 18, "Uzun ve sıhhatli ömür, kalp diriliği için."),
    EsmaItem(63, "الْقَيُّومُ", "El-Kayyûm", "Her şeyi ayakta tutan, varlığı kendinden olan.", 156, "Direnç ve yorgunluktan kurtulmak için."),
    EsmaItem(64, "الْوَاجِدُ", "El-Vâcid", "İstediğini istediği an bulan, hiçbir şeye muhtaç olmayan.", 14, "Kaybedileni bulmak ve zenginlik için."),
    EsmaItem(65, "الْمَاجِدُ", "El-Mâcid", "Kadri ve şânı pek yüce, keremi bol olan.", 48, "Kalp nuru ve izzet için."),
    EsmaItem(66, "الْوَاحِدُ", "El-Vâhid", "Zatında, sıfatlarında ve fiillerinde tek olan.", 19, "Kalbin tevhidi ve vesveseden kurtulmak için."),
    EsmaItem(67, "الْأَحَدُ", "El-Ahad", "Benzeri ve ortağı olmayan mutlak bir.", 13, "Huzur ve samimi ihlas için."),
    EsmaItem(68, "الصَّمَدُ", "Es-Samed", "Hiçbir şeye muhtaç olmayan, her şeyin O'na muhtaç olduğu.", 134, "Kimseye muhtaç olmamak için."),
    EsmaItem(69, "الْقَادِرُ", "El-Kâdir", "Dilediğini dilediği gibi yapmaya gücü yeten.", 305, "İşlerde muvaffakiyet ve güç için."),
    EsmaItem(70, "الْمُقْتَدِرُ", "El-Muktedir", "Kuvvet ve kudret sahipleri üzerinde dilediği gibi tasarruf eden.", 744, "Zorlukları kolay kılmak için."),
    EsmaItem(71, "الْمُقَدِّمُ", "El-Mukaddim", "Dilediğini öne alan, yükselten.", 184, "İlerleme ve başarı için."),
    EsmaItem(72, "الْمُؤَخِّرُ", "El-Muahhir", "Dilediğini geriye bırakan, erteleyen.", 846, "Kötülüklerden uzaklaşmak için."),
    EsmaItem(73, "الْأَوَّلُ", "El-Evvel", "Varlığının başlangıcı olmayan ezeli.", 37, "İşlerin hayırla başlaması için."),
    EsmaItem(74, "الْآخِرُ", "El-Âhir", "Varlığının sonu olmayan ebedi.", 801, "Ömür bereketi ve hüsn-i hâtime için."),
    EsmaItem(75, "الظَّاهِرُ", "Ez-Zâhir", "Varlığı delilleriyle apaçık ortada olan.", 1106, "Kalp gözünün açılması için."),
    EsmaItem(76, "الْبَاطِنُ", "El-Bâtın", "Akılların idrak edemeyeceği kadar gizli ve yüce olan.", 62, "Kalp huzuru ve nefsani arınma için."),
    EsmaItem(77, "الْوَالِي", "El-Vâlî", "Kâinatı idare eden, yöneten mutlak hâkim.", 47, "İdarecilik ve işlerin yolunda gitmesi için."),
    EsmaItem(78, "الْمُتَعَالِي", "El-Müteâlî", "Noksanlıklardan pek yüce ve münezzeh olan.", 541, "Makam ve itibar için."),
    EsmaItem(79, "الْبَرُّ", "El-Berr", "Kullarına iyiliği ve ihsanı bol olan.", 202, "Hayırlı kapılar ve iyilik görmek için."),
    EsmaItem(80, "التَّوَّابُ", "Et-Tevvâb", "Tövbeleri çokça kabul eden, günahları bağışlayan.", 409, "Tövbelerin kabulü ve günahtan dönüş için."),
    EsmaItem(81, "الْمُنْتَقِمُ", "El-Müntakim", "Zalimlerden intikam alan, adaleti sağlayan.", 630, "Zulümden kurtulmak için."),
    EsmaItem(82, "العَفُوُّ", "El-Afüvv", "Kullarını affeden, günah izlerini tamamen silen.", 156, "Af ve mağfiret kazanmak için."),
    EsmaItem(83, "الرَّؤُوفُ", "Er-Raûf", "Şefkati ve merhameti pek bol olan.", 287, "Merhamet görmek ve kalbin yumuşaması için."),
    EsmaItem(84, "مَالِكُ الْمُلْكِ", "Mâlikü'l-Mülk", "Bütün mülkün ezeli ve ebedi tek sahibi.", 212, "Fakirlikten kurtulmak ve itibar için."),
    EsmaItem(85, "ذُو الْجَلَالِ وَالْإِكْرَامِ", "Zü'l-Celâli ve'l-İkrâm", "Büyüklük, azamet ve sonsuz cömertlik sahibi.", 1100, "Duaların kabulü ve hürmet için."),
    EsmaItem(86, "الْمُقْسِطُ", "El-Muksit", "Bütün işlerini denk ve adaletle yapan.", 209, "Huzur ve vesveselerden kurtulmak için."),
    EsmaItem(87, "الْجَامِعُ", "El-Câmi", "İstediğini istediği zaman bir araya getiren.", 114, "Kayıpların bulunması ve aile birliği için."),
    EsmaItem(88, "الْغَنِيُّ", "El-Ganiyy", "Hiçbir şeye muhtaç olmayan sonsuz zengin.", 1060, "Zenginlik ve rızık genişliği için."),
    EsmaItem(89, "الْمُغْنِي", "El-Mugnî", "Dilediğini zengin kılan, ihtiyacını gideren.", 1100, "Fakirlikten kurtulup refaha ermek için."),
    EsmaItem(90, "الْمَانِعُ", "El-Mâni", "Dilemediği şeyin gerçekleşmesine engel olan.", 161, "Kötülüklerden ve kazalardan korunmak için."),
    EsmaItem(91, "الضَّارُّ", "Ed-Dârr", "Zarar veren şeyleri yaratan, hikmetiyle imtihan eden.", 1001, "Zararlardan ve düşmandan emin olmak için."),
    EsmaItem(92, "النَّافِعُ", "En-Nâfi", "Faydalı şeyleri yaratan, menfaat veren.", 201, "Hastalıklardan şifa ve hayırlı kazanç için."),
    EsmaItem(93, "النُّورُ", "En-Nûr", "Âlemleri nurlandıran, hidayet veren mutlak ışık.", 256, "Kalbin nurlanması ve basiret için."),
    EsmaItem(94, "الْهَادِي", "El-Hâdî", "Hidayet veren, doğru yola ulaştıran.", 20, "Doğru yoldan ayrılmamak ve hidayet için."),
    EsmaItem(95, "الْبَدِيعُ", "El-Bedî", "Örneksiz ve emsalsiz güzellikte yaratan.", 86, "Maddi ve manevi güzellik için."),
    EsmaItem(96, "الْبَاقِي", "El-Bâkî", "Varlığının sonu olmayan ebedi.", 113, "Sıhhat ve uzun ömür için."),
    EsmaItem(97, "الْوَارِثُ", "El-Vâris", "Her şey yok olduktan sonra baki kalan tek sahip.", 707, "Uzun ömür ve bol rızık için."),
    EsmaItem(98, "الرَّشِيدُ", "Er-Reşîd", "Bütün işleri dosdoğru ve hikmetli olan.", 514, "Hayırlı kararlar ve başarı için."),
    EsmaItem(99, "الصَّبُورُ", "Es-Sabûr", "Çok sabırlı olan, cezalandırmada acele etmeyen.", 298, "Sabır, metanet ve zorluklara dayanmak için.")
)

class EsmaulHusnaActivity : ComponentActivity() {

    private fun shareEsma(item: EsmaItem) {
        val text = "✨ Esmâü'l-Hüsnâ:\n" +
                "${item.number}. ${item.nameTurkish} (${item.arabic})\n\n" +
                "📖 Anlamı: ${item.meaning}\n" +
                "📿 Zikir Sayısı (Ebced): ${item.ebced}\n" +
                "🤲 Fazileti: ${item.virtue}\n\n" +
                "Namaz Vakti Uygulaması ile paylaşıldı."

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "İsmi Paylaş"))
    }

    private fun openZikirmatikWithTarget(esmaName: String, ebcedCount: Int) {
        // ZikirmatikActivity'yi aç ve hedefi otomatik ata
        try {
            val intent = Intent(this, ZikirmatikActivity::class.java).apply {
                putExtra("SELECTED_DHIKR_TITLE", esmaName)
                putExtra("SELECTED_DHIKR_TARGET", ebcedCount)
            }
            startActivity(intent)
        } catch (_: Exception) {
            // Alternatif dosya adı
            try {
                val intent = Intent(this, Class.forName("com.example.namazvakti.ZikirmaticActivity")).apply {
                    putExtra("SELECTED_DHIKR_TITLE", esmaName)
                    putExtra("SELECTED_DHIKR_TARGET", ebcedCount)
                }
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EsmaulHusnaScreen(
                onBack = { finish() },
                onShare = { shareEsma(it) },
                onZikirStart = { name, count -> openZikirmatikWithTarget(name, count) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EsmaulHusnaScreen(
    onBack: () -> Unit,
    onShare: (EsmaItem) -> Unit,
    onZikirStart: (String, Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedEsmaForDetail by remember { mutableStateOf<EsmaItem?>(null) }

    val filteredList = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            ESMA_LIST
        } else {
            ESMA_LIST.filter {
                it.nameTurkish.contains(searchQuery, ignoreCase = true) ||
                        it.meaning.contains(searchQuery, ignoreCase = true) ||
                        it.number.toString().contains(searchQuery) ||
                        it.ebced.toString().contains(searchQuery)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Esmâü'l-Hüsnâ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Allah'ın 99 İsm-i Şerifi ve Faziletleri",
                            fontSize = 11.sp,
                            color = Color(0xFFC8E6C9)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F3E2C)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D251A),
                            Color(0xFF081710)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            // 1. ARAMA ÇUBUĞU
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("İsim veya fazilet ara (örn: rızık, şifa, Fettah)...", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF81C784))
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFD54F),
                    unfocusedBorderColor = Color(0xFF2E7D32).copy(alpha = 0.5f),
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. 99 İSİM KARTLARI LİSTESİ
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.number }) { item ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedEsmaForDetail = item }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Sıra Numarası Rozeti
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color(0xFF1B5E20), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${item.number}",
                                        color = Color(0xFFFFD54F),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.nameTurkish,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = item.meaning,
                                        fontSize = 11.sp,
                                        color = Color(0xFFCFD8DC),
                                        maxLines = 2,
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📿 Zikir Adedi: ${item.ebced}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFFD54F)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Sağda Büyük Arapça Hat Sanatı
                            Text(
                                text = item.arabic,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFE082),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DETAY, TEFEKKÜR VE ZİKİR BAŞLATMA DİYALOĞU ---
    selectedEsmaForDetail?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedEsmaForDetail = null },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.arabic,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${item.number}. ${item.nameTurkish}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF0F3E2C)
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "📖 Anlamı:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = item.meaning,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "🤲 Fazileti ve Zikir Niyeti:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = item.virtue,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Ebced Bilgisi Kutusu
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📿 Ebced Değeri: ${item.ebced} Defa",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                // Zikirmatikte Başlat Butonu
                Button(
                    onClick = {
                        onZikirStart(item.nameTurkish, item.ebced)
                        selectedEsmaForDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3E2C)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("📿 Zikret (${item.ebced})", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                // Paylaş Butonu
                IconButton(onClick = { onShare(item) }) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Paylaş", tint = Color(0xFF1B5E20))
                }
            }
        )
    }
}