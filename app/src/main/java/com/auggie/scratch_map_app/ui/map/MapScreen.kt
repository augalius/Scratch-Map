package com.auggie.scratch_map_app.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.auggie.scratch_map_app.ui.theme.FogColor
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auggie.scratch_map_app.ui.components.LocationPermissionHandler
import com.auggie.scratch_map_app.ui.map.fog.FogLayerManager
import coil.compose.AsyncImage
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.plugin.gestures.gestures

@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    val lifeCycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifeCycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions()
                viewModel.syncServiceState()
            }
        }
        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose { lifeCycleOwner.lifecycle.removeObserver(observer) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var mapStyleRef by remember { mutableStateOf<Style?>(null) }

    val revealedTiles = uiState.revealedTiles
    val waypoints = uiState.waypoints

    var fogStyleJson by remember { mutableStateOf<String?>(null) }
    var initialStyleLoaded by remember { mutableStateOf(false) }
    val waypointsRef =
        remember { mutableStateOf<List<com.auggie.scratch_map_app.domain.model.Waypoint>>(emptyList()) }
    LaunchedEffect(waypoints) { waypointsRef.value = waypoints }

    LaunchedEffect(revealedTiles, isDark, waypoints, initialStyleLoaded) {
        if (!initialStyleLoaded) {
            fogStyleJson = FogLayerManager.buildStyleJson(
                revealedTiles,
                isDark,
                waypoints
            )
        } else {
            val style = mapStyleRef ?: return@LaunchedEffect
            FogLayerManager.applyFogLayer(style, FogLayerManager.buildFogGeoJson(revealedTiles))
            FogLayerManager.updateWaypoints(style, waypoints)
        }
    }

    // One-shot center + zoom when the button is pressed
    LaunchedEffect(uiState.shouldCenterOnUser, uiState.currentLocation) {
        if (uiState.shouldCenterOnUser && uiState.currentLocation != null) {
            val loc = uiState.currentLocation!!
            mapViewRef?.mapboxMap?.flyTo(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(loc.longitude, loc.latitude))
                    .zoom(16.0)
                    .build()
            )
            viewModel.onCameraCentered()
        }
    }

    // Continuous follows and re-centres whenever location updates
    LaunchedEffect(uiState.isFollowing, uiState.currentLocation) {
        if (uiState.isFollowing && !uiState.shouldCenterOnUser && uiState.currentLocation != null) {
            val loc = uiState.currentLocation!!
            mapViewRef?.mapboxMap?.easeTo(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(loc.longitude, loc.latitude))
                    .build()
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FogColor)
    ) {
        if (!uiState.hasPermissions) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                LocationPermissionHandler(onAllGranted = { viewModel.onPermissionsGranted() })
            }
        } else if (fogStyleJson != null) {
            val styleJson = fogStyleJson!!
            val hasPermissions = uiState.hasPermissions

            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        alpha = 0f

                        val loc = uiState.currentLocation
                        mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .center(
                                    Point.fromLngLat(
                                        loc?.longitude ?: 23.9036,
                                        loc?.latitude ?: 54.8985
                                    )
                                )
                                .zoom(viewModel.savedCameraZoom)
                                .build()
                        )

                        mapboxMap.addOnCameraChangeListener {
                            viewModel.saveZoom(mapboxMap.cameraState.zoom)
                        }

                        @Suppress("DEPRECATION")
                        mapboxMap.loadStyleJson(styleJson) { style ->
                            WaypointMarkerGenerator.addMarkerImages(style)
                            mapStyleRef = style
                            initialStyleLoaded = true
                            fadeInWhenStyled()
                        }

                        // Stop following when the user manually pans or zooms
                        gestures.addOnMoveListener(object :
                            com.mapbox.maps.plugin.gestures.OnMoveListener {
                            override fun onMoveBegin(detector: com.mapbox.android.gestures.MoveGestureDetector) {
                                viewModel.stopFollowing()
                            }

                            override fun onMove(detector: com.mapbox.android.gestures.MoveGestureDetector) =
                                false

                            override fun onMoveEnd(detector: com.mapbox.android.gestures.MoveGestureDetector) {}
                        })
                        gestures.addOnScaleListener(object :
                            com.mapbox.maps.plugin.gestures.OnScaleListener {
                            override fun onScaleBegin(detector: com.mapbox.android.gestures.StandardScaleGestureDetector) {
                                viewModel.stopFollowing()
                            }

                            override fun onScale(detector: com.mapbox.android.gestures.StandardScaleGestureDetector) {}
                            override fun onScaleEnd(detector: com.mapbox.android.gestures.StandardScaleGestureDetector) {}
                        })

                        // Tap on waypoint
                        mapboxMap.addOnMapClickListener { point ->
                            val screen = mapboxMap.pixelForCoordinate(point)
                            val geometry =
                                RenderedQueryGeometry(ScreenCoordinate(screen.x, screen.y))

                            mapboxMap.queryRenderedFeatures(
                                geometry,
                                RenderedQueryOptions(listOf("waypoint-unlocked"), null)
                            ) { result ->
                                val feature =
                                    result.value?.firstOrNull() ?: return@queryRenderedFeatures
                                val name = feature.queriedFeature.feature.getStringProperty("name")
                                    ?: return@queryRenderedFeatures
                                val found =
                                    waypointsRef.value.find { it.name == name && it.isUnlocked }
                                if (found != null) viewModel.showWaypointPopup(found)
                            }

                            false
                        }

                        applyScratchMapChrome(locationEnabled = hasPermissions)

                        mapViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }


        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 96.dp, end = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { viewModel.centerOnUser() },
                containerColor = if (uiState.isFollowing)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surface
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    "Center on my location",
                    tint = if (uiState.isFollowing)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }

    uiState.lastUnlockedWaypoint?.let { waypoint ->
        WaypointUnlockedDialog(
            waypoint = waypoint,
            onDismiss = { viewModel.clearUnlockedWaypoint() }
        )
    }

    uiState.selectedWaypoint?.let { waypoint ->
        WaypointUnlockedDialog(
            waypoint = waypoint,
            onDismiss = { viewModel.clearSelectedWaypoint() }
        )
    }
}

@Composable
private fun WaypointUnlockedDialog(
    waypoint: com.auggie.scratch_map_app.domain.model.Waypoint,
    onDismiss: () -> Unit
) {
    val categoryColor =
        Color(android.graphics.Color.parseColor(waypoint.category.colorHex))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Photo banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    if (waypoint.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = waypoint.imageUrl,
                            contentDescription = waypoint.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            categoryColor.copy(alpha = 0.8f),
                                            categoryColor.copy(alpha = 0.4f)
                                        )
                                    )
                                )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        categoryColor.copy(alpha = if (waypoint.imageUrl.isNotBlank()) 0.25f else 0f),
                                        categoryColor.copy(alpha = if (waypoint.imageUrl.isNotBlank()) 0.45f else 0f)
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = waypoint.category.icon,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = Color.White
                        )
                        Text(
                            "Waypoint Discovered!",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        waypoint.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        waypoint.category.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = categoryColor
                    )
                    if (waypoint.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            waypoint.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp, MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("OK", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
