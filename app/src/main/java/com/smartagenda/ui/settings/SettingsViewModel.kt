package com.smartagenda.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartagenda.ai.AiParseEngine
import com.smartagenda.data.prefs.AiProvider
import com.smartagenda.data.prefs.AppSettings
import com.smartagenda.data.prefs.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val initialized: Boolean = false,
    val providerId: String = AiProvider.DEEPSEEK.id,
    val baseUrl: String = AiProvider.DEEPSEEK.baseUrl,
    val model: String = AiProvider.DEEPSEEK.defaultModel,
    val apiKey: String = "",
    val enabled: Boolean = true,
    val ignoredSources: Set<String> = emptySet(),
    val isSaving: Boolean = false,
    val isTesting: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
) {
    val provider: AiProvider get() = AiProvider.fromId(providerId)
    val isCustom: Boolean get() = provider == AiProvider.CUSTOM
    val isConfigured: Boolean get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()
}

class SettingsViewModel(
    private val store: SettingsDataStore,
    private val engine: AiParseEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = store.settings.first()
            _uiState.update {
                SettingsUiState(
                    initialized = true,
                    providerId = loaded.providerId,
                    baseUrl = loaded.baseUrl,
                    model = loaded.model,
                    apiKey = loaded.apiKey,
                    enabled = loaded.enabled,
                    ignoredSources = loaded.ignoredSources,
                )
            }
        }
    }

    fun selectProvider(provider: AiProvider) {
        _uiState.update {
            it.copy(
                providerId = provider.id,
                baseUrl = provider.baseUrl.takeIf { url -> url.isNotBlank() } ?: it.baseUrl,
                model = provider.defaultModel,
                message = null,
            )
        }
    }

    fun updateBaseUrl(value: String) {
        _uiState.update { it.copy(baseUrl = value, message = null) }
    }

    fun updateModel(value: String) {
        _uiState.update { it.copy(model = value, message = null) }
    }

    fun updateApiKey(value: String) {
        _uiState.update { it.copy(apiKey = value, message = null) }
    }

    fun setEnabled(value: Boolean) {
        _uiState.update { it.copy(enabled = value) }
    }

    fun addIgnoredSource(raw: String) {
        val value = raw.trim()
        if (value.isEmpty()) return
        _uiState.update { it.copy(ignoredSources = it.ignoredSources + value) }
    }

    fun removeIgnoredSource(value: String) {
        _uiState.update { it.copy(ignoredSources = it.ignoredSources - value) }
    }

    fun save(notify: (String) -> Unit = {}) {
        val state = _uiState.value
        if (state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            runCatching {
                store.setProvider(state.provider)
                store.setBaseUrl(state.baseUrl)
                store.setModel(state.model)
                store.setApiKey(state.apiKey)
                store.setEnabled(state.enabled)
                store.setIgnoredSources(state.ignoredSources)
            }.onSuccess {
                _uiState.update {
                    it.copy(isSaving = false, message = "已保存", messageIsError = false)
                }
                notify("已保存")
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message ?: "保存失败",
                        messageIsError = true,
                    )
                }
            }
        }
    }

    fun testConnection() {
        val state = _uiState.value
        if (state.isTesting) return
        if (!state.isConfigured) {
            _uiState.update { it.copy(message = "请先填写 API Key 和模型名", messageIsError = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, message = null) }
            val result = engine.testConnection(
                AppSettings(
                    providerId = state.providerId,
                    baseUrl = state.baseUrl,
                    model = state.model,
                    apiKey = state.apiKey,
                    ignoredSources = state.ignoredSources,
                    enabled = state.enabled,
                ),
            )
            result.onSuccess { reply ->
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        message = "连接成功：${reply.take(40)}",
                        messageIsError = false,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        message = "连接失败：${error.message ?: error.javaClass.simpleName}",
                        messageIsError = true,
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}