package ai.koog.prompt.executor.litert

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.LLMEmbeddingProvider // Placeholder for future
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.message.ToolCall
import ai.koog.prompt.message.ToolResponseMessage
import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

private val logger = KotlinLogging.logger {}

class LiteRTClient(
    private val context: Context,
    private val modelPath: String,
    private val clock: Clock = Clock.System,
    private val maxTokens: Int = 512,
    private val topK: Int = 40,
    private val temperature: Float = 0.8f,
    private val randomSeed: Int = 0,
    private val enableVision: Boolean = false, // New configuration for vision
    private val maxNumImages: Int = 1 // Default based on Gemma-3n documentation
    // Add other options like loraPath, resultListener, errorListener if needed later
) : LLMClient { // LLMEmbeddingProvider will be added if LiteRT supports embeddings through this API

    private var llmInference: LlmInference? = null

    init {
        try {
            val optionsBuilder = LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(maxTokens)
                .setTopK(topK)
                .setTemperature(temperature)
                .setRandomSeed(randomSeed)

            if (enableVision) {
                val graphOptions = com.google.mediapipe.tasks.core.jni.graphconfig.GraphOptions()
                graphOptions.setEnableVisionModality(true)
                // Note: The MediaPipe documentation shows GraphOptions being built and set via LlmInferenceSession.
                // However, LlmInferenceOptions itself might not directly take GraphOptions in the same way.
                // Let's assume for now this needs to be handled at the session level or that LlmInferenceOptions evolves.
                // For now, we'll focus on what LlmInferenceOptions directly supports or what might be a general task option.
                // The `setMaxNumImages` is part of LlmInferenceOptions.
                optionsBuilder.setMaxNumImages(maxNumImages)
                logger.info { "LiteRTClient: Vision modality support configured with maxNumImages: $maxNumImages." }
                // If direct GraphOptions setting on LlmInferenceOptions is not available,
                // this logic might need to move to where LlmInferenceSession is created,
                // or we might need a different approach for global vision enabling.
                // For now, setting MaxNumImages is a direct option.
                // The actual enabling of vision modality might be implicit with MaxNumImages > 0 or specific to session.
            }

            // TODO: Add .setResultListener for async streaming if needed here or per-call
            // TODO: Add .setErrorListener for error handling

            llmInference = LlmInference.createFromOptions(context, optionsBuilder.build())
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize LiteRT LlmInference for model: $modelPath" }
            throw IllegalStateException("Failed to initialize LiteRT LlmInference: ${e.message}", e)
        }
    }

    override suspend fun execute(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): List<Message.Response> {
        require(model.provider == LLMProvider.LiteRT) { "Model ${model.id} is not a LiteRT model." }
        ensureLlmInferenceInitialized()

        // TODO: Handle tools if LiteRT supports them. For now, ignoring.
        if (tools.isNotEmpty()) {
            logger.warn { "LiteRTClient currently does not support tools/function calling. Tools will be ignored." }
        }

        // TODO: Handle multimodal prompts (images) if model.capabilities includes Vision
        // For now, concatenating text content from all messages.
    // Refined approach: Concatenate System prompts, then User prompts.
    // This is a common basic approach for single-string prompt APIs.
    val systemPrompts = prompt.messages.filterIsInstance<Message.System>().joinToString("\n") { it.content }
    val userPrompts = prompt.messages.filterIsInstance<Message.User>().joinToString("\n") { it.content }
    // TODO: Consider how/if Message.Assistant or ToolResponseMessage from prior turns should be included.
    // For basic text input, system + user is a common starting point.
    // The LiteRT generateResponse(String) API likely expects a user's question or instruction.

    var inputText = ""
    if (systemPrompts.isNotBlank()) {
        inputText += systemPrompts + "\n"
    }
    inputText += userPrompts

    // If after filtering, inputText is blank (e.g. only assistant messages), then this is an issue.
    // However, a valid Prompt for execution should typically have user content.
    if (inputText.isBlank()) {
        logger.warn { "Input text for LiteRT model ${model.id} is blank after filtering messages. Prompt was: ${prompt.messages}" }
        // Decide on behavior: throw error, or return empty response?
        // For now, let proceed and let LiteRT handle blank input if it occurs, or fail.
        // A more robust check might be needed depending on LiteRT's behavior with empty strings.
        }

        return try {
            logger.debug { "Sending request to LiteRT model ${model.id} with input: $inputText" }
            val startTime = clock.now()
            val result = llmInference!!.generateResponse(inputText)
            val endTime = clock.now()
            logger.debug { "Received response from LiteRT model ${model.id}: $result" }

            // Assuming result is a simple text response.
            // Token counts are not directly available from generateResponse.
            // We might need to use a tokenizer or rely on future API enhancements for this.
            val responseMetadata = ResponseMetaInfo(
                timestamp = endTime,
                timeToFirstToken = null, // Not available from LlmInference.generateResponse()
                timeToLastToken = endTime - startTime, // Approximation for sync call
                totalTokensCount = null, // Not available from LiteRT API
                inputTokensCount = null, // Not available from LiteRT API
                outputTokensCount = null, // Not available from LiteRT API
                rawResponse = result
            )
            listOf(Message.Assistant(content = result, metaInfo = responseMetadata))
        } catch (e: Exception) {
            logger.error(e) { "Error executing LiteRT prompt for model ${model.id}" }
            throw RuntimeException("LiteRT execution failed for model ${model.id}: ${e.message}", e)
        }
    }

    override fun executeStreaming(prompt: Prompt, model: LLModel): Flow<String> {
        require(model.provider == LLMProvider.LiteRT) { "Model ${model.id} is not a LiteRT model." }
        ensureLlmInferenceInitialized()

        // TODO: Handle multimodal prompts (images) if model.capabilities includes Vision
        val inputText = prompt.messages.joinToString(separator = "\n") { message ->
            when (message) {
                is Message.System -> message.content
                is Message.User -> message.content
                is Message.Assistant -> message.content
                is ToolResponseMessage -> message.content
                is ToolCall -> "Tool call: ${message.toolName} with args ${message.arguments}"
            }
        }

        return flow {
            try {
                llmInference!!.generateResponseAsync(inputText)
                // The actual emission will happen in the ResultListener defined during LlmInference init or per-call.
                // This flow will be connected to that listener.
                // For now, this is a placeholder structure.
                // We need to manage the listener and flow completion properly.
                // This will require a more complex setup with Channels or Callbacks.
                logger.info { "Async generation started for LiteRT model ${model.id}. Implement listener handling." }
                // Simulate receiving chunks for now - THIS IS A PLACEHOLDER
                // emit("Placeholder: Async response part 1 for $inputText")
                // kotlinx.coroutines.delay(100)
                // emit("Placeholder: Async response part 2 for $inputText")
                // For a real implementation, see how OllamaClient uses callbacks with flows.
                throw NotImplementedError("LiteRT streaming is not fully implemented yet. Listener setup required.")
            } catch (e: Exception) {
                logger.error(e) { "Error executing LiteRT streaming prompt for model ${model.id}" }
                // Rethrow or emit an error state if the flow should communicate this
                 throw RuntimeException("LiteRT streaming execution failed for model ${model.id}: ${e.message}", e)
            }
        }
    }

    private fun ensureLlmInferenceInitialized() {
        if (llmInference == null) {
            // This should ideally not happen if constructor succeeded, but as a safeguard:
             try {
                val optionsBuilder = LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(maxTokens)
                    .setTopK(topK)
                    .setTemperature(temperature)
                    .setRandomSeed(randomSeed)
                llmInference = LlmInference.createFromOptions(context, optionsBuilder.build())
                logger.info { "Re-initialized LiteRT LlmInference for model: $modelPath" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to re-initialize LiteRT LlmInference for model: $modelPath" }
                throw IllegalStateException("LiteRT LlmInference was not initialized and re-initialization failed: ${e.message}", e)
            }
        }
    }

    // Consider adding a close() method to release LlmInference resources if needed,
    // though the documentation doesn't explicitly state it for LlmInference itself.
    // LlmInferenceSession has a close() method.
}
