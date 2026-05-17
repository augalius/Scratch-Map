package com.auggie.scratch_map_app.service

import com.auggie.scratch_map_app.domain.model.Trail
import com.auggie.scratch_map_app.domain.model.Waypoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared state holder for tracking service status.
 * Injected into both the service and ViewModels so the UI
 * can quickly observe whether tracking is running.
 */
@Singleton
class ServiceStatusTracker @Inject constructor() {

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _sessionDistanceMeters = MutableStateFlow(0.0)
    val sessionDistanceMeters: StateFlow<Double> = _sessionDistanceMeters.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _lastUnlockedWaypoint = MutableStateFlow<Waypoint?>(null)
    val lastUnlockedWaypoint: StateFlow<Waypoint?> = _lastUnlockedWaypoint.asStateFlow()

    private val _lastUnlockedTrail = MutableStateFlow<Trail?>(null)
    val lastUnlockedTrail: StateFlow<Trail?> = _lastUnlockedTrail.asStateFlow()

    fun onTrackingStarted() {
        _isTracking.value = true
        _sessionDistanceMeters.value = 0.0
        _lastError.value = null
    }

    fun onTrackingStopped() {
        _isTracking.value = false
    }

    fun onDistanceUpdated(distanceMeters: Double) {
        _sessionDistanceMeters.value += distanceMeters
    }

    fun onError(message: String) {
        _lastError.value = message
    }

    fun clearError() {
        _lastError.value = null
    }

    fun onWaypointUnlocked(waypoint: Waypoint) {
        _lastUnlockedWaypoint.value = waypoint
    }

    fun clearLastUnlockedWaypoint() {
        _lastUnlockedWaypoint.value = null
    }

    fun onTrailUnlocked(trail: Trail) {
        _lastUnlockedTrail.value = trail
    }

    fun clearLastUnlockedTrail() {
        _lastUnlockedTrail.value = null
    }
}