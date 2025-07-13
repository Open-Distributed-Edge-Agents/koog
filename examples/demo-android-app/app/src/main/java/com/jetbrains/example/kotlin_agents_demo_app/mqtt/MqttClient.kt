package com.jetbrains.example.kotlin_agents_demo_app.mqtt

import android.content.Context
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MqttClient(
    private val context: Context,
    private val serverUri: String,
    private val clientId: String,
    private val topic: String,
    private val onMessageReceived: (String) -> Unit
) {

    private val mqttClient = MqttAndroidClient(context, serverUri, clientId)

    fun connect() {
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                subscribe()
            }

            override fun connectionLost(cause: Throwable?) {
                // Handle connection lost
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                if (message != null) {
                    onMessageReceived(String(message.payload))
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                // Handle delivery complete
            }
        })

        val options = MqttConnectOptions()
        mqttClient.connect(options)
    }

    private fun subscribe() {
        mqttClient.subscribe(topic, 0)
    }

    fun publish(message: String) {
        val mqttMessage = MqttMessage(message.toByteArray())
        mqttClient.publish(topic, mqttMessage)
    }

    fun disconnect() {
        mqttClient.disconnect()
    }
}
