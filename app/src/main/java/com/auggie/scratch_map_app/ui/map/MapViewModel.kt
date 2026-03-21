package com.auggie.scratch_map_app.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.auggie.scratch_map_app.domain.model.ScratchTile
import com.auggie.scratch_map_app.domain.model.UserLocation
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
import com.auggie.scratch_map_app.domain.usecase.RevealTilesUseCase
import com.auggie.scratch_map_app.domain.location.LocationClient
import com.auggie.scratch_map_app.service.LocationTrackingService
import com.auggie.scratch_map_app.service.ServiceStatusTracker
import com.auggie.scratch_map_app.util.PermissionUtil
import com.auggie.scratch_map_app.util.TrackingPermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class MapUiState(
    val revealedTiles: List<ScratchTile> = emptyList(),
    val currentLocation: UserLocation? = null,
    val hasPermissions: Boolean = false,
    val errorMessage: String? = null,
    val shouldCenterOnUser: Boolean = false,
    val isFollowing: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    application: Application,
    private val tileRepository: ScratchTileRepository,
    private val locationClient: LocationClient,
    private val serviceStatusTracker: ServiceStatusTracker,
    private val revealTilesUseCase: RevealTilesUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var locationPreviewJob: Job? = null

    /**
     * Last camera zoom, restored when the MapView is recreated (e.g. on tab switch).
     * Deliberately kept out of [MapUiState]: the camera listener writes it on every frame,
     * and it is only ever read once, when the MapView is constructed.
     */
    var savedCameraZoom: Double = 16.0
        private set

    init {
        observeAllRevealedTiles()
        observeServiceStatus()
        checkPermissions()
    }

    /**
     * Observe ALL revealed tiles from the database.
     * Tiles are converted to GeoJSON in the UI for the Mapbox fog overlay.
     */
    private fun observeAllRevealedTiles() {
        viewModelScope.launch {
            tileRepository.observeAllTiles().collect { tiles ->
                _uiState.update { it.copy(revealedTiles = tiles) }
            }
        }
    }









    private fun observeServiceStatus() {
        viewModelScope.launch {
            serviceStatusTracker.isTracking.collect { tracking ->
                if (tracking && locationPreviewJob == null) {
                    startLocationPreview()
                }
            }
        }
        viewModelScope.launch {
            serviceStatusTracker.lastError.filterNotNull().collect { error ->
                _uiState.update { it.copy(errorMessage = error) }
                serviceStatusTracker.clearError()
            }
        }
    }

    fun checkPermissions() {
        val context = getApplication<Application>()
        val state = PermissionUtil.getPermissionState(context)
        // Background location is optional — foreground tracking works without it.
        val nowGranted = state == TrackingPermissionState.READY ||
                state == TrackingPermissionState.BACKGROUND_LOCATION_NEEDED
        _uiState.update { it.copy(hasPermissions = nowGranted) }

        if (nowGranted && !LocationTrackingService.isRunning && locationPreviewJob == null) {
            fetchInitialLocation()
            startTracking()
        }
    }

    fun onPermissionsGranted() {
        checkPermissions()
    }

    private fun fetchInitialLocation() {
        viewModelScope.launch {
            try {
                var location = locationClient.getLastKnownLocation()
                if (location != null) {
                    Timber.i(
                        "Initial location (cached): (%.6f, %.6f)",
                        location.latitude,
                        location.longitude
                    )
                } else {
                    try {
                        locationClient.getLocationUpdates(
                            intervalMs = 1000L,
                            minDistanceMeters = 0f
                        )
                            .first()
                            .also { location = it }
                    } catch (e: Exception) {
                        Timber.w(e, "Could not get fresh location fix")
                    }
                }
                if (location != null) {
                    _uiState.update { it.copy(currentLocation = location) }
                    Timber.i(
                        "Using initial location: (%.6f, %.6f)",
                        location!!.latitude,
                        location!!.longitude
                    )
                    revealAtLocation(location!!)
                } else {
                    Timber.w("Could not get any location for initial reveal")
                }
            } catch (e: Exception) {
                Timber.w(e, "Could not get initial location")
            }
        }
    }

    /**
     * Reveal tiles at a given location using density-based directional radii.
     * Used only for the initial position on app open and after a map reset.
     * Ongoing scratching while tracking is handled entirely by LocationTrackingService
     */
    private fun revealAtLocation(location: UserLocation) {
        viewModelScope.launch {
            try {
                revealTilesUseCase(location)
            } catch (e: Exception) {
                Timber.w(e, "Failed to reveal tiles at location")
            }
        }
    }

    fun centerOnUser() {
        _uiState.update { it.copy(shouldCenterOnUser = true, isFollowing = true) }
    }

    fun stopFollowing() {
        _uiState.update { it.copy(isFollowing = false) }
    }

    fun onCameraCentered() {
        _uiState.update { it.copy(shouldCenterOnUser = false) }
    }

    fun saveZoom(zoom: Double) {
        savedCameraZoom = zoom
    }

    /**
     * Start the foreground tracking service
     * Should only be called after permissions are confirmed.
     */
    fun startTracking() {
        val context = getApplication<Application>()
        if (!PermissionUtil.hasAnyLocation(context)) {
            _uiState.update { it.copy(errorMessage = "Location permission required") }
            return
        }
        if (!PermissionUtil.isLocationEnabled(context)) {
            _uiState.update { it.copy(errorMessage = "Please enable location services") }
            return
        }
        LocationTrackingService.start(context)
        _uiState.update { it.copy(errorMessage = null) }

        startLocationPreview()
    }

    private fun startLocationPreview() {
        locationPreviewJob?.cancel()
        locationPreviewJob = viewModelScope.launch {
            try {
                locationClient.getLocationUpdates(intervalMs = 2000L, minDistanceMeters = 1f)
                    .catch { e -> Timber.w(e, "Location preview error") }
                    .collect { location ->
                        _uiState.update { it.copy(currentLocation = location) }
                    }
            } catch (e: Exception) {
                Timber.w(e, "Failed to start location preview")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun syncServiceState() {
        if (LocationTrackingService.isRunning && locationPreviewJob == null) {
            startLocationPreview()
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationPreviewJob?.cancel()
    }
}
