package com.auggie.scratch_map_app.ui.trail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auggie.scratch_map_app.domain.model.ScratchTile
import com.auggie.scratch_map_app.domain.model.Trail
import com.auggie.scratch_map_app.domain.model.TrailCheckpoint
import com.auggie.scratch_map_app.domain.model.UserLocation
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
import com.auggie.scratch_map_app.domain.repository.TrailRepository
import com.auggie.scratch_map_app.domain.location.LocationClient
import com.auggie.scratch_map_app.util.TileCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class TrailUiState(
    val trail: Trail? = null,
    val currentLocation: UserLocation? = null,
    val isLoading: Boolean = true,
    val lastReachedCheckpoint: TrailCheckpoint? = null,
    val revealedTiles: List<ScratchTile> = emptyList()
)

@HiltViewModel
class TrailViewModel @Inject constructor(
    private val trailRepository: TrailRepository,
    private val tileRepository: ScratchTileRepository,
    private val locationClient: LocationClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrailUiState())
    val uiState: StateFlow<TrailUiState> = _uiState.asStateFlow()

    private val triggeredCheckpoints = mutableSetOf<String>()
    private var loadedTrailId: String? = null

    /** Idempotent: a second call for the same trail would duplicate all three collectors. */
    fun loadTrail(trailId: String) {
        if (loadedTrailId == trailId) return
        loadedTrailId = trailId

        viewModelScope.launch {
            trailRepository.observeTrail(trailId).collect { trail ->
                _uiState.update { it.copy(trail = trail, isLoading = false) }
            }
        }
        viewModelScope.launch {
            tileRepository.observeAllTiles().collect { tiles ->
                _uiState.update { it.copy(revealedTiles = tiles) }
            }
        }
        viewModelScope.launch {
            locationClient.getLocationUpdates(intervalMs = 2000L, minDistanceMeters = 1f)
                .catch { e -> Timber.w(e, "Trail location error") }
                .collect { location ->
                    _uiState.update { it.copy(currentLocation = location) }
                    checkProximity(location)
                }
        }
    }

    private fun checkProximity(location: UserLocation) {
        val trail = _uiState.value.trail ?: return
        if (trail.isCompleted) return

        val nextCheckpoint = trail.checkpoints
            .filter { !it.isReached && !triggeredCheckpoints.contains(it.id) }
            .minByOrNull { it.order }
            ?: return

        val distance = TileCalculator.haversineDistance(
            location.latitude, location.longitude,
            nextCheckpoint.latitude, nextCheckpoint.longitude
        )

        if (distance <= nextCheckpoint.unlockRadiusMeters) {
            triggeredCheckpoints.add(nextCheckpoint.id)
            completeCheckpoint(nextCheckpoint)
        }
    }




    private fun completeCheckpoint(checkpoint: TrailCheckpoint) {
        viewModelScope.launch {
            trailRepository.markCheckpointReached(checkpoint.id)
            _uiState.update { it.copy(lastReachedCheckpoint = checkpoint) }
        }
    }

    fun clearReachedCheckpoint() {
        _uiState.update { it.copy(lastReachedCheckpoint = null) }
    }

}
