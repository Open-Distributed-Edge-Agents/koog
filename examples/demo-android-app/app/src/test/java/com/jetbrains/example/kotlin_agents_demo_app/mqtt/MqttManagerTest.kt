package com.jetbrains.example.kotlin_agents_demo_app.mqtt

import android.content.Context
import com.jetbrains.example.kotlin_agents_demo_app.settings.AppSettings
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class MqttManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockAppSettings: AppSettings

    @Before
    fun setup() {
        mockContext = mockk()
        mockAppSettings = mockk()
        coEvery { mockContext.applicationContext } returns mockContext
        coEvery { AppSettings(mockContext) } returns mockAppSettings
    }

    @Test
    fun `test MqttManager can be instantiated`() {
        runBlocking {
            coEvery { mockAppSettings.getMqttSettings() } returns com.jetbrains.example.kotlin_agents_demo_app.settings.MqttSettings(
                isBroker = true,
                brokerIp = "",
                brokerPort = 1883
            )
            val manager = MqttManager(mockContext) {}
            manager.start()
            manager.stop()
        }
    }
}
