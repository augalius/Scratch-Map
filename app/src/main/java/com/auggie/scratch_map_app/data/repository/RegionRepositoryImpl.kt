package com.auggie.scratch_map_app.data.repository

import com.auggie.scratch_map_app.data.local.converter.RegionMapper
import com.auggie.scratch_map_app.data.local.dao.RegionDao
import com.auggie.scratch_map_app.domain.model.Region
import com.auggie.scratch_map_app.domain.repository.RegionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegionRepositoryImpl @Inject constructor(
    private val dao: RegionDao
) : RegionRepository {
    override fun observeAll(): Flow<List<Region>> =
        dao.observeAll().map { it.map(RegionMapper::entityToDomain) }

    override suspend fun insertAll(regions: List<Region>) =
        dao.insertAll(regions.map(RegionMapper::domainToEntity))

    override suspend fun deleteNotIn(ids: List<String>) = dao.deleteNotIn(ids)
}
