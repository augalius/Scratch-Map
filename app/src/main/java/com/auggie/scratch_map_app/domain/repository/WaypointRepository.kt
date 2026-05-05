package com.auggie.scratch_map_app.domain.repository

import com.auggie.scratch_map_app.domain.model.Waypoint
import kotlinx.coroutines.flow.Flow

interface WaypointRepository {
    fun observeAll(): Flow<List<Waypoint>>
    fun observeUnlockedCount(): Flow<Int>

    suspend fun getLockedWaypointsInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): List<Waypoint>
    suspend fun getByStringId(stringId: String): Waypoint?

    suspend fun insertAll(waypoints: List<Waypoint>)
    suspend fun updateContentFields(waypoint: Waypoint)
    suspend fun deleteNotIn(stringIds: List<String>)
    suspend fun markUnlocked(id: Long)
}
