package com.auggie.scratch_map_app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
import com.auggie.scratch_map_app.domain.repository.TrailRepository
import com.auggie.scratch_map_app.domain.repository.WaypointRepository
import com.auggie.scratch_map_app.domain.model.RegionStats
import com.auggie.scratch_map_app.domain.usecase.GetRegionStatsUseCase
import com.auggie.scratch_map_app.util.TileCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class GlobalStats(
    val totalAreaKm2: Double = 0.0,
    val totalWaypointsUnlocked: Int = 0,
    val totalTrailsCompleted: Int = 0,
    val regionsVisited: Int = 0
)

data class StatsUiState(
    val regionStats: List<RegionStats> = emptyList(),
    val globalStats: GlobalStats = GlobalStats()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getRegionStatsUseCase: GetRegionStatsUseCase,
    private val tileRepository: ScratchTileRepository,
    private val waypointRepository: WaypointRepository,
    private val trailRepository: TrailRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val tileAreaKm2 = TileCalculator.tileSizeMeters(55.3, TileCalculator.TILE_ZOOM)
        .let { it * it / 1_000_000.0 }

    init {
        val regionStats = getRegionStatsUseCase()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)

        regionStats
            .onEach { stats -> _uiState.update { it.copy(regionStats = stats) } }
            .launchIn(viewModelScope)

        combine(
            tileRepository.observeTileCount(),
            waypointRepository.observeUnlockedCount(),
            trailRepository.observeAll(),
            regionStats
        ) { totalTiles, totalWaypoints, trails, stats ->
            GlobalStats(
                totalAreaKm2 = totalTiles * tileAreaKm2,
                totalWaypointsUnlocked = totalWaypoints,
                totalTrailsCompleted = trails.count { it.isCompleted },
                regionsVisited = stats.count { it.progress > 0.0 }
            )
        }
            .onEach { stats -> _uiState.update { it.copy(globalStats = stats) } }
            .launchIn(viewModelScope)
    }
}
