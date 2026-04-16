package com.auggie.scratch_map_app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.auggie.scratch_map_app.ui.map.MapScreen
import com.auggie.scratch_map_app.ui.stats.RegionDetailScreen
import com.auggie.scratch_map_app.ui.stats.StatsScreen

@Composable
fun ScratchMapNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Map.route,
    modifier: Modifier = Modifier,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Map.route) { MapScreen() }
        composable(Screen.Stats.route) {
            StatsScreen(onRegionClick = { regionId ->
                navController.navigate(Screen.RegionDetail.route(regionId))
            })
        }
        composable(
            route = Screen.RegionDetail.route,
            arguments = listOf(navArgument("regionId") { type = NavType.StringType })
        ) { backStackEntry ->
            RegionDetailScreen(
                regionId = backStackEntry.arguments?.getString("regionId") ?: "",
                onBack = { navController.popBackStack() },
                onBottomBarVisibilityChanged = onBottomBarVisibilityChanged
            )
        }
    }
}
