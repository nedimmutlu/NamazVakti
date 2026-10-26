package com.ezanvakti.app.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_preferences")

class NotificationDataStore(private val context: Context) {

    companion object {
        // Global ayarlar
        val EXACT_ALARM_ENABLED = booleanPreferencesKey("exact_alarm_enabled")
        val DEFAULT_PRE_MINUTES = intPreferencesKey("default_pre_minutes")
        val KERAHAT_GLOBAL_ENABLED = booleanPreferencesKey("kerahat_global_enabled")
        val KERAHAT_DEFAULT_MINUTES = intPreferencesKey("kerahat_default_minutes")
        val LAST_MIGRATION_VERSION = intPreferencesKey("last_migration_version")

        // Ses ayarları
        val EXACT_SOUND_KEY = stringPreferencesKey("exact_sound_key")
        val PRE_SOUND_KEY = stringPreferencesKey("pre_sound_key")
        val KERAHAT_SOUND_KEY = stringPreferencesKey("kerahat_sound_key")
    }

    // Exact alarm aktif mi?
    val exactAlarmEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences -> preferences[EXACT_ALARM_ENABLED] ?: true }

    // Varsayılan öncesi bildirim süresi
    val defaultPreMinutes: Flow<Int> = context.notificationDataStore.data
        .map { preferences -> preferences[DEFAULT_PRE_MINUTES] ?: 10 }

    // Kerahat global aktif mi?
    val kerahatGlobalEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences -> preferences[KERAHAT_GLOBAL_ENABLED] ?: false }

    // Kerahat varsayılan dakika
    val kerahatDefaultMinutes: Flow<Int> = context.notificationDataStore.data
        .map { preferences -> preferences[KERAHAT_DEFAULT_MINUTES] ?: 15 }

    // Ses key'leri
    val exactSoundKey: Flow<String> = context.notificationDataStore.data
        .map { preferences -> preferences[EXACT_SOUND_KEY] ?: "namaz_vakti" }

    val preSoundKey: Flow<String> = context.notificationDataStore.data
        .map { preferences -> preferences[PRE_SOUND_KEY] ?: "dakika_10" }

    val kerahatSoundKey: Flow<String> = context.notificationDataStore.data
        .map { preferences -> preferences[KERAHAT_SOUND_KEY] ?: "kerahat" }

    suspend fun setExactAlarmEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[EXACT_ALARM_ENABLED] = enabled
        }
    }

    suspend fun setDefaultPreMinutes(minutes: Int) {
        context.notificationDataStore.edit { preferences ->
            preferences[DEFAULT_PRE_MINUTES] = minutes
        }
    }

    suspend fun setKerahatGlobalEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[KERAHAT_GLOBAL_ENABLED] = enabled
        }
    }

    suspend fun setKerahatDefaultMinutes(minutes: Int) {
        context.notificationDataStore.edit { preferences ->
            preferences[KERAHAT_DEFAULT_MINUTES] = minutes
        }
    }

    suspend fun setExactSoundKey(key: String) {
        context.notificationDataStore.edit { preferences ->
            preferences[EXACT_SOUND_KEY] = key
        }
    }

    suspend fun setPreSoundKey(key: String) {
        context.notificationDataStore.edit { preferences ->
            preferences[PRE_SOUND_KEY] = key
        }
    }

    suspend fun setKerahatSoundKey(key: String) {
        context.notificationDataStore.edit { preferences ->
            preferences[KERAHAT_SOUND_KEY] = key
        }
    }

    suspend fun setLastMigrationVersion(version: Int) {
        context.notificationDataStore.edit { preferences ->
            preferences[LAST_MIGRATION_VERSION] = version
        }
    }

    fun getLastMigrationVersion(): Flow<Int> = context.notificationDataStore.data
        .map { preferences -> preferences[LAST_MIGRATION_VERSION] ?: 0 }
}
