package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.model.Trail
import com.auggie.scratch_map_app.domain.model.TrailCheckpoint
import com.auggie.scratch_map_app.domain.model.UserLocation
import com.auggie.scratch_map_app.domain.repository.TrailRepository
import com.auggie.scratch_map_app.domain.usecase.CheckTrailProximityUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class CheckTrailProximityUseCaseTest {

    private lateinit var trailRepository: TrailRepository
    private lateinit var useCase: CheckTrailProximityUseCase

    private val userLocation = UserLocation(
        latitude = 54.6872, longitude = 25.2797,
        accuracy = 10f, speed = null
    )

    private fun trail(id: String, lat: Double, lng: Double, radius: Int = 100) = Trail(
        id = id, name = "Trail $id",
        checkpoints = listOf(
            TrailCheckpoint(
                id = "cp_$id", trailId = id, name = "Start",
                latitude = lat, longitude = lng, order = 1
            )
        ),
        unlockRadiusMeters = radius
    )

    private val nearbyTrail = trail("t_near", 54.6872, 25.2797)

    private val farTrail = trail("t_far", 54.8985, 23.9036)

    @Before
    fun setUp() {
        trailRepository = mock()
        useCase = CheckTrailProximityUseCase(trailRepository)
    }

    @Test
    fun `trail within radius is returned`() = runTest {
        whenever(trailRepository.getLockedTrails()).thenReturn(listOf(nearbyTrail))
        val result = useCase(userLocation)
        assertEquals(1, result.size)
        assertEquals("t_near", result[0].id)
    }

    @Test
    fun `trail within radius triggers markTrailUnlocked`() = runTest {
        whenever(trailRepository.getLockedTrails()).thenReturn(listOf(nearbyTrail))
        useCase(userLocation)
        verify(trailRepository).markTrailUnlocked("t_near")
    }

    @Test
    fun `trail outside radius is not returned`() = runTest {
        whenever(trailRepository.getLockedTrails()).thenReturn(listOf(farTrail))
        val result = useCase(userLocation)
        assertTrue(result.isEmpty())
        verify(trailRepository, never()).markTrailUnlocked(any())
    }

    @Test
    fun `empty locked list returns empty result`() = runTest {
        whenever(trailRepository.getLockedTrails()).thenReturn(emptyList())
        val result = useCase(userLocation)
        assertTrue(result.isEmpty())
        verify(trailRepository, never()).markTrailUnlocked(any())
    }

    @Test
    fun `trail exactly at radius boundary is unlocked`() = runTest {
        val boundaryTrail = trail("t_boundary", 54.6876, 25.2797, radius = 50)
        whenever(trailRepository.getLockedTrails()).thenReturn(listOf(boundaryTrail))
        val result = useCase(userLocation)
        assertEquals(1, result.size)
    }
}
