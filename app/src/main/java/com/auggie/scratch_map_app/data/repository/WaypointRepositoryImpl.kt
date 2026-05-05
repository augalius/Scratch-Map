package com.auggie.scratch_map_app.data.repository

import com.auggie.scratch_map_app.data.local.converter.WaypointMapper
import com.auggie.scratch_map_app.data.local.dao.WaypointDao
import com.auggie.scratch_map_app.domain.model.Waypoint
import com.auggie.scratch_map_app.domain.repository.WaypointRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaypointRepositoryImpl @Inject constructor(
    private val dao: WaypointDao
) : WaypointRepository {

    override fun observeAll(): Flow<List<Waypoint>> =
        dao.observeAll().map { entities -> entities.map(WaypointMapper::entityToDomain) }

    override fun observeUnlockedCount(): Flow<Int> =
        dao.observeUnlockedCount()

    override suspend fun getLockedWaypointsInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): List<Waypoint> =
        dao.getLockedWaypointsInBounds(minLat, maxLat, minLng, maxLng)
            .map(WaypointMapper::entityToDomain)

    override suspend fun getByStringId(stringId: String): Waypoint? =
        dao.getByStringId(stringId)?.let(WaypointMapper::entityToDomain)

    override suspend fun insertAll(waypoints: List<Waypoint>) =
        dao.insertAll(waypoints.map(WaypointMapper::domainToEntity))

    override suspend fun updateContentFields(waypoint: Waypoint) =
        dao.updateContentFields(
            stringId = waypoint.stringId,
            name = waypoint.name,
            description = waypoint.description,
            latitude = waypoint.latitude,
            longitude = waypoint.longitude,
            category = waypoint.category.name,
            unlockRadiusMeters = waypoint.unlockRadiusMeters,
            imageUrl = waypoint.imageUrl
        )

    override suspend fun deleteNotIn(stringIds: List<String>) =
        dao.deleteNotIn(stringIds)

    override suspend fun markUnlocked(id: Long) =
        dao.markUnlocked(id)
}
