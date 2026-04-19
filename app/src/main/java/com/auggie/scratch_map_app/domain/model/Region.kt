package com.auggie.scratch_map_app.domain.model

data class Region(
    val id: String,
    val name: String,
    val description: String = "",
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double,
    val goalTiles: Int
)

data class RegionStats(
    val region: Region,
    val tilesRevealed: Long,
    val waypointsUnlocked: Int,
    val waypointsTotal: Int,
    val achievements: List<Achievement>,
    val revealedAreaKm2: Double = 0.0,
    val goalAreaKm2: Double = 0.0
) {
    val progress: Float
        get() = if (goalAreaKm2 > 0.0)
            (revealedAreaKm2 / goalAreaKm2).toFloat().coerceIn(0f, 1f)
        else
            (tilesRevealed.toFloat() / region.goalTiles).coerceIn(0f, 1f)
}
