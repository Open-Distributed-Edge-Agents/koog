package com.jetbrains.example.kotlin_agents_demo_app.screens.start

import androidx.lifecycle.ViewModel
import com.jetbrains.example.kotlin_agents_demo_app.NavRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StartUiState(
    val demoCards: List<CardItem> = emptyList()
)

data class CardItem(
    val title: String,
    val description: String,
    val agentDemoRoute: NavRoute? = null,
)

class StartViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StartUiState())
    val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()
}