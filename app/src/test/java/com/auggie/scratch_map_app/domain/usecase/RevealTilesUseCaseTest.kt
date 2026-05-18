package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.model.DirectionalRadii
import com.auggie.scratch_map_app.domain.model.UserLocation
import com.auggie.scratch_map_app.domain.repository.RoadGeometryRepository
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class RevealTilesUseCaseTest {

    private lateinit var tileRepository: ScratchTileRepository
    private lateinit var roadGeometry: RoadGeometryRepository
    private lateinit var useCase: RevealTilesUseCase

    private val accurateLocation = UserLocation(
        latitude = 54.6872, longitude = 25.2797,
        accuracy = 10f, speed = null
    )

    private val inaccurateLocation = accurateLocation.copy(accuracy = 100f)

    @Before
    fun setUp() {
        tileRepository = mock()
        roadGeometry = mock()
        useCase = RevealTilesUseCase(tileRepository, roadGeometry)
    }

    private suspend fun stubRadii(revealed: Int) {
        whenever(roadGeometry.getDirectionalRadii(any(), any()))
            .thenReturn(DirectionalRadii.uniform(50))
        whenever(tileRepository.revealTiles(any())).thenReturn(revealed)
    }

    @Test
    fun `inaccurate location returns 0`() = runTest {
        assertEquals(0, useCase(inaccurateLocation))
    }

    @Test
    fun `inaccurate location never queries road geometry`() = runTest {
        useCase(inaccurateLocation)
        verifyNoInteractions(roadGeometry)
    }

    @Test
    fun `inaccurate location never calls tile repository`() = runTest {
        useCase(inaccurateLocation)
        verifyNoInteractions(tileRepository)
    }

    @Test
    fun `accurate location queries road geometry at that position`() = runTest {
        stubRadii(revealed = 0)
        useCase(accurateLocation)
        verify(roadGeometry)
            .getDirectionalRadii(accurateLocation.latitude, accurateLocation.longitude)
    }

    @Test
    fun `accurate location reveals tiles`() = runTest {
        stubRadii(revealed = 5)
        useCase(accurateLocation)
        verify(tileRepository).revealTiles(any())
    }

    @Test
    fun `returns count from tile repository`() = runTest {
        stubRadii(revealed = 7)
        assertEquals(7, useCase(accurateLocation))
    }

    @Test
    fun `returns 0 when repository reveals no new tiles`() = runTest {
        stubRadii(revealed = 0)
        assertEquals(0, useCase(accurateLocation))
    }

    @Test
    fun `location exactly at the accuracy threshold is accepted`() = runTest {
        stubRadii(revealed = 1)
        assertEquals(1, useCase(accurateLocation.copy(accuracy = 50f)))
    }
}
