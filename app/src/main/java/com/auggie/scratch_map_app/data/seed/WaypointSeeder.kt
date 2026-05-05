package com.auggie.scratch_map_app.data.seed

import com.auggie.scratch_map_app.domain.model.Waypoint
import com.auggie.scratch_map_app.domain.model.WaypointCategory
import com.auggie.scratch_map_app.domain.repository.WaypointContentSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaypointSeeder @Inject constructor(
    private val firestore: FirebaseFirestore
) : WaypointContentSource {

    override suspend fun fetchWaypoints(): List<Waypoint> {
        return try {
            val snapshot = firestore.collection("waypoints").get().await()
            snapshot.documents.mapNotNull { doc ->
                runCatching {
                    val categoryStr = (doc.getString("category") ?: "OTHER").uppercase()
                    val category = runCatching { WaypointCategory.valueOf(categoryStr) }
                        .getOrDefault(WaypointCategory.OTHER)
                    Waypoint(
                        stringId = doc.id,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        latitude = doc.getDouble("latitude") ?: return@mapNotNull null,
                        longitude = doc.getDouble("longitude") ?: return@mapNotNull null,
                        description = doc.getString("description") ?: "",
                        category = category,
                        unlockRadiusMeters = (doc.getLong("unlock_radius_meters") ?: 50L).toInt(),
                        imageUrl = doc.getString("image_url") ?: ""
                    )
                }.onFailure { Timber.e(it, "Failed to parse waypoint doc: %s", doc.id) }
                    .getOrNull()
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch waypoints from Firestore")
            emptyList()
        }
    }
}
