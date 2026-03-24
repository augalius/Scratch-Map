package com.auggie.scratch_map_app.util

import kotlin.math.cos
import kotlin.math.sin

/**
 * A rectangle with rounded corners, traced counter-clockwise as a closed ring of
 * (latitude, longitude) pairs.
 *
 * Corner radii are a fraction of each axis, so the corners stay visually proportional whatever
 * the rectangle's aspect ratio.
 */
fun roundedRectRing(
    minLat: Double, minLng: Double,
    maxLat: Double, maxLng: Double,
    cornerFraction: Double,
    arcSteps: Int
): List<Pair<Double, Double>> {
    val rx = (maxLng - minLng) * cornerFraction
    val ry = (maxLat - minLat) * cornerFraction
    val points = mutableListOf<Pair<Double, Double>>()

    fun arc(startDegrees: Double, centerLng: Double, centerLat: Double) {
        for (i in 0..arcSteps) {
            val angle = Math.toRadians(startDegrees + 90.0 * i / arcSteps)
            points.add(centerLat + ry * sin(angle) to centerLng + rx * cos(angle))
        }
    }

    arc(270.0, maxLng - rx, minLat + ry) // bottom-right
    arc(0.0, maxLng - rx, maxLat - ry)   // top-right
    arc(90.0, minLng + rx, maxLat - ry)  // top-left
    arc(180.0, minLng + rx, minLat + ry) // bottom-left

    points.add(points.first())
    return points
}
