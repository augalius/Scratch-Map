package com.auggie.scratch_map_app.domain.repository

import com.auggie.scratch_map_app.domain.model.Trail
import kotlinx.coroutines.flow.Flow

interface TrailRepository {
    fun observeAll(): Flow<List<Trail>>
    fun observeTrail(trailId: String): Flow<Trail?>
    suspend fun insertTrails(trails: List<Trail>)
    suspend fun getLockedTrails(): List<Trail>
    suspend fun markTrailUnlocked(trailId: String)
    suspend fun markCheckpointReached(checkpointId: String)
    suspend fun markCheckpointPending(checkpointId: String)
    suspend fun markAllCheckpointsReached(trailId: String)
    suspend fun markTrailCompleted(trailId: String)
    suspend fun deleteNotIn(ids: List<String>)
}
