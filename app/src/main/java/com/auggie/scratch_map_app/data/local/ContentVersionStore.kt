package com.auggie.scratch_map_app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the current content version.
 */
@Singleton
class ContentVersionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val key = intPreferencesKey("content_version")

    suspend fun getLocalVersion(): Int = dataStore.data.first()[key] ?: 0

    suspend fun saveVersion(version: Int) {
        dataStore.edit { it[key] = version }
    }
}
