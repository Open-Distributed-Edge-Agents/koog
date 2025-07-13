package ai.koog.prompt.executor.litert

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import kotlinx.coroutines.flow.Flow

expect class LiteRTClient(
    context: Any,
    modelPath: String
) : LLMClient {
    val clientId: String
    override suspend fun execute(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): List<Message.Response>
    override fun executeStreaming(prompt: Prompt, model: LLModel): Flow<String>
}
