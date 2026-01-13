package com.tendertracker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tendertracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override fun getAutoSync(): Flow<Boolean> =
        dataStore.data.map { it[KEY_AUTO_SYNC] ?: true }

    override suspend fun setAutoSync(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_SYNC] = enabled }
    }

    override fun getWifiOnlySync(): Flow<Boolean> =
        dataStore.data.map { it[KEY_WIFI_ONLY] ?: false }

    override suspend fun setWifiOnlySync(enabled: Boolean) {
        dataStore.edit { it[KEY_WIFI_ONLY] = enabled }
    }

    override fun getNotificationsEnabled(): Flow<Boolean> =
        dataStore.data.map { it[KEY_NOTIFICATIONS] ?: true }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    override fun getPreferredBackend(): Flow<String> =
        dataStore.data.map { it[KEY_BACKEND] ?: "CPU" }

    override suspend fun setPreferredBackend(backend: String) {
        dataStore.edit { it[KEY_BACKEND] = backend }
    }

    override fun getSelectedModelId(): Flow<String?> =
        dataStore.data.map { it[KEY_MODEL_ID] }

    override suspend fun setSelectedModelId(modelId: String?) {
        dataStore.edit {
            if (modelId != null) {
                it[KEY_MODEL_ID] = modelId
            } else {
                it.remove(KEY_MODEL_ID)
            }
        }
    }

    companion object {
        private val KEY_AUTO_SYNC = booleanPreferencesKey("auto_sync")
        private val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only_sync")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications")
        private val KEY_BACKEND = stringPreferencesKey("preferred_backend")
        private val KEY_MODEL_ID = stringPreferencesKey("selected_model_id")
    }
}
