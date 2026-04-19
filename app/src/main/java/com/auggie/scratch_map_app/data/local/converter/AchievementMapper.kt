package com.auggie.scratch_map_app.data.local.converter

import com.auggie.scratch_map_app.data.local.entity.AchievementDefinitionEntity
import com.auggie.scratch_map_app.domain.model.AchievementDefinition

object AchievementMapper {

    fun entityToDomain(e: AchievementDefinitionEntity) = AchievementDefinition(
        id = e.id,
        title = e.title,
        description = e.description,
        iconName = e.iconName,
        targetValue = e.targetValue,
        metricType = e.metricType
    )

    fun domainToEntity(d: AchievementDefinition) = AchievementDefinitionEntity(
        id = d.id,
        title = d.title,
        description = d.description,
        iconName = d.iconName,
        targetValue = d.targetValue,
        metricType = d.metricType
    )
}
