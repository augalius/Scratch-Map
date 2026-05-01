package com.auggie.scratch_map_app.data.local.converter

import com.auggie.scratch_map_app.data.local.entity.RegionEntity
import com.auggie.scratch_map_app.domain.model.Region
import org.json.JSONArray

object RegionMapper {
    fun entityToDomain(e: RegionEntity) = Region(
        id = e.id, name = e.name, description = e.description,
        minLat = e.minLat, maxLat = e.maxLat,
        minLng = e.minLng, maxLng = e.maxLng,
        goalTiles = e.goalTiles,
        boundaryPoints = decodeBoundary(e.boundaryJson)
    )

    fun domainToEntity(d: Region) = RegionEntity(
        id = d.id, name = d.name, description = d.description,
        minLat = d.minLat, maxLat = d.maxLat,
        minLng = d.minLng, maxLng = d.maxLng,
        goalTiles = d.goalTiles,
        boundaryJson = encodeBoundary(d.boundaryPoints)
    )

    private fun encodeBoundary(points: List<Pair<Double, Double>>): String {
        val arr = JSONArray()
        points.forEach { (lat, lng) ->
            arr.put(JSONArray().put(lat).put(lng))
        }
        return arr.toString()
    }

    private fun decodeBoundary(json: String): List<Pair<Double, Double>> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val pair = arr.getJSONArray(i)
                pair.getDouble(0) to pair.getDouble(1)
            }
        }.getOrDefault(emptyList())
    }
}
