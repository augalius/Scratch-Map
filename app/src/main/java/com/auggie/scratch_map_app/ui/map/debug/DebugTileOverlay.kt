package com.auggie.scratch_map_app.ui.map.debug

import com.auggie.scratch_map_app.domain.model.ScratchTile
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.sinh

/**
 * TEMPORARY: outlines every revealed tile so the traced fog contour can be compared
 * against the raw tile grid. Remove once fog tracing is stable.
 */
internal object DebugTileOverlay {

    private const val SOURCE_ID = "debug-tile-source"
    private const val LAYER_ID = "debug-tile-layer"

    fun update(style: Style, tiles: List<ScratchTile>) {
        val collection = FeatureCollection.fromFeatures(tiles.map(::tileOutline))
        val source = try {
            style.getSourceAs<GeoJsonSource>(SOURCE_ID)
        } catch (_: Exception) {
            null
        }

        if (source != null) {
            source.featureCollection(collection)
        } else {
            style.addSource(geoJsonSource(SOURCE_ID) { featureCollection(collection) })
            style.addLayer(lineLayer(LAYER_ID, SOURCE_ID) {
                lineColor("#ff00ff")
                lineWidth(1.0)
                lineOpacity(0.6)
            })
        }
    }

    private fun tileOutline(tile: ScratchTile): Feature {
        val n = 1 shl tile.zoomLevel
        fun lng(x: Int) = x.toDouble() / n * 360.0 - 180.0
        fun lat(y: Int) = Math.toDegrees(atan(sinh(PI * (1 - 2.0 * y / n))))

        val west = lng(tile.tileX)
        val east = lng(tile.tileX + 1)
        val north = lat(tile.tileY)
        val south = lat(tile.tileY + 1)
        val ring = listOf(
            Point.fromLngLat(west, north),
            Point.fromLngLat(east, north),
            Point.fromLngLat(east, south),
            Point.fromLngLat(west, south),
            Point.fromLngLat(west, north)
        )
        return Feature.fromGeometry(Polygon.fromLngLats(listOf(ring)))
    }
}
