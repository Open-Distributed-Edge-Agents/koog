package ai.koog.prompt.executor.litert.client

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.ModerationResult
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.ModerationResult
import ai.koog.prompt.message.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

public class LiteRTClient : LLMClient {

    override suspend fun execute(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): List<Message.Response> {
        require(model.provider == LLMProvider.LiteRT) { "Model not supported by LiteRT" }
        // To be implemented
        return emptyList()
    }

    override fun executeStreaming(prompt: Prompt, model: LLModel): Flow<String> {
        require(model.provider == LLMProvider.LiteRT) { "Model not supported by LiteRT" }
        // To be implemented
        return flow {  }
    }

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult {
        require(model.provider == LLMProvider.LiteRT) { "Model not supported by LiteRT" }
        // To be implemented
        return ModerationResult(false, emptyMap())
    }
}