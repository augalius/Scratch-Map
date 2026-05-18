package com.auggie.scratch_map_app.data.repository

import com.auggie.scratch_map_app.domain.model.DirectionalRadii
import com.auggie.scratch_map_app.domain.repository.RoadGeometryRepository
import com.auggie.scratch_map_app.data.remote.OverpassRoadDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoadGeometryRepositoryImpl @Inject constructor(
    private val densityService: OverpassRoadDataSource
) : RoadGeometryRepository {

    override suspend fun getDirectionalRadii(lat: Double, lng: Double): DirectionalRadii =
        densityService.getDirectionalRadii(lat, lng)
}
