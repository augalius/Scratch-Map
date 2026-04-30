package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.model.UserLocation
import com.auggie.scratch_map_app.domain.model.Waypoint
import com.auggie.scratch_map_app.domain.repository.SyncRepository
import com.auggie.scratch_map_app.domain.repository.WaypointRepository
import com.auggie.scratch_map_app.util.TileCalculator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckWaypointProximityUseCase @Inject constructor(
    private val waypointRepository: WaypointRepository,
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(location: UserLocation): List<Waypoint> {
        // Coarse spatial pre-filter: only pull locked waypoints near the user (index-backed
        // BETWEEN query) instead of scanning the whole table on every location update.
        // SEARCH_RADIUS_METERS must comfortably exceed the largest waypoint unlock radius so the
        // box never excludes a waypoint whose circle would reach the user.
        val box = TileCalculator.boundingBox(
            location.latitude, location.longitude, SEARCH_RADIUS_METERS
        )
        val locked = waypointRepository.getLockedWaypointsInBounds(
            box.minLat, box.maxLat, box.minLng, box.maxLng
        )
        val unlocked = mutableListOf<Waypoint>()

        for (wp in locked) {
            val distance = TileCalculator.haversineDistance(
                location.latitude, location.longitude,
                wp.latitude, wp.longitude
            )
            if (distance <= wp.unlockRadiusMeters) {
                val now = System.currentTimeMillis()
                waypointRepository.markUnlocked(wp.id)
                syncRepository.syncWaypointUnlock(wp.stringId, now)
                unlocked.add(wp.copy(isUnlocked = true))
            }
        }
        return unlocked
    }

    companion object {
        /**
         * Half-extent of the pre-filter box. Waypoints farther than this from the user are never
         * considered, so it must stay well above the largest configured unlock radius
         * (currently ≤200 m). 1 km leaves a ~5× margin while reducing a country-scale table to a
         * handful of nearby rows per location update.
         */
        private const val SEARCH_RADIUS_METERS = 1_000.0
    }
}
