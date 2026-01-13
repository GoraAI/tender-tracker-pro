package com.tendertracker.domain.repository

import com.tendertracker.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    fun getRecentMessages(limit: Int): Flow<List<ChatMessage>>
    suspend fun sendMessage(message: ChatMessage)
    suspend fun clearHistory()
}
