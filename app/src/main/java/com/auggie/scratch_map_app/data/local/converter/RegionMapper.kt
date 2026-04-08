package com.auggie.scratch_map_app.data.local.converter

import com.auggie.scratch_map_app.data.local.entity.RegionEntity
import com.auggie.scratch_map_app.domain.model.Region

object RegionMapper {
    fun entityToDomain(e: RegionEntity) = Region(
        id = e.id, name = e.name, description = e.description,
        minLat = e.minLat, maxLat = e.maxLat,
        minLng = e.minLng, maxLng = e.maxLng,
        goalTiles = e.goalTiles
    )

    fun domainToEntity(d: Region) = RegionEntity(
        id = d.id, name = d.name, description = d.description,
        minLat = d.minLat, maxLat = d.maxLat,
        minLng = d.minLng, maxLng = d.maxLng,
        goalTiles = d.goalTiles
    )
}
