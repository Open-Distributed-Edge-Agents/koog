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
}
