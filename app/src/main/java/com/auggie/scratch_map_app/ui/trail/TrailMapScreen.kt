package com.auggie.scratch_map_app.ui.trail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auggie.scratch_map_app.domain.model.TrailCheckpoint
import com.auggie.scratch_map_app.ui.map.applyScratchMapChrome
import com.auggie.scratch_map_app.ui.map.fadeInWhenStyled
import com.auggie.scratch_map_app.ui.map.fog.FogLayerManager
import com.auggie.scratch_map_app.util.roundedRectRing
import com.auggie.scratch_map_app.ui.theme.BannerInfo
import com.auggie.scratch_map_app.ui.theme.BannerWarning
import com.auggie.scratch_map_app.ui.theme.FogColor
import com.auggie.scratch_map_app.ui.theme.MarkerGreen
import com.auggie.scratch_map_app.ui.theme.toHexString
import com.auggie.scratch_map_app.ui.theme.toRgbaString
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun TrailMapScreen(
    trailId: String,
    onBack: () -> Unit,
    viewModel: TrailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(trailId) { viewModel.loadTrail(trailId) }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var styleLoaded by remember { mutableStateOf(false) }

    val trail = uiState.trail
    val checkpoints = trail?.checkpoints ?: emptyList()

    // Tight trail bounds (no padding) used for the overlay and the off-trail warning
    var tightSW by remember { mutableStateOf<Point?>(null) }
    var tightNE by remember { mutableStateOf<Point?>(null) }

    LaunchedEffect(checkpoints, styleLoaded) {
        if (!styleLoaded) return@LaunchedEffect
        val style = mapViewRef?.mapboxMap?.style ?: return@LaunchedEffect
        updateCheckpointSource(style, checkpoints)
    }

    LaunchedEffect(uiState.revealedTiles, styleLoaded) {
        if (!styleLoaded) return@LaunchedEffect
        val style = mapViewRef?.mapboxMap?.style ?: return@LaunchedEffect
        val fogFeature = FogLayerManager.buildFogGeoJson(uiState.revealedTiles)
        try {
            style.getSourceAs<GeoJsonSource>("trail-fog-source")?.feature(fogFeature)
        } catch (e: Exception) {
            Timber.w(e, "Failed to update trail fog source")
        }
    }

    var cameraFitted by remember { mutableStateOf(false) }
    LaunchedEffect(checkpoints, styleLoaded) {
        if (checkpoints.isEmpty() || cameraFitted) return@LaunchedEffect
        if (!styleLoaded) return@LaunchedEffect
        val mapbox = mapViewRef?.mapboxMap ?: return@LaunchedEffect

        delay(300)

        val lats = checkpoints.map { it.latitude }
        val lngs = checkpoints.map { it.longitude }

        val latSpan = (lats.max() - lats.min()).coerceAtLeast(0.005)
        val lngSpan = (lngs.max() - lngs.min()).coerceAtLeast(0.005)
        val latPad = latSpan * 0.4
        val lngPad = lngSpan * 0.4

        // Tight bounds expanded by 15% so the clear zone is visibly larger than
        // the checkpoint markers and the warning doesn't fire at the trail edge
        val innerLatPad = latSpan * 0.15
        val innerLngPad = lngSpan * 0.15
        val sw = Point.fromLngLat(lngs.min() - innerLngPad, lats.min() - innerLatPad)
        val ne = Point.fromLngLat(lngs.max() + innerLngPad, lats.max() + innerLatPad)
        tightSW = sw
        tightNE = ne

        val paddedSW = Point.fromLngLat(lngs.min() - lngPad, lats.min() - latPad)
        val paddedNE = Point.fromLngLat(lngs.max() + lngPad, lats.max() + latPad)
        val cameraBounds = CoordinateBounds(paddedSW, paddedNE, false)

        val camera = mapbox.cameraForCoordinateBounds(
            cameraBounds,
            EdgeInsets(160.0, 80.0, 80.0, 80.0),
            null, null
        )
        val minZoom = camera.zoom ?: 12.0

        mapbox.setBounds(
            CameraBoundsOptions.Builder()
                .bounds(cameraBounds)
                .minZoom(minZoom)
                .build()
        )
        mapbox.setCamera(camera)

        //  Dark overlay over the padding zone
        val style = mapbox.style ?: return@LaunchedEffect
        updateOverlaySource(style, paddedSW, paddedNE, sw, ne)

        cameraFitted = true
    }

    //  Off-trail warning: user is in the padding zone
    val loc = uiState.currentLocation
    val sw = tightSW
    val ne = tightNE
    val isOffTrail = loc != null && sw != null && ne != null &&
            (loc.latitude < sw.latitude() || loc.latitude > ne.latitude() ||
                    loc.longitude < sw.longitude() || loc.longitude > ne.longitude())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FogColor)
    ) {
        if (!uiState.isLoading) {
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        alpha = 0f

                        if (checkpoints.isNotEmpty()) {
                            val centerLat = checkpoints.map { it.latitude }.average()
                            val centerLng = checkpoints.map { it.longitude }.average()
                            mapboxMap.setCamera(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat(centerLng, centerLat))
                                    .zoom(14.0)
                                    .build()
                            )
                        }

                        val styleUri = if (isDark) Style.DARK else Style.LIGHT
                        mapboxMap.loadStyle(styleUri) { style ->
                            setupTrailLayers(style)
                            styleLoaded = true
                            fadeInWhenStyled()
                        }
                        applyScratchMapChrome(locationEnabled = true)
                        mapViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        trail?.name ?: "Trail",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (checkpoints.isNotEmpty()) {
                        Text(
                            "${trail?.reachedCount ?: 0} / ${checkpoints.size} checkpoints",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (checkpoints.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { trail?.progress ?: 0f },
                        modifier = Modifier.width(80.dp),
                        color = MarkerGreen
                    )
                }
            }
        }

        val showCheckpointBanner = uiState.lastReachedCheckpoint != null
        val showOffTrailBanner = !showCheckpointBanner && isOffTrail
        val showCompletedBanner =
            !showCheckpointBanner && !showOffTrailBanner && trail?.isCompleted == true

        AnimatedVisibility(
            visible = showCheckpointBanner,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            uiState.lastReachedCheckpoint?.let { cp ->
                LaunchedEffect(cp.id) {
                    delay(3000)
                    viewModel.clearReachedCheckpoint()
                }
                BottomBanner(color = MarkerGreen) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            "Checkpoint reached!",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            cp.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showOffTrailBanner,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            BottomBanner(color = BannerWarning) {
                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        "You're off the trail!",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        "Return to the trail area",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showCompletedBanner,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            BottomBanner(color = BannerInfo) {
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Trail completed!",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BottomBanner(color: Color, content: @Composable RowScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = color, shadowElevation = 6.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

private fun setupTrailLayers(style: Style) {
    try {
        // Fog-of-war layer: mirrors the main scratch map fog inside the clear area
        style.addSource(geoJsonSource("trail-fog-source") {
            featureCollection(FeatureCollection.fromFeatures(emptyList()))
        })
        style.addLayer(fillLayer("trail-fog-layer", "trail-fog-source") {
            fillColor(FogColor.toRgbaString(alpha = 0.95))
            fillAntialias(true)
        })

        style.addSource(geoJsonSource("trail-overlay-source") {
            featureCollection(FeatureCollection.fromFeatures(emptyList()))
        })
        style.addLayer(fillLayer("trail-overlay-layer", "trail-overlay-source") {
            fillColor(FogColor.toHexString())
            fillOpacity(0.95)
        })

        // Red boundary line layers
        style.addSource(geoJsonSource("trail-boundary-source") {
            featureCollection(FeatureCollection.fromFeatures(emptyList()))
        })
        style.addLayer(lineLayer("trail-boundary-glow-outer", "trail-boundary-source") {
            lineColor("#FF1744")
            lineWidth(16.0)
            lineBlur(10.0)
            lineOpacity(0.25)
        })
        style.addLayer(lineLayer("trail-boundary-glow-mid", "trail-boundary-source") {
            lineColor("#FF4444")
            lineWidth(6.0)
            lineBlur(3.0)
            lineOpacity(0.55)
        })
        style.addLayer(lineLayer("trail-boundary-crisp", "trail-boundary-source") {
            lineColor("#FF6B6B")
            lineWidth(2.0)
            lineOpacity(0.9)
        })

        style.addSource(geoJsonSource("trail-checkpoints-source") {
            featureCollection(FeatureCollection.fromFeatures(emptyList()))
        })

        style.addLayer(circleLayer("trail-cp-unreached", "trail-checkpoints-source") {
            filter(Expression.fromRaw("""["==", ["get", "reached"], false]"""))
            circleRadius(14.0)
            circleColor("#607D8B")
            circleStrokeWidth(2.5)
            circleStrokeColor("#FFFFFF")
        })

        style.addLayer(circleLayer("trail-cp-reached", "trail-checkpoints-source") {
            filter(Expression.fromRaw("""["==", ["get", "reached"], true]"""))
            circleRadius(14.0)
            circleColor(MarkerGreen.toHexString())
            circleStrokeWidth(2.5)
            circleStrokeColor("#FFFFFF")
        })

        style.addLayer(symbolLayer("trail-cp-label", "trail-checkpoints-source") {
            textField(Expression.fromRaw("""["get", "order"]"""))
            textFont(listOf("DIN Pro Bold", "Arial Unicode MS Regular"))
            textSize(13.0)
            textColor("#FFFFFF")
            textAllowOverlap(true)
            textIgnorePlacement(true)
        })
    } catch (e: Exception) {
        Timber.w(e, "Failed to setup trail layers")
    }
}

private fun updateOverlaySource(
    style: Style,
    paddedSW: Point, paddedNE: Point,
    tightSW: Point, tightNE: Point
) {
    val outer = listOf(
        Point.fromLngLat(paddedSW.longitude(), paddedSW.latitude()),
        Point.fromLngLat(paddedNE.longitude(), paddedSW.latitude()),
        Point.fromLngLat(paddedNE.longitude(), paddedNE.latitude()),
        Point.fromLngLat(paddedSW.longitude(), paddedNE.latitude()),
        Point.fromLngLat(paddedSW.longitude(), paddedSW.latitude())
    )
    val innerCcw = roundedRectRing(
        minLat = tightSW.latitude(), minLng = tightSW.longitude(),
        maxLat = tightNE.latitude(), maxLng = tightNE.longitude(),
        cornerFraction = 0.10, arcSteps = 10
    ).map { (lat, lng) -> Point.fromLngLat(lng, lat) }
    val inner = innerCcw.reversed()

    val polygon = Polygon.fromLngLats(listOf(outer, inner))
    try {
        style.getSourceAs<GeoJsonSource>("trail-overlay-source")
            ?.featureCollection(FeatureCollection.fromFeature(Feature.fromGeometry(polygon)))
    } catch (e: Exception) {
        Timber.w(e, "Failed to update overlay source")
    }

    val boundaryLine = Feature.fromGeometry(LineString.fromLngLats(innerCcw))
    try {
        style.getSourceAs<GeoJsonSource>("trail-boundary-source")
            ?.featureCollection(FeatureCollection.fromFeature(boundaryLine))
    } catch (e: Exception) {
        Timber.w(e, "Failed to update boundary source")
    }
}


private fun updateCheckpointSource(style: Style, checkpoints: List<TrailCheckpoint>) {
    val features = checkpoints.sortedBy { it.order }.map { cp ->
        Feature.fromGeometry(Point.fromLngLat(cp.longitude, cp.latitude)).apply {
            addStringProperty("id", cp.id)
            addStringProperty("name", cp.name)
            addStringProperty("order", (cp.order + 1).toString())
            addBooleanProperty("reached", cp.isReached)
        }
    }
    try {
        style.getSourceAs<GeoJsonSource>("trail-checkpoints-source")
            ?.featureCollection(FeatureCollection.fromFeatures(features))
    } catch (e: Exception) {
        Timber.w(e, "Failed to update checkpoint source")
    }
}
