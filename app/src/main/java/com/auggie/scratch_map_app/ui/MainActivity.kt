package com.auggie.scratch_map_app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.auggie.scratch_map_app.domain.repository.AccountStatusRepository
import com.auggie.scratch_map_app.domain.repository.AuthRepository
import com.auggie.scratch_map_app.service.LocationTrackingService
import com.auggie.scratch_map_app.ui.auth.AccountSuspendedScreen
import com.auggie.scratch_map_app.ui.navigation.Screen
import com.auggie.scratch_map_app.ui.navigation.ScratchMapNavHost
import com.auggie.scratch_map_app.ui.theme.ScratchMapTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository
    @Inject
    lateinit var accountStatusRepository: AccountStatusRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                    ScratchMapMainScreen(isLoggedIn = isLoggedIn)
                }
            }
        }
    }
}

private data class NavItem(
    val screen: Screen,
    val label: String,
    val selected: ImageVector,
    val unselected: ImageVector
)

@Composable
private fun ScratchMapMainScreen(isLoggedIn: Boolean) {
    val navController = rememberNavController()
    val items = remember {
        listOf(
            NavItem(Screen.Map, "Map", Icons.Filled.Explore, Icons.Outlined.Explore),
            NavItem(Screen.Stats, "Stats", Icons.Filled.Insights, Icons.Outlined.Insights)
        )
    }

    val startDestination = if (isLoggedIn) Screen.Map.route else Screen.Auth.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(
        Screen.Map.route, Screen.Stats.route, Screen.RegionDetail.route
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