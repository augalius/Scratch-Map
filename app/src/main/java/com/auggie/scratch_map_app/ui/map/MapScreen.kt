package com.auggie.scratch_map_app.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.auggie.scratch_map_app.ui.theme.FogColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auggie.scratch_map_app.ui.components.LocationPermissionHandler
import com.auggie.scratch_map_app.ui.map.debug.DebugTileOverlay
import com.auggie.scratch_map_app.ui.map.fog.FogLayerManager
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.animation.flyTo
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

    var fogStyleJson by remember { mutableStateOf<String?>(null) }
    var initialStyleLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(revealedTiles, isDark, initialStyleLoaded) {
        if (!initialStyleLoaded) {
            fogStyleJson = FogLayerManager.buildStyleJson(revealedTiles, isDark)
        } else {
            val style = mapStyleRef ?: return@LaunchedEffect
            FogLayerManager.applyFogLayer(style, FogLayerManager.buildFogGeoJson(revealedTiles))
            if (SHOW_DEBUG_TILES) DebugTileOverlay.update(style, revealedTiles)
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
}

/** Draw raw tile outlines on top of the fog while tuning the contour tracer. */
private const val SHOW_DEBUG_TILES = true
