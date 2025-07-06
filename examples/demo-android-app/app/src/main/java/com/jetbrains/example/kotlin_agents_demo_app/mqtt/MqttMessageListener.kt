package com.jetbrains.example.kotlin_agents_demo_app.mqtt

interface MqttMessageListener {
    fun onMqttMessageArrived(message: String)
    fun getMqttClientId(): String // To help service publish responses to the correct topic
}
