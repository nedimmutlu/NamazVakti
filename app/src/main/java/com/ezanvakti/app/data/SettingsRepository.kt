package com.ezanvakti.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ezanvakti.app.data.room.AppDatabase
import com.ezanvakti.app.data.room.NotificationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "notification_settings")

class SettingsRepository(private val context: Context, private val database: AppDatabase) {

    private val dao = database.notificationSettingsDao()

    // Global Settings
    companion object {
        val EXACT_ALARM_ENABLED = booleanPreferencesKey("exact_alarm_enabled")
        val DEFAULT_PRE_MINUTES = intPreferencesKey("default_pre_minutes")
        val KERAHAT_GLOBAL_ENABLED = booleanPreferencesKey("kerahat_global_enabled")
        val KERAHAT_DEFAULT_MINUTES = intPreferencesKey("kerahat_default_minutes")
        val LAST_MIGRATION = intPreferencesKey("last_migration_version")
        val EXACT_SOUND = stringPreferencesKey("exact_sound_key")
        val PRE_SOUND = stringPreferencesKey("pre_sound_key")
        val KERAHAT_SOUND = stringPreferencesKey("kerahat_sound_key")
    }

    val exactAlarmEnabled: Flow<Boolean> = context.settingsStore.data.map { it[EXACT_ALARM_ENABLED] ?: true }
    val defaultPreMinutes: Flow<Int> = context.settingsStore.data.map { it[DEFAULT_PRE_MINUTES] ?: 10 }
    val kerahatGlobalEnabled: Flow<Boolean> = context.settingsStore.data.map { it[KERAHAT_GLOBAL_ENABLED] ?: false }
    val kerahatDefaultMinutes: Flow<Int> = context.settingsStore.data.map { it[KERAHAT_DEFAULT_MINUTES] ?: 15 }
    val lastMigration: Flow<Int> = context.settingsStore.data.map { it[LAST_MIGRATION] ?: 0 }
    val exactSound: Flow<String> = context.settingsStore.data.map { it[EXACT_SOUND] ?: "namaz_vakti" }
    val preSound: Flow<String> = context.settingsStore.data.map { it[PRE_SOUND] ?: "dakika_10" }
    val kerahatSound: Flow<String> = context.settingsStore.data.map { it[KERAHAT_SOUND] ?: "kerahat" }

    // Prayer-specific Settings
    fun getSettingsFlow(prayerType: String): Flow<NotificationSettings?> = dao.getSettingsFlow(prayerType)

    suspend fun getSettings(prayerType: String): NotificationSettings? = dao.getSettings(prayerType)

    suspend fun updateSettings(settings: NotificationSettings) {
        dao.insertSettings(settings)
    }

    suspend fun setExactAlarmEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[EXACT_ALARM_ENABLED] = enabled }
    }

    suspend fun setDefaultPreMinutes(minutes: Int) {
        context.settingsStore.edit { it[DEFAULT_PRE_MINUTES] = minutes }
    }

    suspend fun setKerahatGlobalEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[KERAHAT_GLOBAL_ENABLED] = enabled }
    }

    suspend fun setKerahatDefaultMinutes(minutes: Int) {
        context.settingsStore.edit { it[KERAHAT_DEFAULT_MINUTES] = minutes }
    }

    suspend fun setLastMigration(version: Int) {
        context.settingsStore.edit { it[LAST_MIGRATION] = version }
    }

    suspend fun setExactSound(sound: String) {
        context.settingsStore.edit { it[EXACT_SOUND] = sound }
    }

    suspend fun setPreSound(sound: String) {
        context.settingsStore.edit { it[PRE_SOUND] = sound }
    }

    suspend fun setKerahatSound(sound: String) {
        context.settingsStore.edit { it[KERAHAT_SOUND] = sound }
    }
}