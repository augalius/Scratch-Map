package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.model.Trail
import com.auggie.scratch_map_app.domain.model.Waypoint
import com.auggie.scratch_map_app.domain.model.WaypointCategory
import com.auggie.scratch_map_app.domain.repository.SyncRepository
import com.auggie.scratch_map_app.domain.repository.TrailRepository
import com.auggie.scratch_map_app.domain.repository.WaypointRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finalises a trail once its last checkpoint is reached: marks it complete, drops a permanent
 * waypoint at its trailhead, and pushes the completion to the backend.
 *
 * Lives here rather than in `TrailViewModel` so the UI does not reach into the data layer, and
 * so the three steps stay together.
 */
@Singleton
class CompleteTrailUseCase @Inject constructor(
    private val trailRepository: TrailRepository,
    private val waypointRepository: WaypointRepository,
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(trail: Trail, completedAt: Long = System.currentTimeMillis()) {
        trailRepository.markTrailCompleted(trail.id)
        insertTrailAsWaypoint(trail)
        syncRepository.syncTrailCompletion(trail.id, completedAt)
    }

    private suspend fun insertTrailAsWaypoint(trail: Trail) {
        val lat = trail.startLat ?: return
        val lng = trail.startLng ?: return
        waypointRepository.insertAll(
            listOf(
                Waypoint(
                    stringId = "${Waypoint.TRAIL_ID_PREFIX}${trail.id}",
                    name = trail.name,
                    latitude = lat,
                    longitude = lng,
                    description = trail.description,
                    category = WaypointCategory.TRAIL,
                    unlockRadiusMeters = trail.unlockRadiusMeters,
                    isUnlocked = true
                )
            )
        )
    }
}
