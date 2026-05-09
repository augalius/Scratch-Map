package com.auggie.scratch_map_app.data.seed

import com.auggie.scratch_map_app.domain.model.Trail
import com.auggie.scratch_map_app.domain.model.TrailCheckpoint
import com.auggie.scratch_map_app.domain.model.TrailDifficulty
import com.auggie.scratch_map_app.domain.repository.TrailContentSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrailSeeder @Inject constructor(
    private val firestore: FirebaseFirestore
) : TrailContentSource {
    override suspend fun fetchTrails(): List<Trail> {
        return try {
            val snapshot = firestore.collection("trails").get().await()
            Timber.d("TrailSeeder: got ${snapshot.documents.size} docs from Firestore")
            snapshot.documents.mapNotNull { doc ->
                runCatching {
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val description = doc.getString("description") ?: ""
                    val difficulty = runCatching {
                        TrailDifficulty.valueOf(doc.getString("difficulty") ?: "EASY")
                    }.getOrDefault(TrailDifficulty.EASY)
                    val distanceKm = doc.getDouble("estimated_distance_km") ?: 0.0

                    @Suppress("UNCHECKED_CAST")
                    val rawCheckpoints =
                        doc.get("checkpoints") as? List<Map<String, Any>> ?: emptyList()
                    val checkpoints = rawCheckpoints.mapIndexed { index, cp ->
                        TrailCheckpoint(
                            id = "${doc.id}_cp_${cp["id"] ?: index}",
                            trailId = doc.id,
                            name = cp["name"] as? String ?: "Checkpoint ${index + 1}",
                            description = cp["description"] as? String ?: "",
                            latitude = (cp["lat"] as? Number)?.toDouble() ?: return@mapNotNull null,
                            longitude = (cp["lng"] as? Number)?.toDouble()
                                ?: return@mapNotNull null,
                            order = (cp["order"] as? Number)?.toInt() ?: index,
                            unlockRadiusMeters = (cp["unlock_radius_meters"] as? Number)?.toInt()
                                ?: 50
                        )
                    }

                    Trail(
                        id = doc.id,
                        name = name,
                        description = description,
                        difficulty = difficulty,
                        estimatedDistanceKm = distanceKm,
                        checkpoints = checkpoints
                    )
                }.onFailure { Timber.w(it, "TrailSeeder: failed to parse trail %s", doc.id) }
                    .getOrNull()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch trails from Firestore")
            emptyList()
        }
    }
}
