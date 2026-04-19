package com.auggie.scratch_map_app.data.repository

import com.auggie.scratch_map_app.data.local.converter.AchievementMapper
import com.auggie.scratch_map_app.data.local.dao.AchievementDefinitionDao
import com.auggie.scratch_map_app.domain.model.AchievementDefinition
import com.auggie.scratch_map_app.domain.repository.AchievementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepositoryImpl @Inject constructor(
    private val definitionDao: AchievementDefinitionDao
) : AchievementRepository {

    override fun observeDefinitions(): Flow<List<AchievementDefinition>> =
        definitionDao.observeAll().map { it.map(AchievementMapper::entityToDomain) }

    override suspend fun insertDefinitions(definitions: List<AchievementDefinition>) =
        definitionDao.insertAll(definitions.map(AchievementMapper::domainToEntity))

    override suspend fun deleteDefinitionsNotIn(ids: List<String>) =
        definitionDao.deleteNotIn(ids)
}
