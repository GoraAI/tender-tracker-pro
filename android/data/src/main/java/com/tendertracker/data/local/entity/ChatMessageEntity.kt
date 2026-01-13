package com.tendertracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tendertracker.domain.model.ChatMessage
import com.tendertracker.domain.model.MessageRole

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val content: String,
    val role: String,
    val tenderId: String?,
    val timestamp: Long
) {
    fun toDomain(): ChatMessage = ChatMessage(
        id = id,
        content = content,
        role = MessageRole.valueOf(role),
        tenderId = tenderId,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(message: ChatMessage): ChatMessageEntity =
            ChatMessageEntity(
                id = message.id,
                content = message.content,
                role = message.role.name,
                tenderId = message.tenderId,
                timestamp = message.timestamp
            )
    }
}
