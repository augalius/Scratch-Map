package com.auggie.scratch_map_app.ui.map.fog

import com.auggie.scratch_map_app.domain.model.ScratchTile
import com.auggie.scratch_map_app.domain.model.Trail
import com.auggie.scratch_map_app.domain.model.Waypoint
import com.auggie.scratch_map_app.ui.map.colorHex
import com.auggie.scratch_map_app.ui.theme.FogColor
import com.auggie.scratch_map_app.ui.theme.toRgbaString
import com.auggie.scratch_map_app.util.TileCalculator
import com.auggie.scratch_map_app.util.TileContourTracer
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Builds and updates the fog-of-war overlay and its marker sources.
 *
 * The fog is one polygon covering the whole world, with a hole punched through it for every
 * contiguous revealed area. Contour tracing lives in [TileContourTracer]; the style document
 * lives in [MapStyleBuilder].
 */
object FogLayerManager {

    private const val SOURCE_ID = "fog-source"
    private const val LAYER_ID = "fog-layer"
    private val FOG_COLOR = FogColor.toRgbaString(alpha = 0.95)

    /** Grow revealed tiles by one tile before tracing, closing single-tile pinholes. */
    private const val DILATION_RADIUS = 1
    private const val SMOOTH_ITERATIONS = 5

    /** Outer ring of the fog polygon; Web Mercator tops out near ±85.06°. */
    private val WORLD_RING = listOf(
        Point.fromLngLat(-180.0, -85.06),
        Point.fromLngLat(180.0, -85.06),
        Point.fromLngLat(180.0, 85.06),
        Point.fromLngLat(-180.0, 85.06),
        Point.fromLngLat(-180.0, -85.06)
    )

    suspend fun buildStyleJson(
        tiles: List<ScratchTile>,
        isDark: Boolean = false,
        waypoints: List<Waypoint> = emptyList(),
        trails: List<Trail> = emptyList()
    ): String = withContext(Dispatchers.Default) {
        val trailFeatures = buildTrailFeatures(trails)
        Timber.d(
            "FogLayer: building style with %d trails, %d with valid coords",
            trails.size, trailFeatures.size
        )
        MapStyleBuilder.build(
            fogJson = buildFogGeoJson(tiles).toJson(),
            waypointJson = FeatureCollection.fromFeatures(buildWaypointFeatures(waypoints)).toJson(),
            trailJson = FeatureCollection.fromFeatures(trailFeatures).toJson(),
            fogSourceId = SOURCE_ID,
            fogLayerId = LAYER_ID,
            fogColor = FOG_COLOR,
            isDark = isDark
        )
    }

    suspend fun buildFogGeoJson(tiles: List<ScratchTile>): Feature =
        withContext(Dispatchers.Default) {
            val rings = mutableListOf(WORLD_RING)
            TileContourTracer.traceSmoothedOutlines(
                tiles = tiles,
                zoom = TileCalculator.TILE_ZOOM,
                dilationRadius = DILATION_RADIUS,
                smoothIterations = SMOOTH_ITERATIONS
            ).forEach { outline ->
                val ring = outline.map { (lat, lng) -> Point.fromLngLat(lng, lat) }
                rings.add(ring + ring.first())
            }
            Feature.fromGeometry(Polygon.fromLngLats(rings))
        }

    fun applyFogLayer(style: Style, fogFeature: Feature) {
        val existingSource = try {
            style.getSourceAs<GeoJsonSource>(SOURCE_ID)
        } catch (_: Exception) {
            null
        }

        if (existingSource != null) {
            existingSource.feature(fogFeature)
        } else {
            style.addSource(geoJsonSource(SOURCE_ID) { feature(fogFeature) })
            style.addLayer(fillLayer(LAYER_ID, SOURCE_ID) {
                fillColor(FOG_COLOR)
                fillAntialias(true)
            })
        }
    }

    fun updateWaypoints(style: Style, waypoints: List<Waypoint>) {
        updateSource(style, "waypoint-source", buildWaypointFeatures(waypoints))
    }

    fun updateTrails(style: Style, trails: List<Trail>) {
        updateSource(style, "trail-source", buildTrailFeatures(trails))
    }

    private fun updateSource(style: Style, sourceId: String, features: List<Feature>) {
        try {
            style.getSourceAs<GeoJsonSource>(sourceId)
                ?.featureCollection(FeatureCollection.fromFeatures(features))
        } catch (e: Exception) {
            Timber.w(e, "Failed to update source %s", sourceId)
        }
    }

    private fun buildWaypointFeatures(waypoints: List<Waypoint>): List<Feature> =
        waypoints.map { wp ->
            Feature.fromGeometry(Point.fromLngLat(wp.longitude, wp.latitude)).apply {
                addStringProperty("name", wp.name)
                addStringProperty("category", wp.category.name)
                addBooleanProperty("unlocked", wp.isUnlocked)
                addStringProperty("color", wp.category.colorHex)
            }
        }

    private fun buildTrailFeatures(trails: List<Trail>): List<Feature> =
        trails.mapNotNull { trail ->
            val lat = trail.startLat ?: return@mapNotNull null
            val lng = trail.startLng ?: return@mapNotNull null
            Feature.fromGeometry(Point.fromLngLat(lng, lat)).apply {
                addStringProperty("trail_id", trail.id)
                addStringProperty("name", trail.name)
                addStringProperty("difficulty", trail.difficulty.name)
                addNumberProperty("progress", trail.progress)
                addBooleanProperty("is_completed", trail.isCompleted)
            }
        }
}
