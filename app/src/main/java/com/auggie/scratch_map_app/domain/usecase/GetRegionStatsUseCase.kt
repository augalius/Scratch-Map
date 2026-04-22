package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.model.Achievement
import com.auggie.scratch_map_app.domain.model.AchievementDefinition
import com.auggie.scratch_map_app.domain.model.Region
import com.auggie.scratch_map_app.domain.model.RegionStats
import com.auggie.scratch_map_app.domain.model.ScratchTile
import com.auggie.scratch_map_app.domain.model.Waypoint
import com.auggie.scratch_map_app.domain.repository.AchievementRepository
import com.auggie.scratch_map_app.domain.repository.RegionRepository
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
import com.auggie.scratch_map_app.domain.repository.WaypointRepository
import com.auggie.scratch_map_app.util.TileCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetRegionStatsUseCase @Inject constructor(
    private val regionRepository: RegionRepository,
    private val tileRepository: ScratchTileRepository,
    private val waypointRepository: WaypointRepository,
    private val achievementRepository: AchievementRepository
) {
    operator fun invoke(): Flow<List<RegionStats>> =
        regionRepository.observeAll().flatMapLatest { regions ->
            if (regions.isEmpty()) return@flatMapLatest flowOf(emptyList())

            val tileFlows = regions.map { r ->
                tileRepository.observeTilesInBounds(r.minLat, r.maxLat, r.minLng, r.maxLng)
            }
            combine(
                tileFlows +
                        waypointRepository.observeAll() +
                        achievementRepository.observeDefinitions()
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val tileLists = values.take(regions.size).map { it as List<ScratchTile> }

                @Suppress("UNCHECKED_CAST")
                val allWaypoints = values[regions.size] as List<Waypoint>

                @Suppress("UNCHECKED_CAST")
                val allDefinitions = values[regions.size + 1] as List<AchievementDefinition>

                regions.mapIndexed { i, region ->
                    val wps = allWaypoints.filter { wp ->
                        wp.latitude in region.minLat..region.maxLat &&
                                wp.longitude in region.minLng..region.maxLng
                    }
                    val tiles = tileLists[i].size.toLong()

                    val applicableDefs = allDefinitions.filter {
                        it.regionId == null || it.regionId == region.id
                    }

                    val centerLat = (region.minLat + region.maxLat) / 2
                    val tileSize =
                        TileCalculator.tileSizeMeters(centerLat, TileCalculator.TILE_ZOOM)
                    val tileAreaKm2 = tileSize * tileSize / 1_000_000.0

                    RegionStats(
                        region = region,
                        tilesRevealed = tiles,
                        waypointsUnlocked = wps.count { it.isUnlocked },
                        waypointsTotal = wps.size,
                        achievements = buildAchievements(region, tiles, wps, applicableDefs),
                        revealedAreaKm2 = tiles * tileAreaKm2,
                        goalAreaKm2 = region.goalTiles * tileAreaKm2
                    )
                }
            }
        }

    private fun buildAchievements(
        region: Region,
        tiles: Long,
        waypoints: List<Waypoint>,
        defs: List<AchievementDefinition>
    ): List<Achievement> {
        val goal = region.goalTiles.toLong()
        val waypointsUnlocked = waypoints.count { it.isUnlocked }.toLong()
        val waypointTotal = waypoints.size.toLong().coerceAtLeast(1)

        return defs.map { def ->
            val id = if (def.regionId == null) "${region.id}_${def.id}" else def.id

            // Resolve target value
            val targetValue = when (def.metricType) {
                "TILES_PERCENT" -> goal
                "WAYPOINTS_ALL" -> waypointTotal
                else -> def.targetValue
            }

            // Resolve current value
            val currentValue = when (def.metricType) {
                "TILES", "TILES_PERCENT" -> tiles
                "WAYPOINTS", "WAYPOINTS_ALL" -> waypointsUnlocked
                else -> 0L
            }

            val description = def.description.replace("{region}", region.name)

            Achievement(
                id = id,
                title = def.title,
                description = description,
                iconName = def.iconName,
                targetValue = targetValue,
                currentValue = currentValue
            )
        }
    }
}
