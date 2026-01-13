package com.tendertracker.domain.repository

import com.tendertracker.domain.model.Tender
import kotlinx.coroutines.flow.Flow

interface TenderRepository {
    fun getAllTenders(): Flow<List<Tender>>
    fun getTenderById(id: String): Flow<Tender?>
    fun searchTenders(query: String): Flow<List<Tender>>
    fun getHighPriorityTenders(minScore: Int = 70): Flow<List<Tender>>
    suspend fun insertTender(tender: Tender)
    suspend fun updateScore(id: String, score: Int, explanation: String)
    suspend fun deleteTender(id: String)
}
