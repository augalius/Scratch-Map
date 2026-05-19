package com.auggie.scratch_map_app.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.auggie.scratch_map_app.domain.repository.SettingsRepository
import com.auggie.scratch_map_app.service.LocationTrackingService
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auggie.scratch_map_app.domain.repository.AccountStatusRepository
import com.auggie.scratch_map_app.domain.repository.AuthRepository
import com.auggie.scratch_map_app.ui.auth.AccountSuspendedScreen
import com.auggie.scratch_map_app.ui.navigation.Screen
import com.auggie.scratch_map_app.ui.navigation.ScratchMapNavHost
import com.auggie.scratch_map_app.ui.theme.ScratchMapTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository
    @Inject
    lateinit var settingsRepository: SettingsRepository
    @Inject
    lateinit var accountStatusRepository: AccountStatusRepository

    private var stoppedForBackground = false

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            val settings = settingsRepository.getSettings()
            if (!settings.isTrackingEnabled && LocationTrackingService.isRunning) {
                LocationTrackingService.stop(this@MainActivity)
                stoppedForBackground = true
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (stoppedForBackground) {
            stoppedForBackground = false
            LocationTrackingService.start(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val isFirstLaunch = isFirstLaunch()
        val isLoggedIn = authRepository.currentUser != null
        setContent {
            ScratchMapTheme {
                val suspended by accountStatusRepository.isDisabled.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                if (suspended) {
                    LaunchedEffect(Unit) {
                        if (LocationTrackingService.isRunning) LocationTrackingService.stop(this@MainActivity)
                    }
                    AccountSuspendedScreen(onSignOut = { scope.launch { authRepository.logout() } })
                } else {
                    ScratchMapMainScreen(isFirstLaunch = isFirstLaunch, isLoggedIn = isLoggedIn)
                }
            }
        }
    }

    private fun isFirstLaunch(): Boolean {
        val prefs = getSharedPreferences("scratch_map_app", Context.MODE_PRIVATE)
        val first = prefs.getBoolean("is_first_launch", true)
        if (first) prefs.edit().putBoolean("is_first_launch", false).apply()
        return first
    }
}

private data class NavItem(
    val screen: Screen,
    val label: String,
    val selected: ImageVector,
    val unselected: ImageVector
)

@Composable
private fun ScratchMapMainScreen(isFirstLaunch: Boolean, isLoggedIn: Boolean) {
    val navController = rememberNavController()
    val items = remember {
        listOf(
            NavItem(Screen.Map, "Map", Icons.Filled.Explore, Icons.Outlined.Explore),
            NavItem(Screen.Stats, "Stats", Icons.Filled.Insights, Icons.Outlined.Insights),
            NavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }

    val startDestination = when {
        isLoggedIn && isFirstLaunch -> Screen.Onboarding.route
        isLoggedIn -> Screen.Map.route
        else -> Screen.Auth.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(
        Screen.Map.route, Screen.Stats.route, Screen.Settings.route, Screen.RegionDetail.route
    )

    var bottomBarScrollVisible by remember { mutableStateOf(true) }

    LaunchedEffect(currentRoute) {
        if (currentRoute != Screen.RegionDetail.route) {
            bottomBarScrollVisible = true
        }
    }

    val navBarSlide by animateDpAsState(
        targetValue = if (showBottomBar && bottomBarScrollVisible) 0.dp else 120.dp,
        animationSpec = tween(durationMillis = 250),
        label = "navBarSlide"
    )

    Scaffold(contentWindowInsets = WindowInsets(0)) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            ScratchMapNavHost(
                navController = navController,
                startDestination = startDestination,
                isFirstLaunch = isFirstLaunch,
                modifier = Modifier.fillMaxSize(),
                onBottomBarVisibilityChanged = { bottomBarScrollVisible = it }
            )

            if (showBottomBar) {
                val currentDest = navBackStackEntry?.destination
                NavigationBar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = navBarSlide)
                ) {
                    items.forEach { item ->
                        val isSelected =
                            currentDest?.hierarchy?.any { it.route == item.screen.route } == true
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (isSelected) item.selected else item.unselected,
                                    item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    }

}