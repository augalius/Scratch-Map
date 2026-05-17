package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.model.Trail
import com.auggie.scratch_map_app.domain.model.UserLocation
import com.auggie.scratch_map_app.domain.repository.TrailRepository
import com.auggie.scratch_map_app.util.TileCalculator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckTrailProximityUseCase @Inject constructor(
    private val trailRepository: TrailRepository
) {
    suspend operator fun invoke(location: UserLocation): List<Trail> {
        val locked = trailRepository.getLockedTrails()
        val unlocked = mutableListOf<Trail>()
        for (trail in locked) {
            val startLat = trail.startLat ?: continue
            val startLng = trail.startLng ?: continue
            val distance = TileCalculator.haversineDistance(
                location.latitude, location.longitude,
                startLat, startLng
            )
            if (distance <= trail.unlockRadiusMeters) {
                trailRepository.markTrailUnlocked(trail.id)
                unlocked.add(trail)
            }
        }
        return unlocked
    }
}
