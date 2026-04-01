package com.auggie.scratch_map_app.data.local.converter

import com.auggie.scratch_map_app.data.local.entity.WaypointEntity
import com.auggie.scratch_map_app.domain.model.Waypoint
import com.auggie.scratch_map_app.domain.model.WaypointCategory

object WaypointMapper {

    fun entityToDomain(entity: WaypointEntity): Waypoint = Waypoint(
        id = entity.id,
        stringId = entity.stringId,
        name = entity.name,
        latitude = entity.latitude,
        longitude = entity.longitude,
        description = entity.description,
        category = try {
            WaypointCategory.valueOf(entity.category)
        } catch (_: Exception) {
            WaypointCategory.OTHER
        },
        unlockRadiusMeters = entity.unlockRadiusMeters,
        isUnlocked = entity.isUnlocked
    )

    fun domainToEntity(domain: Waypoint): WaypointEntity = WaypointEntity(
        id = domain.id,
        stringId = domain.stringId,
        name = domain.name,
        latitude = domain.latitude,
        longitude = domain.longitude,
        description = domain.description,
        category = domain.category.name,
        unlockRadiusMeters = domain.unlockRadiusMeters,
        isUnlocked = domain.isUnlocked
    )
}
