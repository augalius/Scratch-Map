package com.auggie.scratch_map_app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Centralized permission and location-services checking
 */
object PermissionUtil {

    private fun hasFineLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    private fun hasCoarseLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    fun hasAnyLocation(context: Context): Boolean =
        hasFineLocation(context) || hasCoarseLocation(context)

    private fun hasBackgroundLocation(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun getPermissionState(context: Context): TrackingPermissionState {
        return when {
            !hasAnyLocation(context) -> TrackingPermissionState.LOCATION_NEEDED
            !hasFineLocation(context) -> TrackingPermissionState.FINE_LOCATION_NEEDED
            !hasNotificationPermission(context) -> TrackingPermissionState.NOTIFICATION_NEEDED
            !hasBackgroundLocation(context) -> TrackingPermissionState.BACKGROUND_LOCATION_NEEDED
            !isLocationEnabled(context) -> TrackingPermissionState.LOCATION_DISABLED
            else -> TrackingPermissionState.READY
        }
    }
}

enum class TrackingPermissionState {
    LOCATION_NEEDED, // No location permission at all
    FINE_LOCATION_NEEDED, // Only coarse granted
    NOTIFICATION_NEEDED, // Notification permission
    BACKGROUND_LOCATION_NEEDED, // Foreground OK, background needed
    LOCATION_DISABLED, // Permission OK but GPS/location off
    READY // All set, can track

}