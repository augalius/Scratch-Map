package com.auggie.scratch_map_app.domain.repository

import com.auggie.scratch_map_app.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: AuthUser?
    fun observeAuthState(): Flow<AuthUser?>
    suspend fun register(email: String, password: String): Result<AuthUser>
    suspend fun login(email: String, password: String): Result<AuthUser>
    suspend fun logout()
}
