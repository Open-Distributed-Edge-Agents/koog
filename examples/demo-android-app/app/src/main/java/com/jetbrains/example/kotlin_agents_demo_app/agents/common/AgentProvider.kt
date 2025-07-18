package com.jetbrains.example.kotlin_agents_demo_app.agents.common

import ai.koog.agents.android.AndroidAIAgent
import android.app.Application
import com.jetbrains.example.kotlin_agents_demo_app.settings.AppSettings

/**
 * Interface for agent factory
 */
interface AgentProvider {
    /**
     * Title for the agent demo screen
     */
    val title: String

    /**
     * Description of the agent
     */
    val description: String

    suspend fun provideAgent(
        application: Application,
        appSettings: AppSettings,
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AndroidAIAgent<String, String>
}