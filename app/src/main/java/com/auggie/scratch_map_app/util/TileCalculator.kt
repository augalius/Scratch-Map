package com.auggie.scratch_map_app.util

import com.auggie.scratch_map_app.domain.model.DirectionalRadii
import com.auggie.scratch_map_app.domain.model.ScratchTile
import kotlin.math.*

/**
 * Converts GPS coordinates to tile grid coordinates and vice versa
 * Uses the Slippy Map tile system
 */
object TileCalculator {

    const val TILE_ZOOM = 20

    /** Mean Earth radius in meters (spherical approximation). */
    const val EARTH_RADIUS_METERS = 6_371_000.0

    /**
     * Multiplier used to pack a tile's (x, y) into one Long. Safely exceeds the maximum tile
     * index at [TILE_ZOOM] (2^20 ≈ 1.05M), so y can never carry into x.
     */
    private const val TILE_KEY_STRIDE = 10_000_000L

    /** Packs tile coordinates into a single Long, for use as a set/map key. */
    fun tileKey(x: Int, y: Int): Long = x.toLong() * TILE_KEY_STRIDE + y.toLong()

    fun tileKeyX(key: Long): Int = (key / TILE_KEY_STRIDE).toInt()

    fun tileKeyY(key: Long): Int = (key % TILE_KEY_STRIDE).toInt()

    /** Axis-aligned latitude/longitude bounding box, used as a coarse spatial pre-filter. */
    data class BoundingBox(
        val minLat: Double,
        val maxLat: Double,
        val minLng: Double,
        val maxLng: Double
    )

    /**
     * Returns the lat/lng [BoundingBox] enclosing a circle of [radiusMeters] around (lat, lng).
     *
     * Intended as a cheap, index-friendly pre-filter (a `BETWEEN` query) before running the exact
     * [haversineDistance] check on the few candidates that survive. The box is a slight
     * over-estimate (it bounds the circle), so it never excludes a point the circle would include.
     */
    fun boundingBox(lat: Double, lng: Double, radiusMeters: Double): BoundingBox {
        val latDelta = Math.toDegrees(radiusMeters / EARTH_RADIUS_METERS)
        // Longitude degrees shrink toward the poles; guard against cos→0 near ±90°.
        val cosLat = cos(Math.toRadians(lat)).coerceAtLeast(1e-6)
        val lngDelta = Math.toDegrees(radiusMeters / (EARTH_RADIUS_METERS * cosLat))
        return BoundingBox(
            minLat = lat - latDelta,
            maxLat = lat + latDelta,
            minLng = lng - lngDelta,
            maxLng = lng + lngDelta
        )
    }

    fun latLngToTile(lat: Double, lng: Double, zoom: Int): Pair<Int, Int> {
        val n = 1 shl zoom
        val tileX = ((lng + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
        val latRad = Math.toRadians(lat)
        val tileY = ((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n)
            .toInt().coerceIn(0, n - 1)
        return Pair(tileX, tileY)
    }

    fun tileCenterToLatLng(tileX: Int, tileY: Int, zoom: Int): Pair<Double, Double> {
        val n = 1 shl zoom
        val lng = tileX.toDouble() / n * 360.0 - 180.0 + (0.5 / n * 360.0)
        val latRadNorth = atan(sinh(PI * (1 - 2.0 * tileY / n)))
        val latRadSouth = atan(sinh(PI * (1 - 2.0 * (tileY + 1) / n)))
        val lat = Math.toDegrees((latRadNorth + latRadSouth) / 2.0)
        return Pair(lat, lng)
    }

    fun createTilesDirectional(
        lat: Double, lng: Double,
        radii: DirectionalRadii,
        zoom: Int
    ): List<ScratchTile> {
        val maxRadius = radii.maxRadius()
        val (userTileX, userTileY) = latLngToTile(lat, lng, zoom)
        val tileSize = tileSizeMeters(lat, zoom)
        val effectiveMax = maxOf(maxRadius.toDouble(), tileSize * 0.75)

        val latOffset = effectiveMax / 111_111.0
        // Longitude degrees shrink toward the poles; guard against cos→0 near ±90°.
        val cosLat = cos(Math.toRadians(lat)).coerceAtLeast(1e-6)
        val lngOffset = effectiveMax / (111_111.0 * cosLat)
        val (minTileX, maxTileY) = latLngToTile(lat - latOffset, lng - lngOffset, zoom)
        val (maxTileX, minTileY) = latLngToTile(lat + latOffset, lng + lngOffset, zoom)

        val tiles = mutableListOf<ScratchTile>()
        val addedTiles = mutableSetOf<Long>()

        for (x in minTileX..maxTileX) {
            for (y in minTileY..maxTileY) {
                val (tileLat, tileLng) = tileCenterToLatLng(x, y, zoom)
                val dist = haversineDistance(lat, lng, tileLat, tileLng)

                // Compute bearing from user to this tile
                val bearing = bearing(lat, lng, tileLat, tileLng)
                val allowedRadius = radii.radiusAtBearing(bearing)
                val effectiveAllowed = maxOf(allowedRadius, tileSize * 0.75)

                if (dist <= effectiveAllowed) {
                    if (addedTiles.add(tileKey(x, y))) {
                        tiles.add(ScratchTile(x, y, zoom, tileLat, tileLng))
                    }
                }
            }
        }

        // Guarantee user's own tile
        if (!addedTiles.contains(tileKey(userTileX, userTileY))) {
            val (cLat, cLng) = tileCenterToLatLng(userTileX, userTileY, zoom)
            tiles.add(0, ScratchTile(userTileX, userTileY, zoom, cLat, cLng))
        }

        return tiles
    }

    fun bearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLng = Math.toRadians(lng2 - lng1)
        val lat1R = Math.toRadians(lat1)
        val lat2R = Math.toRadians(lat2)
        val y = sin(dLng) * cos(lat2R)
        val x = cos(lat1R) * sin(lat2R) - sin(lat1R) * cos(lat2R) * cos(dLng)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = EARTH_RADIUS_METERS
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun tileSizeMeters(lat: Double, zoom: Int): Double {
        return 40_075_016.686 * cos(Math.toRadians(lat)) / (1 shl zoom)
    }
}