package ai.koog.prompt.executor.litert

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.llm.LiteRTModels
import ai.koog.prompt.message.Message
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

// IMPORTANT: To run these tests, a LiteRT model file (e.g., gemma-1b-it-cpu-float16.task renamed)
// must be pushed to the device/emulator at the path specified by TEST_MODEL_PATH.
// Example: adb push your_model.task /data/local/tmp/test_gemma_model.task
// The LiteRT library and its dependencies must also work on the target emulator/device.
// Documentation suggests emulators are not reliably supported for LiteRT.
const val TEST_MODEL_PATH = "/data/local/tmp/test_gemma_model.task" // Adjust if needed

@RunWith(AndroidJUnit4::class)
class LiteRTClientTest {

    private lateinit var appContext: Context
    private lateinit var client: LiteRTClient

    @Before
    fun setup() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        // Basic client initialization for most tests. Specific tests can re-initialize.
        // This will fail if the model is not at TEST_MODEL_PATH, skipping other tests.
        // Consider using a @Rule or a more flexible setup if model presence is intermittent.
        try {
            client = LiteRTClient(
                context = appContext,
                modelPath = TEST_MODEL_PATH
            )
        } catch (e: Exception) {
            // Make it very clear if setup failed due to model not found or other LiteRT issues
            throw IllegalStateException(
                "Failed to initialize LiteRTClient in setup. " +
                        "Ensure model is at $TEST_MODEL_PATH and LiteRT is supported on this device/emulator. Error: ${e.message}", e
            )
        }
    }

    @Test
    fun testClientInitialization_Successful() {
        // If setup() succeeded, client is not null.
        assertNotNull("LiteRTClient should be initialized", client)
    }

    @Test
    @Ignore("This test requires a valid model at $TEST_MODEL_PATH and a functional LiteRT runtime on the device/emulator.")
    fun testExecute_SimpleTextPrompt_ReturnsResponse() = runBlocking {
        val testPrompt = prompt {
            user("What is the capital of France?")
        }
        val model = LiteRTModels.Gemma3n.E2B // Or any other defined LiteRTModel

        val responses = client.execute(testPrompt, model, emptyList())

        assertNotNull("Response list should not be null", responses)
        assertTrue("Response list should not be empty", responses.isNotEmpty())
        val assistantMessage = responses.first() as? Message.Assistant
        assertNotNull("Response should be an Assistant message", assistantMessage)
        assertTrue("Assistant message content should not be empty", assistantMessage!!.content.isNotBlank())
        // We can't assert specific content like "Paris" as it depends on the actual model's output.
        println("LiteRT Response: ${assistantMessage.content}")
    }

    @Test
    @Ignore("This test requires a valid model at $TEST_MODEL_PATH and a functional LiteRT runtime on the device/emulator.")
    fun testExecute_PromptWithSystemAndUserMessage_ReturnsResponse() = runBlocking {
        val testPrompt = prompt {
            system("You are a helpful assistant.")
            user("Tell me a short joke.")
        }
        val model = LiteRTModels.Gemma3n.E2B

        val responses = client.execute(testPrompt, model, emptyList())

        assertNotNull(responses)
        assertTrue(responses.isNotEmpty())
        val assistantMessage = responses.first() as? Message.Assistant
        assertNotNull(assistantMessage)
        assertTrue(assistantMessage!!.content.isNotBlank())
        println("LiteRT Response (joke): ${assistantMessage.content}")
    }

    @Test
    fun testClientInitialization_InvalidModelPath_ThrowsException() {
        val invalidModelPath = "/data/local/tmp/non_existent_model.task"
        try {
            LiteRTClient(context = appContext, modelPath = invalidModelPath)
            fail("Initialization with invalid model path should throw an exception.")
        } catch (e: IllegalStateException) {
            // Expected exception
            assertTrue(e.message?.contains("Failed to initialize LiteRT LlmInference") == true)
        } catch (e: Exception) {
            fail("Unexpected exception type: ${e.javaClass.simpleName}. Expected IllegalStateException.")
        }
    }

    // TODO: Add test for blank input string if LiteRTClient.execute is modified to proactively handle it.
    // For now, if inputText is blank, it will be passed to LlmInference.generateResponse("").
    // The behavior of LlmInference with blank input is unknown and might vary (empty response or error).

    @Test
    @Ignore("This test requires a valid model at $TEST_MODEL_PATH and a functional LiteRT runtime on the device/emulator.")
    fun testExecuteStreaming_SimpleTextPrompt_ReceivesContentAndCompletes() = runBlocking {
        val testPrompt = prompt {
            user("Write a short sentence about a cat.")
        }
        val model = LiteRTModels.Gemma3n.E2B

        val receivedChunks = mutableListOf<String>()
        var flowCompleted = false
        var flowThrewError: Throwable? = null

        try {
            client.executeStreaming(testPrompt, model)
                .collect { chunk ->
                    println("Stream chunk: $chunk")
                    receivedChunks.add(chunk)
                    assertTrue("Streamed chunk should not be empty", chunk.isNotEmpty())
                }
            flowCompleted = true
        } catch (e: Throwable) {
            flowThrewError = e
        }

        assertNull("Flow should not throw an error: $flowThrewError", flowThrewError)
        assertTrue("Flow should complete", flowCompleted)
        assertTrue("Should receive at least one chunk", receivedChunks.isNotEmpty())

        val fullResponse = receivedChunks.joinToString("")
        assertTrue("Full streamed response should not be blank", fullResponse.isNotBlank())
        println("Full LiteRT Streamed Response: $fullResponse")
    }

    @Test
    @Ignore("This test requires a valid model at $TEST_MODEL_PATH and a functional LiteRT runtime on the device/emulator.")
    fun testExecuteStreaming_BlankInputText_CompletesWithNoEmissions() = runBlocking {
        // This test assumes LiteRTClient is modified or inherently produces blank inputText for such a prompt
        val emptyContentPrompt = prompt {
            // No user or system messages that would produce text
            assistant("This should not be part of input")
         }
        val model = LiteRTModels.Gemma3n.E2B

        val receivedChunks = mutableListOf<String>()
        var flowCompleted = false
        var flowThrewError: Throwable? = null

        // Modify client to ensure inputText is blank for this specific prompt, or ensure prompt leads to it.
        // For this test, we rely on the current inputText logic in LiteRTClient:
        // systemPrompts = ""
        // userPrompts = ""
        // -> inputText = ""

        try {
            client.executeStreaming(emptyContentPrompt, model)
                .collect { chunk ->
                    receivedChunks.add(chunk)
                }
            flowCompleted = true
        } catch (e: Throwable) {
            flowThrewError = e
        }

        assertNull("Flow should not throw an error for blank input: $flowThrewError", flowThrewError)
        assertTrue("Flow should complete even for blank input", flowCompleted)
        assertTrue("Should receive no chunks for blank input", receivedChunks.isEmpty())
        println("Streaming with blank input completed with no emissions, as expected.")
    }

    // --- Multimodal Tests ---

    private fun createDummyImageAttachment(width: Int = 64, height: Int = 64, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG): Attachment.Image {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLUE
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawPaint(paint) // Fill with a color

        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(format, 80, outputStream)
        val byteArray = outputStream.toByteArray()

        val imageFormatStr = when (format) {
            Bitmap.CompressFormat.JPEG -> "jpeg"
            Bitmap.CompressFormat.PNG -> "png"
            else -> "jpeg" // default
        }
        return Attachment.Image(
            content = AttachmentContent.Binary.Bytes(byteArray),
            format = imageFormatStr,
            mimeType = "image/$imageFormatStr"
        )
    }

    @Test
    @Ignore("This test requires a vision-capable model at $TEST_MODEL_PATH and functional LiteRT runtime.")
    fun testExecute_Multimodal_TextAndImage_ReturnsResponse() = runBlocking {
        val clientWithVision = LiteRTClient(
            context = appContext,
            modelPath = TEST_MODEL_PATH,
            enableVision = true // Crucial for this test
        )
        val imageAttachment = createDummyImageAttachment()
        val testPrompt = prompt {
            user("Describe this image.", imageAttachment)
        }
        // Ensure the model used has LLMCapability.Vision
        val model = LiteRTModels.Gemma3n.E2B.copy(capabilities = listOf(LLMCapability.Vision, LLMCapability.Text))


        val responses = clientWithVision.execute(testPrompt, model, emptyList())

        assertNotNull("Response list should not be null", responses)
        assertTrue("Response list should not be empty", responses.isNotEmpty())
        val assistantMessage = responses.first() as? Message.Assistant
        assertNotNull("Response should be an Assistant message", assistantMessage)
        assertTrue("Assistant message content should not be empty", assistantMessage!!.content.isNotBlank())
        println("LiteRT Multimodal Response: ${assistantMessage.content}")
    }

    @Test
    @Ignore("This test requires a vision-capable model at $TEST_MODEL_PATH and functional LiteRT runtime.")
    fun testExecute_Multimodal_ImageOnly_ReturnsResponse() = runBlocking {
        val clientWithVision = LiteRTClient(
            context = appContext,
            modelPath = TEST_MODEL_PATH,
            enableVision = true
        )
        val imageAttachment = createDummyImageAttachment()
        val testPrompt = prompt {
            user(imageAttachment) // User message with only an image
        }
        val model = LiteRTModels.Gemma3n.E2B.copy(capabilities = listOf(LLMCapability.Vision, LLMCapability.Text))

        val responses = clientWithVision.execute(testPrompt, model, emptyList())

        assertNotNull(responses)
        assertTrue(responses.isNotEmpty())
        val assistantMessage = responses.first() as? Message.Assistant
        assertNotNull(assistantMessage)
        assertTrue(assistantMessage!!.content.isNotBlank())
        println("LiteRT Multimodal (Image Only) Response: ${assistantMessage.content}")
    }


    @Test
    @Ignore("This test requires a vision-capable model at $TEST_MODEL_PATH and functional LiteRT runtime.")
    fun testExecuteStreaming_Multimodal_TextAndImage_ReceivesContent() = runBlocking {
        val clientWithVision = LiteRTClient(
            context = appContext,
            modelPath = TEST_MODEL_PATH,
            enableVision = true
        )
        val imageAttachment = createDummyImageAttachment()
        val testPrompt = prompt {
            user("What do you see in this image?", imageAttachment)
        }
        val model = LiteRTModels.Gemma3n.E2B.copy(capabilities = listOf(LLMCapability.Vision, LLMCapability.Text))

        val receivedChunks = mutableListOf<String>()
        var flowCompleted = false
        var flowThrewError: Throwable? = null

        try {
            clientWithVision.executeStreaming(testPrompt, model)
                .collect { chunk ->
                    receivedChunks.add(chunk)
                    assertTrue("Streamed chunk should not be empty", chunk.isNotEmpty())
                }
            flowCompleted = true
        } catch (e: Throwable) {
            flowThrewError = e
        }

        assertNull("Flow should not throw an error: $flowThrewError", flowThrewError)
        assertTrue("Flow should complete", flowCompleted)
        assertTrue("Should receive at least one chunk", receivedChunks.isNotEmpty())
        val fullResponse = receivedChunks.joinToString("")
        assertTrue("Full streamed response should not be blank", fullResponse.isNotBlank())
        println("LiteRT Multimodal Streaming Response: $fullResponse")
    }

    @Test
    fun testExecute_Multimodal_VisionDisabled_ImageIgnored() = runBlocking {
         val clientNoVision = LiteRTClient( // Default enableVision = false
            context = appContext,
            modelPath = TEST_MODEL_PATH
        )
        val imageAttachment = createDummyImageAttachment()
        val testPrompt = prompt {
            user("Describe this image.", imageAttachment) // Image will be logged as ignored
        }
        val model = LiteRTModels.Gemma3n.E2B.copy(capabilities = listOf(LLMCapability.Vision, LLMCapability.Text))

        // This test might still fail if the model at TEST_MODEL_PATH is not found,
        // but it tests the logic branch where vision is disabled in the client.
        // To make it more robust against model-not-found, one might need to mock LlmInference.
        // For now, assuming setup succeeds or fails clearly.
        try {
            val responses = clientNoVision.execute(testPrompt, model, emptyList())
            assertNotNull(responses)
            // Depending on model behavior with just "Describe this image." it might respond or error.
            // The key is that the image processing path shouldn't be hit or should log warnings.
            // This test mainly verifies the code path, actual response isn't strictly checked here.
            println("LiteRT Vision Disabled Response: ${responses.firstOrNull()?.content}")
        } catch (e: IllegalStateException) {
            if (e.message?.contains("Failed to initialize LiteRTClient") == true) {
                // This is acceptable if the model isn't present, test is about client logic path.
                 org.junit.Assume.assumeNoException("Skipping test as model not found, but testing client logic for vision disabled.", e)
            } else {
                throw e
            }
        }
    }
}
