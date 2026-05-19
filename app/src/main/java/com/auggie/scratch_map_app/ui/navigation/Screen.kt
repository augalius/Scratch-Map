package com.auggie.scratch_map_app.ui.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Map : Screen("map")
    data object Stats : Screen("stats")
    data object Settings : Screen("settings")
    data object RegionDetail : Screen("region/{regionId}") {
        fun route(regionId: String) = "region/$regionId"
    }

    data object Trail : Screen("trail/{trailId}") {
        fun route(trailId: String) = "trail/$trailId"
    }
}

