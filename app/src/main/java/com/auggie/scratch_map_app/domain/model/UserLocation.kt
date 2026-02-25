package com.auggie.scratch_map_app.domain.model

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float?
) {
    fun isSufficientlyAccurate(thresholdMeters: Float = 50f): Boolean {
        return accuracy <= thresholdMeters
    }
}