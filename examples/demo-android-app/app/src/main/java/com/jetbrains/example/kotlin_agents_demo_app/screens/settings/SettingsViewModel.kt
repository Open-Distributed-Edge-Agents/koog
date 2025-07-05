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

// State for the UI

// Represents the download status of a model
sealed class ModelDownloadStatus {
    data object NotDownloaded : ModelDownloadStatus()
    data class Downloading(val progress: Float) : ModelDownloadStatus() // progress 0.0 to 1.0
    data class Downloaded(val path: String) : ModelDownloadStatus()
    data class Error(val message: String) : ModelDownloadStatus()
}

data class SettingsUiState(
    val openAiToken: String = "",
    val anthropicToken: String = "", // Kept for future use
    val selectedProvider: String = AppSettings.PROVIDER_OPENAI,

    // LiteRT specific state
    val availableLiteRTModels: List<DownloadableLiteRTModel> = SupportedLiteRTModels.all,
    val liteRTModelDownloadStatus: Map<String, ModelDownloadStatus> = emptyMap(), // Key: modelId (from llModel.id)
    val selectedLiteRTModelId: String = SupportedLiteRTModels.all.firstOrNull()?.llModel?.id ?: "",
    val selectedLiteRTModelPath: String = "", // Path of the currently selected and downloaded LiteRT model

    val isLoading: Boolean = true
)

/**
 * ViewModel for the Settings screen
 */
