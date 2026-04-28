package com.auggie.scratch_map_app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auggie.scratch_map_app.domain.model.AuthUser
import com.auggie.scratch_map_app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: AuthUser? = null
)


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(user = authRepository.currentUser))
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Email and password are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.login(email.trim(), password)
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Login failed") }
                }
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun register(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Email and password are required") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(error = "Passwords do not match") }
            return
        }
        if (password.length < 12) {
            _uiState.update { it.copy(error = "Password must be at least 12 characters") }
            return
        }
        if (!password.any { it.isUpperCase() }) {
            _uiState.update { it.copy(error = "Password must contain at least one uppercase letter") }
            return
        }
        if (!password.any { it.isDigit() }) {
            _uiState.update { it.copy(error = "Password must contain at least one number") }
            return
        }
        if (!password.any { !it.isLetterOrDigit() }) {
            _uiState.update { it.copy(error = "Password must contain at least one special character") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.register(email.trim(), password)
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Registration failed") }
                }
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
