package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.model.UserLocation
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class RevealTilesUseCaseTest {

    private lateinit var tileRepository: ScratchTileRepository
    private lateinit var useCase: RevealTilesUseCase

    private val accurateLocation = UserLocation(
        latitude = 54.6872, longitude = 25.2797,
        accuracy = 10f, speed = null
    )

    private val inaccurateLocation = accurateLocation.copy(accuracy = 100f)

    @Before
    fun setUp() {
        tileRepository = mock()
        useCase = RevealTilesUseCase(tileRepository)
    }

    private suspend fun stubReveal(revealed: Int) {
        whenever(tileRepository.revealTiles(any())).thenReturn(revealed)
    }

    @Test
    fun `inaccurate location returns 0`() = runTest {
        assertEquals(0, useCase(inaccurateLocation))
    }

    @Test
    fun `inaccurate location never calls tile repository`() = runTest {
        useCase(inaccurateLocation)
        verifyNoInteractions(tileRepository)
    }

    @Test
    fun `accurate location reveals tiles`() = runTest {
        stubReveal(revealed = 5)
        useCase(accurateLocation)
        verify(tileRepository).revealTiles(any())
    }

    @Test
    fun `returns count from tile repository`() = runTest {
        stubReveal(revealed = 7)
        assertEquals(7, useCase(accurateLocation))
    }

    @Test
    fun `returns 0 when repository reveals no new tiles`() = runTest {
        stubReveal(revealed = 0)
        assertEquals(0, useCase(accurateLocation))
    }

    @Test
    fun `location exactly at the accuracy threshold is accepted`() = runTest {
        stubReveal(revealed = 1)
        assertEquals(1, useCase(accurateLocation.copy(accuracy = 50f)))
    }
}
