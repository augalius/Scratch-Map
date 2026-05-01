package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.model.Achievement
import com.auggie.scratch_map_app.domain.model.AchievementDefinition
import com.auggie.scratch_map_app.domain.model.AchievementStage
import com.auggie.scratch_map_app.domain.model.Region
import com.auggie.scratch_map_app.domain.model.StageTarget
import com.auggie.scratch_map_app.domain.model.RegionStats
import com.auggie.scratch_map_app.domain.model.ScratchTile
import com.auggie.scratch_map_app.domain.model.Waypoint
import com.auggie.scratch_map_app.domain.model.WaypointCategory
import com.auggie.scratch_map_app.domain.repository.AchievementRepository
import com.auggie.scratch_map_app.domain.repository.RegionRepository
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
import com.auggie.scratch_map_app.domain.repository.WaypointRepository
import com.auggie.scratch_map_app.util.TileCalculator
import com.auggie.scratch_map_app.util.isInsidePolygon
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
                    val boundary = region.boundaryPoints
                    val wps = allWaypoints.filter { wp ->
                        if (boundary.isNotEmpty()) isInsidePolygon(
                            wp.latitude,
                            wp.longitude,
                            boundary
                        )
                        else wp.latitude in region.minLat..region.maxLat &&
                                wp.longitude in region.minLng..region.maxLng
                    }
                    val tiles = if (boundary.isNotEmpty())
                        tileLists[i].count { isInsidePolygon(it.centerLat, it.centerLng, boundary) }
                            .toLong()
                    else
                        tileLists[i].size.toLong()

                    val applicableDefs = allDefinitions.filter {
                        it.isRegional || it.regionId == region.id
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

        return defs.mapNotNull { def ->
            val id = if (def.isRegional) "${region.id}_${def.id}" else def.id

            val categoryGoal: Long = when (def.metricType) {
                "WAYPOINTS_CATEGORY" -> {
                    val cat =
                        runCatching { WaypointCategory.valueOf(def.category ?: "") }.getOrNull()
                    waypoints.count { it.category == cat }.toLong()
                }

                "WAYPOINTS_ALL" -> waypointTotal
                else -> 0L
            }

            // Skip category achievements with no waypoints in this region
            if (def.metricType == "WAYPOINTS_CATEGORY" && categoryGoal == 0L) return@mapNotNull null

            // Resolve stages (empty = not a staged achievement)
            val stages = resolveStages(def.stages, def.metricType, goal, categoryGoal)

            // Resolve target value
            val targetValue = when (def.metricType) {
                "TILES_PERCENT" -> goal
                "WAYPOINTS_ALL" -> waypointTotal
                "WAYPOINTS_CATEGORY" -> categoryGoal
                else -> def.targetValue
            }

            // Resolve current value
            val currentValue = when (def.metricType) {
                "TILES", "TILES_PERCENT" -> tiles
                "WAYPOINTS", "WAYPOINTS_ALL" -> waypointsUnlocked
                "WAYPOINTS_CATEGORY" -> {
                    val cat =
                        runCatching { WaypointCategory.valueOf(def.category ?: "") }.getOrNull()
                    waypoints.count { it.category == cat && it.isUnlocked }.toLong()
                }

                else -> 0L
            }

            val description = def.description.replace("{region}", region.name)

            Achievement(
                id = id,
                title = def.title,
                description = description,
                iconName = def.iconName,
                targetValue = targetValue,
                currentValue = currentValue,
                stages = stages
            )
        }
    }

    /**
     * Resolves each stage threshold against the metric's goal. Both target forms are honoured for
     * every metric: previously percent-based metrics demanded `target_percent` and all others
     * demanded `target_value`, so a mismatch threw and silently dropped the entire stage list.
     */
    private fun resolveStages(
        stages: List<StageTarget>,
        metricType: String,
        goal: Long,
        categoryGoal: Long
    ): List<AchievementStage> {
        if (stages.isEmpty()) return emptyList()

        val percentBase = when (metricType) {
            "WAYPOINTS_CATEGORY", "WAYPOINTS_ALL" -> categoryGoal
            else -> goal
        }
        return stages.map { stage ->
            AchievementStage(
                targetValue = when (stage) {
                    is StageTarget.Percent ->
                        (percentBase * stage.percent / 100).coerceAtLeast(1L)

                    is StageTarget.Absolute -> stage.value
                }
            )
        }
    }
}
