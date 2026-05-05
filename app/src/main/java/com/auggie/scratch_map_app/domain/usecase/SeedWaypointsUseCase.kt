package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.repository.WaypointContentSource
import com.auggie.scratch_map_app.domain.repository.WaypointRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedWaypointsUseCase @Inject constructor(
    private val waypointRepository: WaypointRepository,
    private val seeder: WaypointContentSource
) {
    suspend operator fun invoke() {
        val waypoints = seeder.fetchWaypoints()
        if (waypoints.isEmpty()) return

        waypointRepository.insertAll(waypoints)
        waypoints.forEach { waypointRepository.updateContentFields(it) }
        waypointRepository.deleteNotIn(waypoints.map { it.stringId })
    }
}
