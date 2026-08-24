package com.example.taskmanager.settings.data

import android.content.Context
import com.example.taskmanager.security.CryptoSecurityHelper
import com.example.taskmanager.settings.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)
    suspend fun resetToDefaults()
}

class SettingsRepositoryImpl(context: Context) : SettingsRepository {
    private val prefs = context.getSharedPreferences("app_settings_vault", Context.MODE_PRIVATE)
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _settingsState = MutableStateFlow(loadSettings())
    override val settingsFlow: Flow<AppSettings> = _settingsState.asStateFlow()

    private fun loadSettings(): AppSettings {
        val rawPayload = prefs.getString("encrypted_settings_payload", null)
            ?: prefs.getString("settings_json", null)
            ?: return AppSettings()

        val decryptedJson = CryptoSecurityHelper.decrypt(rawPayload)

        return try {
            json.decodeFromString<AppSettings>(decryptedJson)
        } catch (e: Exception) {
            AppSettings()
        }
    }

    override suspend fun getSettings(): AppSettings {
        return _settingsState.value
    }

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val current = _settingsState.value
        val updated = transform(current).copy(lastUpdatedEpochMs = System.currentTimeMillis())
        _settingsState.value = updated
        val plainJson = json.encodeToString(updated)
        val encryptedPayload = CryptoSecurityHelper.encrypt(plainJson)
        prefs.edit().putString("encrypted_settings_payload", encryptedPayload).apply()
    }

    override suspend fun resetToDefaults() {
        val default = AppSettings()
        _settingsState.value = default
        val plainJson = json.encodeToString(default)
        val encryptedPayload = CryptoSecurityHelper.encrypt(plainJson)
        prefs.edit().putString("encrypted_settings_payload", encryptedPayload).apply()
    }
}
