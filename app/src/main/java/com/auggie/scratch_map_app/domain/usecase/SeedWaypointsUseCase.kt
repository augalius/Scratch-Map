package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.data.seed.WaypointSeeder
import com.auggie.scratch_map_app.domain.repository.WaypointRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedWaypointsUseCase @Inject constructor(
    private val waypointRepository: WaypointRepository,
    private val seeder: WaypointSeeder
) {
    suspend operator fun invoke() {
        val waypoints = seeder.fetchWaypoints()
        if (waypoints.isEmpty()) return

        waypointRepository.insertAll(waypoints)
    }
}
