package com.auggie.scratch_map_app.domain.model

import com.auggie.scratch_map_app.domain.model.Region
import com.auggie.scratch_map_app.domain.model.RegionStats
import org.junit.Assert.*
import org.junit.Test

class RegionStatsTest {

    private val region = Region(
        id = "r1", name = "Vilnius Old Town",
        minLat = 54.67, maxLat = 54.70,
        minLng = 25.27, maxLng = 25.30,
        goalTiles = 200
    )

    private fun stats(
        tilesRevealed: Long = 0L,
        revealedAreaKm2: Double = 0.0,
        goalAreaKm2: Double = 0.0
    ) = RegionStats(
        region = region,
        tilesRevealed = tilesRevealed,
        waypointsUnlocked = 0, waypointsTotal = 0,
        revealedAreaKm2 = revealedAreaKm2, goalAreaKm2 = goalAreaKm2
    )

    @Test
    fun `progress uses area ratio when goalAreaKm2 is positive`() {
        assertEquals(0.5f, stats(revealedAreaKm2 = 1.5, goalAreaKm2 = 3.0).progress, 0.001f)
    }

    @Test
    fun `progress falls back to tile ratio when goalAreaKm2 is zero`() {
        assertEquals(0.5f, stats(tilesRevealed = 100L).progress, 0.001f)
    }

    @Test
    fun `progress is 0 when nothing revealed`() {
        assertEquals(0f, stats().progress, 0.001f)
    }

    @Test
    fun `progress tile ratio is clamped to 1 when tiles exceed goal`() {
        assertEquals(1f, stats(tilesRevealed = 300L).progress, 0.001f)
    }

    @Test
    fun `progress area ratio is clamped to 1 when revealed exceeds goal`() {
        assertEquals(1f, stats(revealedAreaKm2 = 10.0, goalAreaKm2 = 3.0).progress, 0.001f)
    }

    @Test
    fun `progress is 1 when tiles exactly meet goal`() {
        assertEquals(1f, stats(tilesRevealed = 200L).progress, 0.001f)
    }
}
