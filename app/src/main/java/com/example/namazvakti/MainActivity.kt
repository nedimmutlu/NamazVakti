package com.example.namazvakti

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ezanvakti.app.location.Coordinates
import com.ezanvakti.app.location.LocationHelper
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// --- ÖZEL İSLAMİ RENK PALETİ ---
val IslamicDarkBg = Color(0xFF09120E)
val IslamicCardSurface = Color(0xFF132019)
val IslamicGreenPrimary = Color(0xFF10B981)
val IslamicGreenDeep = Color(0xFF064E3B)
val IslamicGold = Color(0xFFD4AF37)
val IslamicGoldLight = Color(0xFFFDE68A)
val TextMuted = Color(0xFF94A3B8)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- GÜNCELLEME SONRASI ESKİ KONUM VE İZİN ÖNBELLEĞİNİ SIFIRLAMA (MIGRATION) ---
        val prefs = getSharedPreferences("namaz", Context.MODE_PRIVATE)
        val lastSavedVersion = prefs.getInt("app_version_code", 0)
        val currentVersion = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L)).versionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionCode
            }
        } catch (_: Exception) {
            1
        }

        if (currentVersion > lastSavedVersion) {
            // Yeni güncelleme yapıldı! Eski cache konumları temizle ve izin diyaloglarının yeniden çıkmasını sağla
            prefs.edit()
                .remove("city")
                .remove("last_city")
                .remove("lat")
                .remove("lng")
                .remove("last_lat")
                .remove("last_lng")
                .putInt("app_version_code", currentVersion)
                .apply()
        }
        // ---------------------------------------------------------------------

        // Arka planda vakitleri çek ve alarmları kur
        PrayerScheduler.refreshInBackground(this)

        setContent {
            MaterialTheme {
                MainDashboardScreen(
                    onOpenQuran = { startActivity(Intent(this, QuranActivity::class.java)) },
                    onOpenCompass = { startActivity(Intent(this, CompassActivity::class.java)) },
                    onOpenZikir = { startActivity(Intent(this, ZikirmatikActivity::class.java)) },
                    onOpenHatim = { startActivity(Intent(this, JuzTrackerActivity::class.java)) },
                    onOpenEsma = { startActivity(Intent(this, EsmaulHusnaActivity::class.java)) },
                    onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    onOpenCalendar = { startActivity(Intent(this, HijriCalendarActivity::class.java)) },
                    onOpenMessages = { startActivity(Intent(this, MessagesActivity::class.java)) }
                )
            }
        }
    }
}

