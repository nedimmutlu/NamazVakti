package com.ezanvakti.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.ezanvakti.app.alarm.KerahatType
import com.ezanvakti.app.data.model.PrayerType
import com.ezanvakti.app.data.room.NotificationSettings
import kotlinx.coroutines.launch

// Renk tanımları
private val DarkBackground = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val PrimaryColor = Color(0xFF38BDF8)
private val AccentColor = Color(0xFF0F766E)
private val WarningColor = Color(0xFFF59E0B)

// Öncesi bildirim dakika seçenekleri
private val preNotificationOptions = listOf(5, 10, 15, 20, 25, 30, 60)

// Kerahat uyarı dakika seçenekleri
private val kerahatOptions = listOf(15, 20, 30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    settings: Map<PrayerType, NotificationSettings>,
    kerahatEnabled: Boolean,
    kerahatMinutes: Int,
    clashWarning: String?,
    onBack: () -> Unit,
    onSave: (Map<PrayerType, NotificationSettings>, Boolean, Int) -> Unit
) {
    val scrollState = rememberScrollState()

    // Her vakit için local state
    val localSettings = remember(settings) {
        mutableStateMapOf<PrayerType, NotificationSettings>().apply {
            putAll(settings)
        }
    }

    // Kerahat state
    var localKerahatEnabled by remember { mutableStateOf(kerahatEnabled) }
    var localKerahatMinutes by remember { mutableStateOf(kerahatMinutes) }

    // Çakışma uyarısı
    var showClashDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bildirim ve Alarm Ayarları",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Çakışma uyarısı kartı
            if (clashWarning != null) {
                WarningCard(message = clashWarning)
            }

            // Global Kerahat Ayarları
            KerahatSettingsCard(
                enabled = localKerahatEnabled,
                minutes = localKerahatMinutes,
                onEnabledChange = { localKerahatEnabled = it },
                onMinutesChange = { localKerahatMinutes = it }
            )

            // Her vakit için ayar kartı
            PrayerType.values().forEach { prayerType ->
                val prayerSettings = localSettings[prayerType] ?: getDefaultSettings(prayerType)
                PrayerNotificationCard(
                    prayerType = prayerType,
                    settings = prayerSettings,
                    onSettingsChange = { newSettings ->
                        localSettings[prayerType] = newSettings
                    }
                )
            }

            // Kaydet butonu
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    onSave(
                        localSettings.toMap(),
                        localKerahatEnabled,
                        localKerahatMinutes
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentColor
                )
            ) {
                Text(
                    text = "AYARLARI KAYDET",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WarningCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️",
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = message,
                color = WarningColor,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun KerahatSettingsCard(
    enabled: Boolean,
    minutes: Int,
    onEnabledChange: (Boolean) -> Unit,
    onMinutesChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "⚠️ Kerahat Vakti Uyarıları",
                color = WarningColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Kerahat vaktine girmeden önce uyarı alabilirsiniz. Kerahat vakitlerinde nafile namaz kılınmaz.",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Aktif/Pasif switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kerahat Uyarılarını Aktifleştir",
                    color = Color.White,
                    fontSize = 15.sp
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryColor,
                        checkedTrackColor = AccentColor
                    )
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Kerahat girmeden kaç dakika önce uyarsın?",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Dakika seçimi - Chip grubu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    kerahatOptions.forEach { option ->
                        FilterChip(
                            selected = minutes == option,
                            onClick = { onMinutesChange(option) },
                            label = {
                                Text(
                                    text = "$option dk",
                                    color = if (minutes == option) Color.White else Color(0xFF94A3B8)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerNotificationCard(
    prayerType: PrayerType,
    settings: NotificationSettings,
    onSettingsChange: (NotificationSettings) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Vakit adı ve ikonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getPrayerIcon(prayerType) + " " + prayerType.displayName,
                    color = PrimaryColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Switch(
                    checked = settings.isPreNotificationEnabled,
                    onCheckedChange = { enabled ->
                        onSettingsChange(settings.copy(isPreNotificationEnabled = enabled))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryColor,
                        checkedTrackColor = AccentColor
                    )
                )
            }

            if (settings.isPreNotificationEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Vakit öncesi bildirim süresi:",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Basit dropdown - Spinner ile
                val minutesOptions = preNotificationOptions.map { "$it dakika önce" }
                val selectedIndex = preNotificationOptions.indexOf(settings.preNotificationMinutes).coerceAtLeast(0)
                var selectedOption by remember { mutableStateOf(minutesOptions[selectedIndex]) }

                OutlinedTextField(
                    value = selectedOption,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Seç",
                            tint = PrimaryColor
                        )
                    }
                )

                // Seçim listesi - basit chip grubu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    preNotificationOptions.forEach { minutes ->
                        FilterChip(
                            selected = settings.preNotificationMinutes == minutes,
                            onClick = {
                                onSettingsChange(settings.copy(preNotificationMinutes = minutes))
                                selectedOption = "$minutes dakika önce"
                            },
                            label = {
                                Text(
                                    text = "$minutes dk",
                                    fontSize = 12.sp,
                                    color = if (settings.preNotificationMinutes == minutes) Color.White else Color(0xFF94A3B8)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentColor
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun getPrayerIcon(prayerType: PrayerType): String {
    return when (prayerType) {
        PrayerType.FAJR -> "🌙"
        PrayerType.SUNRISE -> "🌅"
        PrayerType.DHUHR -> "☀️"
        PrayerType.ASR -> "🌤️"
        PrayerType.MAGHRIB -> "🌇"
        PrayerType.ISHA -> "🌃"
    }
}

private fun getDefaultSettings(prayerType: PrayerType): NotificationSettings {
    return NotificationSettings(
        prayerType = prayerType.name,
        preNotificationMinutes = 10,
        isPreNotificationEnabled = true,
        preNotificationSoundKey = "dakika_10",
        kerahatWarningEnabled = false,
        kerahatPreMinutes = 15,
        kerahatSoundKey = "kerahat"
    )
}
