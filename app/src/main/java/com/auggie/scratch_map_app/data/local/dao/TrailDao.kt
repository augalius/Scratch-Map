package com.auggie.scratch_map_app.data.local.dao

import androidx.room.*
import com.auggie.scratch_map_app.data.local.entity.TrailCheckpointEntity
import com.auggie.scratch_map_app.data.local.entity.TrailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrailDao {

    @Query("SELECT * FROM trails ORDER BY name")
    fun observeAll(): Flow<List<TrailEntity>>

    @Query("SELECT * FROM trails WHERE id = :id")
    suspend fun getById(id: String): TrailEntity?

    @Query("SELECT * FROM trails WHERE is_unlocked = 0")
    suspend fun getLockedTrails(): List<TrailEntity>

    @Query("UPDATE trails SET is_unlocked = 1 WHERE id = :id")
    suspend fun markTrailUnlocked(id: String)

    @Query("SELECT * FROM trail_checkpoints WHERE trail_id = :trailId ORDER BY checkpoint_order")
    fun observeCheckpoints(trailId: String): Flow<List<TrailCheckpointEntity>>

    @Query("SELECT * FROM trail_checkpoints WHERE trail_id = :trailId ORDER BY checkpoint_order")
    suspend fun getCheckpoints(trailId: String): List<TrailCheckpointEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrail(trail: TrailEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheckpoints(checkpoints: List<TrailCheckpointEntity>)

    @Query(
        """UPDATE trail_checkpoints SET
        name = :name, description = :description,
        latitude = :latitude, longitude = :longitude,
        checkpoint_order = :checkpointOrder, unlock_radius_meters = :unlockRadiusMeters,
        question = :question, answer = :answer
        WHERE id = :id"""
    )
    suspend fun updateCheckpointContent(
        id: String, name: String, description: String,
        latitude: Double, longitude: Double,
        checkpointOrder: Int, unlockRadiusMeters: Int,
        question: String, answer: String
    )

    @Query("UPDATE trail_checkpoints SET is_pending_answer = 1 WHERE id = :id")
    suspend fun markCheckpointPending(id: String)

    @Query("UPDATE trail_checkpoints SET is_pending_answer = 0 WHERE id = :id")
    suspend fun clearCheckpointPending(id: String)

    @Query("UPDATE trail_checkpoints SET is_reached = 1 WHERE id = :checkpointId")
    suspend fun markCheckpointReached(checkpointId: String)

    @Query("UPDATE trails SET name = :name, description = :description, difficulty = :difficulty, estimated_distance_km = :distanceKm, unlock_radius_meters = :unlockRadius WHERE id = :id")
    suspend fun updateTrailContent(
        id: String,
        name: String,
        description: String,
        difficulty: String,
        distanceKm: Float,
        unlockRadius: Int
    )

    @Query("UPDATE trail_checkpoints SET is_reached = 1 WHERE trail_id = :trailId")
    suspend fun markAllCheckpointsReached(trailId: String)

    @Query("UPDATE trails SET is_completed = 1 WHERE id = :trailId")
    suspend fun markTrailCompleted(trailId: String)

    @Query("DELETE FROM trails WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<String>)
}
