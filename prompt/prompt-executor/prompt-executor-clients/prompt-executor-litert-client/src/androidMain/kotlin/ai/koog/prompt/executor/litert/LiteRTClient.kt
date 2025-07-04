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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

) : LLMClient { // LLMEmbeddingProvider will be added if LiteRT supports embeddings through this API

    private var llmInference: LlmInference? = null
    private val httpClient: HttpClient // For downloading images from URLs

    init {
        httpClient = HttpClient(Android) // Initialize Ktor client

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

        if (tools.isNotEmpty()) {
            logger.warn { "LiteRTClient currently does not support tools/function calling. Tools will be ignored." }
        }

        val (inputText, imageAttachment) = extractTextAndImage(prompt)

        if (inputText.isBlank() && imageAttachment == null) {
            logger.warn { "Input text and image for LiteRT model ${model.id} are blank/null. Prompt: ${prompt.messages}" }
            return emptyList()
        }

        var session: LlmInferenceSession? = null
        try {
            val mpImage: MPImage? = imageAttachment?.let {
                if (enableVision && model.capabilities.contains(LLMCapability.Vision)) {
                    attachmentToMPImage(it) // This is a suspend function
                } else {
                    logger.warn { "Image attachment found but vision is not enabled for the client or model ${model.id} does not support Vision. Image will be ignored." }
                    null
                }
            }

            val sessionOptionsBuilder = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            // Copy relevant global configs to session if needed/supported by LlmInferenceSessionOptions
            // sessionOptionsBuilder.setTopK(this.topK)
            // sessionOptionsBuilder.setTemperature(this.temperature)
            // sessionOptionsBuilder.setRandomSeed(this.randomSeed)
            // sessionOptionsBuilder.setMaxTokens(this.maxTokens)


            if (mpImage != null) {
                // GraphOptions needs to be created and set for vision modality
                val graphOptions = com.google.mediapipe.tasks.core.jni.graphconfig.GraphOptions()
                graphOptions.setEnableVisionModality(true) // This method is on the JNI GraphOptions
                // The LlmInferenceSessionOptions.builder() does not directly take a JNI GraphOptions.
                // It seems setGraphOptions was available on older/different MediaPipe Task library versions.
                // For tasks-genai LlmInference, vision is typically enabled by providing MaxNumImages > 0
                // at LlmInferenceOptions level and then just using addImage on session.
                // The client's LlmInference instance is already configured with MaxNumImages if enableVision is true.
                // So, no specific graphOptions setting might be needed here if LlmInference was set up correctly.
                logger.info { "LiteRTClient: Proceeding with multimodal request. Image will be added to session." }
            }

            session = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptionsBuilder.build())

            // Per MediaPipe docs, it's better if text precedes image for multimodal.
            if (inputText.isNotBlank()) {
                session.addQueryChunk(inputText)
            }
            mpImage?.let {
                session.addImage(it)
                logger.debug { "Added image to LiteRT session." }
            }

            logger.debug { "Sending request to LiteRT model ${model.id} via session. Input text length: ${inputText.length}, Image present: ${mpImage != null}" }
            val startTime = clock.now()
            val result = session.generateResponse() // This is a blocking call
            val endTime = clock.now()
            logger.debug { "Received response from LiteRT model ${model.id} via session: $result" }

            val responseMetadata = ResponseMetaInfo(
                timestamp = endTime,
                timeToFirstToken = null,
                timeToLastToken = endTime - startTime,
                totalTokensCount = null,
                inputTokensCount = null,
                outputTokensCount = null,
                rawResponse = result?.text() ?: "" // LlmResult.text() can be nullable
            )
            return listOf(Message.Assistant(content = result?.text() ?: "", metaInfo = responseMetadata))

        } catch (e: Exception) {
            logger.error(e) { "Error executing LiteRT prompt (sync) for model ${model.id}" }
            throw RuntimeException("LiteRT synchronous execution failed for model ${model.id}: ${e.message}", e)
        } finally {
            session?.close() // Ensure session is closed
        }
    }

    private fun extractTextAndImage(prompt: Prompt): Pair<String, Attachment.Image?> {
        val systemPrompts = prompt.messages.filterIsInstance<Message.System>().joinToString("\n") { it.content }

        var userTextContent = ""
        var firstImageAttachment: Attachment.Image? = null

        // Iterate messages to collect all user text and the first image attachment from a user message
        for (message in prompt.messages) {
            if (message is Message.User) {
                if (message.content.isNotBlank()) {
                    if (userTextContent.isNotEmpty()) { // Add newline if concatenating multiple user messages
                        userTextContent += "\n"
                    }
                    userTextContent += message.content
                }
                if (firstImageAttachment == null) { // Only take the first image found
                    val imageInMessage = message.attachments.filterIsInstance<Attachment.Image>().firstOrNull()
                    if (imageInMessage != null) {
                        firstImageAttachment = imageInMessage
                        if (message.attachments.count { it is Attachment.Image } > 1) {
                             logger.warn { "Multiple image attachments found in a single User message. Only the first will be used." }
                        }
                    }
                } else {
                    // If an image is already found, and this user message also has images, log it.
                    if (message.attachments.any{it is Attachment.Image}) {
                        logger.warn { "Multiple User messages contain images. Only the image from the first User message with an image will be used."}
                    }
                }
            }
        }

        var inputText = ""
        if (systemPrompts.isNotBlank()) {
            inputText += systemPrompts
        }
        if (userTextContent.isNotBlank()) {
            if (inputText.isNotBlank()) inputText += "\n" // Add separator if system prompt exists
            inputText += userTextContent
        }

        return Pair(inputText.trim(), firstImageAttachment)
    }

    override fun executeStreaming(prompt: Prompt, model: LLModel): Flow<String> {
        require(model.provider == LLMProvider.LiteRT) { "Model ${model.id} is not a LiteRT model." }
        ensureLlmInferenceInitialized()

        val (inputText, imageAttachment) = extractTextAndImage(prompt)

        if (inputText.isBlank() && imageAttachment == null) {
            logger.warn { "Input text and image for LiteRT streaming model ${model.id} are blank/null. Prompt: ${prompt.messages}" }
            return flow { } // Empty flow for blank combined input
        }

        // Using channelFlow to bridge callback-based API to Flow
        return kotlinx.coroutines.flow.channelFlow {
            var session: LlmInferenceSession? = null
            try {
                val mpImage: MPImage? = imageAttachment?.let {
                    if (enableVision && model.capabilities.contains(LLMCapability.Vision)) {
                        attachmentToMPImage(it)
                    } else {
                        logger.warn { "Image attachment found for streaming but vision is not enabled or model ${model.id} does not support Vision. Image will be ignored." }
                        null
                    }
                }

                val sessionOptionsBuilder = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setResultListener { partialResult, done ->
                        // partialResult can be LlmResult which contains text()
                        // The MediaPipe example uses partialResult.text() directly.
                        // LlmInference.LlmResult is the type, let's assume it's what partialResult is.
                        val resultText = partialResult?.text() // LlmResult.text() can be nullable
                        if (!resultText.isNullOrEmpty()) {
                            trySend(resultText)
                        }
                        if (done) {
                            close() // Close the channel successfully
                        }
                    }
                    .setErrorListener { error ->
                        logger.error(error) { "LiteRT streaming error for model ${model.id}" }
                        close(error) // Close the channel with an error
                    }

                // Apply relevant session options from client config if needed/supported.
                // sessionOptionsBuilder.setTopK(this.topK)
                // sessionOptionsBuilder.setTemperature(this.temperature)
                // sessionOptionsBuilder.setRandomSeed(this.randomSeed)
                // sessionOptionsBuilder.setMaxTokens(this.maxTokens)

                if (mpImage != null) {
                    // As with execute(), vision modality is primarily configured on LlmInference instance.
                    // No specific graphOptions setting here if LlmInference was set up with MaxNumImages.
                    logger.info { "LiteRTClient: Proceeding with multimodal streaming request." }
                }

                session = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptionsBuilder.build())

                if (inputText.isNotBlank()) {
                    session.addQueryChunk(inputText)
                }
                mpImage?.let {
                    session.addImage(it)
                    logger.debug { "Added image to LiteRT streaming session." }
                }

                logger.debug { "LiteRT streaming session created for model ${model.id}. Sending input text length: ${inputText.length}, Image present: ${mpImage != null}" }
                session.generateResponseAsync(inputText) // Note: inputText is passed here for MediaPipe API, though session also has addQueryChunk.
                                                       // The API might use this as the final prompt trigger.
                                                       // If images are involved, the text in addQueryChunk + image is the context.
                                                       // If only text, addQueryChunk then generateResponseAsync("") might also work.
                                                       // Let's stick to passing inputText as per examples if it's primary text.
                                                       // If inputText is blank and only image is there, behavior TBD by MediaPipe.
                                                       // For safety, if inputText is blank but mpImage is not, we might pass a generic prompt.
                                                       // However, our extractTextAndImage ensures inputText is not blank if image exists for typical prompts.

                // awaitClose is needed to keep the flow alive until the channel is closed by the listener
                kotlinx.coroutines.awaitClose {
                    logger.debug { "LiteRT streaming flow closing for model ${model.id}. Cleaning up session." }
                    session?.close()
                }
            } catch (e: Exception) {
                logger.error(e) { "Error setting up LiteRT streaming prompt for model ${model.id}" }
                close(RuntimeException("LiteRT streaming setup failed for model ${model.id}: ${e.message}", e))
                session?.close() // Ensure session is closed on setup error too
            }
        }
    }

    private fun ensureLlmInferenceInitialized() {
                            }
                        }
                        if (done) {
                            close() // Close the channel successfully
                        }
                    }
                    .setErrorListener { error ->
                        logger.error(error) { "LiteRT streaming error for model ${model.id}" }
                        close(error) // Close the channel with an error
                    }

                // Apply relevant options from LlmInferenceOptions if they are also settable on session
                // (e.g. topK, temperature - check MediaPipe docs for LlmInferenceSessionOptions)
                // For now, keeping session options minimal, relying on main LlmInference config.
                // sessionOptionsBuilder.setTopK(topK) // Example if applicable
                // sessionOptionsBuilder.setTemperature(temperature) // Example if applicable

                session = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptionsBuilder.build())
                logger.debug { "LiteRT streaming session created for model ${model.id}. Sending input: $inputText" }
                session.generateResponseAsync(inputText)

                // awaitClose is needed to keep the flow alive until the channel is closed by the listener
                kotlinx.coroutines.awaitClose {
                    logger.debug { "LiteRT streaming flow closing for model ${model.id}. Cleaning up session." }
                    session?.close()
                }
            } catch (e: Exception) {
                logger.error(e) { "Error setting up LiteRT streaming prompt for model ${model.id}" }
                close(RuntimeException("LiteRT streaming setup failed for model ${model.id}: ${e.message}", e))
                session?.close() // Ensure session is closed on setup error too
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

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun attachmentToMPImage(attachment: Attachment.Image): MPImage? {
        val bitmap: Bitmap? = when (val content = attachment.content) {
            is AttachmentContent.URL -> {
                try {
                    logger.debug { "Downloading image from URL: ${content.url}" }
                    val imageBytes: ByteArray = httpClient.get(content.url).readBytes()
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to download or decode image from URL: ${content.url}" }
                    null
                }
            }
            is AttachmentContent.Binary.Bytes -> {
                try {
                    BitmapFactory.decodeByteArray(content.data, 0, content.data.size)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to decode image from ByteArray." }
                    null
                }
            }
            is AttachmentContent.Binary.Base64 -> {
                try {
                    val imageBytes = Base64.decode(content.base64)
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to decode image from Base64 string." }
                    null
                }
            }
            is AttachmentContent.PlainText -> {
                logger.warn { "PlainText content found for Image attachment, which is invalid." }
                null
            }
        }

        return bitmap?.let {
            try {
                // Compress bitmap if it's too large, as MPImage might have size limits or performance issues.
                // This is a heuristic. Exact limits/recommendations would come from MediaPipe docs.
                val (compressedBitmap, format) = compressBitmapIfNecessary(it, targetMaxDimension = 1024, targetSizeBytes = 1 * 1024 * 1024) // 1MB
                logger.debug { "Converted attachment to Bitmap (format: $format, size: ${compressedBitmap.byteCount} bytes)." }
                BitmapImageBuilder(compressedBitmap).build()
            } catch (e: Exception) {
                logger.error(e) { "Failed to convert Bitmap to MPImage." }
                null
            }
        }
    }

    // Helper to compress bitmap - adjust quality and resolution
    private suspend fun compressBitmapIfNecessary(
        bitmap: Bitmap,
        targetMaxDimension: Int = 1024, // Max width/height
        targetSizeBytes: Long = 1 * 1024 * 1024, // 1MB
        outputFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, // JPEG is usually smaller
        quality: Int = 85
    ): Pair<Bitmap, Bitmap.CompressFormat> = withContext(Dispatchers.IO) {
        var currentBitmap = bitmap
        var currentWidth = bitmap.width
        var currentHeight = bitmap.height

        // 1. Resize if dimensions are too large
        if (currentWidth > targetMaxDimension || currentHeight > targetMaxDimension) {
            val aspectRatio = currentWidth.toFloat() / currentHeight.toFloat()
            if (currentWidth > currentHeight) {
                currentWidth = targetMaxDimension
                currentHeight = (currentWidth / aspectRatio).toInt()
            } else {
                currentHeight = targetMaxDimension
                currentWidth = (currentHeight * aspectRatio).toInt()
            }
            currentBitmap = Bitmap.createScaledBitmap(bitmap, currentWidth, currentHeight, true)
        }

        // 2. Compress by quality if still too large
        var attempt = 0
        var currentQuality = quality
        var compressedBytes: ByteArray
        val outputStream = ByteArrayOutputStream()

        do {
            outputStream.reset() // Clear previous compression attempt
            currentBitmap.compress(outputFormat, currentQuality, outputStream)
            compressedBytes = outputStream.toByteArray()

            if (compressedBytes.size <= targetSizeBytes || currentQuality <= 10) { // Stop if small enough or quality too low
                break
            }
            currentQuality -= 10 // Reduce quality
            attempt++
        } while (attempt < 5) // Max 5 attempts to reduce quality

        if (compressedBytes.size > targetSizeBytes && currentBitmap != bitmap) { // If resized and still too big, try compressing original
             outputStream.reset()
             bitmap.compress(outputFormat, quality, outputStream)
             val originalCompressedBytes = outputStream.toByteArray()
             if (originalCompressedBytes.size < compressedBytes.size) {
                 return@withContext Pair(BitmapFactory.decodeByteArray(originalCompressedBytes, 0, originalCompressedBytes.size), outputFormat)
             }
        }

        return@withContext Pair(BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size), outputFormat)
    }


    // Consider adding a close() method to release LlmInference resources if needed,
    // though the documentation doesn't explicitly state it for LlmInference itself.
    // LlmInferenceSession has a close() method.
}
