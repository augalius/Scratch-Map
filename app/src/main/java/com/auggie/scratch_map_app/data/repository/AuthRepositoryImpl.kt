package com.auggie.scratch_map_app.data.repository

import com.auggie.scratch_map_app.domain.model.AuthUser
import com.auggie.scratch_map_app.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override val currentUser: AuthUser?
        get() = firebaseAuth.currentUser?.let { AuthUser(it.uid, it.email ?: "") }

    override fun observeAuthState(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.let { AuthUser(it.uid, it.email ?: "") })
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun register(email: String, password: String): Result<AuthUser> = runCatching {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: error("Registration failed: no user returned")
        AuthUser(user.uid, user.email ?: "")
    }

    override suspend fun login(email: String, password: String): Result<AuthUser> = runCatching {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: error("Login failed: no user returned")
        AuthUser(user.uid, user.email ?: "")
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }
}
