package com.auggie.scratch_map_app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.auggie.scratch_map_app.domain.model.AppSettings
import com.auggie.scratch_map_app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object Keys {
        val IS_TRACKING_ENABLED = booleanPreferencesKey("is_tracking_enabled")
        val VIBRATE_ON_DISCOVERY = booleanPreferencesKey("vibrate_on_discovery")
    }

    override fun observeSettings(): Flow<AppSettings> =
        dataStore.data.map { it.toAppSettings() }

    override suspend fun getSettings(): AppSettings =
        dataStore.data.first().toAppSettings()

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(getSettings())
        dataStore.edit { prefs ->
            prefs[Keys.IS_TRACKING_ENABLED] = updated.isTrackingEnabled
            prefs[Keys.VIBRATE_ON_DISCOVERY] = updated.vibrateOnDiscovery
        }
    }

    private fun Preferences.toAppSettings() = AppSettings(
        isTrackingEnabled = this[Keys.IS_TRACKING_ENABLED] ?: true,
        vibrateOnDiscovery = this[Keys.VIBRATE_ON_DISCOVERY] ?: true
    )
}
