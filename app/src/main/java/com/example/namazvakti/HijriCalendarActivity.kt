package com.example.namazvakti

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Dini Gün Modeli
data class HolyDay(
    val title: String,
    val gregorianDate: String, // "10 Aralık 2026"
    val hijriDate: String,     // "1 Receb 1448"
    val isoDate: String,       // "2026-12-10" (hesaplama için)
    val category: String,      // "Kandil", "Ramazan", "Bayram", "Üç Aylar"
    val description: String    // Kısa fazileti
)

val HOLY_DAYS_LIST = listOf(
    HolyDay(
        title = "Üç Ayların Başlangıcı",
        gregorianDate = "10 Aralık 2026, Perşembe",
        hijriDate = "1 Receb 1448",
        isoDate = "2026-12-10",
        category = "Üç Aylar",
        description = "Manevi hasat mevsimi olan Receb, Şaban ve Ramazan aylarının başlangıcıdır."
    ),
    HolyDay(
        title = "Regaip Kandili",
        gregorianDate = "13 Aralık 2026, Pazar",
        hijriDate = "4 Receb 1448",
        isoDate = "2026-12-13",
        category = "Kandil",
        description = "Allah'ın mağfiret ve lütuflarının bolca ihsan edildiği, rağbet gecesidir."
    ),
    HolyDay(
        title = "Miraç Kandili",
        gregorianDate = "4 Ocak 2027, Pazartesi",
        hijriDate = "26 Receb 1448",
        isoDate = "2027-01-04",
        category = "Kandil",
        description = "Peygamber Efendimiz'in (s.a.v.) semaya yükselip 5 vakit namaz müjdesiyle döndüğü gece."
    ),
    HolyDay(
        title = "Berat Kandili",
        gregorianDate = "22 Ocak 2027, Cuma",
        hijriDate = "14 Şaban 1448",
        isoDate = "2027-01-22",
        category = "Kandil",
        description = "Günahlardan arınma, af ve beraat gecesi; amellerin Allah'a arz edildiği mübarek gece."
    ),
    HolyDay(
        title = "Ramazan-ı Şerif Başlangıcı",
        gregorianDate = "8 Şubat 2027, Pazartesi",
        hijriDate = "1 Ramazan 1448",
        isoDate = "2027-02-08",
        category = "Ramazan",
        description = "On bir ayın sultanı, Kur'an ayı ve farz olan oruç ibadetinin ilk günü."
    ),
    HolyDay(
        title = "Kadir Gecesi",
        gregorianDate = "5 Mart 2027, Cuma",
        hijriDate = "26 Ramazan 1448",
        isoDate = "2027-03-05",
        category = "Ramazan",
        description = "Bin aydan daha hayırlı olan, Kur'an-ı Kerim'in Levh-i Mahfuz'dan indirildiği mukaddes gece."
    ),
    HolyDay(
        title = "Ramazan Bayramı (1. Gün)",
        gregorianDate = "10 Mart 2027, Çarşamba",
        hijriDate = "1 Şevval 1448",
        isoDate = "2027-03-10",
        category = "Bayram",
        description = "Ramazan mektebini tamamlayan müminlerin sevinç ve kardeşlik bayramı."
    ),
    HolyDay(
        title = "Kurban Bayramı (1. Gün)",
        gregorianDate = "17 Mayıs 2027, Pazartesi",
        hijriDate = "10 Zilhicce 1448",
        isoDate = "2027-05-17",
        category = "Bayram",
        description = "Hz. İbrahim ve Hz. İsmail'in teslimiyetini yad ettiğimiz kurban ve hac bayramı."
    ),
    HolyDay(
        title = "Hicri Yılbaşı (1449)",
        gregorianDate = "6 Haziran 2027, Pazar",
        hijriDate = "1 Muharrem 1449",
        isoDate = "2027-06-06",
        category = "Özel Gün",
        description = "Kutlu hicretin başlangıcı kabul edilen yeni Hicri senenin ilk günü."
    ),
    HolyDay(
        title = "Aşure Günü",
        gregorianDate = "15 Haziran 2027, Salı",
        hijriDate = "10 Muharrem 1449",
        isoDate = "2027-06-15",
        category = "Özel Gün",
        description = "Pek çok peygamberin kurtuluşa erdiği, bereket ve paylaşma günü."
    ),
    HolyDay(
        title = "Mevlid Kandili",
        gregorianDate = "14 Ağustos 2027, Cumartesi",
        hijriDate = "12 Rebiülevvel 1449",
        isoDate = "2027-08-14",
        category = "Kandil",
        description = "Âlemlere rahmet olarak gönderilen Efendimiz Muhammed Mustafa'nın (s.a.v.) veladet gecesi."
    )
)

class HijriCalendarActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HijriCalendarScreen(
                onBack = { finish() },
                onSetReminder = { holyDay ->
                    Toast.makeText(this, "${holyDay.title} için hatırlatıcı kuruldu.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijriCalendarScreen(
    onBack: () -> Unit,
    onSetReminder: (HolyDay) -> Unit
) {
    var selectedCategoryFilter by remember { mutableStateOf("Tümü") }
    val categories = listOf("Tümü", "Kandil", "Ramazan", "Bayram")

    // En yakın mübarek günü hesaplama
    val nextUpcomingDay = remember {
        val today = LocalDate.now()
        HOLY_DAYS_LIST.firstOrNull {
            try {
                val targetDate = LocalDate.parse(it.isoDate)
                !targetDate.isBefore(today)
            } catch (_: Exception) {
                false
            }
        } ?: HOLY_DAYS_LIST[0]
    }

    val daysLeft = remember(nextUpcomingDay) {
        try {
            val today = LocalDate.now()
            val target = LocalDate.parse(nextUpcomingDay.isoDate)
            ChronoUnit.DAYS.between(today, target).coerceAtLeast(0)
        } catch (_: Exception) {
            4L
        }
    }

    val filteredList = remember(selectedCategoryFilter) {
        if (selectedCategoryFilter == "Tümü") {
            HOLY_DAYS_LIST
        } else {
            HOLY_DAYS_LIST.filter { it.category == selectedCategoryFilter }
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
                            text = "Dini Günler ve Geceler",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Hicri 1448 - 1449 Takvimi",
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
            // 1. EN YAKIN MÜBAREK GÜN KARTI (GERİ SAYIM HERO BANNER)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD54F).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🌙", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "EN YAKIN MÜBAREK VAKİT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF81C784)
                                )
                                Text(
                                    text = nextUpcomingDay.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFD54F)
                                )
                            }
                        }

                        // Geri Sayım Rozeti
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFD54F).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F))
                        ) {
                            Text(
                                text = "$daysLeft Gün Kaldı",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${nextUpcomingDay.gregorianDate}  •  ${nextUpcomingDay.hijriDate}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = nextUpcomingDay.description,
                        fontSize = 11.sp,
                        color = Color(0xFFCFD8DC),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. KATEGORİ FİLTRELEME CHIPS (Tümü, Kandiller, Ramazan, Bayramlar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = (selectedCategoryFilter == cat)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFF1B5E20) else Color.White.copy(alpha = 0.08f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedCategoryFilter = cat }
                    ) {
                        Text(
                            text = cat,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (isSelected) Color.White else Color(0xFFB0BEC5),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. DİNİ GÜNLER LİSTESİ
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { item ->
                    val badgeColor = when (item.category) {
                        "Kandil" -> Color(0xFFFFB300)
                        "Ramazan" -> Color(0xFF00E676)
                        "Bayram" -> Color(0xFF42A5F5)
                        else -> Color(0xFFAB47BC)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Kategori Simgesi
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (item.category) {
                                                "Kandil" -> "🏮"
                                                "Ramazan" -> "🌙"
                                                "Bayram" -> "🎉"
                                                else -> "✨"
                                            },
                                            fontSize = 14.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = item.hijriDate,
                                            fontSize = 11.sp,
                                            color = Color(0xFF81C784),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Hatırlatıcı / Bildirim Butonu
                                IconButton(
                                    onClick = { onSetReminder(item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Hatırlatıcı Kur",
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Miladi Tarih
                            Text(
                                text = "📅 ${item.gregorianDate}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFE082)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Fazilet ve Manevi Not
                            Text(
                                text = item.description,
                                fontSize = 11.sp,
                                color = Color(0xFFCFD8DC),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}