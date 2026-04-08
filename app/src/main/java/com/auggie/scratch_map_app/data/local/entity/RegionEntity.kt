package com.auggie.scratch_map_app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "regions")
data class RegionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    @ColumnInfo(name = "min_lat") val minLat: Double,
    @ColumnInfo(name = "max_lat") val maxLat: Double,
    @ColumnInfo(name = "min_lng") val minLng: Double,
    @ColumnInfo(name = "max_lng") val maxLng: Double,
    @ColumnInfo(name = "goal_tiles") val goalTiles: Int
)
