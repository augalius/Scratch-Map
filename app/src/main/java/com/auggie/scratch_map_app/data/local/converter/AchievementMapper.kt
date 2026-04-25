package com.auggie.scratch_map_app.data.local.converter

import com.auggie.scratch_map_app.data.local.entity.AchievementDefinitionEntity
import com.auggie.scratch_map_app.domain.model.AchievementDefinition
import com.auggie.scratch_map_app.domain.model.StageTarget
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * Maps between [AchievementDefinitionEntity] and [AchievementDefinition].
 *
 * Stage thresholds are stored as a JSON blob because their count varies per achievement; that
 * encoding is a persistence detail and is confined to this file.
 */
object AchievementMapper {

    fun entityToDomain(e: AchievementDefinitionEntity) = AchievementDefinition(
        id = e.id,
        title = e.title,
        description = e.description,
        iconName = e.iconName,
        targetValue = e.targetValue,
        metricType = e.metricType,
        regionId = e.regionId,
        isRegional = e.isRegional,
        category = e.category,
        stages = decodeStages(e.stagesJson)
    )

    fun domainToEntity(d: AchievementDefinition) = AchievementDefinitionEntity(
        id = d.id,
        title = d.title,
        description = d.description,
        iconName = d.iconName,
        targetValue = d.targetValue,
        metricType = d.metricType,
        regionId = d.regionId,
        isRegional = d.isRegional,
        category = d.category,
        stagesJson = encodeStages(d.stages)
    )

    fun encodeStages(stages: List<StageTarget>): String =
        JSONArray().apply {
            stages.forEach { stage ->
                put(JSONObject().apply {
                    when (stage) {
                        is StageTarget.Percent -> put("target_percent", stage.percent)
                        is StageTarget.Absolute -> put("target_value", stage.value)
                    }
                })
            }
        }.toString()

    fun decodeStages(json: String): List<StageTarget> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.getJSONObject(i)
                when {
                    obj.has("target_percent") -> StageTarget.Percent(obj.getInt("target_percent"))
                    obj.has("target_value") -> StageTarget.Absolute(obj.getLong("target_value"))
                    else -> null
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Malformed achievement stages JSON: %s", json)
            emptyList()
        }
    }
}
