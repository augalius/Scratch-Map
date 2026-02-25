package com.auggie.scratch_map_app.domain.model

/**
 * Represents a single revealed tile on the scratch map.
 * The world is divided into a grid using Slippy Map tile coordinates.
 */
data class ScratchTile(
    val tileX: Int,
    val tileY: Int,
    val zoomLevel: Int,
    val centerLat: Double,
    val centerLng: Double
)