package com.auggie.scratch_map_app.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auggie.scratch_map_app.domain.model.RegionStats
import com.auggie.scratch_map_app.domain.usecase.GetRegionStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class RegionDetailUiState(
    val stats: RegionStats? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class RegionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getRegionStatsUseCase: GetRegionStatsUseCase
) : ViewModel() {

    private val regionId: String = checkNotNull(savedStateHandle["regionId"])

    val uiState: StateFlow<RegionDetailUiState> = getRegionStatsUseCase()
        .map { list -> list.find { it.region.id == regionId } }
        .map { stats -> RegionDetailUiState(stats = stats, isLoading = stats == null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RegionDetailUiState())
}
