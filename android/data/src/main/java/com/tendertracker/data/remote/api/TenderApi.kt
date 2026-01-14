package com.tendertracker.data.remote.api

import com.tendertracker.data.remote.dto.LoginRequest
import com.tendertracker.data.remote.dto.RegisterRequest
import com.tendertracker.data.remote.dto.ScoreResponse
import com.tendertracker.data.remote.dto.ScoreSubmission
import com.tendertracker.data.remote.dto.TenderListResponse
import com.tendertracker.data.remote.dto.TenderResponse
import com.tendertracker.data.remote.dto.TokenResponse
import com.tendertracker.data.remote.dto.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TenderApi {
    @GET("tenders")
    suspend fun getTenders(
        @Query("since") since: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): Response<TenderListResponse>

    @GET("tenders/{id}")
    suspend fun getTender(
        @Path("id") id: String
    ): Response<TenderResponse>

    @POST("tenders/{id}/score")
    suspend fun submitScore(
        @Path("id") tenderId: String,
        @Body score: ScoreSubmission
    ): Response<ScoreResponse>

    @POST("auth/login")
    suspend fun login(
        @Body credentials: LoginRequest
    ): Response<TokenResponse>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<TokenResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(): Response<TokenResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<UserResponse>
}
