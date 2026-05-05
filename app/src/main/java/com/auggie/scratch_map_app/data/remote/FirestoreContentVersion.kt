package com.auggie.scratch_map_app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the content version that the backend bumps when content is published (`config/content.version`).
 */
@Singleton
class FirestoreContentVersion @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /** 0 when missing or unreadable, so the Remote Config value wins. */
    suspend fun fetchContentVersion(): Int =
        try {
            firestore.collection("config").document("content").get().await()
                .getLong("version")?.toInt() ?: 0
        } catch (e: Exception) {
            Timber.w(e, "Firestore content version fetch failed")
            0
        }
}
