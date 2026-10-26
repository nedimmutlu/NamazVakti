package com.example.namazvakti

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Hazır Zikir Modeli
data class DhikrPreset(
    val title: String,
    val arabic: String,
    val meaning: String,
    val defaultTarget: Int
)

val DHIKR_PRESETS = listOf(
    DhikrPreset("Sübhanallah", "سُبْحَانَ اللَّهِ", "Allah noksan sıfatlardan münezzehtir", 33),
    DhikrPreset("Elhamdülillah", "الْحَمْدُ لِلَّهِ", "Hamd ve övgü yalnızca Allah'adır", 33),
    DhikrPreset("Allahu Ekber", "اللَّهُ أَكْبَرُ", "Allah en büyüktür", 33),
    DhikrPreset("Lâ ilâhe illallah", "لَا إِلٰهَ إِلَّا اللَّهُ", "Allah'tan başka ilah yoktur", 100),
    DhikrPreset("Salavat-ı Şerife", "اللَّهُمَّ صَلِّ عَلَى سَيِّدِنَا مُحَمَّدٍ", "Allah'ım Efendimiz Muhammed'e salat eyle", 100),
    DhikrPreset("Estağfirullah", "أَسْتَغْفِرُ اللَّهَ", "Allah'tan bağışlanma dilerim", 100),
    DhikrPreset("Serbest Zikir", "ذِكْرٌ", "Gönlünüzden geçen dualar ve zikirler", 99)
)

class ZikirmatikActivity : ComponentActivity() {

    private fun triggerVibration(isGoalReached: Boolean = false) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (isGoalReached) {
                // Hedefe ulaşıldığında kuvvetli çift titreşim
                val timings = longArrayOf(0, 150, 100, 250)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(400)
                }
            } else {
                // Her dokunuşta hafif ve tatmin edici tıklama titreşimi
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(30)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZikirmatikScreen(
                onBack = { finish() },
                onVibrate = { isGoal -> triggerVibration(isGoal) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZikirmatikScreen(
    onBack: () -> Unit,
    onVibrate: (Boolean) -> Unit
) {
    var selectedPreset by remember { mutableStateOf(DHIKR_PRESETS[0]) }
    var count by remember { mutableIntStateOf(0) }
    var target by remember { mutableIntStateOf(selectedPreset.defaultTarget) }
    var roundCount by remember { mutableIntStateOf(0) } // Devir / Tur
    var totalCount by remember { mutableIntStateOf(0) } // Toplam çekilen
    var niyeNote by remember { mutableStateOf("Dualarımın ve niyetlerimin kabulü için") }
    var isVibrationEnabled by remember { mutableStateOf(true) }

    // Dialog durumları
    var showNiyetDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }

    // Dokunma basma animasyonu
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "btnScale"
    )

    // Sayacı artırma fonksiyonu
    val incrementCount = {
        val nextCount = count + 1
        totalCount += 1

        if (isVibrationEnabled) {
            val reachedGoal = (nextCount >= target && target > 0)
            onVibrate(reachedGoal)
        }

        if (target > 0 && nextCount >= target) {
            roundCount += 1
            count = 0 // Yeni tura başla
        } else {
            count = nextCount
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
                            text = "Akıllı Zikirmatik",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Toplam Çekilen: $totalCount",
                            fontSize = 11.sp,
                            color = Color(0xFFC8E6C9)
                        )
                    }
                },
                actions = {
                    // Titreşim Açık / Kapalı
                    IconButton(onClick = { isVibrationEnabled = !isVibrationEnabled }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Titreşim",
                            tint = if (isVibrationEnabled) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.5f)
                        )
                    }
                    // Sıfırla
                    IconButton(onClick = {
                        count = 0
                        roundCount = 0
                    }) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. ZİKİR SEÇİCİ YATAY LİSTE (CHIPS)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(DHIKR_PRESETS) { preset ->
                    val isSelected = preset.title == selectedPreset.title
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF1B5E20) else Color.White.copy(alpha = 0.08f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F)) else null,
                        modifier = Modifier.clickable {
                            selectedPreset = preset
                            target = preset.defaultTarget
                            count = 0
                        }
                    ) {
                        Text(
                            text = "${preset.title} (${preset.defaultTarget})",
                            color = if (isSelected) Color.White else Color(0xFFB0BEC5),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. NİYET / DUA KARTI
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.06f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNiyetDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📌 NİYET & DUA NOTUM (Düzenlemek için dokunun)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF81C784)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "\"$niyeNote\"",
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Düzenle",
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. SEÇİLİ ZİKRİN ARAPÇASI VE ANLAMI
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selectedPreset.arabic,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectedPreset.meaning,
                    fontSize = 12.sp,
                    color = Color(0xFFCFD8DC),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. HEDEF VE DEVİR BİLGİ ŞERİDİ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1B5E20).copy(alpha = 0.3f),
                    modifier = Modifier.clickable { showTargetDialog = true }
                ) {
                    Text(
                        text = "Hedef: $target (Değiştir)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF81C784),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF8F00).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Devir: $roundCount Tur",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB300),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 5. ANA ZİKİRMATİK BUTONU (Dokunsal & Animasyonlu Büyük Halka)
            val progress = if (target > 0) (count.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(260.dp)
                    .scale(buttonScale)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        incrementCount()
                    }
            ) {
                // Dış Altın/Yeşil İlerleme Çemberi
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFFD54F),
                    trackColor = Color.White.copy(alpha = 0.1f),
                    strokeWidth = 8.dp
                )

                // İç Gövde (Metalik/Zümrüt Derinlik Efekti)
                Box(
                    modifier = Modifier
                        .size(236.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1E4633),
                                    Color(0xFF0A2216)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = Color(0xFFFFD54F).copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$count",
                            fontSize = 72.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "ÇEKMEK İÇİN DOKUNUN",
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF81C784)
                        )
                        if (target > 0) {
                            Text(
                                text = "Kalan: ${target - count}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 6. ALT KONTROL PANELİ (Sıfırla & Hedef Ayarla)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        count = 0
                        roundCount = 0
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC62828)
                    )
                ) {
                    Text("Sıfırla", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = { showTargetDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B5E20)
                    )
                ) {
                    Text("Hedef Belirle", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }

    // --- NİYET DEĞİŞTİRME DİYALOĞU ---
    if (showNiyetDialog) {
        var tempNiyet by remember { mutableStateOf(niyeNote) }
        AlertDialog(
            onDismissRequest = { showNiyetDialog = false },
            title = { Text("Niyet / Dua Notunuz") },
            text = {
                OutlinedTextField(
                    value = tempNiyet,
                    onValueChange = { tempNiyet = it },
                    label = { Text("Niyetinizi yazın") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    niyeNote = tempNiyet
                    showNiyetDialog = false
                }) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNiyetDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // --- HEDEF SAYI DEĞİŞTİRME DİYALOĞU ---
    if (showTargetDialog) {
        var tempTarget by remember { mutableStateOf(target.toString()) }
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text("Hedef Sayı Belirle") },
            text = {
                OutlinedTextField(
                    value = tempTarget,
                    onValueChange = { tempTarget = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Hedef sayı (Örn: 33, 99, 100, 500)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newT = tempTarget.toIntOrNull() ?: 33
                    target = if (newT > 0) newT else 33
                    count = 0
                    showTargetDialog = false
                }) {
                    Text("Uygula")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}