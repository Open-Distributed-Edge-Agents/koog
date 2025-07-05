package com.jetbrains.example.kotlin_agents_demo_app.agents.weather

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.llm.LiteRTModels // Added import
import ai.koog.prompt.llm.LLMCapability // Added import
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.jetbrains.example.kotlin_agents_demo_app.agents.common.AgentProvider
import com.jetbrains.example.kotlin_agents_demo_app.agents.common.ExitTool
import com.jetbrains.example.kotlin_agents_demo_app.settings.AppSettings
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Factory for creating weather forecast agents
 */
object WeatherAgentProvider : AgentProvider {
    override val title: String = "Weather Forecast"
    override val description: String = "Hi, I'm a weather agent. I can provide weather forecasts for any location."

    override suspend fun provideAgent(
        appSettings: AppSettings,
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String,
    ): AIAgent {
        val currentSettings = appSettings.getCurrentSettings()
        val promptExecutor: ai.koog.prompt.executor.model.PromptExecutor
        val agentModel: ai.koog.prompt.llm.LLModel

        when (currentSettings.selectedProvider) {
            AppSettings.PROVIDER_LITERT -> {
                require(currentSettings.liteRTModelPath.isNotBlank()) { "LiteRT model path is not configured." }
                val liteRTModelId = currentSettings.liteRTModelId.ifEmpty {
                    // Default to first available LiteRT model if not set - this should ideally be handled by SettingsViewModel defaulting
                    ai.koog.prompt.llm.LiteRTModels.Gemma3n.E2B.id
                }
                agentModel = listOf(ai.koog.prompt.llm.LiteRTModels.Gemma3n.E2B, ai.koog.prompt.llm.LiteRTModels.Gemma3n.E4B)
                    .find { it.id == liteRTModelId }
                    ?: throw IllegalArgumentException("Selected LiteRT model ID '$liteRTModelId' not found.")

                val enableVisionForClient = agentModel.capabilities.contains(ai.koog.prompt.llm.LLMCapability.Vision)

                val liteRTClient = ai.koog.prompt.executor.litert.LiteRTClient(
                    context = appSettings.context,
                    modelPath = currentSettings.liteRTModelPath,
                    enableVision = enableVisionForClient
                    // other LiteRTClient params can be exposed in AppSettings if needed
                )
                promptExecutor = ai.koog.prompt.executor.llms.SingleLLMPromptExecutor(liteRTClient)
            }
            AppSettings.PROVIDER_OPENAI -> {
                require(currentSettings.openAiToken.isNotBlank()) { "OpenAI API key is not configured." }
                promptExecutor = simpleOpenAIExecutor(currentSettings.openAiToken)
                agentModel = OpenAIModels.Chat.GPT4o // Or make this configurable in AppSettings too
            }
            else -> throw IllegalArgumentException("Unsupported LLM provider selected: ${currentSettings.selectedProvider}")
        }


        // Create tool registry with weather tools
        val toolRegistry = ToolRegistry {
            tool(WeatherTools.CurrentDatetimeTool)
            tool(WeatherTools.AddDatetimeTool)
            tool(WeatherTools.WeatherForecastTool)

            tool(ExitTool)
        }

        val strategy = strategy(title) {
            val nodeRequestLLM by nodeLLMRequestMultiple()
            val nodeAssistantMessage by node<String, String> { message -> onAssistantMessage(message) }
            val nodeExecuteToolMultiple by nodeExecuteMultipleTools(parallelTools = true)
            val nodeSendToolResultMultiple by nodeLLMSendMultipleToolResults()
            val nodeCompressHistory by nodeLLMCompressHistory<List<ReceivedToolResult>>()

            edge(nodeStart forwardTo nodeRequestLLM)

            edge(
                nodeRequestLLM forwardTo nodeExecuteToolMultiple
                    onMultipleToolCalls { true }
            )

            edge(
                nodeRequestLLM forwardTo nodeAssistantMessage
                    transformed { it.first() }
                    onAssistantMessage { true }
            )

            edge(nodeAssistantMessage forwardTo nodeRequestLLM)

            // Finish condition - if exit tool is called, go to nodeFinish with tool call result.
            edge(
                nodeExecuteToolMultiple forwardTo nodeFinish
                    onCondition  { it.singleOrNull()?.tool == ExitTool.name }
                    transformed { it.single().result!!.toStringDefault() }
            )

            edge(
                (nodeExecuteToolMultiple forwardTo nodeCompressHistory)
                    onCondition { _ -> llm.readSession { prompt.messages.size > 100 } }
            )

            edge(nodeCompressHistory forwardTo nodeSendToolResultMultiple)

            edge(
                (nodeExecuteToolMultiple forwardTo nodeSendToolResultMultiple)
                    onCondition { _ -> llm.readSession { prompt.messages.size <= 100 } }
            )

            edge(
                (nodeSendToolResultMultiple forwardTo nodeExecuteToolMultiple)
                    onMultipleToolCalls { true }
            )

            edge(
                nodeSendToolResultMultiple forwardTo nodeAssistantMessage
                    transformed { it.first() }
                    onAssistantMessage { true }
            )

        }

        // Create agent config with proper prompt
        val agentConfig = AIAgentConfig(
            prompt = prompt("test") {
                system(
                    """
                    You are a helpful weather assistant.
                    You can provide weather forecasts for any location in the world and help the user plan their activities.
                    
                    Use the tools at your disposal to:
                    1. Get the current date and time
                    2. Add days, hours, or minutes to a date
                    3. Get weather forecasts for specific locations and dates
                    
                    ALWAYS USE current_datetime and add_datetime tools to perform date operations, do not try to guess.
                    
                    When providing weather forecasts, be helpful and informative, explaining the weather conditions in a clear way.
                    """.trimIndent()
                )
            },
            model = agentModel, // Use the dynamically selected model
            maxAgentIterations = 50
        )

        // Create the runner
        return AIAgent(
            promptExecutor = promptExecutor, // Use the dynamically created executor
            strategy = strategy,
            agentConfig = agentConfig,
            toolRegistry = toolRegistry,
        ) {
            handleEvents {
                onToolCall { tool: Tool<*, *>, toolArgs: Tool.Args ->
                    onToolCallEvent("Tool ${tool.name}, args $toolArgs")
                }

                @OptIn(ExperimentalUuidApi::class)
                onAgentRunError { strategyName: String, sessionUuid: Uuid?, throwable: Throwable ->
                    onErrorEvent("${throwable.message}")
                }

                onAgentFinished { strategyName: String, result: String? ->
                    // Skip finish event handling
                }
            }
        }
    }
}