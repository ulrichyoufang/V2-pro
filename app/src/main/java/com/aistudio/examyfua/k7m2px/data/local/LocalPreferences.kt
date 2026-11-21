package com.aistudio.examyfua.k7m2px.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.yfuaDataStore by preferencesDataStore(name = "yfua_prefs")

class LocalPreferences(private val context: Context) {
    private object Keys {
        val OFFLINE_MODE_ENTERED = booleanPreferencesKey("offline_mode_entered")
        val CUSTOM_FOCUS_MINUTES = intPreferencesKey("custom_focus_minutes")
        val CUSTOM_BREAK_MINUTES = intPreferencesKey("custom_break_minutes")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        val LAST_SYNC_EPOCH_MS = longPreferencesKey("last_sync_epoch_ms")
        val LAST_DRIVE_BACKUP_EPOCH_MS = longPreferencesKey("last_drive_backup_epoch_ms")
        val FCM_TOKEN = stringPreferencesKey("fcm_token")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    }

    val offlineModeEntered: Flow<Boolean> = context.yfuaDataStore.data.map {
        it[Keys.OFFLINE_MODE_ENTERED] ?: false
    }

    val darkModeEnabled: Flow<Boolean> = context.yfuaDataStore.data.map {
        it[Keys.DARK_MODE_ENABLED] ?: false
    }

    val soundEnabled: Flow<Boolean> = context.yfuaDataStore.data.map {
        it[Keys.SOUND_ENABLED] ?: true
    }

    val hapticsEnabled: Flow<Boolean> = context.yfuaDataStore.data.map {
        it[Keys.HAPTICS_ENABLED] ?: true
    }

    val customFocusMinutes: Flow<Int> = context.yfuaDataStore.data.map {
        it[Keys.CUSTOM_FOCUS_MINUTES] ?: 35
    }

    val customBreakMinutes: Flow<Int> = context.yfuaDataStore.data.map {
        it[Keys.CUSTOM_BREAK_MINUTES] ?: 7
    }

    val dailyReminderEnabled: Flow<Boolean> = context.yfuaDataStore.data.map {
        it[Keys.DAILY_REMINDER_ENABLED] ?: false
    }

    val dailyReminderTime: Flow<Pair<Int, Int>> = context.yfuaDataStore.data.map {
        (it[Keys.DAILY_REMINDER_HOUR] ?: 19) to (it[Keys.DAILY_REMINDER_MINUTE] ?: 0)
    }

    val lastSyncEpochMs: Flow<Long> = context.yfuaDataStore.data.map {
        it[Keys.LAST_SYNC_EPOCH_MS] ?: 0L
    }

    val lastDriveBackupEpochMs: Flow<Long> = context.yfuaDataStore.data.map {
        it[Keys.LAST_DRIVE_BACKUP_EPOCH_MS] ?: 0L
    }

    val fcmToken: Flow<String?> = context.yfuaDataStore.data.map {
        it[Keys.FCM_TOKEN]
    }

    suspend fun setOfflineModeEntered(entered: Boolean) = withContext(Dispatchers.IO) {
        context.yfuaDataStore.edit { it[Keys.OFFLINE_MODE_ENTERED] = entered }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        context.yfuaDataStore.edit { it[Keys.DARK_MODE_ENABLED] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        context.yfuaDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        context.yfuaDataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setCustomPomodoro(focusMinutes: Int, breakMinutes: Int) = withContext(Dispatchers.IO) {
        context.yfuaDataStore.edit {
            it[Keys.CUSTOM_FOCUS_MINUTES] = focusMinutes.coerceIn(5, 120)
            it[Keys.CUSTOM_BREAK_MINUTES] = breakMinutes.coerceIn(1, 45)
        }
    }

    suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) = withContext(Dispatchers.IO) {
        context.yfuaDataStore.edit {
            it[Keys.DAILY_REMINDER_ENABLED] = enabled
            it[Keys.DAILY_REMINDER_HOUR] = hour
            it[Keys.DAILY_REMINDER_MINUTE] = minute
        }
    }

    suspend fun setLastSyncEpochMs(epochMs: Long) = withContext(Dispatchers.IO) {
        context.yfuaDataStore.edit { it[Keys.LAST_SYNC_EPOCH_MS] = epochMs }
    }

    suspend fun setLastDriveBackupEpochMs(epochMs: Long) = withContext(Dispatchers.IO) {
        context.yfuaDataStore.edit { it[Keys.LAST_DRIVE_BACKUP_EPOCH_MS] = epochMs }
    }

    suspend fun setFcmToken(token: String) = withContext(Dispatchers.IO) {
        context.yfuaDataStore.edit { it[Keys.FCM_TOKEN] = token }
    }
}
