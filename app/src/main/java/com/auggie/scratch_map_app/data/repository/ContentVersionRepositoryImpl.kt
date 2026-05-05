package com.auggie.scratch_map_app.data.repository

import com.auggie.scratch_map_app.data.local.ContentVersionStore
import com.auggie.scratch_map_app.data.remote.FirestoreContentVersion
import com.auggie.scratch_map_app.data.remote.RemoteContentConfig
import com.auggie.scratch_map_app.domain.repository.ContentVersionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentVersionRepositoryImpl @Inject constructor(
    private val remoteConfig: RemoteContentConfig,
    private val firestoreVersion: FirestoreContentVersion,
    private val versionStore: ContentVersionStore
) : ContentVersionRepository {

    /**
     * Content is published via Firestore; Remote Config is still honoured so versions set
     * there before Firestore publishing existed keep working. Whichever is higher wins.
     */
    override suspend fun getRemoteVersion(): Int = coroutineScope {
        val fromRemoteConfig = async { remoteConfig.fetchContentVersion() }
        val fromFirestore = async { firestoreVersion.fetchContentVersion() }
        maxOf(fromRemoteConfig.await(), fromFirestore.await())
    }

    override suspend fun getLocalVersion(): Int = versionStore.getLocalVersion()

    override suspend fun saveLocalVersion(version: Int) = versionStore.saveVersion(version)
}
