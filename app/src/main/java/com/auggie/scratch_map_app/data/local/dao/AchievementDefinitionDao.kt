package com.auggie.scratch_map_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.auggie.scratch_map_app.data.local.entity.AchievementDefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDefinitionDao {
    @Query("SELECT * FROM achievement_definitions ORDER BY target_value ASC")
    fun observeAll(): Flow<List<AchievementDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(definitions: List<AchievementDefinitionEntity>)

    @Query("DELETE FROM achievement_definitions WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<String>)

}
