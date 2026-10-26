package com.example.namazvakti

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PrayerReminderConfig(
    val title: String,
    val isEnabled: Boolean,
    val selectedMinutes: Int,
    val onToggle: (Boolean) -> Unit,
    val onMinutesChange: (Int) -> Unit
)

class SettingsActivity : ComponentActivity() {

    private val prefs by lazy {
        getSharedPreferences("alarm_settings_prefs", Context.MODE_PRIVATE)
    }

    private var testMediaPlayer: MediaPlayer? = null

    private fun playTestSound(uriString: String?, onFinished: () -> Unit) {
        stopTestSound()
        try {
            val soundUri = if (!uriString.isNullOrEmpty()) {
                Uri.parse(uriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            testMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                setDataSource(this@SettingsActivity, soundUri)
                prepare()
                start()
                setOnCompletionListener {
                    stopTestSound()
                    onFinished()
                }
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Ses çalınamadı veya dosya izni gerekiyor.", Toast.LENGTH_SHORT).show()
            onFinished()
        }
    }

    private fun stopTestSound() {
        testMediaPlayer?.stop()
        testMediaPlayer?.release()
        testMediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTestSound()
        // Sayfadan çıkıldığında en güncel alarmları planla
        PrayerScheduler.scheduleFromCache(this)
    }

    fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) result = it.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result ?: "Özel Ses Dosyası"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SettingsScreen(
                prefs = prefs,
                onBack = {
                    PrayerScheduler.scheduleFromCache(this)
                    finish()
                },
                onPlayTest = { uri, onEnd -> playTestSound(uri, onEnd) },
                onStopTest = { stopTestSound() },
                getFileName = { uri -> getFileName(uri) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: android.content.SharedPreferences,
    onBack: () -> Unit,
    onPlayTest: (String?, () -> Unit) -> Unit,
    onStopTest: () -> Unit,
    getFileName: (Uri) -> String
) {
    val context = LocalContext.current

    var selectedSoundName by remember {
        mutableStateOf(prefs.getString("alarm_sound_name", "Varsayılan Ezan / Bildirim Sesi") ?: "Varsayılan Ezan / Bildirim Sesi")
    }
    var selectedSoundUri by remember {
        mutableStateOf(prefs.getString("alarm_sound_uri", null))
    }
    var isTestingSound by remember { mutableStateOf(false) }

    var ezanNotifyEnabled by remember {
        mutableStateOf(prefs.getBoolean("ezan_notify_enabled", true))
    }
    var kerahatNotifyEnabled by remember {
        mutableStateOf(prefs.getBoolean("kerahat_notify_enabled", true))
    }
    var kerahatMinutes by remember {
        mutableIntStateOf(prefs.getInt("kerahat_minutes", 20))
    }

    var imsakMinutes by remember { mutableIntStateOf(prefs.getInt("imsak_pre", 15)) }
    var imsakEnabled by remember { mutableStateOf(prefs.getBoolean("imsak_pre_enabled", true)) }

    var gunesMinutes by remember { mutableIntStateOf(prefs.getInt("gunes_pre", 15)) }
    var gunesEnabled by remember { mutableStateOf(prefs.getBoolean("gunes_pre_enabled", true)) }

    var ogleMinutes by remember { mutableIntStateOf(prefs.getInt("ogle_pre", 15)) }
    var ogleEnabled by remember { mutableStateOf(prefs.getBoolean("ogle_pre_enabled", true)) }

    var ikindiMinutes by remember { mutableIntStateOf(prefs.getInt("ikindi_pre", 15)) }
    var ikindiEnabled by remember { mutableStateOf(prefs.getBoolean("ikindi_pre_enabled", true)) }

    var aksamMinutes by remember { mutableIntStateOf(prefs.getInt("aksam_pre", 15)) }
    var aksamEnabled by remember { mutableStateOf(prefs.getBoolean("aksam_pre_enabled", true)) }

    var yatsiMinutes by remember { mutableIntStateOf(prefs.getInt("yatsi_pre", 15)) }
    var yatsiEnabled by remember { mutableStateOf(prefs.getBoolean("yatsi_pre_enabled", true)) }

    fun applyAndReschedule() {
        PrayerScheduler.scheduleFromCache(context)
    }

    val mp3PickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(it)
            selectedSoundName = fileName
            selectedSoundUri = it.toString()
            prefs.edit()
                .putString("alarm_sound_name", fileName)
                .putString("alarm_sound_uri", it.toString())
                .apply()
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }

            if (uri != null) {
                val ringtoneName = try {
                    RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Sistem Alarmı"
                } catch (_: Exception) {
                    "Sistem Alarmı"
                }

                selectedSoundName = ringtoneName
                selectedSoundUri = uri.toString()
                prefs.edit()
                    .putString("alarm_sound_name", ringtoneName)
                    .putString("alarm_sound_uri", uri.toString())
                    .apply()
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
                    Text(
                        text = "Bildirim ve Alarm Ayarları",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F3E2C)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. ÖZEL ALARM / EZAN SESİ PANELİ
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD54F).copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🔊 EZAN & ALARM SESİ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD54F)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = selectedSoundName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (isTestingSound) {
                                        onStopTest()
                                        isTestingSound = false
                                    } else {
                                        isTestingSound = true
                                        onPlayTest(selectedSoundUri) {
                                            isTestingSound = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        if (isTestingSound) Color(0xFFC62828) else Color(0xFF1B5E20),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isTestingSound) Icons.Default.Close else Icons.Default.PlayArrow,
                                    contentDescription = "Test Et",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { mp3PickerLauncher.launch("audio/*") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("📂 Dosyadan MP3", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm Sesi Seçin")
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    }
                                    ringtonePickerLauncher.launch(intent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("🎵 Sistem Sesleri", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. EZAN VAKTİNDE BİLDİR
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "🕌 Ezan Vaktinde Bildir", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            Text(text = "Vakit girdiğinde anında bildirim ve günün ayeti/hadisi", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = ezanNotifyEnabled,
                            onCheckedChange = { isChecked ->
                                ezanNotifyEnabled = isChecked
                                prefs.edit().putBoolean("ezan_notify_enabled", isChecked).apply()
                                applyAndReschedule()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFD54F),
                                checkedTrackColor = Color(0xFF1B5E20)
                            )
                        )
                    }
                }
            }

            // 3. KERAHAT VAKTİ UYARI SİSTEMİ
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "⚠️ Kerahat Vakti Uyarı Sistemi", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFFFB300))
                                Text(text = "Güneş doğuşu, öğle öncesi (istiva) ve akşam öncesi uyarır", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = kerahatNotifyEnabled,
                                onCheckedChange = { isChecked ->
                                    kerahatNotifyEnabled = isChecked
                                    prefs.edit().putBoolean("kerahat_notify_enabled", isChecked).apply()
                                    applyAndReschedule()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFFFD54F),
                                    checkedTrackColor = Color(0xFF1B5E20)
                                )
                            )
                        }

                        if (kerahatNotifyEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "Kerahat girmeden kaç dakika önce uyarılsın?", fontSize = 12.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(15, 20, 30, 45).forEach { min ->
                                    val isSelected = (kerahatMinutes == min)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFFFFB300) else Color.White.copy(alpha = 0.08f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                kerahatMinutes = min
                                                prefs.edit().putInt("kerahat_minutes", min).apply()
                                                applyAndReschedule()
                                            }
                                    ) {
                                        Text(
                                            text = "$min dk",
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. BAŞLIK
            item {
                Text(
                    text = "VAKİT ÖNCESİ HATIRLATICILAR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // 5. HER VAKİT İÇİN KARTLAR (Doğrudan ve kesin tiplerle)
            item {
                PrayerReminderCard(
                    config = PrayerReminderConfig(
                        title = "İmsak (Sabah)",
                        isEnabled = imsakEnabled,
                        selectedMinutes = imsakMinutes,
                        onToggle = { enabled: Boolean ->
                            imsakEnabled = enabled
                            prefs.edit().putBoolean("imsak_pre_enabled", enabled).apply()
                            applyAndReschedule()
                        },
                        onMinutesChange = { min: Int ->
                            imsakMinutes = min
                            prefs.edit().putInt("imsak_pre", min).apply()
                            applyAndReschedule()
                        }
                    )
                )
            }

            item {
                PrayerReminderCard(
                    config = PrayerReminderConfig(
                        title = "Güneş Doğuşu",
                        isEnabled = gunesEnabled,
                        selectedMinutes = gunesMinutes,
                        onToggle = { enabled: Boolean ->
                            gunesEnabled = enabled
                            prefs.edit().putBoolean("gunes_pre_enabled", enabled).apply()
                            applyAndReschedule()
                        },
                        onMinutesChange = { min: Int ->
                            gunesMinutes = min
                            prefs.edit().putInt("gunes_pre", min).apply()
                            applyAndReschedule()
                        }
                    )
                )
            }

            item {
                PrayerReminderCard(
                    config = PrayerReminderConfig(
                        title = "Öğle Namazı",
                        isEnabled = ogleEnabled,
                        selectedMinutes = ogleMinutes,
                        onToggle = { enabled: Boolean ->
                            ogleEnabled = enabled
                            prefs.edit().putBoolean("ogle_pre_enabled", enabled).apply()
                            applyAndReschedule()
                        },
                        onMinutesChange = { min: Int ->
                            ogleMinutes = min
                            prefs.edit().putInt("ogle_pre", min).apply()
                            applyAndReschedule()
                        }
                    )
                )
            }

            item {
                PrayerReminderCard(
                    config = PrayerReminderConfig(
                        title = "İkindi Namazı",
                        isEnabled = ikindiEnabled,
                        selectedMinutes = ikindiMinutes,
                        onToggle = { enabled: Boolean ->
                            ikindiEnabled = enabled
                            prefs.edit().putBoolean("ikindi_pre_enabled", enabled).apply()
                            applyAndReschedule()
                        },
                        onMinutesChange = { min: Int ->
                            ikindiMinutes = min
                            prefs.edit().putInt("ikindi_pre", min).apply()
                            applyAndReschedule()
                        }
                    )
                )
            }

            item {
                PrayerReminderCard(
                    config = PrayerReminderConfig(
                        title = "Akşam Namazı",
                        isEnabled = aksamEnabled,
                        selectedMinutes = aksamMinutes,
                        onToggle = { enabled: Boolean ->
                            aksamEnabled = enabled
                            prefs.edit().putBoolean("aksam_pre_enabled", enabled).apply()
                            applyAndReschedule()
                        },
                        onMinutesChange = { min: Int ->
                            aksamMinutes = min
                            prefs.edit().putInt("aksam_pre", min).apply()
                            applyAndReschedule()
                        }
                    )
                )
            }

            item {
                PrayerReminderCard(
                    config = PrayerReminderConfig(
                        title = "Yatsı Namazı",
                        isEnabled = yatsiEnabled,
                        selectedMinutes = yatsiMinutes,
                        onToggle = { enabled: Boolean ->
                            yatsiEnabled = enabled
                            prefs.edit().putBoolean("yatsi_pre_enabled", enabled).apply()
                            applyAndReschedule()
                        },
                        onMinutesChange = { min: Int ->
                            yatsiMinutes = min
                            prefs.edit().putInt("yatsi_pre", min).apply()
                            applyAndReschedule()
                        }
                    )
                )
            }
        }
    }
}

@Composable
fun PrayerReminderCard(config: PrayerReminderConfig) {
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
                Column {
                    Text(
                        text = config.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = if (config.isEnabled) "${config.selectedMinutes} dakika öncesi açık" else "Kapalı",
                        fontSize = 11.sp,
                        color = if (config.isEnabled) Color(0xFF81C784) else Color.Gray
                    )
                }

                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = config.onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFFD54F),
                        checkedTrackColor = Color(0xFF1B5E20)
                    )
                )
            }

            if (config.isEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5, 10, 15, 20, 25, 30).forEach { m ->
                        val isSel = (config.selectedMinutes == m)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0xFF1B5E20) else Color.White.copy(alpha = 0.06f),
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F)) else null,
                            modifier = Modifier.clickable { config.onMinutesChange(m) }
                        ) {
                            Text(
                                text = "${m}d",
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color(0xFFFFD54F) else Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}