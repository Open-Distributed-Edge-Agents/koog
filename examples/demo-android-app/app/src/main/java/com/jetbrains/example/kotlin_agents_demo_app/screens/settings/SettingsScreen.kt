package com.jetbrains.example.kotlin_agents_demo_app.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jetbrains.example.kotlin_agents_demo_app.theme.AppDimension
import com.jetbrains.example.kotlin_agents_demo_app.theme.AppTheme

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSaveSettings: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreenContent(
        uiState = uiState,
        viewModel = viewModel, // Pass ViewModel for available models and update functions
        onNavigateBack = onNavigateBack,
        onSaveSettings = {
            viewModel.saveSettings()
            onSaveSettings()
        }
    )
}

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ArrowDropDown

// ... (keep existing imports)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onSaveSettings: () -> Unit
) {
    val providerOptions = listOf(AppSettings.PROVIDER_OPENAI, AppSettings.PROVIDER_LITERT)
    var liteRTModelDropdownExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = MaterialTheme.colorScheme.onSurface) },
                colors = TopAppBarDefaults.topAppBarColors(
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSaveSettings) {
                        Icon(Icons.Default.Check, "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppDimension.spacingContentPadding)
        ) {
            // LLM Provider Selection
            Text(
                text = "LLM Provider",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = AppDimension.spacingMedium)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                providerOptions.forEach { provider ->
                    Row(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = AppDimension.spacingExtraSmall)
                            .clickable { viewModel.updateSelectedProvider(provider) },
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (provider == uiState.selectedProvider),
                            onClick = { viewModel.updateSelectedProvider(provider) }
                        )
                        Text(
                            text = provider,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = AppDimension.spacingSmall)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimension.spacingLarge))

            // OpenAI Specific Settings
            if (uiState.selectedProvider == AppSettings.PROVIDER_OPENAI) {
                Text(
                    text = "OpenAI Settings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = AppDimension.spacingSmall)
                )
                OutlinedTextField(
                    value = uiState.openAiToken,
                    onValueChange = viewModel::updateOpenAiToken,
                    label = { Text("OpenAI API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(AppDimension.spacingMedium))
            }

            // LiteRT Specific Settings
            if (uiState.selectedProvider == AppSettings.PROVIDER_LITERT) {
                Text(
                    text = "LiteRT Settings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = AppDimension.spacingSmall)
                )

                // LiteRT Model Selection Dropdown
                Box {
                    OutlinedTextField(
                        value = viewModel.liteRTAvailableModels.find { it.id == uiState.liteRTModelId }?.displayName ?: "Select Model",
                        onValueChange = { }, // Not directly changeable
                        label = { Text("LiteRT Model") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "Select Model", Modifier.clickable { liteRTModelDropdownExpanded = true }) },
                        modifier = Modifier.fillMaxWidth().clickable { liteRTModelDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = liteRTModelDropdownExpanded,
                        onDismissRequest = { liteRTModelDropdownExpanded = false }
                    ) {
                        viewModel.liteRTAvailableModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.displayName) },
                                onClick = {
                                    viewModel.updateLiteRTModelId(model.id)
                                    liteRTModelDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppDimension.spacingMedium))

                // LiteRT Model Path
                OutlinedTextField(
                    value = uiState.liteRTModelPath,
                    onValueChange = viewModel::updateLiteRTModelPath,
                    label = { Text("LiteRT Model Path (.task file)") },
                    placeholder = { Text("e.g., /data/local/tmp/model.task") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    text = "Absolute path to the .task model file on the device. \nExample for adb: /data/local/tmp/model.task \nApp-specific dir: /Android/data/com.jetbrains.example.kotlin_agents_demo_app/files/your_model.task (use a file manager to place it here).",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = AppDimension.spacingExtraSmall)
                )
                Spacer(modifier = Modifier.height(AppDimension.spacingMedium))
            }

            // Anthropic Token field (conditionally visible if we add Anthropic as a provider choice)
            // For now, it's managed by AppSettingsData but not actively selectable.
            // If you want to show it always for now, or when no specific provider is chosen:
            /*
            Spacer(modifier = Modifier.height(AppDimension.spacingLarge))
            Text(
                text = "Other API Keys (Legacy)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = AppDimension.spacingSmall)
            )
            OutlinedTextField(
                value = uiState.anthropicToken,
                onValueChange = viewModel::updateAnthropicToken,
                label = { Text("Anthropic Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            */
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Preview(showBackground = true)
@Composable
fun SettingsScreenContentPreview() {
    AppTheme {
        // This preview will need a mock SettingsViewModel or more complex setup.
        // For a basic preview, we pass a default uiState.
        // A real ViewModel instance might be problematic for previews if it has external dependencies (like Application context).
        val previewUiState = SettingsUiState(
            isLoading = false,
            selectedProvider = AppSettings.PROVIDER_LITERT,
            openAiToken = "preview_openai_token",
            liteRTModelId = LiteRTModels.Gemma3n.E2B.id,
            liteRTModelPath = "/path/to/model.task"
        )
        // In a real scenario, you might need a fake ViewModel for previews or use a library for previewing ViewModels.
        // For now, we'll assume a basic ViewModel can be instantiated or provide a simpler preview.
        SettingsScreenContent(
            uiState = previewUiState,
            // Creating a real ViewModel here for preview might fail if it needs specific Android dependencies not available in preview.
            // It's often better to make Composable functions take only the state they need and lambdas for events.
            viewModel = viewModel(), // This is okay if ViewModel has no complex init dependencies for preview.
            onNavigateBack = {},
            onSaveSettings = {}
        )
    }
}
