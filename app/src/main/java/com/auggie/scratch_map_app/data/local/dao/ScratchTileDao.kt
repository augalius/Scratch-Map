package com.auggie.scratch_map_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.auggie.scratch_map_app.data.local.entity.ScratchTileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScratchTileDao {
    @Query("SELECT * FROM scratch_tiles")
    fun observeAll(): Flow<List<ScratchTileEntity>>

    @Query(
        """SELECT * FROM scratch_tiles 
        WHERE center_lat BETWEEN :minLat AND :maxLat 
        AND center_lng BETWEEN :minLng AND :maxLng"""
    )
    fun observeInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): Flow<List<ScratchTileEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTiles(tiles: List<ScratchTileEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM scratch_tiles")
    fun observeCount(): Flow<Long>

    @Query("SELECT COUNT(*) FROM scratch_tiles")
    suspend fun getCount(): Long

    @Query("SELECT * FROM scratch_tiles WHERE synced_to_cloud = 0 ORDER BY tile_x, tile_y")
    suspend fun getUnsynced(): List<ScratchTileEntity>

    /**
     * Ordered deliberately: [com.auggie.scratch_map_app.data.remote.FirestoreSyncService] derives
     * cloud chunk ids from each tile's index in this list, so an unstable order would shuffle
     * tiles between chunks and orphan already-uploaded data.
     */
    @Query("SELECT * FROM scratch_tiles ORDER BY tile_x, tile_y")
    suspend fun getAll(): List<ScratchTileEntity>

    @Query("UPDATE scratch_tiles SET synced_to_cloud = 1")
    suspend fun markAllSynced()

    @Query("UPDATE scratch_tiles SET synced_to_cloud = 0")
    suspend fun markAllUnsynced()
}