package com.auggie.scratch_map_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.auggie.scratch_map_app.data.local.entity.WaypointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaypointDao {
    @Query("SELECT * FROM waypoints ORDER BY name ASC")
    fun observeAll(): Flow<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints WHERE is_unlocked = 0")
    suspend fun getLockedWaypoints(): List<WaypointEntity>

    /**
     * Locked waypoints whose center falls within the given lat/lng box.
     * Uses the `(latitude, longitude)` index as a coarse spatial pre-filter so proximity
     * checks scan only nearby rows instead of the whole table.
     */
    @Query(
        """SELECT * FROM waypoints
        WHERE is_unlocked = 0
        AND latitude BETWEEN :minLat AND :maxLat
        AND longitude BETWEEN :minLng AND :maxLng"""
    )
    suspend fun getLockedWaypointsInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): List<WaypointEntity>

    @Query("SELECT * FROM waypoints WHERE string_id = :stringId LIMIT 1")
    suspend fun getByStringId(stringId: String): WaypointEntity?

    @Query("SELECT COUNT(*) FROM waypoints WHERE is_unlocked = 1")
    fun observeUnlockedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(waypoints: List<WaypointEntity>)

    @Query("UPDATE waypoints SET is_unlocked = 1 WHERE id = :id")
    suspend fun markUnlocked(id: Long)

    @Query(
        """UPDATE waypoints SET
        name = :name, description = :description,
        latitude = :latitude, longitude = :longitude,
        category = :category, unlock_radius_meters = :unlockRadiusMeters,
        image_url = :imageUrl
        WHERE string_id = :stringId"""
    )
    suspend fun updateContentFields(
        stringId: String, name: String, description: String,
        latitude: Double, longitude: Double, category: String,
        unlockRadiusMeters: Int, imageUrl: String
    )

    /**
     * Drops waypoints no longer present in remote content. [localPrefix] is a SQL LIKE pattern
     * shielding locally created waypoints (trail completions) from being swept away by a
     * content-version bump.
     */
    @Query(
        """DELETE FROM waypoints
        WHERE string_id NOT IN (:stringIds)
        AND string_id NOT LIKE :localPrefix"""
    )
    suspend fun deleteNotIn(stringIds: List<String>, localPrefix: String)
}
