package com.auggie.scratch_map_app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auggie.scratch_map_app.domain.repository.AuthRepository
import com.auggie.scratch_map_app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isTrackingEnabled: Boolean = true,
    val vibrateOnDiscovery: Boolean = true,
    val userEmail: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { s ->
                _uiState.update {
                    it.copy(
                        isTrackingEnabled = s.isTrackingEnabled,
                        vibrateOnDiscovery = s.vibrateOnDiscovery
                    )
                }
            }
        }
        _uiState.update { it.copy(userEmail = authRepository.currentUser?.email ?: "") }
    }

    fun toggleTracking() {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(isTrackingEnabled = !it.isTrackingEnabled) }
        }
    }

    fun toggleVibration() {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(vibrateOnDiscovery = !it.vibrateOnDiscovery) }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
