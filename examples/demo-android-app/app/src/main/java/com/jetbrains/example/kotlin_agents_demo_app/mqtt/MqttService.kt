package com.jetbrains.example.kotlin_agents_demo_app.mqtt

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.jetbrains.example.kotlin_agents_demo_app.settings.AppSettings
// import com.jetbrains.example.kotlin_agents_demo_app.screens.agentdemo.AgentDemoViewModel // No longer directly used
import io.moquette.broker.Server
import io.moquette.broker.config.MemoryConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.eclipse.paho.client.mqttv5.client.MqttClient
import org.eclipse.paho.client.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.client.mqttv5.common.MqttMessage
import org.eclipse.paho.client.mqttv5.client.IMqttToken
import org.eclipse.paho.client.mqttv5.client.MqttCallback
import org.eclipse.paho.client.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.client.mqttv5.common.MqttException
import java.io.IOException
import java.util.*

class MqttService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job) // Service instance scope

    private lateinit var appSettings: AppSettings
    private var mqttBroker: Server? = null
    // Removed pahoClient from instance variables, will be in companion object

    companion object {
        private const val TAG = "MqttService"
        const val COMMANDS_TOPIC = "agents/commands"
        fun getResponseTopicForClient(clientId: String) = "agents/responses/$clientId"

        var messageListener: MqttMessageListener? = null
        private var currentClientId: String = ""
        private var pahoClient: MqttClient? = null // Static client instance
        private val staticScope = CoroutineScope(Dispatchers.IO + SupervisorJob()) // Scope for static methods

        // To be called from service's onCreate to initialize appSettings for static methods
        private var staticAppSettings: AppSettings? = null
        private fun initializeStaticDependencies(context: Context) {
            if (staticAppSettings == null) {
                staticAppSettings = AppSettings(context.applicationContext)
            }
        }


        fun generateOrGetClientId(context: Context): String {
            if (currentClientId.isEmpty()) {
                val prefs = context.getSharedPreferences("mqtt_prefs", Context.MODE_PRIVATE)
                currentClientId = prefs.getString("mqtt_client_id", null) ?: run {
                    val newId = "koogAgent-${UUID.randomUUID().toString().takeLast(6)}"
                    prefs.edit().putString("mqtt_client_id", newId).apply()
                    newId
                }
            }
            return currentClientId
        }

        // Static methods for publishing
        fun staticPublishCommand(message: String) {
            val topic = COMMANDS_TOPIC
            staticPublishMessage(topic, message)
        }

        fun staticPublishResponse(response: String) {
            val clientId = pahoClient?.clientId ?: messageListener?.getMqttClientId() ?: currentClientId
            if (clientId.isEmpty()) {
                 Log.e(TAG, "Client ID is empty for static publishResponse.")
                 // Attempt to generate if truly empty and listener also failed.
                 // This scenario should be rare if client is connected or listener is set.
                 // val tempId = generateOrGetClientId(applicationContext) // Needs context
                 // For now, log error and return if no id.
                 return
            }
            val topic = getResponseTopicForClient(clientId)
            staticPublishMessage(topic, response)
        }

        private fun staticPublishMessage(topic: String, message: String, qos: Int = 1, retained: Boolean = false) {
            if (pahoClient?.isConnected == true) {
                try {
                    val mqttMessage = MqttMessage(message.toByteArray())
                    mqttMessage.qos = qos
                    mqttMessage.isRetained = retained
                    pahoClient?.publish(topic, mqttMessage)
                    Log.d(TAG, "StaticPublished to $topic: $message (QoS $qos, Retained $retained)")
                } catch (e: MqttException) {
                    Log.e(TAG, "StaticError publishing to $topic", e)
                }
            } else {
                Log.w(TAG, "StaticClient not connected, cannot publish to $topic. Message: $message")
                staticScope.launch {
                    staticAppSettings?.let { settingsHolder ->
                        val settings = settingsHolder.getCurrentSettings().first()
                        if (settings.mqttBrokerEnabled && !settings.mqttClientEnabled && topic == COMMANDS_TOPIC) {
                            Log.i(TAG, "StaticBroker-only mode, attempting one-off publish for command.")
                            var tempClient: MqttClient? = null
                            try {
                                val tempClientId = "captainStaticOneOff-${UUID.randomUUID().toString().takeLast(4)}"
                                val brokerSelfAddress = "tcp://127.0.0.1:1883"
                                tempClient = MqttClient(brokerSelfAddress, tempClientId, MemoryPersistence())
                                val connOpts = MqttConnectionOptions()
                                connOpts.isCleanStart = true
                                connOpts.connectionTimeout = 5
                                tempClient.connect(connOpts)
                                if (tempClient.isConnected) {
                                    val mqttMsg = MqttMessage(message.toByteArray())
                                    mqttMsg.qos = qos
                                    mqttMsg.isRetained = retained
                                    tempClient.publish(topic, mqttMsg)
                                    Log.d(TAG, "StaticBroker (temp client) published to $topic: $message")
                                    tempClient.disconnectForcibly(100, 100)
                                }
                            } catch (e: MqttException) {
                                Log.e(TAG, "StaticBroker temp client error publishing to $topic", e)
                            } finally {
                                try { tempClient?.close(true) } catch (ex: Exception) { /*ignore*/ }
                            }
                        }
                    } ?: Log.e(TAG, "staticAppSettings not initialized for one-off publish.")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        appSettings = AppSettings(applicationContext)
        Companion.initializeStaticDependencies(applicationContext) // Initialize for static methods
        Log.d(TAG, "MqttService Created. Static AppSettings Initialized: ${staticAppSettings != null}")


        scope.launch { // Instance scope for managing instance's broker/client lifecycle
            appSettings.context.settingsDataStore.data.collect { preferences ->
                val brokerEnabled = preferences[AppSettings.MQTT_BROKER_ENABLED_KEY] ?: false
                val clientEnabled = preferences[AppSettings.MQTT_CLIENT_ENABLED_KEY] ?: false
                val brokerAddress = preferences[AppSettings.MQTT_BROKER_ADDRESS_KEY] ?: "tcp://10.0.2.2:1883"

                if (brokerEnabled) {
                    startBroker()
                } else {
                    stopBroker()
                }

                if (clientEnabled) {
                    val connectAddress = if (brokerEnabled) "tcp://127.0.0.1:1883" else brokerAddress
                    connectClient(connectAddress) // This will set the static pahoClient
                } else {
                    stopClient() // This will clear the static pahoClient
                }
            }
        }
    }

    private fun startBroker() {
        if (mqttBroker?.isRunning == true) {
            Log.d(TAG, "Broker already running.")
            return
        }
        try {
            val properties = Properties()
            properties.setProperty("port", "1883") // Default MQTT port
            properties.setProperty("host", "0.0.0.0") // Listen on all interfaces
            // Consider security for a real app: properties.setProperty("allow_anonymous", "false"); properties.setProperty("password_file", "path/to/passwords");

            mqttBroker = Server()
            val memoryConfig = MemoryConfig(properties)
            mqttBroker?.startServer(memoryConfig)
            Log.i(TAG, "Moquette MQTT Broker started on port 1883")
        } catch (e: IOException) {
            Log.e(TAG, "Error starting Moquette MQTT Broker", e)
        }
    }

    private fun stopBroker() {
        mqttBroker?.stopServer()
        if (mqttBroker != null) {
            Log.i(TAG, "Moquette MQTT Broker stopped")
        }
        mqttBroker = null
    }

    private fun connectClient(brokerUri: String) {
        if (pahoClient?.isConnected == true && pahoClient?.serverURI == brokerUri) {
            Log.d(TAG, "Client already connected to $brokerUri.")
            return
        }
        stopClient() // Stop any existing client connection first

        try {
            val clientId = generateOrGetClientId(applicationContext)
            pahoClient = MqttClient(brokerUri, clientId, MemoryPersistence())
            val connOpts = MqttConnectionOptions()
            connOpts.isCleanStart = true
            connOpts.isAutomaticReconnect = true // Enable auto reconnect

            pahoClient?.setCallback(object : MqttCallback {
                override fun disconnected(disconnectResponse: org.eclipse.paho.client.mqttv5.client.MqttDisconnectResponse?) {
                    Log.w(TAG, "MQTT Client Disconnected: ${disconnectResponse?.reasonString}")
                    // Reconnect logic is handled by connOpts.isAutomaticReconnect = true
                }

                override fun mqttErrorOccurred(exception: MqttException?) {
                    Log.e(TAG, "MQTT Error: ${exception?.message}", exception)
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val msgString = message.toString()
                    Log.d(TAG, "Message arrived. Topic: $topic, Message: $msgString")
                    if (topic == COMMANDS_TOPIC) {
                        messageListener?.onMqttMessageArrived(msgString)
                    }
                    // Potentially handle responses to self if subscribed to "agents/responses/${pahoClient?.clientId}"
                    // Or if captain, and subscribed to "agents/responses/#"
                    if (topic?.startsWith("agents/responses/") == true) {
                        // Could pass this to a different listener method if needed
                        // For now, captain will see all responses via its generic subscription
                        Log.d(TAG, "Response message received on $topic: $msgString")
                        // If a specific UI element needs to show this, messageListener might need another method.
                    }
                }

                override fun deliveryComplete(token: IMqttToken?) {
                    Log.d(TAG, "Delivery complete for token: ${token?.messageId}")
                }

                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.i(TAG, "MQTT Client Connected to $serverURI. Reconnect: $reconnect. Client ID: ${pahoClient?.clientId}")
                    subscribeToCommands()
                    // If this client is a "captain" (broker also running on this instance), it should listen to all responses.
                    scope.launch {
                        if (appSettings.getCurrentSettings().first().mqttBrokerEnabled) {
                            subscribeToTopic("agents/responses/#")
                        }
                    }
                }

                override fun authPacketArrived(authPacket: org.eclipse.paho.client.mqttv5.common.packet.MqttAuth?) {
                    // Not used for now
                }
            })

            Log.i(TAG, "Attempting to connect MQTT client to: $brokerUri with client ID: $clientId")
            pahoClient?.connect(connOpts)
        } catch (e: MqttException) {
            Log.e(TAG, "Error connecting MQTT Client to $brokerUri", e)
        }
    }

    private fun subscribeToCommands() {
        subscribeToTopic(COMMANDS_TOPIC)
    }

    private fun subscribeToTopic(topic: String, qos: Int = 1) {
        try {
            pahoClient?.let {
                if (it.isConnected) {
                    it.subscribe(topic, qos)
                    Log.i(TAG, "Subscribed to topic: $topic with QoS $qos")
                } else {
                    Log.w(TAG, "Cannot subscribe, client not connected.")
                }
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Error subscribing to topic $topic", e)
        }
    }

    // Called by UI/ViewModel to send a command (if captain) or any message if just a client
    fun publishCommand(message: String) {
        val topic = COMMANDS_TOPIC
        publishMessage(topic, message)
    }

    // Called by UI/ViewModel (after AI processing) to send a response
    fun publishResponse(response: String) {
        val clientId = pahoClient?.clientId ?: run {
            Log.e(TAG, "Cannot publish response, client ID unknown.")
            // Attempt to use listener's client ID if pahoClient is null somehow (should not happen if connected)
            messageListener?.getMqttClientId() ?: run {
                 Log.e(TAG, "Listener client ID also unknown for response.")
                 return
            }
        }
        val topic = getResponseTopicForClient(clientId)
        publishMessage(topic, response)
    }

    private fun publishMessage(topic: String, message: String, qos: Int = 1, retained: Boolean = false) {
        if (pahoClient?.isConnected == true) {
            try {
                val mqttMessage = MqttMessage(message.toByteArray())
                mqttMessage.qos = qos
                mqttMessage.isRetained = retained
                pahoClient?.publish(topic, mqttMessage)
                Log.d(TAG, "Published to $topic: $message (QoS $qos, Retained $retained)")
            } catch (e: MqttException) {
                Log.e(TAG, "Error publishing to $topic", e)
            }
        } else {
            Log.w(TAG, "Client not connected, cannot publish to $topic. Message: $message")
            // Attempt a one-off publish if broker is enabled but client wasn't (e.g. captain's first message)
            // This is for the case where the app is set to "Broker enabled" but "Client disabled",
            // yet the user (captain) sends a message from the UI.
            scope.launch {
                val settings = appSettings.getCurrentSettings().first()
                if (settings.mqttBrokerEnabled && !settings.mqttClientEnabled && topic == COMMANDS_TOPIC) {
                    Log.i(TAG, "Broker-only mode, attempting one-off publish to self for command.")
                    var tempClient: MqttClient? = null
                    try {
                        val tempClientId = "captainOneOffPub-${UUID.randomUUID().toString().takeLast(4)}"
                        // Captain publishing to its own broker.
                        val brokerSelfAddress = "tcp://127.0.0.1:1883"
                        tempClient = MqttClient(brokerSelfAddress, tempClientId, MemoryPersistence())
                        val connOpts = MqttConnectionOptions()
                        connOpts.isCleanStart = true
                        connOpts.connectionTimeout = 5 // Short timeout for one-off
                        tempClient.connect(connOpts)
                        if (tempClient.isConnected) {
                            val mqttMsg = MqttMessage(message.toByteArray())
                            mqttMsg.qos = qos
                            mqttMsg.isRetained = retained
                            tempClient.publish(topic, mqttMsg)
                            Log.d(TAG, "Broker (as temp client) published to $topic: $message")
                            tempClient.disconnectForcibly(100,100) // quick disconnect
                        }
                    } catch (e: MqttException) {
                        Log.e(TAG, "Broker temp client error publishing to $topic", e)
                    } finally {
                        try { tempClient?.close(true) } catch (ex: Exception) { /* ignore close error */ }
                    }
                }
            }
        }
    }

    private fun stopClient() {
        try {
            pahoClient?.let {
                if (it.isConnected) {
                    // Unsubscribe from all topics? Paho Java client doesn't have a simple "unsubscribeAll"
                    // For clean session, this is less critical. For persistent, important.
                    Log.d(TAG, "Disconnecting Paho client: ${it.clientId}")
                    it.disconnectForcibly(1000, 1000, true) // connectTimeout, quiesceTimeout, sendDisconnectPacket
                }
                it.close(true) // Force close, release resources
            }
            if (pahoClient != null) Log.i(TAG, "MQTT Client stopped.")
        } catch (e: MqttException) {
            Log.e(TAG, "Error stopping MQTT Client", e)
        }
        pahoClient = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MqttService onStartCommand")
        // Ensure client ID is generated/loaded early if service is started this way
        generateOrGetClientId(applicationContext)
        return START_STICKY // Keep service running
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MqttService Destroyed")
        stopBroker()
        stopClient()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // We are not using binding for now, returning null.
        // If binding is needed, create a Binder class.
        return null
    }
}
