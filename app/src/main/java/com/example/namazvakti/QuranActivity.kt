package com.example.namazvakti

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class AyahItem(
    val verseNumber: Int,
    val arabicText: String,
    val turkishMeal: String,
    val audioUrl: String,
    val surahNumber: Int = 1
)

data class SurahInfo(
    val number: Int,
    val nameTr: String,
    val nameAr: String,
    val versesCount: Int,
    val juzNumber: Int
)

enum class MealAuthor(val idName: String, val title: String, val editionKey: String) {
    DIYANET("diyanet", "Diyanet İşleri Başkanlığı", "tr.diyanet"),
    ELMALILI("elmalili", "Elmalılı Hamdi Yazır", "tr.yazir"),
    SUAT_YILDIRIM("suat_yildirim", "Prof. Dr. Suat Yıldırım", "tr.yildirim"),
    SULEYMAN_ATES("suleyman_ates", "Prof. Dr. Süleyman Ateş", "tr.ates"),
    ALI_BULAC("ali_bulac", "Ali Bulaç", "tr.bulac")
}

class QuranActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                QuranReaderScreen(
                    onBack = { finish() },
                    onPlayAudio = { url, speed, onDone, onError ->
                        playAyahAudio(url, speed, onDone, onError)
                    },
                    onStopAudio = { stopAudio() },
                    onSetSpeed = { speed -> setPlaybackSpeed(speed) }
                )
            }
        }
    }

    private fun playAyahAudio(
        url: String,
        speed: Float,
        onDone: () -> Unit,
        onError: () -> Unit
    ) {
        try {
            stopAudio()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { mp ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val params = mp.playbackParams
                            params.speed = speed
                            mp.playbackParams = params
                        } catch (_: Exception) {}
                    }
                    mp.start()
                }
                setOnCompletionListener {
                    onDone()
                }
                setOnErrorListener { _, _, _ ->
                    onError()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onError()
        }
    }

    private fun setPlaybackSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        val params = mp.playbackParams
                        params.speed = speed
                        mp.playbackParams = params
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    onBack: () -> Unit,
    onPlayAudio: (url: String, speed: Float, onDone: () -> Unit, onError: () -> Unit) -> Unit,
    onStopAudio: () -> Unit,
    onSetSpeed: (Float) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val prefs = remember { context.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE) }

    // Durumlar
    var isSurahMode by remember { mutableStateOf(false) }
    var selectedSurahNumber by remember { mutableStateOf(prefs.getInt("last_surah", 1)) }
    var selectedJuzNumber by remember { mutableStateOf(prefs.getInt("last_juz", 1)) }
    var pageInJuz by remember { mutableStateOf(1) } // Her cüz için tam 1..20
    var selectedMeal by remember { mutableStateOf(MealAuthor.DIYANET) }

    var arabicFontSize by remember { mutableStateOf(24f) }
    var turkishFontSize by remember { mutableStateOf(14f) }
    var currentSpeedIndex by remember { mutableStateOf(0) }
    val speedOptions = listOf(1.0f, 1.25f, 1.5f)

    var bookmarkedAyah by remember { mutableStateOf(prefs.getInt("bookmarked_ayah", 1)) }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayingIndex by remember { mutableStateOf(-1) }
    var isLoadingAyahs by remember { mutableStateOf(false) }
    var ayahs by remember { mutableStateOf<List<AyahItem>>(emptyList()) }

    var showPickerModal by remember { mutableStateOf(false) }
    var showMealModal by remember { mutableStateOf(false) }

    val allSurahs = remember { getAllSurahList() }
    val currentSurah = allSurahs.find { it.number == selectedSurahNumber } ?: allSurahs.first()

    // Bütün cüzler standart 20 sayfadır
    val totalPagesInCurrentJuz = 20

    // Aktif sayfa hesabı (1..600)
    val currentAbsolutePage = remember(selectedJuzNumber, pageInJuz) {
        ((selectedJuzNumber - 1) * 20) + pageInJuz
    }

    // --- AYETLERİ YÜKLEME ---
    fun loadContent(surahNo: Int, juzNo: Int, pageIdx: Int, inSurahMode: Boolean, autoPlayFirst: Boolean = false) {
        isLoadingAyahs = true
        onStopAudio()
        isPlaying = false
        currentPlayingIndex = -1

        coroutineScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                if (inSurahMode) {
                    fetchSurahAyahs(surahNo, selectedMeal)
                } else {
                    val absPage = ((juzNo - 1) * 20) + pageIdx
                    fetchPageAyahs(absPage, selectedMeal)
                }
            }
            ayahs = loaded
            isLoadingAyahs = false
            try {
                listState.scrollToItem(0)
            } catch (_: Exception) {}

            if (autoPlayFirst && loaded.isNotEmpty()) {
                isPlaying = true
                currentPlayingIndex = 0
            }
        }
    }

    LaunchedEffect(selectedSurahNumber, selectedJuzNumber, pageInJuz, isSurahMode, selectedMeal) {
        loadContent(selectedSurahNumber, selectedJuzNumber, pageInJuz, isSurahMode, false)
    }

    fun playFromIndex(index: Int) {
        if (index < 0 || index >= ayahs.size) return
        val item = ayahs[index]
        currentPlayingIndex = index
        isPlaying = true

        coroutineScope.launch {
            try {
                listState.animateScrollToItem(index)
            } catch (_: Exception) {}
        }

        onPlayAudio(
            item.audioUrl,
            speedOptions[currentSpeedIndex],
            {
                if (currentPlayingIndex < ayahs.size - 1) {
                    playFromIndex(currentPlayingIndex + 1)
                } else {
                    if (isSurahMode) {
                        if (selectedSurahNumber < 114) {
                            val nextSurah = selectedSurahNumber + 1
                            selectedSurahNumber = nextSurah
                            prefs.edit().putInt("last_surah", nextSurah).apply()
                            Toast.makeText(
                                context,
                                "Sıradaki sure: ${allSurahs.find { it.number == nextSurah }?.nameTr}",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadContent(nextSurah, selectedJuzNumber, pageInJuz, inSurahMode = true, autoPlayFirst = true)
                        } else {
                            isPlaying = false
                            currentPlayingIndex = -1
                            Toast.makeText(context, "Kur'an-ı Kerim tamamlandı! Hatminiz mübarek olsun.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        if (pageInJuz < totalPagesInCurrentJuz) {
                            val nextPage = pageInJuz + 1
                            pageInJuz = nextPage
                            Toast.makeText(context, "$nextPage. Sayfaya geçildi", Toast.LENGTH_SHORT).show()
                            loadContent(selectedSurahNumber, selectedJuzNumber, nextPage, inSurahMode = false, autoPlayFirst = true)
                        } else {
                            if (selectedJuzNumber < 30) {
                                val nextJuz = selectedJuzNumber + 1
                                selectedJuzNumber = nextJuz
                                pageInJuz = 1
                                prefs.edit().putInt("last_juz", nextJuz).apply()
                                Toast.makeText(context, "$nextJuz. Cüze geçildi", Toast.LENGTH_SHORT).show()
                                loadContent(selectedSurahNumber, nextJuz, 1, inSurahMode = false, autoPlayFirst = true)
                            } else {
                                isPlaying = false
                                currentPlayingIndex = -1
                                Toast.makeText(context, "30 Cüz Hatmi tamamlandı!", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            },
            {
                isPlaying = false
                currentPlayingIndex = -1
                Toast.makeText(context, "Ses çalınırken bir hata oluştu", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFF09120E),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF132019),
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Kur'an-ı Kerim ve Meali",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isSurahMode) {
                                "${currentSurah.nameTr} (${currentSurah.juzNumber}. Cüz)"
                            } else {
                                "${selectedJuzNumber}. Cüz • Sayfa $pageInJuz / 20"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (arabicFontSize > 18f) {
                            arabicFontSize -= 2f
                            turkishFontSize -= 1f
                        }
                    }) {
                        Text("A-", color = Color(0xFFFDE68A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    TextButton(onClick = {
                        if (arabicFontSize < 38f) {
                            arabicFontSize += 2f
                            turkishFontSize += 1f
                        }
                    }) {
                        Text("A+", color = Color(0xFFFDE68A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(
                color = Color(0xFF0F1B14),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (isSurahMode && selectedSurahNumber > 1) {
                                    selectedSurahNumber -= 1
                                    prefs.edit().putInt("last_surah", selectedSurahNumber).apply()
                                } else if (!isSurahMode) {
                                    if (pageInJuz > 1) {
                                        pageInJuz -= 1
                                    } else if (selectedJuzNumber > 1) {
                                        selectedJuzNumber -= 1
                                        pageInJuz = 20
                                        prefs.edit().putInt("last_juz", selectedJuzNumber).apply()
                                    }
                                }
                            },
                            enabled = if (isSurahMode) selectedSurahNumber > 1 else (pageInJuz > 1 || selectedJuzNumber > 1),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF192A20))
                        ) {
                            Text("◀", color = Color(0xFFD4AF37), fontSize = 12.sp)
                        }

                        Surface(
                            onClick = { showPickerModal = true },
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF192A20),
                            border = BorderStroke(1.dp, Color(0xFF274333)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFFD4AF37),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSurahMode) currentSurah.nameTr else "${selectedJuzNumber}. Cüz (s. $pageInJuz/20)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (isSurahMode && selectedSurahNumber < 114) {
                                    selectedSurahNumber += 1
                                    prefs.edit().putInt("last_surah", selectedSurahNumber).apply()
                                } else if (!isSurahMode) {
                                    if (pageInJuz < totalPagesInCurrentJuz) {
                                        pageInJuz += 1
                                    } else if (selectedJuzNumber < 30) {
                                        selectedJuzNumber += 1
                                        pageInJuz = 1
                                        prefs.edit().putInt("last_juz", selectedJuzNumber).apply()
                                    }
                                }
                            },
                            enabled = if (isSurahMode) selectedSurahNumber < 114 else (pageInJuz < totalPagesInCurrentJuz || selectedJuzNumber < 30),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF192A20))
                        ) {
                            Text("▶", color = Color(0xFFD4AF37), fontSize = 12.sp)
                        }

                        Surface(
                            onClick = {
                                currentSpeedIndex = (currentSpeedIndex + 1) % speedOptions.size
                                onSetSpeed(speedOptions[currentSpeedIndex])
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF243B2C),
                            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "${speedOptions[currentSpeedIndex]}x",
                                color = Color(0xFFFDE68A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                            )
                        }

                        Button(
                            onClick = {
                                if (isPlaying) {
                                    onStopAudio()
                                    isPlaying = false
                                    currentPlayingIndex = -1
                                } else {
                                    if (ayahs.isNotEmpty()) {
                                        playFromIndex(0)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPlaying) Color(0xFFDC2626) else Color(0xFF15803D)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPlaying) "Durdur" else "Dinle",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Meal: ${selectedMeal.title} ▾",
                            color = Color(0xFFD4AF37),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { showMealModal = true }
                                .padding(vertical = 2.dp)
                        )
                        Text(
                            text = if (isSurahMode) "Kaldığın: $bookmarkedAyah. Ayet" else "Sayfa $pageInJuz / 20",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (isLoadingAyahs) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF10B981))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Sayfa yükleniyor...", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                }
            } else if (ayahs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sayfa yüklenemedi", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { loadContent(selectedSurahNumber, selectedJuzNumber, pageInJuz, isSurahMode) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Tekrar Dene")
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    itemsIndexed(ayahs) { index, item ->
                        val isCurrentPlaying = (currentPlayingIndex == index && isPlaying)
                        val isBookmarked = (bookmarkedAyah == item.verseNumber)
                        val surahName = allSurahs.find { it.number == item.surahNumber }?.nameTr ?: ""

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentPlaying) Color(0xFF173022) else Color(0xFF111E17)
                            ),
                            border = BorderStroke(
                                width = if (isCurrentPlaying) 1.5.dp else 1.dp,
                                color = if (isCurrentPlaying) Color(0xFF10B981) else Color(0xFF1E3529)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(if (isCurrentPlaying) Color(0xFF10B981) else Color(0xFF1B5E20)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item.verseNumber.toString(),
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (surahName.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "$surahName Suresi",
                                                color = Color(0xFF10B981),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            onClick = {
                                                if (isCurrentPlaying) {
                                                    onStopAudio()
                                                    isPlaying = false
                                                    currentPlayingIndex = -1
                                                } else {
                                                    playFromIndex(index)
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF192A20)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    if (isCurrentPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = if (isCurrentPlaying) Color(0xFF10B981) else Color(0xFFD4AF37),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isCurrentPlaying) "Durdur" else "Dinle",
                                                    color = Color.White,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Surface(
                                            onClick = {
                                                bookmarkedAyah = item.verseNumber
                                                prefs.edit().putInt("bookmarked_ayah", item.verseNumber).apply()
                                                Toast.makeText(context, "${item.verseNumber}. Ayet kaydedildi", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isBookmarked) Color(0xFF2E5339) else Color(0xFF192A20)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (isBookmarked) Color(0xFFFDE68A) else Color.Gray,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isBookmarked) "Kaldım" else "İşaretle",
                                                    color = if (isBookmarked) Color(0xFFFDE68A) else Color.White.copy(alpha = 0.8f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = item.arabicText,
                                    fontSize = arabicFontSize.sp,
                                    color = Color.White,
                                    lineHeight = (arabicFontSize * 1.6f).sp,
                                    textAlign = TextAlign.Right,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = item.turkishMeal,
                                    fontSize = turkishFontSize.sp,
                                    color = Color(0xFFD1D5DB),
                                    lineHeight = (turkishFontSize * 1.4f).sp
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF132019)),
                            border = BorderStroke(1.dp, Color(0xFF243B2C))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isSurahMode) {
                                        "📖 ${currentSurah.nameTr} Tamamlandı"
                                    } else {
                                        if (pageInJuz == 20) {
                                            "🎉 ${selectedJuzNumber}. Cüz Tamamlandı!"
                                        } else {
                                            "📖 Sayfa $pageInJuz / 20"
                                        }
                                    },
                                    color = Color(0xFFD4AF37),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            if (isSurahMode && selectedSurahNumber > 1) {
                                                val prev = selectedSurahNumber - 1
                                                selectedSurahNumber = prev
                                                prefs.edit().putInt("last_surah", prev).apply()
                                            } else if (!isSurahMode) {
                                                if (pageInJuz > 1) {
                                                    pageInJuz -= 1
                                                } else if (selectedJuzNumber > 1) {
                                                    selectedJuzNumber -= 1
                                                    pageInJuz = 20
                                                    prefs.edit().putInt("last_juz", selectedJuzNumber).apply()
                                                }
                                            }
                                        },
                                        enabled = if (isSurahMode) selectedSurahNumber > 1 else (pageInJuz > 1 || selectedJuzNumber > 1),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1E3529),
                                            disabledContainerColor = Color(0xFF0F1B14)
                                        )
                                    ) {
                                        Text(
                                            if (isSurahMode) "← Önceki Sure" else "← Önceki Sayfa",
                                            color = Color.White
                                        )
                                    }

                                    Text(
                                        text = if (isSurahMode) "$selectedSurahNumber / 114" else "$pageInJuz / 20",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Button(
                                        onClick = {
                                            if (isSurahMode && selectedSurahNumber < 114) {
                                                val next = selectedSurahNumber + 1
                                                selectedSurahNumber = next
                                                prefs.edit().putInt("last_surah", next).apply()
                                            } else if (!isSurahMode) {
                                                if (pageInJuz < 20) {
                                                    pageInJuz += 1
                                                } else if (selectedJuzNumber < 30) {
                                                    selectedJuzNumber += 1
                                                    pageInJuz = 1
                                                    prefs.edit().putInt("last_juz", selectedJuzNumber).apply()
                                                }
                                            }
                                        },
                                        enabled = if (isSurahMode) selectedSurahNumber < 114 else (pageInJuz < 20 || selectedJuzNumber < 30),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981),
                                            disabledContainerColor = Color(0xFF0F1B14)
                                        )
                                    ) {
                                        Text(
                                            if (isSurahMode) {
                                                "Sonraki Sure →"
                                            } else {
                                                if (pageInJuz == 20) "Sıradaki Cüz →" else "Sonraki Sayfa →"
                                            },
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPickerModal) {
        var pickerSearch by remember { mutableStateOf("") }
        var currentTab by remember { mutableStateOf(if (isSurahMode) 0 else 1) }

        AlertDialog(
            onDismissRequest = { showPickerModal = false },
            containerColor = Color(0xFF132019),
            title = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TabButton("Sureler", currentTab == 0) { currentTab = 0 }
                        TabButton("Cüzler", currentTab == 1) { currentTab = 1 }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pickerSearch,
                        onValueChange = { pickerSearch = it },
                        placeholder = { Text("Sure veya Cüz ara...", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF274333),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            text = {
                Box(modifier = Modifier.height(350.dp)) {
                    if (currentTab == 0) {
                        val filtered = allSurahs.filter {
                            it.nameTr.contains(pickerSearch, ignoreCase = true) ||
                                    it.number.toString().contains(pickerSearch)
                        }
                        LazyColumn {
                            items(filtered.size) { i ->
                                val s = filtered[i]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isSurahMode = true
                                            selectedSurahNumber = s.number
                                            prefs.edit().putInt("last_surah", s.number).apply()
                                            showPickerModal = false
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${s.number}.",
                                        color = Color(0xFFD4AF37),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.width(34.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = s.nameTr, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(text = "${s.versesCount} Ayet • ${s.juzNumber}. Cüz", color = Color.Gray, fontSize = 11.sp)
                                    }
                                    Text(text = s.nameAr, color = Color(0xFF10B981), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Divider(color = Color(0xFF1E3529))
                            }
                        }
                    } else {
                        val juzList = (1..30).toList().filter { it.toString().contains(pickerSearch) }
                        LazyColumn {
                            items(juzList.size) { i ->
                                val j = juzList[i]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isSurahMode = false
                                            selectedJuzNumber = j
                                            pageInJuz = 1
                                            prefs.edit().putInt("last_juz", j).apply()
                                            showPickerModal = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "$j. Cüz", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "20 Sayfa", color = Color(0xFF10B981), fontSize = 12.sp)
                                }
                                Divider(color = Color(0xFF1E3529))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPickerModal = false }) {
                    Text("Kapat", color = Color(0xFF10B981))
                }
            }
        )
    }

    if (showMealModal) {
        AlertDialog(
            onDismissRequest = { showMealModal = false },
            containerColor = Color(0xFF132019),
            title = {
                Text("Meal Müellifi Seçin", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    MealAuthor.values().forEach { meal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMeal = meal
                                    showMealModal = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedMeal == meal),
                                onClick = {
                                    selectedMeal = meal
                                    showMealModal = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = meal.title,
                                color = if (selectedMeal == meal) Color(0xFF10B981) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (selectedMeal == meal) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMealModal = false }) {
                    Text("Kapat", color = Color(0xFF10B981))
                }
            }
        )
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF10B981) else Color(0xFF1E3529)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}

fun httpGet(urlString: String): String? {
    return try {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
        }
        if (conn.responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            sb.toString()
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun fetchSurahAyahs(surahNo: Int, meal: MealAuthor): List<AyahItem> = withContext(Dispatchers.IO) {
    if (surahNo == 1) return@withContext getSampleFatiha(meal)

    try {
        val arDeferred = async { httpGet("https://api.alquran.cloud/v1/surah/$surahNo/quran-uthmani") }
        val trDeferred = async { httpGet("https://api.alquran.cloud/v1/surah/$surahNo/${meal.editionKey}") }

        val arBody = arDeferred.await()
        val trBody = trDeferred.await()

        if (arBody != null && trBody != null) {
            val arJson = JSONObject(arBody).getJSONObject("data").getJSONArray("ayahs")
            val trJson = JSONObject(trBody).getJSONObject("data").getJSONArray("ayahs")

            val list = mutableListOf<AyahItem>()
            val len = minOf(arJson.length(), trJson.length())
            for (i in 0 until len) {
                val ar = arJson.getJSONObject(i)
                val tr = trJson.getJSONObject(i)
                val verseNo = ar.getInt("numberInSurah")
                val audioUrl = String.format(Locale.US, "https://everyayah.com/data/Alafasy_128kbps/%03d%03d.mp3", surahNo, verseNo)

                list.add(
                    AyahItem(
                        verseNumber = verseNo,
                        arabicText = ar.getString("text"),
                        turkishMeal = tr.getString("text"),
                        audioUrl = audioUrl,
                        surahNumber = surahNo
                    )
                )
            }
            if (list.isNotEmpty()) return@withContext list
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext emptyList()
}

suspend fun fetchPageAyahs(pageNo: Int, meal: MealAuthor): List<AyahItem> = withContext(Dispatchers.IO) {
    if (pageNo == 1) {
        return@withContext getSampleFatiha(meal)
    }

    try {
        val arDeferred = async { httpGet("https://api.alquran.cloud/v1/page/$pageNo/quran-uthmani") }
        val trDeferred = async { httpGet("https://api.alquran.cloud/v1/page/$pageNo/${meal.editionKey}") }

        val arBody = arDeferred.await()
        val trBody = trDeferred.await()

        if (arBody != null && trBody != null) {
            val arJson = JSONObject(arBody).getJSONObject("data").getJSONArray("ayahs")
            val trJson = JSONObject(trBody).getJSONObject("data").getJSONArray("ayahs")

            val list = mutableListOf<AyahItem>()
            val len = minOf(arJson.length(), trJson.length())
            for (i in 0 until len) {
                val ar = arJson.getJSONObject(i)
                val tr = trJson.getJSONObject(i)
                val surahNo = ar.getJSONObject("surah").getInt("number")
                val verseNo = ar.getInt("numberInSurah")

                val audioUrl = String.format(Locale.US, "https://everyayah.com/data/Alafasy_128kbps/%03d%03d.mp3", surahNo, verseNo)

                list.add(
                    AyahItem(
                        verseNumber = verseNo,
                        arabicText = ar.getString("text"),
                        turkishMeal = tr.getString("text"),
                        audioUrl = audioUrl,
                        surahNumber = surahNo
                    )
                )
            }
            if (list.isNotEmpty()) return@withContext list
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return@withContext emptyList()
}

fun getSampleFatiha(meal: MealAuthor): List<AyahItem> {
    val meals = when (meal) {
        MealAuthor.DIYANET -> listOf(
            "Rahmân ve Rahîm olan Allah'ın ismiyle.",
            "Hamd, Âlemlerin Rabbi olan Allah'a mahsustur.",
            "O, Rahmân'dır, Rahîm'dir.",
            "Hesap ve ceza gününün malikidir.",
            "Yalnız sana ibadet eder ve yalnız senden yardım dileriz.",
            "Bizi dosdoğru yola ilet;",
            "Kendilerine lütufta bulunduğun kimselerin yoluna; gazaba uğrayanların ve sapmışların yoluna değil."
        )
        MealAuthor.ELMALILI -> listOf(
            "Rahmân ve Rahîm olan Allah'ın adıyla.",
            "Hamd, âlemlerin Rabbi olan Allah'a mahsustur.",
            "O, Rahmân ve Rahîm'dir.",
            "Din gününün sahibidir.",
            "Ancak sana kulluk eder ve yalnız senden yardım dileriz.",
            "Bizi doğru yola hidayet eyle;",
            "Kendilerine nimet verdiğin mutlu kimselerin yoluna; gazaba uğrayanların ve sapıtanların değil."
        )
        MealAuthor.SUAT_YILDIRIM -> listOf(
            "Rahmân ve Rahîm olan Allah'ın adıyla.",
            "Bütün övgüler âlemlerin Rabbi Allah'a aittir.",
            "O, Rahmân'dır, Rahîm'dir.",
            "Hesap ve mükâfat gününün tek hâkimidir.",
            "Yalnız Sana kulluk eder, yalnız Senden yardım dileriz.",
            "Bizi doğru yola eriştir;",
            "Nimetine erdirdiğin kimselerin yoluna; gazaba uğrayanların ve sapmışların yoluna değil."
        )
        MealAuthor.SULEYMAN_ATES -> listOf(
            "Rahmân ve Rahîm olan Allah'ın adıyla.",
            "Övgü, âlemlerin Rabbi Allah'a yaraşır.",
            "O, esirgeyen, bağışlayandır.",
            "Ceza gününün hükümdarıdır.",
            "Yalnız Sana tapar ve yalnız Senden yardım isteriz.",
            "Bizi doğru yola ilet;",
            "Nimet verdiğin kimselerin yoluna; gazaba uğrayanların ve sapıtanların değil."
        )
        MealAuthor.ALI_BULAC -> listOf(
            "Rahman ve Rahim olan Allah'ın adıyla.",
            "Hamd, alemlerin Rabbi olan Allah'adır.",
            "O, Rahman'dır, Rahim'dir.",
            "Din gününün malikidir.",
            "Biz yalnızca Sana ibadet eder ve yalnızca Senden yardım dileriz.",
            "Bizi dosdoğru yola ilet;",
            "Kendilerine nimet verdiğin kimselerin yoluna; gazaba uğrayanların ve sapmışların yoluna değil."
        )
    }

    val arabs = listOf(
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
        "الرَّحْمَٰنِ الرَّحِيمِ",
        "مَالِكِ يَوْمِ الدِّينِ",
        "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
        "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
        "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ"
    )

    return (0..6).map { i ->
        AyahItem(
            verseNumber = i + 1,
            arabicText = arabs[i],
            turkishMeal = meals[i],
            audioUrl = String.format(Locale.US, "https://everyayah.com/data/Alafasy_128kbps/001%03d.mp3", i + 1),
            surahNumber = 1
        )
    }
}

fun getAllSurahList(): List<SurahInfo> = listOf(
    SurahInfo(1, "Fâtiha", "الفاتحة", 7, 1),
    SurahInfo(2, "Bakara", "البقرة", 286, 1),
    SurahInfo(3, "Âl-i İmrân", "آل عمران", 200, 3),
    SurahInfo(4, "Nisâ", "النساء", 176, 4),
    SurahInfo(5, "Mâide", "المائدة", 120, 6),
    SurahInfo(6, "En'âm", "الأنعام", 165, 7),
    SurahInfo(7, "A'râf", "الأعراف", 206, 8),
    SurahInfo(8, "Enfâl", "الأنفال", 75, 9),
    SurahInfo(9, "Tevbe", "التوبة", 129, 10),
    SurahInfo(10, "Yûnus", "يونس", 109, 11),
    SurahInfo(11, "Hûd", "هود", 123, 11),
    SurahInfo(12, "Yûsuf", "يوسف", 111, 12),
    SurahInfo(13, "Ra'd", "الرعد", 43, 13),
    SurahInfo(14, "İbrâhîm", "إبراهيم", 52, 13),
    SurahInfo(15, "Hicr", "الحجر", 99, 14),
    SurahInfo(16, "Nahl", "النحل", 128, 14),
    SurahInfo(17, "İsrâ", "الإسراء", 111, 15),
    SurahInfo(18, "Kehf", "الكهف", 110, 15),
    SurahInfo(19, "Meryem", "مريم", 98, 16),
    SurahInfo(20, "Tâhâ", "طه", 135, 16),
    SurahInfo(21, "Enbiyâ", "الأنبياء", 112, 17),
    SurahInfo(22, "Hac", "الحج", 78, 17),
    SurahInfo(23, "Mü'minûn", "المؤمنون", 118, 18),
    SurahInfo(24, "Nûr", "النور", 64, 18),
    SurahInfo(25, "Furkân", "الفرقان", 77, 18),
    SurahInfo(26, "Şu'arâ", "الشعراء", 227, 19),
    SurahInfo(27, "Neml", "النمل", 93, 19),
    SurahInfo(28, "Kasas", "القصص", 88, 20),
    SurahInfo(29, "Ankebût", "العنكبوت", 69, 20),
    SurahInfo(30, "Rûm", "الروم", 60, 21),
    SurahInfo(31, "Lokmân", "لقمان", 34, 21),
    SurahInfo(32, "Secde", "السجدة", 30, 21),
    SurahInfo(33, "Ahzâb", "الأحزاب", 73, 21),
    SurahInfo(34, "Sebe'", "سبأ", 54, 22),
    SurahInfo(35, "Fâtır", "فاطر", 45, 22),
    SurahInfo(36, "Yâsîn", "يس", 83, 22),
    SurahInfo(37, "Sâffât", "الصافات", 182, 23),
    SurahInfo(38, "Sâd", "ص", 88, 23),
    SurahInfo(39, "Zümer", "الزمر", 75, 23),
    SurahInfo(40, "Mü'min (Gâfir)", "غافر", 85, 24),
    SurahInfo(41, "Fussilet", "فصلت", 54, 24),
    SurahInfo(42, "Şûrâ", "الشورى", 53, 25),
    SurahInfo(43, "Zuhruf", "الزخرف", 89, 25),
    SurahInfo(44, "Duhân", "الدخان", 59, 25),
    SurahInfo(45, "Câsiye", "الجاثية", 37, 25),
    SurahInfo(46, "Ahkâf", "الأحقاف", 35, 26),
    SurahInfo(47, "Muhammed", "محمد", 38, 26),
    SurahInfo(48, "Fetih", "الفتح", 29, 26),
    SurahInfo(49, "Hucurât", "الحجرات", 18, 26),
    SurahInfo(50, "Kâf", "ق", 45, 26),
    SurahInfo(51, "Zâriyât", "الذاريات", 60, 26),
    SurahInfo(52, "Tûr", "الطور", 49, 27),
    SurahInfo(53, "Necm", "النجم", 62, 27),
    SurahInfo(54, "Kamer", "القمر", 55, 27),
    SurahInfo(55, "Rahmân", "الرحمن", 78, 27),
    SurahInfo(56, "Vâkı'a", "الواقعة", 96, 27),
    SurahInfo(57, "Hadîd", "الحديد", 29, 27),
    SurahInfo(58, "Mücâdele", "المجادلة", 22, 28),
    SurahInfo(59, "Haşr", "الحشر", 24, 28),
    SurahInfo(60, "Mümtehine", "الممتحنة", 13, 28),
    SurahInfo(61, "Saff", "الصف", 14, 28),
    SurahInfo(62, "Cum'a", "الجمعة", 11, 28),
    SurahInfo(63, "Münâfikûn", "المنافقون", 11, 28),
    SurahInfo(64, "Teğâbün", "التغابن", 18, 28),
    SurahInfo(65, "Talâk", "الطلاق", 12, 28),
    SurahInfo(66, "Tahrîm", "التحريم", 12, 28),
    SurahInfo(67, "Mülk", "الملك", 30, 29),
    SurahInfo(68, "Kalem", "القلم", 52, 29),
    SurahInfo(69, "Hâkka", "الحاقة", 52, 29),
    SurahInfo(70, "Me'âric", "المعارج", 44, 29),
    SurahInfo(71, "Nûh", "نوح", 28, 29),
    SurahInfo(72, "Cin", "الجن", 28, 29),
    SurahInfo(73, "Müzzemmil", "المزمل", 20, 29),
    SurahInfo(74, "Müddessir", "المدثر", 56, 29),
    SurahInfo(75, "Kıyâme", "القيامة", 40, 29),
    SurahInfo(76, "İnsân", "الإنسان", 31, 29),
    SurahInfo(77, "Mürselât", "المرسلات", 50, 29),
    SurahInfo(78, "Nebe'", "النبأ", 40, 30),
    SurahInfo(79, "Nâzi'ât", "النازعات", 46, 30),
    SurahInfo(80, "Abese", "عبس", 42, 30),
    SurahInfo(81, "Tekvîr", "التكوير", 29, 30),
    SurahInfo(82, "İnfitâr", "الانفطار", 19, 30),
    SurahInfo(83, "Mutaffifîn", "المطففين", 36, 30),
    SurahInfo(84, "İnşikâk", "الانشقاق", 25, 30),
    SurahInfo(85, "Bürûc", "البروج", 22, 30),
    SurahInfo(86, "Târık", "الطارق", 17, 30),
    SurahInfo(87, "A'lâ", "الأعلى", 19, 30),
    SurahInfo(88, "Gâşiye", "الغاشية", 26, 30),
    SurahInfo(89, "Fecr", "الفجر", 30, 30),
    SurahInfo(90, "Beled", "البلد", 20, 30),
    SurahInfo(91, "Şems", "الشمس", 15, 30),
    SurahInfo(92, "Leyl", "الليل", 21, 30),
    SurahInfo(93, "Duhâ", "الضحى", 11, 30),
    SurahInfo(94, "İnşirâh", "الشرح", 8, 30),
    SurahInfo(95, "Tîn", "التين", 8, 30),
    SurahInfo(96, "Alak", "العلق", 19, 30),
    SurahInfo(97, "Kadir", "القدر", 5, 30),
    SurahInfo(98, "Beyyine", "البينة", 8, 30),
    SurahInfo(99, "Zilzâl", "الزلزلة", 8, 30),
    SurahInfo(100, "Âdiyât", "العاديات", 11, 30),
    SurahInfo(101, "Kâri'a", "القارعة", 11, 30),
    SurahInfo(102, "Tekâsür", "التكاثر", 8, 30),
    SurahInfo(103, "Asr", "العصر", 3, 30),
    SurahInfo(104, "Hümeze", "الهمزة", 9, 30),
    SurahInfo(105, "Fîl", "الفيل", 5, 30),
    SurahInfo(106, "Kureyş", "قريş", 4, 30),
    SurahInfo(107, "Mâ'ûn", "الماعون", 7, 30),
    SurahInfo(108, "Kevser", "الكوثر", 3, 30),
    SurahInfo(109, "Kâfirûn", "الكافرون", 6, 30),
    SurahInfo(110, "Nasr", "النصر", 3, 30),
    SurahInfo(111, "Tebbet", "المسد", 5, 30),
    SurahInfo(112, "İhlâs", "الإخلاص", 4, 30),
    SurahInfo(113, "Felak", "الفلق", 5, 30),
    SurahInfo(114, "Nâs", "الناس", 6, 30)
)