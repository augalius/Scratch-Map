package com.auggie.scratch_map_app.domain.repository

import com.auggie.scratch_map_app.domain.model.AchievementDefinition
import kotlinx.coroutines.flow.Flow

interface AchievementRepository {
    fun observeDefinitions(): Flow<List<AchievementDefinition>>
    suspend fun insertDefinitions(definitions: List<AchievementDefinition>)
    suspend fun deleteDefinitionsNotIn(ids: List<String>)
}
