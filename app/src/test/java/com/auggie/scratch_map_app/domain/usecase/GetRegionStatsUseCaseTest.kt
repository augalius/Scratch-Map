package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.model.Region
import com.auggie.scratch_map_app.domain.model.ScratchTile
import com.auggie.scratch_map_app.domain.model.Waypoint
import com.auggie.scratch_map_app.domain.model.WaypointCategory
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
    private lateinit var useCase: GetRegionStatsUseCase

    private val region = Region(
        id = "r1", name = "Old Town",
        minLat = 54.67, maxLat = 54.70,
        minLng = 25.27, maxLng = 25.30,
        goalTiles = 100
    )

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

    private fun setup(
        regions: List<Region> = listOf(region),
        tiles: List<ScratchTile> = emptyList(),
        waypoints: List<Waypoint> = emptyList()
    ) {
        whenever(regionRepository.observeAll()).thenReturn(flowOf(regions))
        whenever(tileRepository.observeTilesInBounds(any(), any(), any(), any())).thenReturn(
            flowOf(
                tiles
            )
        )
        whenever(waypointRepository.observeAll()).thenReturn(flowOf(waypoints))
    }

    @Before
    fun setUp() {
        regionRepository = mock()
        tileRepository = mock()
        waypointRepository = mock()
        useCase = GetRegionStatsUseCase(
            regionRepository,
            tileRepository,
            waypointRepository
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
        val result = useCase().first()
        assertEquals(2, result.size)
        assertEquals("r1", result[0].region.id)
        assertEquals("r2", result[1].region.id)
    }
}
