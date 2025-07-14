package ai.koog.prompt.executor.litert.client

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

public actual class LiteRTClient actual constructor() {
    public actual suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): List<Message.Response> {
        throw NotImplementedError("LiteRT is not supported on this platform")
    }

    public actual fun executeStreaming(
        prompt: Prompt,
        model: LLModel
    ): Flow<String> {
        throw NotImplementedError("LiteRT is not supported on this platform")
    }
}
