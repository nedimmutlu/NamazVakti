package com.ezanvakti.app.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationSettingsDao {

    @Query("SELECT * FROM notification_settings WHERE prayerType = :prayerType")
    suspend fun getSettings(prayerType: String): NotificationSettings?

    @Query("SELECT * FROM notification_settings WHERE prayerType = :prayerType")
    fun getSettingsFlow(prayerType: String): Flow<NotificationSettings?>

    @Query("SELECT * FROM notification_settings")
    suspend fun getAllSettings(): List<NotificationSettings>

    @Query("SELECT * FROM notification_settings")
    fun getAllSettingsFlow(): Flow<List<NotificationSettings>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: NotificationSettings)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSettings(settings: List<NotificationSettings>)

    @Update
    suspend fun updateSettings(settings: NotificationSettings)

    @Query("DELETE FROM notification_settings WHERE prayerType = :prayerType")
    suspend fun deleteSettings(prayerType: String)
}
