package com.auggie.scratch_map_app.domain.repository

import com.auggie.scratch_map_app.domain.model.AchievementDefinition
import com.auggie.scratch_map_app.domain.model.Region
import com.auggie.scratch_map_app.domain.model.Waypoint

/**
 * Read-only sources of authored content, fetched from the backend and mirrored locally.
 *
 * These exist so the seeding use cases depend on the domain rather than on concrete
 * Firestore classes. Implementations return an empty list on failure — a failed fetch must
 * never be mistaken for "the content set is now empty", which would delete local rows.
 */
interface RegionContentSource {
    suspend fun fetchRegions(): List<Region>
}

interface WaypointContentSource {
    suspend fun fetchWaypoints(): List<Waypoint>
}

interface AchievementContentSource {
    suspend fun fetchAchievements(): List<AchievementDefinition>
}

/** Tracks the authored-content version, so seeding only runs when the backend bumps it. */
interface ContentVersionRepository {
    suspend fun getRemoteVersion(): Int
    suspend fun getLocalVersion(): Int
    suspend fun saveLocalVersion(version: Int)
}
