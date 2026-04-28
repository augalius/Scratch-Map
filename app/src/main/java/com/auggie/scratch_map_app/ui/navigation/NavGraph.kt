package com.auggie.scratch_map_app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.auggie.scratch_map_app.ui.auth.AuthScreen
import com.auggie.scratch_map_app.ui.auth.AuthViewModel
import com.auggie.scratch_map_app.ui.map.MapScreen
import com.auggie.scratch_map_app.ui.stats.RegionDetailScreen
import com.auggie.scratch_map_app.ui.stats.StatsScreen

@Composable
fun ScratchMapNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Auth.route,
    modifier: Modifier = Modifier,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {}
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(authState.user) {
        val currentRoute = navController.currentDestination?.route
        if (authState.user != null && currentRoute == Screen.Auth.route) {
            navController.navigate(Screen.Map.route) { popUpTo(Screen.Auth.route) { inclusive = true } }
        } else if (authState.user == null && currentRoute != Screen.Auth.route) {
            navController.navigate(Screen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Auth.route) { AuthScreen(authViewModel) }

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
