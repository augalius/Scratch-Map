package com.auggie.scratch_map_app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "waypoints",
    indices = [
        Index(value = ["latitude", "longitude"])
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
    val unlockRadiusMeters: Int = 50,
    @ColumnInfo(name = "is_unlocked")
    val isUnlocked: Boolean = false,
    @ColumnInfo(name = "unlocked_at_millis")
    val unlockedAtMillis: Long? = null
)
