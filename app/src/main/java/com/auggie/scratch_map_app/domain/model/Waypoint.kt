package com.auggie.scratch_map_app.domain.model

enum class WaypointCategory {
    CHURCH, MONUMENT, PARK, VIEWPOINT, MUSEUM, HISTORIC, LANDMARK, FORT, OTHER
}

data class Waypoint(
    val id: Long = 0,
    val stringId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String = "",
    val category: WaypointCategory = WaypointCategory.OTHER,
    val unlockRadiusMeters: Int = 50,
    val isUnlocked: Boolean = false
)
