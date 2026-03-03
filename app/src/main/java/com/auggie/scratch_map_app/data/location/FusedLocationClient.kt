package com.auggie.scratch_map_app.data.location

import android.content.Context
import android.os.Looper
import com.auggie.scratch_map_app.domain.location.LocationClient
import com.auggie.scratch_map_app.domain.model.UserLocation
import com.auggie.scratch_map_app.util.PermissionUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Interface for getting location updates from Google Play Services.
 */
class FusedLocationClient(
    private val context: Context,
    private val fusedClient: FusedLocationProviderClient
) : LocationClient {

    override fun getLocationUpdates(
        intervalMs: Long,
        minDistanceMeters: Float
    ): Flow<UserLocation> = callbackFlow {

        if (!PermissionUtil.hasAnyLocation(context)) {
            throw LocationClient.PermissionMissingException()
        }
        if (!PermissionUtil.isLocationEnabled(context)) {
            throw LocationClient.GpsDisabledException()
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateDistanceMeters(minDistanceMeters)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMaxUpdateDelayMillis(intervalMs * 2)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    // trySend, not launch { send() }: the latter spawns an unbounded coroutine
                    // per fix and lets updates arrive out of order.
                    trySend(
                        UserLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            speed = if (location.hasSpeed()) location.speed else null
                        )
                    )
                }
            }
        }

        // Start location updates
        try {
            fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            throw LocationClient.PermissionMissingException()
        }

        // When the flow collector cancels, stop location updates
        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    override suspend fun getLastKnownLocation(): UserLocation? {
        if (!PermissionUtil.hasAnyLocation(context)) return null

        return try {
            val location = fusedClient.lastLocation.await() ?: return null
            UserLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                speed = if (location.hasSpeed()) location.speed else null
            )
        } catch (e: SecurityException) {
            Timber.w(e, "SecurityException getting last location")
            null
        }
    }
}