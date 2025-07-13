package com.jetbrains.example.kotlin_agents_demo_app.mqtt

import android.content.Context
import com.jetbrains.example.kotlin_agents_demo_app.settings.AppSettings
import java.util.UUID

class MqttManager(
    private val context: Context,
    private val onMessageReceived: (String) -> Unit
) {

    private var mqttClient: MqttClient? = null
    private var mqttBroker: MqttBroker? = null
    val clientId = UUID.randomUUID().toString()
    private val topic = "koog-demo"

    suspend fun start() {
        val settings = AppSettings(context).getMqttSettings()
        if (settings.isBroker) {
            mqttBroker = MqttBroker()
            mqttBroker?.start()
        }

        val serverUri = if (settings.isBroker) {
            "tcp://localhost:1883"
        } else {
            "tcp://${settings.brokerIp}:${settings.brokerPort}"
        }

        mqttClient = MqttClient(context, serverUri, clientId, topic, onMessageReceived)
        mqttClient?.connect()
    }

    fun publish(message: String) {
        mqttClient?.publish("$clientId: $message")
    }

    fun stop() {
        mqttClient?.disconnect()
        mqttBroker?.stop()
    }
}
