package com.auggie.scratch_map_app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "scratch_tiles",
    primaryKeys = ["tile_x", "tile_y", "zoom_level"],
    indices = [Index(value = ["center_lat", "center_lng"])]
)
data class ScratchTileEntity(
    @ColumnInfo(name = "tile_x") val tileX: Int,
    @ColumnInfo(name = "tile_y") val tileY: Int,
    @ColumnInfo(name = "zoom_level") val zoomLevel: Int,
    @ColumnInfo(name = "center_lat") val centerLat: Double,
    @ColumnInfo(name = "center_lng") val centerLng: Double,
    @ColumnInfo(name = "synced_to_cloud") val syncedToCloud: Boolean = false
)