package com.auggie.scratch_map_app.domain.model

/**
 * An authored achievement template, before it is evaluated against a region's progress.
 *
 * [metricType] selects how [targetValue] and the current value are derived — see
 * `GetRegionStatsUseCase`.
 */
data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val targetValue: Long,
    val metricType: String,
    val regionId: String? = null
)
