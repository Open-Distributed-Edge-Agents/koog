package com.jetbrains.example.kotlin_agents_demo_app.agents.litert

import ai.koog.agents.Agent
import ai.koog.prompt.executor.PromptExecutor
import ai.koog.prompt.executor.litert.client.LiteRTClient
import ai.koog.prompt.executor.llms.DefaultPromptExecutor
import ai.koog.prompt.llm.LLMProvider
import com.jetbrains.example.kotlin_agents_demo_app.agents.AgentProvider

object LiteRTAgentProvider : AgentProvider {
    override val name: String = "LiteRT Agent"

    override fun getAgent(): Agent {
        val executor: PromptExecutor = DefaultPromptExecutor(
            mapOf(
                LLMProvider.LiteRT to LiteRTClient()
            )
        )
        // To be implemented
        return Agent(executor)
    }
}