import com.jetbrains.example.kotlin_agents_demo_app.common.DownloadProgress
import com.jetbrains.example.kotlin_agents_demo_app.common.ModelDownloader
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings(application)
    private val httpClient = HttpClient(Android) // Create Ktor client
    private val modelDownloader = ModelDownloader(httpClient) // Create downloader instance
    private val modelsDir = File(application.getExternalFilesDir(null), "litert_models")


    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        loadSettings()
        checkAllModelFileStatuses() // Check file system for downloaded models
    }

    private fun checkAllModelFileStatuses() {
        viewModelScope.launch {
            val currentStatuses = _uiState.value.liteRTModelDownloadStatus.toMutableMap()
            var settingsChanged = false
            var currentSelectedPath = _uiState.value.selectedLiteRTModelPath
            val currentSelectedId = _uiState.value.selectedLiteRTModelId

            _uiState.value.availableLiteRTModels.forEach { modelInfo ->
                val modelFile = File(modelsDir, modelInfo.fileName)
                if (modelFile.exists() && modelFile.length() > 0) { // Check if file exists and is not empty
                    if (currentStatuses[modelInfo.llModel.id] !is ModelDownloadStatus.Downloaded ||
                        (currentStatuses[modelInfo.llModel.id] as ModelDownloadStatus.Downloaded).path != modelFile.absolutePath) {
                        currentStatuses[modelInfo.llModel.id] = ModelDownloadStatus.Downloaded(modelFile.absolutePath)
                        if (modelInfo.llModel.id == currentSelectedId) {
                             currentSelectedPath = modelFile.absolutePath
                             settingsChanged = true // Path for selected model updated
                        }
                    }
                } else {
                    // If file doesn't exist, but status was Downloaded, change to NotDownloaded
                    if (currentStatuses[modelInfo.llModel.id] is ModelDownloadStatus.Downloaded) {
                        currentStatuses[modelInfo.llModel.id] = ModelDownloadStatus.NotDownloaded
                    }
                    if (modelInfo.llModel.id == currentSelectedId) {
                        // If the currently selected model's file is missing, clear its path
                        if (currentSelectedPath.isNotBlank()) {
                            currentSelectedPath = ""
                            settingsChanged = true // Path for selected model cleared
                        }
                    }
                }
            }
            _uiState.update {
                it.copy(
                    liteRTModelDownloadStatus = currentStatuses,
                    selectedLiteRTModelPath = currentSelectedPath
                )
            }
            // If the selected model's path was changed due to file system check, re-save settings
            if (settingsChanged) {
                saveLiteRTModelSelectionToAppSettings(currentSelectedId, currentSelectedPath)
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = appSettings.getCurrentSettings()
            // Initial statuses will be further refined by checkAllModelFileStatuses
            val initialModelStatuses = SupportedLiteRTModels.all.associate {
                it.llModel.id to ModelDownloadStatus.NotDownloaded as ModelDownloadStatus
            }.toMutableMap()

            val loadedSelectedId = settings.liteRTModelId.ifEmpty { SupportedLiteRTModels.all.firstOrNull()?.llModel?.id ?: "" }
            // Path will be verified by checkAllModelFileStatuses against file system
            val loadedSelectedPath = settings.liteRTModelPath


            _uiState.value = SettingsUiState(
                openAiToken = settings.openAiToken,
                anthropicToken = settings.anthropicToken,
                selectedProvider = settings.selectedProvider,
                availableLiteRTModels = SupportedLiteRTModels.all,
                liteRTModelDownloadStatus = initialModelStatuses, // Will be updated by checkAllModelFileStatuses
                selectedLiteRTModelId = loadedSelectedId,
                selectedLiteRTModelPath = loadedSelectedPath, // Will be verified
                isLoading = false // Set to false after initial load attempt
            )
            // Call file status check after initial state is set from AppSettings
            checkAllModelFileStatuses()
        }
    }
                }
            }

            _uiState.value = SettingsUiState(
                openAiToken = settings.openAiToken,
                anthropicToken = settings.anthropicToken,
                selectedProvider = settings.selectedProvider,
                availableLiteRTModels = SupportedLiteRTModels.all,
                liteRTModelDownloadStatus = initialModelStatuses,
                selectedLiteRTModelId = currentSelectedId,
                selectedLiteRTModelPath = if (initialModelStatuses[currentSelectedId] is ModelDownloadStatus.Downloaded) currentSelectedPath else "",
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
                    liteRTModelId = currentSettingsState.selectedLiteRTModelId, // Use selectedLiteRTModelId
                    liteRTModelPath = currentSettingsState.selectedLiteRTModelPath  // Use selectedLiteRTModelPath
                )
            )
        }
    }

    fun startDownload(modelInfo: DownloadableLiteRTModel) {
        viewModelScope.launch {
            val modelFile = File(modelsDir, modelInfo.fileName)
            val modelId = modelInfo.llModel.id

            // Update status to Downloading(0f)
            _uiState.update {
                val newStatuses = it.liteRTModelDownloadStatus.toMutableMap()
                newStatuses[modelId] = ModelDownloadStatus.Downloading(0f)
                it.copy(liteRTModelDownloadStatus = newStatuses)
            }

            modelDownloader.downloadFile(modelInfo.downloadUrl, modelFile)
                .catch { e ->
                    _uiState.update {
                        val newStatuses = it.liteRTModelDownloadStatus.toMutableMap()
                        newStatuses[modelId] = ModelDownloadStatus.Error(e.message ?: "Unknown download error")
                        it.copy(liteRTModelDownloadStatus = newStatuses)
                    }
                }
                .onCompletion { cause ->
                    if (cause != null && cause !is kotlinx.coroutines.CancellationException) {
                        // Handled by .catch, or if flow completes without 'Completed' but with an error
                         _uiState.update {
                            val newStatuses = it.liteRTModelDownloadStatus.toMutableMap()
                            if (newStatuses[modelId] !is ModelDownloadStatus.Error) { // Don't overwrite specific error
                                newStatuses[modelId] = ModelDownloadStatus.Error(cause.message ?: "Download flow failed")
                            }
                            it.copy(liteRTModelDownloadStatus = newStatuses)
                        }
                    }
                }
                .collect { progress ->
                    when (progress) {
                        is DownloadProgress.InProgress -> {
                            _uiState.update {
                                val newStatuses = it.liteRTModelDownloadStatus.toMutableMap()
                                newStatuses[modelId] = ModelDownloadStatus.Downloading(progress.progress)
                                it.copy(liteRTModelDownloadStatus = newStatuses)
                            }
                        }
                        is DownloadProgress.Completed -> {
                            _uiState.update {
                                val newStatuses = it.liteRTModelDownloadStatus.toMutableMap()
                                newStatuses[modelId] = ModelDownloadStatus.Downloaded(progress.file.absolutePath)
                                // Automatically select the downloaded model and save settings
                                it.copy(
                                    liteRTModelDownloadStatus = newStatuses,
                                    selectedLiteRTModelId = modelId,
                                    selectedLiteRTModelPath = progress.file.absolutePath
                                )
                            }
                            saveSettings() // Save after successful download and selection
                        }
                        is DownloadProgress.Failed -> { // Should be caught by .catch, but as a safeguard
                            _uiState.update {
                                val newStatuses = it.liteRTModelDownloadStatus.toMutableMap()
                                newStatuses[modelId] = ModelDownloadStatus.Error(progress.error.message ?: "Download failed")
                                it.copy(liteRTModelDownloadStatus = newStatuses)
                            }
                        }
                    }
                }
        }
    }

    fun selectDownloadedLiteRTModel(modelInfo: DownloadableLiteRTModel) {
        viewModelScope.launch {
            val modelFile = File(modelsDir, modelInfo.fileName)
            if (modelFile.exists()) {
                _uiState.update {
                    it.copy(
                        selectedLiteRTModelId = modelInfo.llModel.id,
                        selectedLiteRTModelPath = modelFile.absolutePath
                    )
                }
                saveSettings() // Persist the selection
            } else {
                // Should not happen if status is Downloaded, but handle defensively
                _uiState.update {
                    val newStatuses = it.liteRTModelDownloadStatus.toMutableMap()
                    newStatuses[modelInfo.llModel.id] = ModelDownloadStatus.Error("File not found, please re-download.")
                    it.copy(liteRTModelDownloadStatus = newStatuses, selectedLiteRTModelPath = "")
                }
            }
        }
    }

    // Helper to update AppSettings specifically for LiteRT model selection
    private fun saveLiteRTModelSelectionToAppSettings(modelId: String, modelPath: String) {
         viewModelScope.launch {
            val currentFullSettings = appSettings.getCurrentSettings()
            appSettings.setCurrentSettings(
                currentFullSettings.copy(
                    liteRTModelId = modelId,
                    liteRTModelPath = modelPath
                )
            )
        }
    }

     fun deleteLiteRTModel(modelInfo: DownloadableLiteRTModel) {
        viewModelScope.launch {
            val modelFile = File(modelsDir, modelInfo.fileName)
            var newPathForSelected = _uiState.value.selectedLiteRTModelPath
            if (modelFile.exists()) {
                modelFile.delete()
            }
            // Update status and potentially clear selection if deleted model was selected
            _uiState.update {
                val newStatuses = it.liteRTModelDownloadStatus.toMutableMap()
                newStatuses[modelInfo.llModel.id] = ModelDownloadStatus.NotDownloaded
                if (it.selectedLiteRTModelId == modelInfo.llModel.id) {
                    newPathForSelected = "" // Clear path if selected model is deleted
                    it.copy(
                        liteRTModelDownloadStatus = newStatuses,
                        selectedLiteRTModelPath = newPathForSelected,
                        // Optionally, select a default model or no model:
                        // selectedLiteRTModelId = SupportedLiteRTModels.all.firstOrNull()?.llModel?.id ?: ""
                    )
                } else {
                    it.copy(liteRTModelDownloadStatus = newStatuses)
                }
            }
             // If selected model path changed (was deleted), update AppSettings
            if (_uiState.value.selectedLiteRTModelId == modelInfo.llModel.id) {
                 saveLiteRTModelSelectionToAppSettings(_uiState.value.selectedLiteRTModelId, newPathForSelected)
            }
        }
    }
}