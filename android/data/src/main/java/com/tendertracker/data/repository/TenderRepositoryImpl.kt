package com.tendertracker.data.repository

import com.tendertracker.data.local.dao.TenderDao
import com.tendertracker.data.local.entity.TenderEntity
import com.tendertracker.domain.model.Tender
import com.tendertracker.domain.repository.TenderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TenderRepositoryImpl @Inject constructor(
    private val tenderDao: TenderDao
) : TenderRepository {

    override fun getAllTenders(): Flow<List<Tender>> =
        tenderDao.getAllTenders().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTenderById(id: String): Flow<Tender?> =
        tenderDao.getTenderById(id).map { it?.toDomain() }

    override fun searchTenders(query: String): Flow<List<Tender>> =
        tenderDao.searchTenders(query).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getHighPriorityTenders(minScore: Int): Flow<List<Tender>> =
        tenderDao.getHighPriorityTenders(minScore).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insertTender(tender: Tender) {
        tenderDao.insertTender(TenderEntity.fromDomain(tender))
    }

    override suspend fun updateScore(id: String, score: Int, explanation: String) {
        tenderDao.updateScore(id, score, explanation)
    }

    override suspend fun deleteTender(id: String) {
        tenderDao.deleteTender(id)
    }
}
