package com.auggie.scratch_map_app.domain.location

import com.auggie.scratch_map_app.domain.model.UserLocation
import kotlinx.coroutines.flow.Flow

interface LocationClient {

    /**
     * Get a flow of location updates.
     */
    fun getLocationUpdates(intervalMs: Long, minDistanceMeters: Float): Flow<UserLocation>

    /**
     * Get a single best-effort last known location, or null if unavailable
     */
    suspend fun getLastKnownLocation(): UserLocation?

    class PermissionMissingException : Exception("Location permission not granted")
    class GpsDisabledException : Exception("GPS service disabled")


}