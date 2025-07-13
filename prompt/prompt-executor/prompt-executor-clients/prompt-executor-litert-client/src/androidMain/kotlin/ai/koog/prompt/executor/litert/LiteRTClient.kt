package ai.koog.prompt.executor.litert

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

import java.util.UUID

actual class LiteRTClient actual constructor(
    private val context: Any,
    private val modelPath: String
) : LLMClient {

    actual val clientId: String = UUID.randomUUID().toString()
    private var llmInference: LlmInference? = null

    private fun getLlmInference(resultListener: LlmInference.ErrorListener, partialResultListener: (partialResult: String, done: Boolean) -> Unit): LlmInference {
        if (llmInference == null) {
            val options = LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setResultListener(partialResultListener)
                .setErrorListener(resultListener)
                .build()
            llmInference = LlmInference.createFromOptions(context as Context, options)
        }
        return llmInference as LlmInference
    }

    override suspend fun execute(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): List<Message.Response> {
        require(model.provider == LLMProvider.LiteRT) { "Model not supported by LiteRT" }
        val llmInference = getLlmInference({ _ -> }, { _, _ -> })
        val result = llmInference.generateResponse(prompt.render())
        return listOf(Message.Assistant(result))
    }

    override fun executeStreaming(prompt: Prompt, model: LLModel): Flow<String> {
        require(model.provider == LLMProvider.LiteRT) { "Model not supported by LiteRT" }
        return callbackFlow {
            val llmInference = getLlmInference(
                { error -> close(error) },
                { partialResult, done ->
                    trySend(partialResult)
                    if (done) {
                        close()
                    }
                }
            )
            llmInference.generateResponseAsync(prompt.render())
            awaitClose { llmInference.close() }
        }
    }
}
