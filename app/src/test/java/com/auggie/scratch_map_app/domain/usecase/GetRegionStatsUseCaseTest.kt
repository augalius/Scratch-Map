package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.model.AchievementDefinition
import com.auggie.scratch_map_app.domain.model.Region
import com.auggie.scratch_map_app.domain.model.ScratchTile
import com.auggie.scratch_map_app.domain.model.StageTarget
import com.auggie.scratch_map_app.domain.model.Waypoint
import com.auggie.scratch_map_app.domain.model.WaypointCategory
import com.auggie.scratch_map_app.domain.repository.AchievementRepository
import com.auggie.scratch_map_app.domain.repository.RegionRepository
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
import com.auggie.scratch_map_app.domain.repository.WaypointRepository
import com.auggie.scratch_map_app.domain.usecase.GetRegionStatsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class GetRegionStatsUseCaseTest {

    private lateinit var regionRepository: RegionRepository
    private lateinit var tileRepository: ScratchTileRepository
    private lateinit var waypointRepository: WaypointRepository
    private lateinit var achievementRepository: AchievementRepository
    private lateinit var useCase: GetRegionStatsUseCase

    private val squareBoundary = listOf(
        54.67 to 25.27, 54.70 to 25.27,
        54.70 to 25.30, 54.67 to 25.30
    )

    private val region = Region(
        id = "r1", name = "Old Town",
        minLat = 54.67, maxLat = 54.70,
        minLng = 25.27, maxLng = 25.30,
        goalTiles = 100
    )

    private val regionWithBoundary = region.copy(boundaryPoints = squareBoundary)

    private fun waypoint(
        id: String, lat: Double, lng: Double,
        unlocked: Boolean = false,
        category: WaypointCategory = WaypointCategory.OTHER
    ) = Waypoint(
        stringId = id, name = id,
        latitude = lat, longitude = lng,
        isUnlocked = unlocked, category = category
    )

    private fun tile(lat: Double, lng: Double) =
        ScratchTile(tileX = 0, tileY = 0, zoomLevel = 16, centerLat = lat, centerLng = lng)

    private fun def(
        id: String, metricType: String,
        targetValue: Long = 100L,
        isRegional: Boolean = true,
        regionId: String? = null,
        category: String? = null,
        stages: List<StageTarget> = emptyList(),
        description: String = "desc"
    ) = AchievementDefinition(
        id = id, title = id, description = description,
        iconName = "icon", targetValue = targetValue,
        metricType = metricType, isRegional = isRegional,
        regionId = regionId, category = category, stages = stages
    )

    private fun setup(
        regions: List<Region> = listOf(region),
        tiles: List<ScratchTile> = emptyList(),
        waypoints: List<Waypoint> = emptyList(),
        defs: List<AchievementDefinition> = emptyList()
    ) {
        whenever(regionRepository.observeAll()).thenReturn(flowOf(regions))
        whenever(tileRepository.observeTilesInBounds(any(), any(), any(), any())).thenReturn(
            flowOf(
                tiles
            )
        )
        whenever(waypointRepository.observeAll()).thenReturn(flowOf(waypoints))
        whenever(achievementRepository.observeDefinitions()).thenReturn(flowOf(defs))
    }

    @Before
    fun setUp() {
        regionRepository = mock()
        tileRepository = mock()
        waypointRepository = mock()
        achievementRepository = mock()
        useCase = GetRegionStatsUseCase(
            regionRepository,
            tileRepository,
            waypointRepository,
            achievementRepository
        )
    }


    @Test
    fun `empty regions emits empty list`() = runTest {
        whenever(regionRepository.observeAll()).thenReturn(flowOf(emptyList()))
        val result = useCase().first()
        assertTrue(result.isEmpty())
    }


    @Test
    fun `waypoint inside bounding box is included`() = runTest {
        setup(waypoints = listOf(waypoint("wp_in", 54.685, 25.285)))
        assertEquals(1, useCase().first()[0].waypointsTotal)
    }

    @Test
    fun `waypoint outside bounding box is excluded`() = runTest {
        setup(waypoints = listOf(waypoint("wp_out", 54.60, 25.20)))
        assertEquals(0, useCase().first()[0].waypointsTotal)
    }

    @Test
    fun `only inside-bounds waypoints counted when no boundary polygon`() = runTest {
        val inside = waypoint("wp_in", 54.685, 25.285)
        val outside = waypoint("wp_out", 54.60, 25.20)
        setup(waypoints = listOf(inside, outside))
        assertEquals(1, useCase().first()[0].waypointsTotal)
    }


    @Test
    fun `waypoint inside polygon boundary is included`() = runTest {
        setup(
            regions = listOf(regionWithBoundary),
            waypoints = listOf(waypoint("wp_in", 54.685, 25.285))
        )
        assertEquals(1, useCase().first()[0].waypointsTotal)
    }

    @Test
    fun `waypoint outside polygon boundary is excluded`() = runTest {
        setup(
            regions = listOf(regionWithBoundary),
            waypoints = listOf(waypoint("wp_out", 54.60, 25.20))
        )
        assertEquals(0, useCase().first()[0].waypointsTotal)
    }

    @Test
    fun `polygon boundary splits waypoints correctly`() = runTest {
        val inside = waypoint("wp_in", 54.685, 25.285)
        val outside = waypoint("wp_out", 54.60, 25.20)
        setup(regions = listOf(regionWithBoundary), waypoints = listOf(inside, outside))
        val stats = useCase().first()[0]
        assertEquals(1, stats.waypointsTotal)
        assertEquals(0, stats.waypointsUnlocked)
    }


    @Test
    fun `tiles in region are counted`() = runTest {
        setup(tiles = listOf(tile(54.685, 25.285), tile(54.688, 25.288)))
        assertEquals(2L, useCase().first()[0].tilesRevealed)
    }

    @Test
    fun `no tiles gives zero count`() = runTest {
        setup()
        assertEquals(0L, useCase().first()[0].tilesRevealed)
    }

    @Test
    fun `tiles outside polygon boundary are not counted`() = runTest {
        setup(
            regions = listOf(regionWithBoundary),
            tiles = listOf(tile(54.685, 25.285), tile(54.60, 25.20))
        )
        assertEquals(1L, useCase().first()[0].tilesRevealed)
    }


    @Test
    fun `waypointsUnlocked counts only unlocked waypoints in region`() = runTest {
        setup(
            waypoints = listOf(
                waypoint("wp1", 54.685, 25.285, unlocked = true),
                waypoint("wp2", 54.686, 25.286, unlocked = false),
                waypoint("wp3", 54.687, 25.287, unlocked = true)
            )
        )
        val stats = useCase().first()[0]
        assertEquals(2, stats.waypointsUnlocked)
        assertEquals(3, stats.waypointsTotal)
    }





    @Test
    fun `TILES metric currentValue equals tile count`() = runTest {
        setup(
            tiles = listOf(tile(54.685, 25.285), tile(54.686, 25.286)),
            defs = listOf(def("a1", "TILES"))
        )
        assertEquals(2L, useCase().first()[0].achievements[0].currentValue)
    }

    @Test
    fun `TILES_PERCENT metric currentValue equals tile count`() = runTest {
        setup(tiles = listOf(tile(54.685, 25.285)), defs = listOf(def("a1", "TILES_PERCENT")))
        assertEquals(1L, useCase().first()[0].achievements[0].currentValue)
    }

    @Test
    fun `WAYPOINTS metric currentValue equals unlocked count`() = runTest {
        setup(
            waypoints = listOf(
                waypoint("wp1", 54.685, 25.285, unlocked = true),
                waypoint("wp2", 54.686, 25.286, unlocked = false)
            ),
            defs = listOf(def("a1", "WAYPOINTS"))
        )
        assertEquals(1L, useCase().first()[0].achievements[0].currentValue)
    }

    @Test
    fun `WAYPOINTS_CATEGORY skipped when no matching category in region`() = runTest {
        setup(
            waypoints = listOf(waypoint("wp1", 54.685, 25.285, category = WaypointCategory.PARK)),
            defs = listOf(def("a1", "WAYPOINTS_CATEGORY", category = "CHURCH"))
        )
        assertTrue(useCase().first()[0].achievements.isEmpty())
    }

    @Test
    fun `WAYPOINTS_CATEGORY currentValue counts unlocked category waypoints`() = runTest {
        setup(
            waypoints = listOf(
                waypoint("c1", 54.685, 25.285, unlocked = true, category = WaypointCategory.CHURCH),
                waypoint("c2", 54.686, 25.286, unlocked = false, category = WaypointCategory.CHURCH)
            ),
            defs = listOf(def("a1", "WAYPOINTS_CATEGORY", category = "CHURCH"))
        )
        assertEquals(1L, useCase().first()[0].achievements[0].currentValue)
    }

    @Test
    fun `achievement description placeholder is replaced with region name`() = runTest {
        setup(defs = listOf(def("a1", "TILES", description = "Explore {region}")))
        assertEquals("Explore Old Town", useCase().first()[0].achievements[0].description)
    }

    @Test
    fun `regional achievement id is prefixed with region id`() = runTest {
        setup(defs = listOf(def("a1", "TILES", isRegional = true)))
        assertEquals("r1_a1", useCase().first()[0].achievements[0].id)
    }

    @Test
    fun `non-regional achievement id is not prefixed`() = runTest {
        setup(defs = listOf(def("ach_global", "TILES", isRegional = false, regionId = "r1")))
        assertEquals("ach_global", useCase().first()[0].achievements[0].id)
    }

    @Test
    fun `TILES_PERCENT targetValue resolves to goalTiles`() = runTest {
        setup(defs = listOf(def("a1", "TILES_PERCENT", targetValue = 999L)))
        assertEquals(100L, useCase().first()[0].achievements[0].targetValue)
    }

    @Test
    fun `achievement with absolute stages resolves stage targets`() = runTest {
        val stages = listOf(
            StageTarget.Absolute(25L), StageTarget.Absolute(50L), StageTarget.Absolute(100L)
        )
        setup(defs = listOf(def("a1", "TILES", stages = stages)))
        val ach = useCase().first()[0].achievements[0]
        assertEquals(3, ach.stages.size)
        assertEquals(25L, ach.stages[0].targetValue)
        assertEquals(100L, ach.stages[2].targetValue)
    }

    @Test
    fun `TILES_PERCENT stages convert target_percent to tile count`() = runTest {
        val stages = listOf(
            StageTarget.Percent(25), StageTarget.Percent(50), StageTarget.Percent(100)
        )
        setup(defs = listOf(def("a1", "TILES_PERCENT", stages = stages)))
        val ach = useCase().first()[0].achievements[0]
        assertEquals(3, ach.stages.size)
        assertEquals(25L, ach.stages[0].targetValue)
        assertEquals(50L, ach.stages[1].targetValue)
        assertEquals(100L, ach.stages[2].targetValue)
    }


    @Test
    fun `multiple regions each get their own stats`() = runTest {
        val region2 = Region(
            id = "r2", name = "New Town",
            minLat = 54.71, maxLat = 54.74,
            minLng = 25.31, maxLng = 25.35,
            goalTiles = 200
        )
        whenever(regionRepository.observeAll()).thenReturn(flowOf(listOf(region, region2)))
        whenever(tileRepository.observeTilesInBounds(any(), any(), any(), any())).thenReturn(
            flowOf(
                emptyList()
            )
        )
        whenever(waypointRepository.observeAll()).thenReturn(flowOf(emptyList()))
        whenever(achievementRepository.observeDefinitions()).thenReturn(flowOf(emptyList()))
        val result = useCase().first()
        assertEquals(2, result.size)
        assertEquals("r1", result[0].region.id)
        assertEquals("r2", result[1].region.id)
    }
}
