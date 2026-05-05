package com.auggie.scratch_map_app.data.remote

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the content version from Firebase Remote Config.
 */
@Singleton
class RemoteContentConfig @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) {
    suspend fun fetchContentVersion(): Int =
        try {
            remoteConfig.fetchAndActivate().await()
            remoteConfig.getLong("content_version").toInt()
        } catch (e: Exception) {
            Timber.w(e, "Remote config fetch failed, using cached/default")
            remoteConfig.getLong("content_version").toInt()
        }
}
