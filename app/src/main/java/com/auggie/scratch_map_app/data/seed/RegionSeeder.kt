package com.auggie.scratch_map_app.data.seed

import com.auggie.scratch_map_app.domain.model.Region
import com.auggie.scratch_map_app.domain.repository.RegionContentSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegionSeeder @Inject constructor(
    private val firestore: FirebaseFirestore
) : RegionContentSource {

    override suspend fun fetchRegions(): List<Region> {
        return try {
            val snapshot = firestore.collection("regions").get().await()
            snapshot.documents.mapNotNull { doc ->
                runCatching {
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val goalTiles = (doc.getLong("goal_tiles") ?: return@mapNotNull null).toInt()

                    @Suppress("UNCHECKED_CAST")
                    val rawBoundary = doc.get("boundary") as? List<Map<String, Any>> ?: emptyList()
                    val boundaryPoints = rawBoundary.mapNotNull { pt ->
                        val lat = (pt["lat"] as? Number)?.toDouble() ?: return@mapNotNull null
                        val lng = (pt["lng"] as? Number)?.toDouble() ?: return@mapNotNull null
                        lat to lng
                    }

                    val minLat = if (boundaryPoints.isNotEmpty()) boundaryPoints.minOf { it.first }
                    else doc.getDouble("min_lat") ?: return@mapNotNull null
                    val maxLat = if (boundaryPoints.isNotEmpty()) boundaryPoints.maxOf { it.first }
                    else doc.getDouble("max_lat") ?: return@mapNotNull null
                    val minLng = if (boundaryPoints.isNotEmpty()) boundaryPoints.minOf { it.second }
                    else doc.getDouble("min_lng") ?: return@mapNotNull null
                    val maxLng = if (boundaryPoints.isNotEmpty()) boundaryPoints.maxOf { it.second }
                    else doc.getDouble("max_lng") ?: return@mapNotNull null

                    Region(
                        id = doc.id,
                        name = name,
                        description = doc.getString("description") ?: "",
                        minLat = minLat, maxLat = maxLat,
                        minLng = minLng, maxLng = maxLng,
                        goalTiles = goalTiles,
                        boundaryPoints = boundaryPoints
                    )
                }.onFailure { Timber.e(it, "Failed to parse region doc: %s", doc.id) }
                    .getOrNull()
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch regions from Firestore")
            emptyList()
        }
    }
}
