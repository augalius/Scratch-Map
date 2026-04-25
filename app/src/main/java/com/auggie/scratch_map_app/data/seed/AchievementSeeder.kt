package com.auggie.scratch_map_app.data.seed

import com.auggie.scratch_map_app.domain.model.AchievementDefinition
import com.auggie.scratch_map_app.domain.model.StageTarget
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local starter achievements, used until achievement content is served from a backend.
 * `{region}` in a description is replaced with the region's name.
 */
@Singleton
class AchievementSeeder @Inject constructor() {

    fun fetchAchievements(): List<AchievementDefinition> = listOf(
        AchievementDefinition(
            id = "first_steps",
            title = "First Steps",
            description = "Reveal 100 tiles in {region}",
            iconName = "directions_walk",
            targetValue = 100,
            metricType = "TILES",
            isRegional = true
        ),
        AchievementDefinition(
            id = "cartographer",
            title = "Cartographer",
            description = "Explore {region}",
            iconName = "map",
            targetValue = 0,
            metricType = "TILES_PERCENT",
            isRegional = true,
            stages = listOf(
                StageTarget.Percent(10),
                StageTarget.Percent(25),
                StageTarget.Percent(50),
                StageTarget.Percent(100)
            )
        ),
        AchievementDefinition(
            id = "sightseer",
            title = "Sightseer",
            description = "Discover 3 waypoints in {region}",
            iconName = "place",
            targetValue = 3,
            metricType = "WAYPOINTS",
            isRegional = true
        ),
        AchievementDefinition(
            id = "pilgrim",
            title = "Pilgrim",
            description = "Visit every church in {region}",
            iconName = "church",
            targetValue = 0,
            metricType = "WAYPOINTS_CATEGORY",
            isRegional = true,
            category = "CHURCH"
        ),
        AchievementDefinition(
            id = "kaunas_landmarks",
            title = "Kaunas Landmarks",
            description = "Discover every waypoint in Kaunas",
            iconName = "location_city",
            targetValue = 0,
            metricType = "WAYPOINTS_ALL",
            regionId = "kaunas"
        )
    )
}
