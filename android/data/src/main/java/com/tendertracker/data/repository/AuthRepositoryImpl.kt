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
                tokenResponse.refreshToken?.let { tokenManager.setRefreshToken(it) }

                // Fetch user info
                val userResponse = api.getCurrentUser()
                if (userResponse.isSuccessful) {
                    val user = userResponse.body()!!.toUser()
                    _currentUser.value = user
                    Result.success(user)
                } else {
                    // Still logged in even if we can't get user info
                    val placeholderUser = User(
                        id = "unknown",
                        email = email,
                        fullName = null,
                        companyName = null,
                        isActive = true
                    )
                    _currentUser.value = placeholderUser
                    Result.success(placeholderUser)
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Login failed"
                Result.failure(Exception(parseErrorMessage(errorBody)))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Unable to connect to server. Please check your connection."))
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
                // Registration returns a token directly
                val tokenResponse = response.body()!!
                tokenManager.setAccessToken(tokenResponse.accessToken)
                tokenResponse.refreshToken?.let { tokenManager.setRefreshToken(it) }

                // Fetch user info
                val userResponse = api.getCurrentUser()
                if (userResponse.isSuccessful) {
                    val user = userResponse.body()!!.toUser()
                    _currentUser.value = user
                    Result.success(user)
                } else {
                    val placeholderUser = User(
                        id = "unknown",
                        email = email,
                        fullName = fullName,
                        companyName = companyName,
                        isActive = true
                    )
                    _currentUser.value = placeholderUser
                    Result.success(placeholderUser)
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Registration failed"
                Result.failure(Exception(parseErrorMessage(errorBody)))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Unable to connect to server. Please check your connection."))
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
                tokenResponse.refreshToken?.let { tokenManager.setRefreshToken(it) }
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

    /**
     * Parse error message from API response JSON
     */
    private fun parseErrorMessage(errorBody: String): String {
        return try {
            val json = org.json.JSONObject(errorBody)
            // FastAPI errors use "detail", Railway infrastructure errors use "message"
            val detail = json.optString("detail").takeIf { it.isNotEmpty() }
            val message = json.optString("message").takeIf { it.isNotEmpty() }
            when {
                detail != null && detail.startsWith("[") -> {
                    // Validation error array - extract first msg
                    val arr = org.json.JSONArray(detail)
                    arr.optJSONObject(0)?.optString("msg") ?: "Validation error"
                }
                detail != null -> detail
                message == "Application not found" -> "Server is unavailable. Please try again later."
                message != null -> message
                else -> "An error occurred. Please try again."
            }
        } catch (e: Exception) {
            "An error occurred. Please try again."
        }
    }
}
