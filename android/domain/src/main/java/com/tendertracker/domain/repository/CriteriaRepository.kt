package com.tendertracker.domain.repository

import com.tendertracker.domain.model.UserCriteria
import com.tendertracker.domain.util.Result

/**
 * Repository for managing user scoring criteria.
 */
interface CriteriaRepository {
    /**
     * Get all criteria for the current user.
     */
    suspend fun getCriteria(): Result<List<UserCriteria>>

    /**
     * Get a specific criteria by ID.
     */
    suspend fun getCriteriaById(id: String): Result<UserCriteria>

    /**
     * Create new criteria.
     */
    suspend fun createCriteria(criteria: UserCriteria): Result<UserCriteria>

    /**
     * Update existing criteria.
     */
    suspend fun updateCriteria(criteria: UserCriteria): Result<UserCriteria>

    /**
     * Delete criteria by ID.
     */
    suspend fun deleteCriteria(id: String): Result<Unit>

    /**
     * Get the default criteria (if any).
     */
    suspend fun getDefaultCriteria(): Result<UserCriteria?>
}
