package com.auggie.scratch_map_app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievement_definitions")
data class AchievementDefinitionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    @ColumnInfo(name = "icon_name") val iconName: String,
    @ColumnInfo(name = "target_value") val targetValue: Long,
    @ColumnInfo(name = "metric_type") val metricType: String,
    @ColumnInfo(name = "region_id") val regionId: String? = null,
    @ColumnInfo(name = "is_regional") val isRegional: Boolean = false,
    @ColumnInfo(name = "category") val category: String? = null,
    @ColumnInfo(name = "stages_json") val stagesJson: String = "[]"
)
