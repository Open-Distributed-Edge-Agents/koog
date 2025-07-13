package com.jetbrains.example.kotlin_agents_demo_app.screens.start

import androidx.lifecycle.ViewModel
import com.jetbrains.example.kotlin_agents_demo_app.NavRoute
import com.jetbrains.example.kotlin_agents_demo_app.agents.litert.LiteRTAgentProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StartUiState(
    val demoCards: List<CardItem> = listOf(
        CardItem(
            title = "Calculator",
            description = "A calculator agent that can solve math problems. Ask it any calculation and get the result.",
            agentDemoRoute = NavRoute.AgentDemoRoute.CalculatorScreen
        ),
        CardItem(
            title = "Weather Forecast",
            description = "A weather agent that can provide forecasts for any location. Ask about weather conditions, dates, and more.",
            agentDemoRoute = NavRoute.AgentDemoRoute.WeatherScreen
        ),
        CardItem(
            title = "LiteRT",
            description = "A LiteRT agent that runs locally on your device.",
            agentDemoRoute = NavRoute.AgentDemoRoute.LiteRTScreen
        ),
    )
)

data class CardItem(
    val title: String,
    val description: String,
    val agentDemoRoute: NavRoute.AgentDemoRoute? = null,
)

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel

class StartViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(StartUiState())
    val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

    fun downloadModel(modelName: String) {
        val downloadManager = getApplication<Application>().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse("https://huggingface.co/google/$modelName/resolve/main/model.tflite")
        val request = DownloadManager.Request(uri)
            .setTitle("$modelName.task")
            .setDescription("Downloading $modelName")
            .setDestinationInExternalFilesDir(getApplication(), null, "$modelName.task")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        downloadManager.enqueue(request)
    }
}
