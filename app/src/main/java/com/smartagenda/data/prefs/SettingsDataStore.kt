package com.smartagenda.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

enum class AiProvider(
    val id: String,
    val label: String,
    val baseUrl: String,
    val defaultModel: String,
) {
    DEEPSEEK("deepseek", "DeepSeek", "https://api.deepseek.com/v1/", "deepseek-chat"),
    JSNU("jsnu", "模型广场（JSNU）", "https://model.jsnu.edu.cn/v1/", "qwen3.8-27b"),
    OPENAI("openai", "OpenAI", "https://api.openai.com/v1/", "gpt-4o-mini"),
    MOONSHOT("moonshot", "Moonshot", "https://api.moonshot.cn/v1/", "moonshot-v1-8k"),
    CUSTOM("custom", "自定义（OpenAI 兼容）", "", "deepseek-chat");

    companion object {
        fun fromId(id: String): AiProvider = entries.firstOrNull { it.id == id } ?: DEEPSEEK
    }
}

data class AppSettings(
    val providerId: String = AiProvider.DEEPSEEK.id,
    val baseUrl: String = AiProvider.DEEPSEEK.baseUrl,
    val model: String = AiProvider.DEEPSEEK.defaultModel,
    val apiKey: String = "",
    val ignoredSources: Set<String> = emptySet(),
    val enabled: Boolean = true,
) {
    val provider: AiProvider get() = AiProvider.fromId(providerId)
    val isConfigured: Boolean get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val provider = stringPreferencesKey("provider_id")
        val baseUrl = stringPreferencesKey("base_url")
        val model = stringPreferencesKey("model")
        val apiKeyEncrypted = stringPreferencesKey("api_key_encrypted")
        val ignoredSources = stringSetPreferencesKey("ignored_sources")
        val enabled = booleanPreferencesKey("ai_enabled")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val providerId = prefs[Keys.provider] ?: AiProvider.DEEPSEEK.id
        val provider = AiProvider.fromId(providerId)
        val encryptedKey = prefs[Keys.apiKeyEncrypted].orEmpty()
        AppSettings(
            providerId = providerId,
            baseUrl = prefs[Keys.baseUrl]?.takeIf { it.isNotBlank() } ?: provider.baseUrl,
            model = prefs[Keys.model]?.takeIf { it.isNotBlank() } ?: provider.defaultModel,
            apiKey = SecureKeyStore.decrypt(encryptedKey).orEmpty(),
            ignoredSources = prefs[Keys.ignoredSources] ?: emptySet(),
            enabled = prefs[Keys.enabled] ?: true,
        )
    }.flowOn(Dispatchers.IO)

    suspend fun setProvider(provider: AiProvider) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.provider] = provider.id
            prefs[Keys.baseUrl] = provider.baseUrl
            prefs[Keys.model] = provider.defaultModel
        }
    }

    suspend fun setBaseUrl(value: String) {
        context.settingsDataStore.edit { it[Keys.baseUrl] = value.trim() }
    }

    suspend fun setModel(value: String) {
        context.settingsDataStore.edit { it[Keys.model] = value.trim() }
    }

    suspend fun setApiKey(value: String) {
        val encrypted = if (value.isBlank()) {
            ""
        } else {
            withContext(Dispatchers.IO) { SecureKeyStore.encrypt(value.trim()) }
        }
        context.settingsDataStore.edit { it[Keys.apiKeyEncrypted] = encrypted }
    }

    suspend fun setIgnoredSources(value: Set<String>) {
        context.settingsDataStore.edit { it[Keys.ignoredSources] = value }
    }

    suspend fun setEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.enabled] = value }
    }
}