@Composable
fun MainDashboardScreen(
    onOpenQuran: () -> Unit,
    onOpenCompass: () -> Unit,
    onOpenZikir: () -> Unit,
    onOpenHatim: () -> Unit,
    onOpenEsma: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenMessages: () -> Unit
) {
    val context = LocalContext.current
    var cityName by remember { mutableStateOf("Yükleniyor...") }
    var todayTimes by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var nextPrayerName by remember { mutableStateOf("Yükleniyor...") }
    var nextPrayerTimeStr by remember { mutableStateOf("") }
    var countdownStr by remember { mutableStateOf("00:00:00") }
    var kerahatMsg by remember { mutableStateOf<String?>(null) }
    var hijriDateStr by remember { mutableStateOf("29 Rebiülahir 1448") }

    // --- KONUM ALMA (LocationHelper ile gerçek GPS) ---
    val locationHelper = remember { LocationHelper(context) }

    LaunchedEffect(Unit) {
        locationHelper.fetchCurrentLocation { coords ->
            // Konum alınınca PrayerScheduler ile vakitleri arka planda yenile
            PrayerScheduler.refreshInBackground(context)
        }
    }

    // --- OTOMATİK İZİN KONTROLÜ VEYA YÖNLENDİRMESİ ---
    CheckAndRequestPermissions()

    // Canlı 1 saniyelik geri sayım ve vakit hesaplama döngüsü
    LaunchedEffect(Unit) {
        while (true) {
            val prefs = context.getSharedPreferences("namaz", Context.MODE_PRIVATE)
            cityName = prefs.getString("city", prefs.getString("last_city", "Yükleniyor...")) ?: "Yükleniyor..."
            val cache = prefs.getString("cache", "") ?: ""
            val now = System.currentTimeMillis()

            if (cache.isNotEmpty()) {
                val timeZone = TimeZone.getTimeZone("GMT+3")
                val calNow = Calendar.getInstance(timeZone)
                val curDay = calNow.get(Calendar.DAY_OF_YEAR)
                val curYear = calNow.get(Calendar.YEAR)

                val sdfClock = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                    this.timeZone = timeZone
                }

                val all = cache.split(";").filter { it.contains("|") }.mapNotNull {
                    val p = it.split("|")
                    val t = p[0].toLongOrNull()
                    if (t != null) t to p[1] else null
                }

                // Bugünün 6 vakti
                val todays = all.filter {
                    val c = Calendar.getInstance(timeZone).apply { timeInMillis = it.first }
                    c.get(Calendar.DAY_OF_YEAR) == curDay && c.get(Calendar.YEAR) == curYear
                }.map { it.second to sdfClock.format(Date(it.first)) }
                todayTimes = todays

                // Sıradaki vakit
                val future = all.filter { it.first > now }
                if (future.isNotEmpty()) {
                    val next = future.first()
                    nextPrayerName = next.second
                    nextPrayerTimeStr = sdfClock.format(Date(next.first))

                    val diff = next.first - now
                    val h = (diff / (1000 * 60 * 60)) % 24
                    val m = (diff / (1000 * 60)) % 60
                    val s = (diff / 1000) % 60
                    countdownStr = String.format("%02d:%02d:%02d", h, m, s)
                }

                // Kerahat kontrolü (45 dk kuralı)
                val sunrise = all.firstOrNull { it.second == "Güneş" && Math.abs(it.first - now) < 24 * 3600000 }?.first
                val dhuhr = all.firstOrNull { it.second == "Öğle" && Math.abs(it.first - now) < 24 * 3600000 }?.first
                val maghrib = all.firstOrNull { it.second == "Akşam" && Math.abs(it.first - now) < 24 * 3600000 }?.first
                val margin = 45 * 60 * 1000L

                kerahatMsg = when {
                    sunrise != null && now in sunrise..(sunrise + margin) -> "⚠️ Sabah Kerahati (Güneş Doğuşu)"
                    dhuhr != null && now in (dhuhr - margin)..dhuhr -> "⚠️ İstiva Kerahati (Öğle Öncesi)"
                    maghrib != null && now in (maghrib - margin)..maghrib -> "⚠️ Akşam Kerahati (Güneş Batışı)"
                    else -> null
                }
            }
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IslamicDarkBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. ÜST BAŞLIK & KONUM ALANI ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = IslamicGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = cityName,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "$hijriDateStr · ${SimpleDateFormat("d MMMM yyyy", Locale("tr")).format(Date())}",
                            color = IslamicGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .background(IslamicCardSurface, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ayarlar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // --- 2. KERAHAT UYARI BİLGİSİ ---
            if (kerahatMsg != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = kerahatMsg ?: "",
                                color = Color(0xFFF59E0B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // --- 3. BÜYÜK ZÜMRÜT GERİ SAYIM KARTI (HERO BANNER) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF064E3B),
                                        Color(0xFF0F5132),
                                        Color(0xFF022C22)
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "SIRADAKİ VAKTE KALAN SÜRE",
                                color = IslamicGoldLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "$nextPrayerName $nextPrayerTimeStr",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = countdownStr,
                                color = Color.White,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Diyanet İşleri Başkanlığı Uyumlu",
                                color = Color(0xFFA7F3D0),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // --- 4. GÜNLÜK 6 VAKİT LİSTESİ ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = IslamicCardSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Günün Namaz Vakitleri",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val prayers = listOf("İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (p in prayers) {
                                val time = todayTimes.firstOrNull { it.first == p }?.second ?: "--:--"
                                val isNext = (p == nextPrayerName)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isNext) Color(0xFF0F766E) else Color(0xFF0F172A))
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = p,
                                        color = if (isNext) Color.White else Color(0xFFCBD5E1),
                                        fontSize = 14.sp,
                                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = time,
                                        color = if (isNext) IslamicGoldLight else IslamicGreenPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 5. İSLAMİ ARAÇLAR HIZLI ERİŞİM BUTONLARI ---
            item {
                Text(
                    text = "İslami Araçlar",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 1. Satır
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolButton(
                        modifier = Modifier.weight(1f),
                        icon = "📖",
                        title = "Kur'an",
                        subtitle = "Meal & Sesli",
                        onClick = onOpenQuran
                    )
                    ToolButton(
                        modifier = Modifier.weight(1f),
                        icon = "🧭",
                        title = "Kıble",
                        subtitle = "Pusula",
                        onClick = onOpenCompass
                    )
                    ToolButton(
                        modifier = Modifier.weight(1f),
                        icon = "📿",
                        title = "Zikir",
                        subtitle = "Sayaç",
                        onClick = onOpenZikir
                    )
                    ToolButton(
                        modifier = Modifier.weight(1f),
                        icon = "📜",
                        title = "Hatim",
                        subtitle = "Cüz Takip",
                        onClick = onOpenHatim
                    )
                }
            }

            // 2. Satır
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolButton(
                        modifier = Modifier.weight(1f),
                        icon = "✨",
                        title = "Esmâ",
                        subtitle = "99 İsim",
                        onClick = onOpenEsma
                    )
                    ToolButton(
                        modifier = Modifier.weight(1f),
                        icon = "🌙",
                        title = "Kandiller",
                        subtitle = "Dini Günler",
                        onClick = onOpenCalendar
                    )
                    ToolButton(
                        modifier = Modifier.weight(1f),
                        icon = "💬",
                        title = "Mesajlar",
                        subtitle = "Tebrik",
                        onClick = onOpenMessages
                    )
                }
            }
        }
    }
}

