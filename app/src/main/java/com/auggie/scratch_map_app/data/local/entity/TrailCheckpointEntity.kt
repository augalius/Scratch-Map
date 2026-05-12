package com.auggie.scratch_map_app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trail_checkpoints")
data class TrailCheckpointEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "trail_id") val trailId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String = "",
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "checkpoint_order") val checkpointOrder: Int = 0,
    @ColumnInfo(name = "unlock_radius_meters") val unlockRadiusMeters: Int = 50,
    @ColumnInfo(name = "is_reached") val isReached: Boolean = false,
    @ColumnInfo(name = "question", defaultValue = "") val question: String = "",
    @ColumnInfo(name = "answer", defaultValue = "") val answer: String = "",
    @ColumnInfo(name = "reached_at_millis") val reachedAtMillis: Long? = null
)
