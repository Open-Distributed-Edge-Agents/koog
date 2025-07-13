package com.jetbrains.example.kotlin_agents_demo_app.screens.mqtt

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jetbrains.example.kotlin_agents_demo_app.theme.AppDimension

@Composable
fun MqttSettingsScreen(
    viewModel: MqttSettingsViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    MqttSettingsScreenContent(
        uiState = uiState,
        onModeChange = viewModel::onModeChange,
        onBrokerIpChange = viewModel::onBrokerIpChange,
        onBrokerPortChange = viewModel::onBrokerPortChange,
        onSaveSettings = {
            viewModel.saveSettings()
            onNavigateBack()
        },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MqttSettingsScreenContent(
    uiState: MqttSettingsUiState,
    onModeChange: (MqttMode) -> Unit,
    onBrokerIpChange: (String) -> Unit,
    onBrokerPortChange: (String) -> Unit,
    onSaveSettings: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MQTT Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppDimension.spacingContentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = uiState.mode == MqttMode.BROKER,
                    onClick = { onModeChange(MqttMode.BROKER) }
                )
                Text("Broker")
                Spacer(modifier = Modifier.width(AppDimension.spacingMedium))
                RadioButton(
                    selected = uiState.mode == MqttMode.CLIENT,
                    onClick = { onModeChange(MqttMode.CLIENT) }
                )
                Text("Client")
            }

            if (uiState.mode == MqttMode.CLIENT) {
                OutlinedTextField(
                    value = uiState.brokerIp,
                    onValueChange = onBrokerIpChange,
                    label = { Text("Broker IP Address") }
                )
                Spacer(modifier = Modifier.height(AppDimension.spacingMedium))
                OutlinedTextField(
                    value = uiState.brokerPort,
                    onValueChange = onBrokerPortChange,
                    label = { Text("Broker Port") }
                )
            }

            Spacer(modifier = Modifier.height(AppDimension.spacingMedium))
            Button(onClick = onSaveSettings) {
                Text("Save")
            }
        }
    }
}
