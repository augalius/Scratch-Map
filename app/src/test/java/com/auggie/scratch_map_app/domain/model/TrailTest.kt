package com.auggie.scratch_map_app.domain.model

import com.auggie.scratch_map_app.domain.model.Trail
import com.auggie.scratch_map_app.domain.model.TrailCheckpoint
import org.junit.Assert.*
import org.junit.Test

class TrailTest {

    private fun cp(id: String, order: Int, lat: Double, lng: Double, reached: Boolean = false) =
        TrailCheckpoint(
            id = id, trailId = "t1", name = "CP $id",
            latitude = lat, longitude = lng,
            order = order, isReached = reached
        )

    private val cp1 = cp("cp1", 1, 54.69, 25.27)
    private val cp2 = cp("cp2", 2, 54.70, 25.28)
    private val cp3 = cp("cp3", 3, 54.71, 25.29)

    private fun trail(cps: List<TrailCheckpoint>) =
        Trail(id = "t1", name = "Test Trail", checkpoints = cps)


    @Test
    fun `startLat returns latitude of checkpoint with lowest order`() {
        val t = trail(listOf(cp2, cp1, cp3))
        assertEquals(54.69, t.startLat!!, 0.0001)
    }

    @Test
    fun `startLng returns longitude of checkpoint with lowest order`() {
        val t = trail(listOf(cp2, cp1, cp3))
        assertEquals(25.27, t.startLng!!, 0.0001)
    }

    @Test
    fun `startLat returns null when no checkpoints`() {
        assertNull(trail(emptyList()).startLat)
    }

    @Test
    fun `startLng returns null when no checkpoints`() {
        assertNull(trail(emptyList()).startLng)
    }

    @Test
    fun `startLat returns correct value when only one checkpoint`() {
        assertEquals(54.69, trail(listOf(cp1)).startLat!!, 0.0001)
    }


    @Test
    fun `reachedCount counts only reached checkpoints`() {
        val t = trail(listOf(cp1.copy(isReached = true), cp2, cp3.copy(isReached = true)))
        assertEquals(2, t.reachedCount)
    }

    @Test
    fun `reachedCount returns 0 when none reached`() {
        assertEquals(0, trail(listOf(cp1, cp2, cp3)).reachedCount)
    }

    @Test
    fun `reachedCount returns total when all reached`() {
        val t = trail(listOf(cp1.copy(isReached = true), cp2.copy(isReached = true)))
        assertEquals(2, t.reachedCount)
    }


    @Test
    fun `progress returns ratio of reached to total`() {
        val t = trail(listOf(cp1.copy(isReached = true), cp2, cp3))
        assertEquals(1f / 3f, t.progress, 0.001f)
    }

    @Test
    fun `progress returns 0 when no checkpoints`() {
        assertEquals(0f, trail(emptyList()).progress, 0.001f)
    }

    @Test
    fun `progress returns 0 when no checkpoints reached`() {
        assertEquals(0f, trail(listOf(cp1, cp2)).progress, 0.001f)
    }

    @Test
    fun `progress returns 1 when all checkpoints reached`() {
        val t = trail(
            listOf(
                cp1.copy(isReached = true),
                cp2.copy(isReached = true),
                cp3.copy(isReached = true)
            )
        )
        assertEquals(1f, t.progress, 0.001f)
    }
}
