package com.jetbrains.example.kotlin_agents_demo_app.agents.litert

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.litert.LiteRTClient
import ai.koog.prompt.llm.LiteRTModels
import com.jetbrains.example.kotlin_agents_demo_app.agents.common.AgentProvider
import com.jetbrains.example.kotlin_agents_demo_app.settings.AppSettings

object LiteRTAgentProvider : AgentProvider {
    override val title: String = "LiteRT"
    override val description: String = "Hi, I'm a LiteRT agent. I'm running locally on your device."

    override suspend fun provideAgent(
        appSettings: AppSettings,
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AIAgent {
        val context = appSettings.getApplicationContext()
        val modelPath = "/data/local/tmp/llm/model.task" // TODO: make this configurable
        val executor = LiteRTClient(context, modelPath)

        val strategy = strategy(title) {
            val nodeRequestLLM by nodeLLMRequest()
            val nodeAssistantMessage by node<String, String> { message -> onAssistantMessage(message) }

            edge(nodeStart forwardTo nodeRequestLLM)
            edge(nodeRequestLLM forwardTo nodeAssistantMessage)
            edge(nodeAssistantMessage forwardTo nodeRequestLLM)
        }

        val agentConfig = AIAgentConfig(
            prompt = prompt("chat") {
                system("You are a helpful assistant.")
            },
            model = LiteRTModels.Gemma.GEMMA_3_1B
        )

        return AIAgent(
            promptExecutor = executor,
            strategy = strategy,
            agentConfig = agentConfig
        )
    }
}
