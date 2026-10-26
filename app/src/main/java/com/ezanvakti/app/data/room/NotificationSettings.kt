package com.ezanvakti.app.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_settings")
data class NotificationSettings(
    @PrimaryKey val prayerType: String,
    val preNotificationMinutes: Int, // 5, 10, 15, 20, 25, 30, 60, or custom
    val isPreNotificationEnabled: Boolean,
    val preNotificationSoundKey: String,
    val kerahatWarningEnabled: Boolean,
    val kerahatPreMinutes: Int, // 15, 20, 30
    val kerahatSoundKey: String,
    val updatedAt: Long = System.currentTimeMillis()
)
