package com.auggie.scratch_map_app.data.seed

import com.auggie.scratch_map_app.domain.model.AchievementDefinition
import com.auggie.scratch_map_app.domain.model.StageTarget
import com.auggie.scratch_map_app.domain.repository.AchievementContentSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementSeeder @Inject constructor(
    private val firestore: FirebaseFirestore
) : AchievementContentSource {

    override suspend fun fetchAchievements(): List<AchievementDefinition> {
        return try {
            val snapshot = firestore.collection("achievements").get().await()
            snapshot.documents.mapNotNull { doc ->
                runCatching {
                    AchievementDefinition(
                        id = doc.id,
                        title = doc.getString("title") ?: return@mapNotNull null,
                        description = doc.getString("description") ?: "",
                        iconName = doc.getString("icon_name") ?: "",
                        targetValue = doc.getLong("target_value") ?: 0L,
                        metricType = doc.getString("metric_type") ?: return@mapNotNull null,
                        regionId = doc.getString("region_id"),
                        isRegional = doc.getBoolean("is_regional") ?: false,
                        category = doc.getString("category"),
                        stages = parseStages(doc.get("stages"))
                    )
                }.onFailure { Timber.e(it, "Failed to parse achievement doc: %s", doc.id) }
                    .getOrNull()
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch achievements from Firestore")
            emptyList()
        }
    }

    /**
     * Accepts both stage forms. Firestore hands back numbers as Long or Double, so these are
     * read as [Number] — narrowing to Long alone silently turned valid stages into zeroes.
     */
    private fun parseStages(raw: Any?): List<StageTarget> =
        (raw as? List<*>)?.filterIsInstance<Map<*, *>>()?.mapNotNull { stage ->
            (stage["target_percent"] as? Number)
                ?.let { return@mapNotNull StageTarget.Percent(it.toInt()) }
            (stage["target_value"] as? Number)
                ?.let { StageTarget.Absolute(it.toLong()) }
        } ?: emptyList()
}
