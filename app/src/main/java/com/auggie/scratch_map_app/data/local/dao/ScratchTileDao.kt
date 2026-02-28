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
}