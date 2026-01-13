package com.tendertracker.domain.model

data class ChatMessage(
    val id: String,
    val content: String,
    val role: MessageRole,
    val tenderId: String?,
    val timestamp: Long
)

enum class MessageRole {
    USER, ASSISTANT
}
