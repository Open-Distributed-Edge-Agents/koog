package com.jetbrains.example.kotlin_agents_demo_app.mqtt

import io.moquette.broker.Server
import io.moquette.broker.config.MemoryConfig
import java.io.IOException
import java.util.*

class MqttBroker {

    private val broker = Server()

    @Throws(IOException::class)
    fun start() {
        val properties = Properties()
        properties.setProperty("port", "1883")
        properties.setProperty("host", "0.0.0.0")
        broker.start(MemoryConfig(properties))
    }

    fun stop() {
        broker.stop()
    }
}
