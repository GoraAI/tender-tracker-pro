package com.tendertracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tendertracker.data.local.entity.TenderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TenderDao {
    @Query("SELECT * FROM tenders ORDER BY createdAt DESC")
    fun getAllTenders(): Flow<List<TenderEntity>>

    @Query("SELECT * FROM tenders WHERE id = :id")
    fun getTenderById(id: String): Flow<TenderEntity?>

    @Query("""
        SELECT * FROM tenders
        WHERE title LIKE '%' || :query || '%'
        OR reference LIKE '%' || :query || '%'
        OR requirementsSummary LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchTenders(query: String): Flow<List<TenderEntity>>

    @Query("SELECT * FROM tenders WHERE score >= :minScore ORDER BY score DESC")
    fun getHighPriorityTenders(minScore: Int): Flow<List<TenderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTender(tender: TenderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenders(tenders: List<TenderEntity>)

    @Query("UPDATE tenders SET score = :score, scoreExplanation = :explanation WHERE id = :id")
    suspend fun updateScore(id: String, score: Int, explanation: String)

    @Query("DELETE FROM tenders WHERE id = :id")
    suspend fun deleteTender(id: String)

    @Query("SELECT COUNT(*) FROM tenders")
    suspend fun getTenderCount(): Int
}
