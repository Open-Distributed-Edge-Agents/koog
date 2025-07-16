package com.jetbrains.example.kotlin_agents_demo_app.agents.litert

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.llm.litert.LiteRTClient
import com.jetbrains.example.kotlin_agents_demo_app.agents.common.AgentProvider
import com.jetbrains.example.kotlin_agents_demo_app.settings.AppSettings

object LiteRTAgentProvider : AgentProvider {
    override val title: String = "LiteRT Gemma"
    override val description: String = "A LiteRT agent that uses the Gemma model to chat."

    override suspend fun provideAgent(
        appSettings: AppSettings,
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AIAgent {
        val client = LiteRTClient()
        // To be implemented
        return AIAgent(
            client = client,
            model = TODO(),
            tools = emptyList(),
            onToolCallCallback = onToolCallEvent,
            onToolResultCallback = {},
            onModelErrorCallback = onErrorEvent,
            onAssistantMessageCallback = onAssistantMessage
        )
    }
}
