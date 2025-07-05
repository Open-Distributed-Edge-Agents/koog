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
import androidx.compose.material.icons.filled.Delete // Added for Delete Icon
import androidx.compose.material3.LinearProgressIndicator // Added for progress bar

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

                // LiteRT Model Management Section
                uiState.availableLiteRTModels.forEach { downloadableModel ->
                    val modelId = downloadableModel.llModel.id
                    val status = uiState.liteRTModelDownloadStatus[modelId] ?: ModelDownloadStatus.NotDownloaded

                    Spacer(modifier = Modifier.height(AppDimension.spacingMedium))
                    Text(downloadableModel.llModel.displayName, style = MaterialTheme.typography.titleSmall)

                    when (status) {
                        is ModelDownloadStatus.NotDownloaded -> {
                            Button(onClick = { viewModel.startDownload(downloadableModel) }) {
                                Text("Download")
                            }
                        }
                        is ModelDownloadStatus.Downloading -> {
                            LinearProgressIndicator(
                                progress = { status.progress }, // Ensure this is a Float
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Downloading: ${(status.progress * 100).toInt()}%")
                        }
                        is ModelDownloadStatus.Downloaded -> {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text("Downloaded at: ${status.path.substringAfterLast('/')}", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.weight(1f))
                                if (uiState.selectedLiteRTModelId == modelId) {
                                    Text(" (Selected)", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                } else {
                                    Button(onClick = { viewModel.selectDownloadedLiteRTModel(downloadableModel) }, modifier = Modifier.padding(end = AppDimension.spacingSmall)) {
                                        Text("Select")
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteLiteRTModel(downloadableModel) }) {
                                    Icon(Icons.Filled.Delete, "Delete Model")
                                }
                            }
                        }
                        is ModelDownloadStatus.Error -> {
                            Text("Error: ${status.message}", color = MaterialTheme.colorScheme.error)
                            Button(onClick = { viewModel.startDownload(downloadableModel) }) {
                                Text("Retry Download")
                            }
                        }
                    }
                    Divider(modifier = Modifier.padding(top = AppDimension.spacingSmall))
                }
                Spacer(modifier = Modifier.height(AppDimension.spacingMedium))

                // Display currently selected model and path (read-only)
                if(uiState.selectedLiteRTModelId.isNotBlank() && uiState.selectedLiteRTModelPath.isNotBlank()){
                    Text("Selected Model: ${uiState.availableLiteRTModels.find{it.llModel.id == uiState.selectedLiteRTModelId}?.llModel?.displayName ?: "None"}", style = MaterialTheme.typography.labelLarge)
                    Text("Model Path: ${uiState.selectedLiteRTModelPath}", style = MaterialTheme.typography.bodySmall)
                } else if (uiState.selectedLiteRTModelId.isNotBlank() && uiState.selectedLiteRTModelPath.isBlank() && uiState.liteRTModelDownloadStatus[uiState.selectedLiteRTModelId] !is ModelDownloadStatus.Downloaded) {
                     Text("Selected Model: ${uiState.availableLiteRTModels.find{it.llModel.id == uiState.selectedLiteRTModelId}?.llModel?.displayName ?: "None"} (File missing or not downloaded)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                }


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
