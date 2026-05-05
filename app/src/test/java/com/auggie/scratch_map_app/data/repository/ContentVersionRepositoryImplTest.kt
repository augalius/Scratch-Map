package com.auggie.scratch_map_app.data.repository

import com.auggie.scratch_map_app.data.local.ContentVersionStore
import com.auggie.scratch_map_app.data.remote.FirestoreContentVersion
import com.auggie.scratch_map_app.data.remote.RemoteContentConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ContentVersionRepositoryImplTest {

    private lateinit var remoteConfig: RemoteContentConfig
    private lateinit var firestoreVersion: FirestoreContentVersion
    private lateinit var repository: ContentVersionRepositoryImpl

    @Before
    fun setUp() {
        remoteConfig = mock()
        firestoreVersion = mock()
        val store: ContentVersionStore = mock()
        repository = ContentVersionRepositoryImpl(remoteConfig, firestoreVersion, store)
    }

    @Test
    fun `Firestore publish wins when higher`() = runTest {
        whenever(remoteConfig.fetchContentVersion()).thenReturn(3)
        whenever(firestoreVersion.fetchContentVersion()).thenReturn(4)
        assertEquals(4, repository.getRemoteVersion())
    }

    @Test
    fun `Remote Config still wins when higher`() = runTest {
        whenever(remoteConfig.fetchContentVersion()).thenReturn(7)
        whenever(firestoreVersion.fetchContentVersion()).thenReturn(4)
        assertEquals(7, repository.getRemoteVersion())
    }

    @Test
    fun `missing Firestore version falls back to Remote Config`() = runTest {
        whenever(remoteConfig.fetchContentVersion()).thenReturn(2)
        whenever(firestoreVersion.fetchContentVersion()).thenReturn(0)
        assertEquals(2, repository.getRemoteVersion())
    }
}
