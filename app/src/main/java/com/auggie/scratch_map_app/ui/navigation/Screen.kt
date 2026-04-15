package com.auggie.scratch_map_app.ui.navigation

sealed class Screen(val route: String) {
    data object Map : Screen("map")
    data object Stats : Screen("stats")
}
