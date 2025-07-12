package ai.koog.prompt.executor.litert

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class LiteRTClientTest {

    private lateinit var mockContext: Context
    private lateinit var mockLlmInference: LlmInference

    @Before
    fun setup() {
        mockContext = mockk()
        mockLlmInference = mockk()
        mockkStatic(LlmInference::class)
        every { LlmInference.createFromOptions(any(), any()) } returns mockLlmInference
    }

    @Test
    fun `test LiteRTClient can be instantiated`() {
        val client = LiteRTClient(mockContext, "model/path")
        assertNotNull(client)
    }
}
