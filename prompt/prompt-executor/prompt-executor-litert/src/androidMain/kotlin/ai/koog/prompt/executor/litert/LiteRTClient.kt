package ai.koog.prompt.executor.litert

import ai.koog.prompt.dsl.Prompt
import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LiteRTClient(
    context: Context,
    modelPath: String
) {
    private val llmInference: LlmInference

    init {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .build()
        llmInference = LlmInference.createFromOptions(context, options)
    }

    fun generate(prompt: Prompt): Flow<String> = callbackFlow {
        val promptText = prompt.messages.joinToString("\n") { it.content.toString() }
        
        llmInference.generateResponseAsync(promptText) { partialResult, done ->
            trySend(partialResult)
            if (done) {
                close()
            }
        }
        awaitClose { llmInference.close() }
    }
}