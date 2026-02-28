package com.auggie.scratch_map_app.data.local.converter

import com.auggie.scratch_map_app.data.local.entity.ScratchTileEntity
import com.auggie.scratch_map_app.domain.model.ScratchTile

object TileMapper {
    fun entityToDomain(entity: ScratchTileEntity): ScratchTile = ScratchTile(
        tileX = entity.tileX, tileY = entity.tileY, zoomLevel = entity.zoomLevel,
        centerLat = entity.centerLat, centerLng = entity.centerLng
    )

    fun domainToEntity(tile: ScratchTile): ScratchTileEntity = ScratchTileEntity(
        tileX = tile.tileX, tileY = tile.tileY, zoomLevel = tile.zoomLevel,
        centerLat = tile.centerLat, centerLng = tile.centerLng
    )
}