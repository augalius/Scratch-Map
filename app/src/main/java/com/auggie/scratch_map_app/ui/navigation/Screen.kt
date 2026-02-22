package com.auggie.scratch_map_app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
}
