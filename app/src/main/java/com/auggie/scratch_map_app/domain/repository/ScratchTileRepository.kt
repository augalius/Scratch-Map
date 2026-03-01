package com.auggie.scratch_map_app.domain.repository

import com.auggie.scratch_map_app.domain.model.ScratchTile
import kotlinx.coroutines.flow.Flow

interface ScratchTileRepository {
    fun observeAllTiles(): Flow<List<ScratchTile>>
    fun observeTilesInBounds(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): Flow<List<ScratchTile>>
    suspend fun revealTiles(tiles: List<ScratchTile>): Int
    fun observeTileCount(): Flow<Long>
    suspend fun getTotalTileCount(): Long
}