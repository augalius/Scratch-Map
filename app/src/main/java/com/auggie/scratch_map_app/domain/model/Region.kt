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
