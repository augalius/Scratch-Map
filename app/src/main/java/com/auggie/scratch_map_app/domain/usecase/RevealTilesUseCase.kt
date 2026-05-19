package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.model.TrackingDefaults
import com.auggie.scratch_map_app.domain.model.UserLocation
import com.auggie.scratch_map_app.domain.repository.RoadGeometryRepository
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
import com.auggie.scratch_map_app.util.TileCalculator
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RevealTilesUseCase @Inject constructor(
    private val tileRepository: ScratchTileRepository,
    private val roadGeometryRepository: RoadGeometryRepository
) {
    suspend operator fun invoke(location: UserLocation): Int {
        if (!location.isSufficientlyAccurate(TrackingDefaults.ACCURACY_THRESHOLD_METERS)) {
            Timber.d(
                "Location rejected: accuracy %.1fm > threshold %.1fm",
                location.accuracy, TrackingDefaults.ACCURACY_THRESHOLD_METERS
            )
            return 0
        }

        val radii =
            roadGeometryRepository.getDirectionalRadii(location.latitude, location.longitude)

        val tiles = TileCalculator.createTilesDirectional(
            lat = location.latitude,
            lng = location.longitude,
            radii = radii,
            zoom = TileCalculator.TILE_ZOOM
        )

        if (tiles.isEmpty()) return 0

        val newCount = tileRepository.revealTiles(tiles)
        if (newCount > 0) {
            Timber.i(
                "Revealed %d new tiles at (%.6f, %.6f) radii=[%s]",
                newCount, location.latitude, location.longitude, radii
            )
        }
        return newCount
    }
}
