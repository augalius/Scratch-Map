package com.auggie.scratch_map_app.data.remote

import com.auggie.scratch_map_app.domain.model.ScratchTile
import com.auggie.scratch_map_app.domain.repository.AuthRepository
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
import com.auggie.scratch_map_app.domain.repository.SyncRepository
import com.auggie.scratch_map_app.domain.repository.WaypointRepository
import com.auggie.scratch_map_app.util.TileCalculator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Syncs data from and to Firestore.
 */
@Singleton
class FirestoreSyncService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val tileRepository: ScratchTileRepository,
    private val waypointRepository: WaypointRepository
) : SyncRepository {
    companion object {
        private const val CHUNK_SIZE = 5_000
        private const val TILES = "tiles"
    }

    /** Uid to write progress for, or null when signed out. */
    private fun writableUid(): String? = authRepository.currentUser?.uid

    /**
     * Upload all unsynced tiles to Firestore.
     */
    override suspend fun flushTiles() {
        val uid = writableUid() ?: return

        val unsynced = tileRepository.getUnsyncedTiles()
        if (unsynced.isEmpty()) return

        val allTiles = tileRepository.getAllTiles()
        if (allTiles.isEmpty()) return

        val unsyncedKeys = unsynced.mapTo(HashSet()) { Triple(it.tileX, it.tileY, it.zoomLevel) }

        val chunks = allTiles.chunked(CHUNK_SIZE)
        val dirtyIndices = chunks.mapIndexedNotNull { index, chunk ->
            if (chunk.any {
                    Triple(
                        it.tileX,
                        it.tileY,
                        it.zoomLevel
                    ) in unsyncedKeys
                }) index else null
        }

        Timber.i(
            "flushTiles: %d unsynced tiles → writing %d/%d chunk(s)",
            unsynced.size,
            dirtyIndices.size,
            chunks.size
        )

        var allSucceeded = true
        dirtyIndices.forEach { index ->
            val data = chunks[index].flatMap { tile ->
                listOf(tile.tileX, tile.tileY)
            }
            runCatching {
                firestore.collection("users").document(uid)
                    .collection(TILES).document("chunk_$index")
                    .set(mapOf("d" to data))
                    .await()
            }.onFailure {
                allSucceeded = false
            }
        }

        if (allSucceeded) {
            tileRepository.markAllTilesSynced()
        }
    }

    /**
     * Download all tiles from Firestore and insert into Room.
     * Only runs when Room is empty
     */
    override suspend fun restoreTiles() {
        val uid = authRepository.currentUser?.uid ?: return

        val snapshot = firestore.collection("users").document(uid)
            .collection(TILES).get().await()

        val tiles = parseChunkedTiles(snapshot.documents)
        if (tiles.isNotEmpty()) {
            tileRepository.revealTiles(tiles)
            tileRepository.markAllTilesSynced()
        }
    }

    private fun parseChunkedTiles(
        docs: List<com.google.firebase.firestore.DocumentSnapshot>
    ): List<ScratchTile> =
        docs.sortedBy { it.id }.flatMap { doc ->
            val flat = doc.get("d") as? List<*> ?: return@flatMap emptyList()
            (flat.indices step 2).mapNotNull { i ->
                if (i + 1 >= flat.size) return@mapNotNull null
                runCatching {
                    val x = (flat[i] as? Number)?.toInt() ?: return@mapNotNull null
                    val y = (flat[i + 1] as? Number)?.toInt() ?: return@mapNotNull null
                    val zoom = TileCalculator.TILE_ZOOM
                    val (lat, lng) = TileCalculator.tileCenterToLatLng(x, y, zoom)
                    ScratchTile(x, y, zoom, lat, lng)
                }.getOrNull()
            }
        }

    /**
     * Upload all unsynced waypoint unlocks to Firestore.
     */
    override suspend fun syncWaypointUnlock(stringId: String, unlockedAt: Long) {
        val uid = writableUid() ?: return

        // waypointId/uid allow counting unlocks with a collection-group query.
        firestore.collection("users").document(uid)
            .collection("waypoints").document(stringId)
            .set(
                mapOf("unlockedAt" to unlockedAt, "waypointId" to stringId, "uid" to uid),
                SetOptions.merge()
            )
            .await()
    }

    override suspend fun restoreWaypointUnlocks() {
        val uid = authRepository.currentUser?.uid ?: return
        val docs = firestore.collection("users").document(uid)
            .collection("waypoints").get().await()
        docs.documents.forEach { doc ->
            val wp = waypointRepository.getByStringId(doc.id) ?: return@forEach
            if (!wp.isUnlocked) {
                waypointRepository.markUnlocked(wp.id)
            }
        }
    }



}
