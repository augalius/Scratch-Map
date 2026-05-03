package com.auggie.scratch_map_app.domain.repository

/**
 * Two-way sync of user progress (revealed tiles, waypoint unlocks)
 * with the remote backend.
 */
interface SyncRepository {
    /** Uploads tiles revealed since the last successful upload. */
    suspend fun flushTiles()

    /** Downloads remote tiles into the local store. */
    suspend fun restoreTiles()

    suspend fun syncWaypointUnlock(stringId: String, unlockedAt: Long)
    suspend fun restoreWaypointUnlocks()

    /**
     * Writes the signed-in user's profile summary (email, last activity, app version, progress
     * counts) stored on the backend. Never throws.
     */
    suspend fun upsertProfile()
}
