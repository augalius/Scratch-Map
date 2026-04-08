package com.auggie.scratch_map_app.domain.repository

import com.auggie.scratch_map_app.domain.model.Region
import kotlinx.coroutines.flow.Flow

interface RegionRepository {
    fun observeAll(): Flow<List<Region>>
    suspend fun insertAll(regions: List<Region>)
    suspend fun deleteNotIn(ids: List<String>)
}
