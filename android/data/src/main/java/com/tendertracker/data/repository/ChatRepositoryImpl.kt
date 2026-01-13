package com.tendertracker.data.repository

import com.tendertracker.data.local.dao.ChatMessageDao
import com.tendertracker.data.local.entity.ChatMessageEntity
import com.tendertracker.domain.model.ChatMessage
import com.tendertracker.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatMessageDao: ChatMessageDao
) : ChatRepository {

    override fun getMessages(): Flow<List<ChatMessage>> =
        chatMessageDao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getRecentMessages(limit: Int): Flow<List<ChatMessage>> =
        chatMessageDao.getRecentMessages(limit).map { entities ->
            entities.reversed().map { it.toDomain() }
        }

    override suspend fun sendMessage(message: ChatMessage) {
        chatMessageDao.insertMessage(ChatMessageEntity.fromDomain(message))
    }

    override suspend fun clearHistory() {
        chatMessageDao.clearAll()
    }
}
