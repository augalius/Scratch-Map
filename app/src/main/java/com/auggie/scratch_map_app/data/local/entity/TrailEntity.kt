package com.auggie.scratch_map_app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trails")
data class TrailEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String = "",
    @ColumnInfo(name = "difficulty") val difficulty: String = "EASY",
    @ColumnInfo(name = "estimated_distance_km") val estimatedDistanceKm: Double = 0.0,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "is_unlocked", defaultValue = "0") val isUnlocked: Boolean = false,
    @ColumnInfo(name = "unlock_radius_meters", defaultValue = "100") val unlockRadiusMeters: Int = 100
)
