package com.auggie.scratch_map_app.data.remote

import com.auggie.scratch_map_app.di.ApplicationScope
import com.auggie.scratch_map_app.domain.repository.AccountStatusRepository
import com.auggie.scratch_map_app.domain.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens to `users/{uid}.disabled`, which only the backend can change.
 * One document read on attach and one per profile change.
 */
@Singleton
class FirestoreAccountStatusRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    authRepository: AuthRepository,
    @ApplicationScope appScope: CoroutineScope
) : AccountStatusRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isDisabled: StateFlow<Boolean> = authRepository.observeAuthState()
        .map { it?.uid }
        .distinctUntilChanged()
        .flatMapLatest { uid -> if (uid == null) flowOf(false) else observeDisabled(uid) }
        .stateIn(appScope, SharingStarted.Eagerly, false)

    private fun observeDisabled(uid: String): Flow<Boolean> = callbackFlow {
        val registration = firestore.collection(USERS).document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Keep the last known value; a read failure must not lock anyone out.
                    Timber.w(error, "Account status listener failed")
                    return@addSnapshotListener
                }
                trySend(snapshot?.getBoolean(FIELD_DISABLED) == true)
            }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    private companion object {
        const val USERS = "users"
        const val FIELD_DISABLED = "disabled"
    }
}
