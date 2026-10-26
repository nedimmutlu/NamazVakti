package com.example.namazvakti

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
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

// Cüz Bilgi Modeli
data class JuzTrackerItem(
    val number: Int,
    val title: String,
    val startEndInfo: String
)

val TRACKER_JUZ_LIST = (1..30).map { juzNo ->
    JuzTrackerItem(
        number = juzNo,
        title = "$juzNo. Cüz",
        startEndInfo = when (juzNo) {
            1 -> "Fâtiha 1 - Bakara 141"
            2 -> "Bakara 142 - Bakara 252"
            3 -> "Bakara 253 - Âl-i İmrân 92"
            4 -> "Âl-i İmrân 93 - Nisâ 23"
            5 -> "Nisâ 24 - Nisâ 147"
            6 -> "Nisâ 148 - Mâide 81"
            7 -> "Mâide 82 - En'âm 110"
            8 -> "En'âm 111 - A'râf 87"
            9 -> "A'râf 88 - Enfâl 40"
            10 -> "Enfâl 41 - Tevbe 92"
            11 -> "Tevbe 93 - Hûd 5"
            12 -> "Hûd 6 - Yûsuf 52"
            13 -> "Yûsuf 53 - İbrâhîm 52"
            14 -> "Hicr 1 - Nahl 128"
            15 -> "İsrâ 1 - Kehf 74"
            16 -> "Kehf 75 - Tâhâ 135"
            17 -> "Enbiyâ 1 - Hac 78"
            18 -> "Mü'minûn 1 - Furkân 20"
            19 -> "Furkân 21 - Neml 55"
            20 -> "Neml 56 - Ankebût 45"
            21 -> "Ankebût 46 - Ahzâb 30"
            22 -> "Ahzâb 31 - Yâsîn 27"
            23 -> "Yâsîn 28 - Zümer 31"
            24 -> "Zümer 32 - Fussilet 46"
            25 -> "Fussilet 47 - Câsiye 37"
            26 -> "Ahkâf 1 - Zâriyât 30"
            27 -> "Zâriyât 31 - Hadîd 29"
            28 -> "Mücâdele 1 - Tahrîm 12"
            29 -> "Mülk 1 - Mürselât 50"
            30 -> "Nebe 1 - Nâs 6 (Amme Cüzü)"
            else -> "$juzNo. Cüz"
        }
    )
}

class JuzTrackerActivity : ComponentActivity() {

