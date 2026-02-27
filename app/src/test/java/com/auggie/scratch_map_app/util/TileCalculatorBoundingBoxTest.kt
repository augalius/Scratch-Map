package com.auggie.scratch_map_app.util

import com.auggie.scratch_map_app.util.TileCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TileCalculatorBoundingBoxTest {

    private val lat = 54.6872
    private val lng = 25.2797

    @Test
    fun `box is centered on the origin point`() {
        val box = TileCalculator.boundingBox(lat, lng, 5_000.0)

        assertEquals(lat, (box.minLat + box.maxLat) / 2, 1e-9)
        assertEquals(lng, (box.minLng + box.maxLng) / 2, 1e-9)
        assertTrue(lat in box.minLat..box.maxLat)
        assertTrue(lng in box.minLng..box.maxLng)
    }

    @Test
    fun `latitude half-extent matches radius over earth radius`() {
        val radius = 5_000.0
        val box = TileCalculator.boundingBox(lat, lng, radius)

        val expectedLatDelta = Math.toDegrees(radius / TileCalculator.EARTH_RADIUS_METERS)
        assertEquals(expectedLatDelta, box.maxLat - lat, 1e-9)
    }

    @Test
    fun `longitude extent is wider than latitude extent away from the equator`() {
        val box = TileCalculator.boundingBox(lat, lng, 5_000.0)

        val latExtent = box.maxLat - box.minLat
        val lngExtent = box.maxLng - box.minLng
        // cos(54.7°) < 1, so a fixed ground distance spans more longitude degrees.
        assertTrue(lngExtent > latExtent)
    }

    @Test
    fun `the bounding box fully contains the unlock circle`() {
        val radius = 200.0
        val box = TileCalculator.boundingBox(lat, lng, radius)

        // A point exactly `radius` north must still fall inside the box (box bounds the circle).
        val northLatDelta = Math.toDegrees(radius / TileCalculator.EARTH_RADIUS_METERS)
        val pointNorthLat = lat + northLatDelta
        assertTrue(pointNorthLat <= box.maxLat + 1e-9)
    }
}
