package com.jetbrains.example.kotlin_agents_demo_app.screens.mqtt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.example.kotlin_agents_demo_app.settings.AppSettings
import com.jetbrains.example.kotlin_agents_demo_app.settings.MqttSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MqttMode {
    BROKER,
    CLIENT
}

data class MqttSettingsUiState(
    val mode: MqttMode = MqttMode.CLIENT,
    val brokerIp: String = "",
    val brokerPort: String = "",
    val isLoading: Boolean = true
)

class MqttSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings(application)

    private val _uiState = MutableStateFlow(MqttSettingsUiState())
    val uiState: StateFlow<MqttSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = appSettings.getMqttSettings()
            _uiState.value = MqttSettingsUiState(
                mode = if (settings.isBroker) MqttMode.BROKER else MqttMode.CLIENT,
                brokerIp = settings.brokerIp,
                brokerPort = settings.brokerPort.toString(),
                isLoading = false
            )
        }
    }

    fun onModeChange(mode: MqttMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    fun onBrokerIpChange(ip: String) {
        _uiState.value = _uiState.value.copy(brokerIp = ip)
    }

    fun onBrokerPortChange(port: String) {
        _uiState.value = _uiState.value.copy(brokerPort = port)
    }

    fun saveSettings() {
        viewModelScope.launch {
            val currentSettings = _uiState.value
            appSettings.setMqttSettings(
                MqttSettings(
                    isBroker = currentSettings.mode == MqttMode.BROKER,
                    brokerIp = currentSettings.brokerIp,
                    brokerPort = currentSettings.brokerPort.toIntOrNull() ?: 1883
                )
            )
        }
    }
}
