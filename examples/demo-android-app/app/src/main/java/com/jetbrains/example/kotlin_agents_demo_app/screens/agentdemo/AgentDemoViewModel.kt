package com.jetbrains.example.kotlin_agents_demo_app.screens.agentdemo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.jetbrains.example.kotlin_agents_demo_app.agents.common.AgentProvider
import com.jetbrains.example.kotlin_agents_demo_app.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Define message types for the chat
sealed class Message {
    data class UserMessage(val text: String) : Message()
    data class AgentMessage(val text: String) : Message()
    data class SystemMessage(val text: String) : Message()
    data class ErrorMessage(val text: String) : Message()
    data class ToolCallMessage(val text: String) : Message()
    data class ResultMessage(val text: String) : Message()
    data class RemoteMessage(val text: String, val senderId: String?) : Message() // For messages from MQTT
}

// Define UI state for the agent demo screen
data class AgentDemoUiState(
    val title: String = "Agent Demo",
    val messages: List<Message> = listOf(Message.SystemMessage("Hi, I'm an agent that can help you")),
    val inputText: String = "",
    val isInputEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isChatEnded: Boolean = false,

    // For handling user responses when agent asks a question
    val userResponseRequested: Boolean = false,
    val currentUserResponse: String? = null,
)

import com.jetbrains.example.kotlin_agents_demo_app.mqtt.MqttMessageListener
import com.jetbrains.example.kotlin_agents_demo_app.mqtt.MqttService // Required for getMqttClientId and publish

class AgentDemoViewModel(
    application: Application,
    private val agentProvider: AgentProvider
) : AndroidViewModel(application), MqttMessageListener {

    // UI state
    private val _uiState = MutableStateFlow(AgentDemoUiState(
        title = agentProvider.title,
        messages = listOf(Message.SystemMessage(agentProvider.description))
    ))
    val uiState: StateFlow<AgentDemoUiState> = _uiState.asStateFlow()

    // Update input text
    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    // Send user message and start agent processing
    fun sendMessage() {
        val userInput = _uiState.value.inputText.trim()
        if (userInput.isEmpty()) return

        // If agent is waiting for a response to a question
        if (_uiState.value.userResponseRequested) {
            _uiState.update {
                it.copy(
                    messages = it.messages + Message.UserMessage(userInput),
                    inputText = "",
                    isLoading = true, // Will be set to false after agent processes this specific response
                    userResponseRequested = false, // We are providing the response now
                    currentUserResponse = userInput // This triggers the waiting runAgent to continue
                )
            }
            // No need to call runAgent here, it's already waiting for currentUserResponse to be non-null
        } else { // Initial message flow
            _uiState.update {
                it.copy(
                    messages = it.messages + Message.UserMessage(userInput),
                    inputText = "",
                    isInputEnabled = false,
                    isLoading = true
                )
            }

            // If this app instance is a captain (broker mode), publish the command
            viewModelScope.launch {
                val settings = AppSettings(getApplication()).getCurrentSettings().first()
                if (settings.mqttBrokerEnabled) {
                    MqttService.staticPublishCommand(userInput)
                }
            }

            viewModelScope.launch {
                runAgent(userInput, isRemoteMessage = false)
            }
        }
    }

    // Run the agent
    private suspend fun runAgent(input: String, isRemoteMessage: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                val agent = agentProvider.provideAgent(
                    appSettings = AppSettings(getApplication()),
                    onToolCallEvent = { message ->
                        viewModelScope.launch {
                            _uiState.update {
                                it.copy(messages = it.messages + Message.ToolCallMessage(message))
                            }
                        }
                    },
                    onErrorEvent = { errorMessage ->
                        viewModelScope.launch {
                            _uiState.update {
                                it.copy(
                                    messages = it.messages + Message.ErrorMessage(errorMessage),
                                    isInputEnabled = true,
                                    isLoading = false
                                )
                            }
                        }
                    },
                    onAssistantMessage = { message ->
                        // Agent is asking a question
                        _uiState.update {
                            it.copy(
                                messages = it.messages + Message.AgentMessage(message),
                                isInputEnabled = true, // Enable input for user's response
                                isLoading = false,
                                userResponseRequested = true // Signal that agent is waiting
                            )
                        }

                        // Suspend until user provides a response via sendMessage() -> updates currentUserResponse
                        val userResponse = _uiState
                            .map { it.currentUserResponse }
                            .filterNotNull()
                            .first() // waits for the first non-null response

                        // Reset for next interaction
                        _uiState.update { it.copy(currentUserResponse = null, userResponseRequested = false) }
                        userResponse // Return the collected response to the agent
                    },
                )

                val result = agent.runAndGetResult(input)

                if (isRemoteMessage) {
                    // If it was a remote message, publish the response
                    MqttService.staticPublishResponse(result.orEmpty())
                }

                _uiState.update {
                    it.copy(
                        messages = it.messages +
                                Message.ResultMessage(result.orEmpty()) +
                                Message.SystemMessage("The agent has completed this interaction."),
                        isInputEnabled = true, // Ready for new input
                        isLoading = false,
                        isChatEnded = false // Chat can continue unless explicitly ended by a tool
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        messages = it.messages + Message.ErrorMessage("Error: ${e.message}"),
                        isInputEnabled = true,
                        isLoading = false
                    )
                }
                if (isRemoteMessage) {
                     MqttService.staticPublishResponse("Error processing remote command: ${e.message}")
                }
            }
        }
    }

    // Restart the chat
    fun restartChat() {
        _uiState.update {
            AgentDemoUiState( // Reset to initial state
                title = agentProvider.title,
                messages = listOf(Message.SystemMessage(agentProvider.description))
            )
        }
    }

    // --- MqttMessageListener Implementation ---
    override fun onMqttMessageArrived(message: String) {
        // Add message to UI (distinguish it as remote)
        // For now, assuming message is from another "user" or agent via MQTT
        // We might need to parse senderId if included in message format later
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    messages = it.messages + Message.RemoteMessage(message, senderId = "MQTT"), // Placeholder sender
                    isInputEnabled = false, // Disable input while processing remote message
                    isLoading = true
                )
            }
            // Process the message using the agent
            runAgent(message, isRemoteMessage = true)
        }
    }

    override fun getMqttClientId(): String {
        // This ViewModel needs access to the client ID used by MqttService
        // For now, MqttService stores it statically after generation or retrieval.
        return MqttService.generateOrGetClientId(getApplication())
    }
}