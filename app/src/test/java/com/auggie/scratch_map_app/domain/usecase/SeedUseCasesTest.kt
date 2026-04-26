package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.data.seed.AchievementSeeder
import com.auggie.scratch_map_app.data.seed.RegionSeeder
import com.auggie.scratch_map_app.data.seed.WaypointSeeder
import com.auggie.scratch_map_app.domain.model.AchievementDefinition
import com.auggie.scratch_map_app.domain.model.Region
import com.auggie.scratch_map_app.domain.model.Waypoint
import com.auggie.scratch_map_app.domain.repository.AchievementRepository
import com.auggie.scratch_map_app.domain.repository.RegionRepository
import com.auggie.scratch_map_app.domain.repository.WaypointRepository
import com.auggie.scratch_map_app.domain.usecase.SeedAchievementsUseCase
import com.auggie.scratch_map_app.domain.usecase.SeedRegionsUseCase
import com.auggie.scratch_map_app.domain.usecase.SeedWaypointsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class SeedUseCasesTest {

    // Repositories
    private lateinit var regionRepository: RegionRepository
    private lateinit var waypointRepository: WaypointRepository
    private lateinit var achievementRepository: AchievementRepository

    // Seeders — inline mock with mockito
    private lateinit var regionSeeder: RegionSeeder
    private lateinit var waypointSeeder: WaypointSeeder
    private lateinit var achievementSeeder: AchievementSeeder

    private val region = Region(
        id = "r1", name = "Old Town", minLat = 54.67, maxLat = 54.70,
        minLng = 25.27, maxLng = 25.30, goalTiles = 200
    )

    private val waypoint = Waypoint(
        stringId = "wp_1", name = "Tower",
        latitude = 54.69, longitude = 25.28
    )

    private val achievementDef = AchievementDefinition(
        id = "ach_1", title = "Explorer", description = "Walk 10 km",
        iconName = "walk", targetValue = 10L, metricType = "DISTANCE_KM"
    )

    @Before
    fun setUp() {
        regionRepository = mock()
        waypointRepository = mock()
        achievementRepository = mock()
        regionSeeder = mock()
        waypointSeeder = mock()
        achievementSeeder = mock()
    }


    @Test
    fun `seedRegions does nothing when seeder returns empty list`() = runTest {
        whenever(regionSeeder.fetchRegions()).thenReturn(emptyList())
        SeedRegionsUseCase(regionRepository, regionSeeder)()
        verify(regionRepository, never()).insertAll(any())
        verify(regionRepository, never()).deleteNotIn(any())
    }

    @Test
    fun `seedRegions inserts regions from seeder`() = runTest {
        whenever(regionSeeder.fetchRegions()).thenReturn(listOf(region))
        SeedRegionsUseCase(regionRepository, regionSeeder)()
        verify(regionRepository).insertAll(listOf(region))
    }

    @Test
    fun `seedRegions deletes regions not in seeder list`() = runTest {
        whenever(regionSeeder.fetchRegions()).thenReturn(listOf(region))
        SeedRegionsUseCase(regionRepository, regionSeeder)()
        verify(regionRepository).deleteNotIn(listOf("r1"))
    }






    @Test
    fun `seedWaypoints does nothing when seeder returns empty list`() = runTest {
        whenever(waypointSeeder.fetchWaypoints()).thenReturn(emptyList())
        SeedWaypointsUseCase(waypointRepository, waypointSeeder)()
        verify(waypointRepository, never()).insertAll(any())
    }

    @Test
    fun `seedWaypoints inserts waypoints from seeder`() = runTest {
        whenever(waypointSeeder.fetchWaypoints()).thenReturn(listOf(waypoint))
        SeedWaypointsUseCase(waypointRepository, waypointSeeder)()
        verify(waypointRepository).insertAll(listOf(waypoint))
    }




    @Test
    fun `seedAchievements does nothing when seeder returns empty list`() = runTest {
        whenever(achievementSeeder.fetchAchievements()).thenReturn(emptyList())
        SeedAchievementsUseCase(achievementRepository, achievementSeeder)()
        verify(achievementRepository, never()).insertDefinitions(any())
        verify(achievementRepository, never()).deleteDefinitionsNotIn(any())
    }

    @Test
    fun `seedAchievements inserts definitions from seeder`() = runTest {
        whenever(achievementSeeder.fetchAchievements()).thenReturn(listOf(achievementDef))
        SeedAchievementsUseCase(achievementRepository, achievementSeeder)()
        verify(achievementRepository).insertDefinitions(listOf(achievementDef))
    }

    @Test
    fun `seedAchievements deletes definitions not in seeder list`() = runTest {
        whenever(achievementSeeder.fetchAchievements()).thenReturn(listOf(achievementDef))
        SeedAchievementsUseCase(achievementRepository, achievementSeeder)()
        verify(achievementRepository).deleteDefinitionsNotIn(listOf("ach_1"))
    }
}
