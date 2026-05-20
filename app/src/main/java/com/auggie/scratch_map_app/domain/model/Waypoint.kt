package com.auggie.scratch_map_app.domain.model

enum class WaypointCategory {
    CHURCH, MONUMENT, PARK, VIEWPOINT, MUSEUM, HISTORIC, LANDMARK, FORT, OTHER, TRAIL
}

data class Waypoint(
    val id: Long = 0,
    val stringId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String = "",
    val category: WaypointCategory = WaypointCategory.OTHER,
    val unlockRadiusMeters: Int = 200,
    val imageUrl: String = "",
    val isUnlocked: Boolean = false
) {
    companion object {
        /**
         * Prefix for waypoints created on the device when a trail is completed. These represent
         * user progress rather than synced content, so content seeding must never delete them.
         */
        const val TRAIL_ID_PREFIX = "trail_"
    }
}
