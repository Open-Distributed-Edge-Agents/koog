package com.jetbrains.example.kotlin_agents_demo_app.agents.litert

import ai.koog.agents.android.AndroidAIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.node
import ai.koog.agents.core.dsl.extension.nodeStart
import ai.koog.agents.core.dsl.extension.nodeFinish
import ai.koog.agents.core.dsl.extension.edge
import ai.koog.agents.core.dsl.extension.forwardTo
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.PromptExecutor
import com.jetbrains.example.kotlin_agents_demo_app.agents.common.AgentProvider
import com.jetbrains.example.kotlin_agents_demo_app.settings.AppSettings
import ai.koog.prompt.executor.litert.LiteRTClient
import ai.koog.prompt.model.LLMParams
import ai.koog.prompt.model.LLModel
import android.app.Application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class LiteRTPromptExecutor(private val liteRTClient: LiteRTClient) : PromptExecutor {
    override suspend fun execute(prompt: ai.koog.prompt.dsl.Prompt, llmParams: LLMParams): Flow<String> {
        return liteRTClient.generate(prompt)
    }
}

/**
 * Factory for creating LiteRT agents
 */
object LiteRTAgentProvider : AgentProvider {
    override val title: String = "LiteRT"
    override val description: String = "A demo of the LiteRT agent."

    override suspend fun provideAgent(
        application: Application,
        appSettings: AppSettings,
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String,
    ): AndroidAIAgent<String, String> {
        val modelPath = appSettings.getCurrentSettings().liteRtModelPath
        require(modelPath.isNotEmpty()) { "LiteRT model path is not configured." }

        val liteRTClient = LiteRTClient(application, modelPath)
        val executor = LiteRTPromptExecutor(liteRTClient)

        val strategy = strategy(title) {
            val nodeRequestLLM by node<String, String> { input ->
                CoroutineScope(Dispatchers.IO).launch {
                    executor.execute(prompt(input), LLMParams()).collect {
                        onAssistantMessage(it)
                    }
                }
                ""
            }
            edge(nodeStart forwardTo nodeRequestLLM)
            edge(nodeRequestLLM forwardTo nodeFinish)
        }

        return AndroidAIAgent(
            promptExecutor = executor,
            strategy = strategy,
            agentConfig = AIAgentConfig(prompt = prompt(""), model = LLModel("litert")),
        )
    }
}
