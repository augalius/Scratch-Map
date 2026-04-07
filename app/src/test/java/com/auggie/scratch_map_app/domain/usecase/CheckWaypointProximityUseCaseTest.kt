package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.model.UserLocation
import com.auggie.scratch_map_app.domain.model.Waypoint
import com.auggie.scratch_map_app.domain.repository.WaypointRepository
import com.auggie.scratch_map_app.domain.usecase.CheckWaypointProximityUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class CheckWaypointProximityUseCaseTest {

    private lateinit var waypointRepository: WaypointRepository
    private lateinit var useCase: CheckWaypointProximityUseCase

    private val userLocation = UserLocation(
        latitude = 54.6872, longitude = 25.2797,
        accuracy = 10f, speed = null
    )

    private val nearbyWaypoint = Waypoint(
        id = 1L, stringId = "wp_near", name = "Nearby",
        latitude = 54.6872, longitude = 25.2797,
        unlockRadiusMeters = 50
    )

    private val farWaypoint = Waypoint(
        id = 2L, stringId = "wp_far", name = "Far Away",
        latitude = 54.8985, longitude = 23.9036,
        unlockRadiusMeters = 50
    )

    @Before
    fun setUp() {
        waypointRepository = mock()
        useCase = CheckWaypointProximityUseCase(waypointRepository)
    }

    @Test
    fun `waypoint within radius is unlocked and returned`() = runTest {
        whenever(waypointRepository.getLockedWaypointsInBounds(any(), any(), any(), any())).thenReturn(listOf(nearbyWaypoint))

        val result = useCase(userLocation)

        assertEquals(1, result.size)
        assertEquals("wp_near", result[0].stringId)
        assertTrue(result[0].isUnlocked)
    }

    @Test
    fun `waypoint within radius triggers markUnlocked in repository`() = runTest {
        whenever(waypointRepository.getLockedWaypointsInBounds(any(), any(), any(), any())).thenReturn(listOf(nearbyWaypoint))

        useCase(userLocation)

        verify(waypointRepository).markUnlocked(1L)
    }

    @Test
    fun `waypoint outside radius is not unlocked`() = runTest {
        whenever(waypointRepository.getLockedWaypointsInBounds(any(), any(), any(), any())).thenReturn(listOf(farWaypoint))

        val result = useCase(userLocation)

        assertTrue(result.isEmpty())
        verify(waypointRepository, never()).markUnlocked(any())
    }

    @Test
    fun `waypoint exactly at radius boundary is unlocked`() = runTest {
        val boundaryWaypoint = Waypoint(
            id = 3L, stringId = "wp_boundary", name = "Boundary",
            latitude = 54.6876, longitude = 25.2797, // ~44m north
            unlockRadiusMeters = 50
        )
        whenever(waypointRepository.getLockedWaypointsInBounds(any(), any(), any(), any())).thenReturn(listOf(boundaryWaypoint))

        val result = useCase(userLocation)

        assertEquals(1, result.size)
    }
}
