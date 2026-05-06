package com.auggie.scratch_map_app

import android.app.Application
import android.content.Context
import com.auggie.scratch_map_app.di.ApplicationScope
import com.auggie.scratch_map_app.domain.repository.AuthRepository
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
import com.auggie.scratch_map_app.domain.repository.SyncRepository
import com.auggie.scratch_map_app.domain.usecase.SeedContentUseCase
import com.mapbox.common.MapboxOptions
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ScratchMapApp : Application() {

    @Inject
    lateinit var seedContentUseCase: SeedContentUseCase

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var syncRepository: SyncRepository

    @Inject
    lateinit var tileRepository: ScratchTileRepository

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        appScope.launch { migrateTilesOnce() }
        appScope.launch { restoreOnSignIn() }
    }

    /** One-time migration: re-upload tiles revealed before cloud sync tracked them. */
    private suspend fun migrateTilesOnce() {
        val prefs = getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_TILES_MIGRATED, false)) return

        val count = tileRepository.getTotalTileCount()
        Timber.i("tiles migration: %d local tiles found, marking unsynced", count)
        if (count > 0) tileRepository.markAllTilesUnsynced()
        prefs.edit().putBoolean(KEY_TILES_MIGRATED, true).apply()
    }

    /**
     * Each time a user is first seen (app start while signed in, or a fresh sign-in): refresh
     * content, pull cloud progress down, push local tiles up, and update the profile summary.
     *
     * Seeding waits for sign-in because Firestore rules only let signed-in users read content.
     * Seeding while signed out would fetch nothing yet still record the new content version,
     * leaving a fresh install without content until the next publish.
     */
    private suspend fun restoreOnSignIn() {
        var lastRestoredUid: String? = null
        authRepository.observeAuthState().collect { user ->
            if (user == null || user.uid == lastRestoredUid) return@collect
            lastRestoredUid = user.uid
            Timber.i("Auth: signed in as %s", user.uid)
            seedContentUseCase()
            syncRepository.restoreTiles()
            syncRepository.restoreWaypointUnlocks()
            syncRepository.flushTiles()
            syncRepository.upsertProfile()
        }
    }

    private companion object {
        const val KEY_TILES_MIGRATED = "tiles_v2_e"
    }
}
