package com.jetbrains.example.kotlin_agents_demo_app.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.example.kotlin_agents_demo_app.settings.AppSettings
import com.jetbrains.example.kotlin_agents_demo_app.settings.AppSettingsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import ai.koog.prompt.llm.LiteRTModels

// State for the UI
data class SettingsUiState(
    val openAiToken: String = "",
    val anthropicToken: String = "", // Kept for future use, not actively configured in UI for now
    val selectedProvider: String = AppSettings.PROVIDER_OPENAI,
    val liteRTModelId: String = LiteRTModels.Gemma3n.E2B.id, // Default to a specific LiteRT model
    val liteRTModelPath: String = "",
    val isLoading: Boolean = true
)

/**
 * ViewModel for the Settings screen
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Available LiteRT models for the dropdown
    val liteRTAvailableModels = listOf(
        LiteRTModels.Gemma3n.E2B,
        LiteRTModels.Gemma3n.E4B
        // Add other LiteRT models here as they are defined
    )

    init {
        // Load settings when ViewModel is created
        loadSettings()
    }

    /**
     * Load settings from AppSettings
     */
    private fun loadSettings() {
        viewModelScope.launch {
            val settings = appSettings.getCurrentSettings()
            _uiState.value = SettingsUiState(
                openAiToken = settings.openAiToken,
                anthropicToken = settings.anthropicToken,
                selectedProvider = settings.selectedProvider,
                liteRTModelId = settings.liteRTModelId.ifEmpty { liteRTAvailableModels.first().id }, // Default if empty
                liteRTModelPath = settings.liteRTModelPath,
                isLoading = false
            )
        }
    }

    /**
     * Update OpenAI token in the UI state
     */
    fun updateOpenAiToken(token: String) {
        _uiState.value = _uiState.value.copy(openAiToken = token)
    }

    /**
     * Update Anthropic token in the UI state
     */
    fun updateAnthropicToken(token: String) {
        _uiState.value = _uiState.value.copy(anthropicToken = token)
    }

    fun updateSelectedProvider(provider: String) {
        _uiState.value = _uiState.value.copy(selectedProvider = provider)
    }

    fun updateLiteRTModelId(modelId: String) {
        _uiState.value = _uiState.value.copy(liteRTModelId = modelId)
    }

    fun updateLiteRTModelPath(path: String) {
        _uiState.value = _uiState.value.copy(liteRTModelPath = path)
    }

    /**
     * Save settings to AppSettings
     */
    fun saveSettings() {
        viewModelScope.launch {
            val currentSettingsState = _uiState.value
            appSettings.setCurrentSettings(
                AppSettingsData(
                    openAiToken = currentSettingsState.openAiToken,
                    anthropicToken = currentSettingsState.anthropicToken,
                    selectedProvider = currentSettingsState.selectedProvider,
                    liteRTModelId = currentSettingsState.liteRTModelId,
                    liteRTModelPath = currentSettingsState.liteRTModelPath
                )
            )
        }
    }
}