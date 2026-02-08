package com.tendertracker.domain.repository

import com.tendertracker.domain.model.QualificationResult
import com.tendertracker.domain.model.TenderApplication

interface ApplicationRepository {
    suspend fun checkQualification(tenderId: String): Result<QualificationResult>

    suspend fun createApplication(tenderId: String): Result<TenderApplication>

    suspend fun getApplication(applicationId: String): Result<TenderApplication>

    suspend fun updateFields(applicationId: String, fieldUpdates: Map<String, Any>): Result<TenderApplication>

    suspend fun updatePricing(applicationId: String, pricingData: Map<String, Any>): Result<TenderApplication>

    suspend fun updateTodoStatus(applicationId: String, todoId: String, completed: Boolean): Result<TenderApplication>

    suspend fun submitApplication(applicationId: String): Result<TenderApplication>

    suspend fun deleteApplication(applicationId: String): Result<Unit>
}
