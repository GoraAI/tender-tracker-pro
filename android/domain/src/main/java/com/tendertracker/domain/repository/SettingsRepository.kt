package com.tendertracker.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getAutoSync(): Flow<Boolean>
    suspend fun setAutoSync(enabled: Boolean)

    fun getWifiOnlySync(): Flow<Boolean>
    suspend fun setWifiOnlySync(enabled: Boolean)

    fun getNotificationsEnabled(): Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)

    fun getPreferredBackend(): Flow<String>
    suspend fun setPreferredBackend(backend: String)

    fun getSelectedModelId(): Flow<String?>
    suspend fun setSelectedModelId(modelId: String?)
}
