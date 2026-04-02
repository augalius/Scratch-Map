package com.auggie.scratch_map_app.data.seed

import com.auggie.scratch_map_app.domain.model.Waypoint
import com.auggie.scratch_map_app.domain.model.WaypointCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local starter set of waypoints around Kaunas, used until content is served from a backend.
 */
@Singleton
class WaypointSeeder @Inject constructor() {

    fun fetchWaypoints(): List<Waypoint> = listOf(
        Waypoint(
            stringId = "kaunas_castle",
            name = "Kaunas Castle",
            latitude = 54.8990,
            longitude = 23.8855,
            category = WaypointCategory.FORT
        ),
        Waypoint(
            stringId = "kaunas_town_hall",
            name = "Kaunas Town Hall",
            latitude = 54.8971,
            longitude = 23.8862,
            category = WaypointCategory.HISTORIC
        ),
        Waypoint(
            stringId = "kaunas_cathedral",
            name = "Kaunas Cathedral Basilica",
            latitude = 54.8968,
            longitude = 23.8899,
            category = WaypointCategory.CHURCH
        ),
        Waypoint(
            stringId = "resurrection_church",
            name = "Christ's Resurrection Church",
            latitude = 54.9014,
            longitude = 23.9283,
            category = WaypointCategory.CHURCH
        ),
        Waypoint(
            stringId = "war_museum",
            name = "Vytautas the Great War Museum",
            latitude = 54.8990,
            longitude = 23.9124,
            category = WaypointCategory.MUSEUM
        ),
        Waypoint(
            stringId = "aleksotas_viewpoint",
            name = "Aleksotas Viewpoint",
            latitude = 54.8925,
            longitude = 23.8945,
            category = WaypointCategory.VIEWPOINT
        )
    )
}
