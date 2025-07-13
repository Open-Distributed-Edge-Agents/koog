package ai.koog.prompt.executor.litert

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class LiteRTClient actual constructor(
    private val context: Any,
    private val modelPath: String
) : LLMClient {

    actual val clientId: String = "js-dummy-client-id"

    override suspend fun execute(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): List<Message.Response> {
        throw NotImplementedError("LiteRT is not supported on this platform")
    }

    override fun executeStreaming(prompt: Prompt, model: LLModel): Flow<String> {
        return flow {
            throw NotImplementedError("LiteRT is not supported on this platform")
        }
    }
}
