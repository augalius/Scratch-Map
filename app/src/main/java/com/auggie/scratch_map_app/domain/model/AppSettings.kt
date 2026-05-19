package com.auggie.scratch_map_app.domain.model

/** User-configurable preferences. Everything here is reachable from the Settings screen. */
data class AppSettings(
    val isTrackingEnabled: Boolean = true,
    val vibrateOnDiscovery: Boolean = true
)

/**
 * Tracking parameters that are not user-configurable.
 *
 * These previously lived in [AppSettings] and were persisted to DataStore, but no code path ever
 * wrote a value other than the default — so they were constants wrapped in a preferences store.
 * If they ever need to be tunable, move them back and add the Settings controls.
 */
object TrackingDefaults {
    const val INTERVAL_MS = 5_000L
    const val ACCURACY_THRESHOLD_METERS = 50f
}