@Composable
fun ToolButton(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = IslamicCardSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// --- OTOMATİK İZİN KONTROLÜ VE UYARI DİYALOĞU ---
@Composable
fun CheckAndRequestPermissions() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessageType by remember { mutableStateOf("location") }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (!fineGranted && !coarseGranted) {
            showDialog = true
            dialogMessageType = "location_settings"
        } else {
            // Konum izni verildikten hemen sonra bildirim iznini kontrol et (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notifGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!notifGranted) {
                    dialogMessageType = "notification"
                    showDialog = true
                }
            }
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Bildirim izni reddedildiyse ayarlara yönlendirme opsiyonu sunabiliriz veya alarm iznine geçebiliriz
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Bildirim sonrasında tam alarm izni kontrolü
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                dialogMessageType = "alarm"
                showDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        // 0. Konum İzni Kontrolü (Her ilk yüklemede veya güncellemede öncelikli ve zorunlu)
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted && !coarseLocationGranted) {
            dialogMessageType = "location"
            showDialog = true
            return@LaunchedEffect
        }

        // 1. Android 13+ Bildirim İzni Kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                dialogMessageType = "notification"
                showDialog = true
                return@LaunchedEffect
            }
        }

        // 2. Android 12+ Tam Zamanlı Alarm İzni Kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                dialogMessageType = "alarm"
                showDialog = true
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { /* Yaşlı kullanıcıların yanlışlıkla kapatmasını önlemek için dışarı tıklama ile kapanmasın */ },
            title = {
                Text(
                    text = when (dialogMessageType) {
                        "location", "location_settings" -> "📍 Konum İzni Gerekli"
                        "notification" -> "🔔 Bildirim İzni"
                        else -> "⏰ Tam Alarm İzni"
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            },
            text = {
                Text(
                    text = when (dialogMessageType) {
                        "location" -> "Namaz vakitlerinin ve kıble yönünün bulunduğunuz şehre göre tam hesaplanabilmesi için konum iznine ihtiyaç vardır.\n\nLütfen açılan pencerede 'İzin Ver' seçeneğine dokunarak konum erişimini açınız."
                        "location_settings" -> "Konum izni verilmediği için namaz vakitleri hesaplanamıyor.\n\nLütfen Ayarlar ekranından Konum iznini 'Uygulamayı kullanırken izin ver' olarak seçiniz."
                        "notification" -> "Ezan vakitlerinde bildirim ve ezan sesi alabilmeniz için bildirim izni vermeniz gerekmektedir."
                        else -> "Ezan alarmlarının vaktinde eksiksiz çalabilmesi için tam zamanlı alarm iznine ihtiyaç duyulmaktadır."
                    },
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        when (dialogMessageType) {
                            "location" -> {
                                multiplePermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            "location_settings" -> {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                            "notification" -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            "alarm" -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("İzin Ver / Ayarlara Git", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    // "Daha Sonra" denildiğinde sıradaki izne (Bildirim veya Alarm) geçelim
                    when (dialogMessageType) {
                        "location", "location_settings" -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val notifGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!notifGranted) {
                                    dialogMessageType = "notification"
                                    showDialog = true
                                    return@TextButton
                                }
                            }
                            // Bildirim izni varsa veya Android 13 altıysa alarm iznine geç
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                if (!alarmManager.canScheduleExactAlarms()) {
                                    dialogMessageType = "alarm"
                                    showDialog = true
                                }
                            }
                        }
                        "notification" -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                if (!alarmManager.canScheduleExactAlarms()) {
                                    dialogMessageType = "alarm"
                                    showDialog = true
                                }
                            }
                        }
                    }
                }) {
                    Text("Daha Sonra", color = Color.Gray)
                }
            }
        )
    }
}