package com.auggie.scratch_map_app.data.repository

import com.auggie.scratch_map_app.data.local.converter.TileMapper
import com.auggie.scratch_map_app.data.local.dao.ScratchTileDao
import com.auggie.scratch_map_app.domain.model.ScratchTile
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScratchTileRepositoryImpl @Inject constructor(
    private val dao: ScratchTileDao
) : ScratchTileRepository {

    override fun observeAllTiles(): Flow<List<ScratchTile>> =
        dao.observeAll().map { it.map(TileMapper::entityToDomain) }

    override fun observeTilesInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): Flow<List<ScratchTile>> =
        dao.observeInBounds(minLat, maxLat, minLng, maxLng)
            .map { it.map(TileMapper::entityToDomain) }

    override suspend fun revealTiles(tiles: List<ScratchTile>): Int {
        if (tiles.isEmpty()) return 0
        val rowIds = dao.insertTiles(tiles.map(TileMapper::domainToEntity))
        return rowIds.count { it != -1L }
    }

    override fun observeTileCount(): Flow<Long> = dao.observeCount()

    override suspend fun getTotalTileCount(): Long = dao.getCount()

    override suspend fun getUnsyncedTiles(): List<ScratchTile> =
        dao.getUnsynced().map(TileMapper::entityToDomain)

    override suspend fun getAllTiles(): List<ScratchTile> =
        dao.getAll().map(TileMapper::entityToDomain)

    override suspend fun markAllTilesSynced() = dao.markAllSynced()
    override suspend fun markAllTilesUnsynced() = dao.markAllUnsynced()
}