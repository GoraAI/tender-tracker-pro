package com.tendertracker.domain.repository

import com.tendertracker.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    val currentUser: Flow<User?>

    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(
        email: String,
        password: String,
        fullName: String?,
        companyName: String?
    ): Result<User>
    suspend fun logout()
    suspend fun refreshToken(): Result<Unit>
}
