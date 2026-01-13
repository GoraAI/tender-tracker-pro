package com.tendertracker.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tendertracker.app.ui.chat.ChatContext
import com.tendertracker.app.ui.chat.ChatResponseGenerator
import com.tendertracker.domain.model.ChatMessage
import com.tendertracker.domain.model.MessageRole
import com.tendertracker.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isTyping: Boolean = false,
    val error: String? = null,
    val contextTenderId: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val responseGenerator: ChatResponseGenerator
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            chatRepository.getMessages()
                .catch { e ->
                    _state.update { it.copy(error = e.message) }
                }
                .collect { messages ->
                    _state.update { it.copy(messages = messages) }
                }
        }
    }

    fun onInputChange(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun onSendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            // Add user message
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = text,
                role = MessageRole.USER,
                tenderId = _state.value.contextTenderId,
                timestamp = System.currentTimeMillis()
            )
            chatRepository.sendMessage(userMessage)
            _state.update { it.copy(inputText = "", isTyping = true) }

            // Generate AI response
            val context = ChatContext(tenderId = _state.value.contextTenderId)
            val result = responseGenerator.generateResponse(text, context)

            val responseContent = result.getOrElse { e ->
                "Error: ${e.message ?: "Failed to generate response"}"
            }

            val aiResponse = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = responseContent,
                role = MessageRole.ASSISTANT,
                tenderId = _state.value.contextTenderId,
                timestamp = System.currentTimeMillis()
            )
            chatRepository.sendMessage(aiResponse)
            _state.update { it.copy(isTyping = false) }
        }
    }

    fun setContext(tenderId: String?) {
        _state.update { it.copy(contextTenderId = tenderId) }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearHistory()
        }
    }
}
