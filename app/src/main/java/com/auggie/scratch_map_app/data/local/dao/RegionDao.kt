package com.auggie.scratch_map_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.auggie.scratch_map_app.data.local.entity.RegionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegionDao {
    @Query("SELECT * FROM regions ORDER BY name ASC")
    fun observeAll(): Flow<List<RegionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(regions: List<RegionEntity>)

    @Query("DELETE FROM regions WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<String>)
}
