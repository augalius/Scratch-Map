package com.auggie.scratch_map_app.util

/**
 * Ray-casting point-in-polygon test.
 *
 * [polygon] is a closed ring of (latitude, longitude) pairs; the closing point may be omitted.
 * Points exactly on an edge are not guaranteed to be classified consistently, which is fine for
 * the region boundaries this is used on.
 */
fun isInsidePolygon(lat: Double, lng: Double, polygon: List<Pair<Double, Double>>): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val (iLat, iLng) = polygon[i]
        val (jLat, jLng) = polygon[j]
        if ((iLng > lng) != (jLng > lng) &&
            lat < (jLat - iLat) * (lng - iLng) / (jLng - iLng) + iLat
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}
