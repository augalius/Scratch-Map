package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.repository.ContentVersionRepository
import com.auggie.scratch_map_app.domain.usecase.SeedAchievementsUseCase
import com.auggie.scratch_map_app.domain.usecase.SeedContentUseCase
import com.auggie.scratch_map_app.domain.usecase.SeedRegionsUseCase
import com.auggie.scratch_map_app.domain.usecase.SeedWaypointsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class SeedContentUseCaseTest {

    private lateinit var versionRepository: ContentVersionRepository
    private lateinit var seedRegions: SeedRegionsUseCase
    private lateinit var seedWaypoints: SeedWaypointsUseCase
    private lateinit var seedAchievements: SeedAchievementsUseCase
    private lateinit var useCase: SeedContentUseCase

    @Before
    fun setUp() {
        versionRepository = mock()
        seedRegions = mock()
        seedWaypoints = mock()
        seedAchievements = mock()
        useCase = SeedContentUseCase(
            versionRepository,
            seedRegions, seedWaypoints, seedAchievements
        )
    }


    @Test
    fun `remote equals local skips all seeding`() = runTest {
        whenever(versionRepository.getRemoteVersion()).thenReturn(5)
        whenever(versionRepository.getLocalVersion()).thenReturn(5)
        useCase()
        verify(seedRegions, never()).invoke()
        verify(seedWaypoints, never()).invoke()
        verify(seedAchievements, never()).invoke()
    }

    @Test
    fun `remote less than local skips all seeding`() = runTest {
        whenever(versionRepository.getRemoteVersion()).thenReturn(3)
        whenever(versionRepository.getLocalVersion()).thenReturn(5)
        useCase()
        verify(seedRegions, never()).invoke()
        verify(seedWaypoints, never()).invoke()
    }

    @Test
    fun `remote equals local does not save version`() = runTest {
        whenever(versionRepository.getRemoteVersion()).thenReturn(5)
        whenever(versionRepository.getLocalVersion()).thenReturn(5)
        useCase()
        verify(versionRepository, never()).saveLocalVersion(any())
    }

    @Test
    fun `both versions zero skips seeding`() = runTest {
        whenever(versionRepository.getRemoteVersion()).thenReturn(0)
        whenever(versionRepository.getLocalVersion()).thenReturn(0)
        useCase()
        verify(seedRegions, never()).invoke()
    }


    @Test
    fun `remote greater than local calls all three seed use cases`() = runTest {
        whenever(versionRepository.getRemoteVersion()).thenReturn(6)
        whenever(versionRepository.getLocalVersion()).thenReturn(5)
        useCase()
        verify(seedRegions).invoke()
        verify(seedWaypoints).invoke()
        verify(seedAchievements).invoke()
    }

    @Test
    fun `remote greater than local saves new version`() = runTest {
        whenever(versionRepository.getRemoteVersion()).thenReturn(6)
        whenever(versionRepository.getLocalVersion()).thenReturn(5)
        useCase()
        verify(versionRepository).saveLocalVersion(6)
    }

    @Test
    fun `version is saved after seeding completes not before`() = runTest {
        whenever(versionRepository.getRemoteVersion()).thenReturn(6)
        whenever(versionRepository.getLocalVersion()).thenReturn(5)
        val order = inOrder(seedRegions, seedWaypoints, seedAchievements, versionRepository)
        useCase()
        order.verify(seedRegions).invoke()
        order.verify(seedWaypoints).invoke()
        order.verify(seedAchievements).invoke()
        order.verify(versionRepository).saveLocalVersion(6)
    }

    @Test
    fun `first ever install seeds when local is 0 and remote is 1`() = runTest {
        whenever(versionRepository.getRemoteVersion()).thenReturn(1)
        whenever(versionRepository.getLocalVersion()).thenReturn(0)
        useCase()
        verify(seedRegions).invoke()
        verify(versionRepository).saveLocalVersion(1)
    }

    @Test
    fun `large version jump still triggers seeding`() = runTest {
        whenever(versionRepository.getRemoteVersion()).thenReturn(100)
        whenever(versionRepository.getLocalVersion()).thenReturn(1)
        useCase()
        verify(seedRegions).invoke()
        verify(versionRepository).saveLocalVersion(100)
    }
}
