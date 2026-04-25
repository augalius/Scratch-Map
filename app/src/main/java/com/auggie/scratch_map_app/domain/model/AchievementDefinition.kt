package com.auggie.scratch_map_app.domain.model

/**
 * A stage threshold on a staged achievement, expressed either as a percentage of the metric's
 * goal or as an absolute count. Modelling both forms explicitly means a definition authored in
 * one form can no longer be silently discarded when read back in the other.
 */
sealed interface StageTarget {
    data class Percent(val percent: Int) : StageTarget
    data class Absolute(val value: Long) : StageTarget
}

/**
 * An authored achievement template, before it is evaluated against a region's progress.
 *
 * [metricType] selects how [targetValue] and the current value are derived — see
 * `GetRegionStatsUseCase`. A regional definition is instantiated once per region.
 */
data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val targetValue: Long,
    val metricType: String,
    val regionId: String? = null,
    val isRegional: Boolean = false,
    val category: String? = null,
    val stages: List<StageTarget> = emptyList()
)
