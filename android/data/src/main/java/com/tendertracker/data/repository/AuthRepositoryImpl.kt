package com.tendertracker.data.repository

import com.tendertracker.data.local.TokenManager
import com.tendertracker.data.remote.api.TenderApi
import com.tendertracker.data.remote.dto.LoginRequest
import com.tendertracker.data.remote.dto.RegisterRequest
import com.tendertracker.data.remote.dto.UserResponse
import com.tendertracker.domain.model.User
import com.tendertracker.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: TenderApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)

    override val isLoggedIn: Flow<Boolean> =
        tokenManager.getAccessTokenFlow().map { it != null }

    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val tokenResponse = response.body()!!
                tokenManager.setAccessToken(tokenResponse.accessToken)
                tokenManager.setRefreshToken(tokenResponse.refreshToken)

                // Fetch user info
                val userResponse = api.getCurrentUser()
                if (userResponse.isSuccessful) {
                    val user = userResponse.body()!!.toUser()
                    _currentUser.value = user
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to get user info"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Login failed"
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        fullName: String?,
        companyName: String?
    ): Result<User> {
        return try {
            val response = api.register(
                RegisterRequest(
                    email = email,
                    password = password,
                    fullName = fullName,
                    companyName = companyName
                )
            )
            if (response.isSuccessful) {
                val user = response.body()!!.toUser()
                // After registration, login to get tokens
                login(email, password)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Registration failed"
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        tokenManager.clearTokens()
        _currentUser.value = null
    }

    override suspend fun refreshToken(): Result<Unit> {
        return try {
            val response = api.refreshToken()
            if (response.isSuccessful) {
                val tokenResponse = response.body()!!
                tokenManager.setAccessToken(tokenResponse.accessToken)
                tokenManager.setRefreshToken(tokenResponse.refreshToken)
                Result.success(Unit)
            } else {
                // Token refresh failed, clear tokens
                tokenManager.clearTokens()
                _currentUser.value = null
                Result.failure(Exception("Token refresh failed"))
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
            _currentUser.value = null
            Result.failure(e)
        }
    }

    private fun UserResponse.toUser(): User = User(
        id = id,
        email = email,
        fullName = fullName,
        companyName = companyName,
        isActive = isActive
    )
}
