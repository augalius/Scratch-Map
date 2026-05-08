package com.auggie.scratch_map_app.domain.model

data class TrailCheckpoint(
    val id: String,
    val trailId: String,
    val name: String,
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val order: Int = 0,
    val unlockRadiusMeters: Int = 50,
    val isReached: Boolean = false
)
