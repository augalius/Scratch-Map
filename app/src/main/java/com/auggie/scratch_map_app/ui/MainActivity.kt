package com.auggie.scratch_map_app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.auggie.scratch_map_app.ui.navigation.Screen
import com.auggie.scratch_map_app.ui.navigation.ScratchMapNavHost
import com.auggie.scratch_map_app.ui.theme.ScratchMapTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScratchMapTheme {
                ScratchMapMainScreen()
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
private fun ScratchMapMainScreen() {
    val navController = rememberNavController()
    val items = remember {
        listOf(
            NavItem(Screen.Map, "Map", Icons.Filled.Explore, Icons.Outlined.Explore),
            NavItem(Screen.Stats, "Stats", Icons.Filled.Insights, Icons.Outlined.Insights)
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(
        Screen.Map.route, Screen.Stats.route
    )

    Scaffold(contentWindowInsets = WindowInsets(0)) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            ScratchMapNavHost(
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )

            if (showBottomBar) {
                val currentDest = navBackStackEntry?.destination
                NavigationBar(
                    modifier = Modifier.align(Alignment.BottomCenter)
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