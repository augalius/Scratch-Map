package com.auggie.scratch_map_app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "waypoints",
    indices = [
        Index(value = ["string_id"], unique = true),
        Index(value = ["latitude", "longitude"]),
        Index(value = ["category"]),
        Index(value = ["is_unlocked"])
    ]
)
data class WaypointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "string_id")
    val stringId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String = "",
    val category: String = "OTHER",
    @ColumnInfo(name = "unlock_radius_meters")
    val unlockRadiusMeters: Int = 200,
    @ColumnInfo(name = "image_url")
    val imageUrl: String = "",
    @ColumnInfo(name = "is_unlocked")
    val isUnlocked: Boolean = false
)
