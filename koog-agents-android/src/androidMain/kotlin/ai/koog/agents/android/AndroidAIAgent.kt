package ai.koog.agents.android

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.AgentOutput
import ai.koog.agents.core.agent.AgentState
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.AgentFeature
import ai.koog.agents.core.dsl.builder.AgentLogic
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.PromptExecutor

class AndroidAIAgent<I, O>(
    promptExecutor: PromptExecutor,
    strategy: AgentLogic<I, O>,
    agentConfig: AIAgentConfig,
    toolRegistry: ToolRegistry = ToolRegistry.EMPTY,
    vararg features: AgentFeature<*, *>
) : AIAgent<I, O>(promptExecutor, strategy, agentConfig, toolRegistry, *features) {
    suspend fun run(input: I): O? {
        start(input)
        while (state !is AgentState.Finished) {
            tick()
        }
        return (output as? AgentOutput.Exit)?.result
    }
}