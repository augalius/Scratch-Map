package com.auggie.scratch_map_app.domain.model

data class Trail(
    val id: String,
    val name: String,
    val description: String = "",
    val difficulty: TrailDifficulty = TrailDifficulty.EASY,
    val estimatedDistanceKm: Double = 0.0,
    val checkpoints: List<TrailCheckpoint> = emptyList(),
    val isCompleted: Boolean = false,
    val isUnlocked: Boolean = false,
    val unlockRadiusMeters: Int = 100
) {
    /** Trailhead. Resolved once per instance — callers always read latitude and longitude together. */
    val start: TrailCheckpoint? = checkpoints.minByOrNull { it.order }
    val startLat: Double? get() = start?.latitude
    val startLng: Double? get() = start?.longitude
    val reachedCount: Int get() = checkpoints.count { it.isReached }
    val progress: Float get() = if (checkpoints.isEmpty()) 0f else reachedCount.toFloat() / checkpoints.size
}