    private val prefs by lazy {
        getSharedPreferences("hatim_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kaydedilmiş tamamlanan cüzleri yükle
        val savedSet = prefs.getStringSet("completed_juzs", emptySet()) ?: emptySet()
        val initialCompleted = savedSet.mapNotNull { it.toIntOrNull() }.toSet()

        setContent {
            JuzTrackerScreen(
                initialCompleted = initialCompleted,
                onSaveCompleted = { updatedSet ->
                    prefs.edit()
                        .putStringSet("completed_juzs", updatedSet.map { it.toString() }.toSet())
                        .apply()
                },
                onBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuzTrackerScreen(
    initialCompleted: Set<Int>,
    onSaveCompleted: (Set<Int>) -> Unit,
    onBack: () -> Unit
) {
    var completedJuzs by remember { mutableStateOf(initialCompleted) }
    var viewMode by remember { mutableIntStateOf(0) } // 0: 30'luk Matris (Grid), 1: Detaylı Liste
    var showResetDialog by remember { mutableStateOf(false) }
    var showDuaDialog by remember { mutableStateOf(false) }

    val completedCount = completedJuzs.size
    val progress = (completedCount / 30f).coerceIn(0f, 1f)
    val percentage = (progress * 100).toInt()

    fun toggleJuz(juzNumber: Int) {
        val next = if (completedJuzs.contains(juzNumber)) {
            completedJuzs - juzNumber
        } else {
            completedJuzs + juzNumber
        }
        completedJuzs = next
        onSaveCompleted(next)
    }

    fun resetAll() {
        completedJuzs = emptySet()
        onSaveCompleted(emptySet())
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
                            text = "Hatim & Cüz Takibi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Tamamlanan: $completedCount / 30 Cüz (%$percentage)",
                            fontSize = 11.sp,
                            color = Color(0xFFC8E6C9)
                        )
                    }
                },
                actions = {
                    // Yeni Hatme Başla / Sıfırla Butonu
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sıfırla",
                            tint = Color.White
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
                            Color(0xFF0F2B1D),
                            Color(0xFF091710)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            // 1. HATİM DURUM KARTI & İLERLEME ÇUBUĞU
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "HATM-İ ŞERİF DURUMU",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81C784)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "%$percentage Tamamlandı",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (completedCount == 30) Color(0xFF00E676) else Color(0xFFFFD54F)
                            )
                        }

                        // Rozet (Kalan Cüz)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (completedCount == 30) Color(0xFF00E676).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = if (completedCount == 30) "Hatim Bitti 🎉" else "Kalan: ${30 - completedCount} Cüz",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (completedCount == 30) Color(0xFF00E676) else Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // İlerleme Çubuğu
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFFFD54F),
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )

                    // Hatim Tamamlandıysa Hatim Duası Butonu
                    if (completedCount == 30) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showDuaDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("🤲 Hatim Duasını Oku", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. GÖRÜNÜM SEÇİCİ (Matris vs Liste Sekmesi)
            TabRow(
                selectedTabIndex = viewMode,
                containerColor = Color.White.copy(alpha = 0.05f),
                contentColor = Color(0xFFFFD54F),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = viewMode == 0,
                    onClick = { viewMode = 0 },
                    text = { Text("30'luk Cüz Matrisi", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = viewMode == 1,
                    onClick = { viewMode = 1 },
                    text = { Text("Detaylı Liste", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. İÇERİK: MATRİS (GRID) VEYA LİSTE
            if (viewMode == 0) {
                // 30'luk Cüz Matrisi (5 sütun x 6 satır - Tek bakışta her şey görünür)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(TRACKER_JUZ_LIST, key = { it.number }) { juz ->
                        val isDone = completedJuzs.contains(juz.number)

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDone) Color(0xFF1B5E20) else Color.White.copy(alpha = 0.06f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isDone) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { toggleJuz(juz.number) }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "${juz.number}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDone) Color(0xFFFFD54F) else Color.White
                                    )
                                    Text(
                                        text = if (isDone) "Okundu" else "Cüz",
                                        fontSize = 10.sp,
                                        color = if (isDone) Color.White else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Detaylı Liste Görünümü
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TRACKER_JUZ_LIST, key = { it.number }) { juz ->
                        val isDone = completedJuzs.contains(juz.number)

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDone) Color(0xFF143322) else Color.White.copy(alpha = 0.05f),
                            border = if (isDone) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggleJuz(juz.number) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Dairesel Onay Kutucuğu
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isDone) Color(0xFF00E676) else Color.White.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF0F3E2C),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = "${juz.number}. Cüz ${if (isDone) "✓" else ""}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isDone) Color(0xFFFFD54F) else Color.White
                                        )
                                        Text(
                                            text = juz.startEndInfo,
                                            fontSize = 11.sp,
                                            color = Color(0xFFB0BEC5)
                                        )
                                    }
                                }

                                Text(
                                    text = if (isDone) "Tamamlandı" else "Okunmadı",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDone) Color(0xFF81C784) else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- SIFIRLA / YENİ HATME BAŞLA DİYALOĞU ---
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Yeni Hatme Başla", fontWeight = FontWeight.Bold) },
            text = { Text("Mevcut hatim kaydınız sıfırlanıp tüm cüzler okunmadı olarak işaretlenecektir. Emin misiniz?") },
            confirmButton = {
                TextButton(onClick = {
                    resetAll()
                    showResetDialog = false
                }) {
                    Text("Evet, Sıfırla", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // --- HATİM DUASI DİYALOĞU ---
    if (showDuaDialog) {
        AlertDialog(
            onDismissRequest = { showDuaDialog = false },
            title = { Text("🤲 Hatim Duası", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Elhamdülillâhi Rabbi'l-âlemîn. Ve's-salâtü ve's-selâmü alâ Seyyidinâ Muhammedin ve alâ âlihî ve sahbihî ecmaîn.\n\n" +
                                "Ey Rabbimiz! Okuduğumuz Kur'ân-ı Kerîm'i dergâh-ı izzetinde kabul eyle. Hasıl olan sevabı başta Sevgili Peygamberimiz Hazret-i Muhammed Mustafa'nın (s.a.v.) mübarek ruhuna, âline, ashabına, geçmişlerimizin ve cümle müminlerin ruhlarına hediye eyledik, vasıl eyle. Kalplerimizi Kur'an nuruyla aydınlat, bizleri doğru yoldan ayırma. Âmin.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF1E293B)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDuaDialog = false }) {
                    Text("Âmin", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}