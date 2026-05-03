package com.auggie.scratch_map_app

import android.app.Application
import android.content.Context
import com.auggie.scratch_map_app.di.ApplicationScope
import com.auggie.scratch_map_app.domain.repository.AuthRepository
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
import com.auggie.scratch_map_app.domain.repository.SyncRepository
import com.auggie.scratch_map_app.domain.usecase.SeedAchievementsUseCase
import com.auggie.scratch_map_app.domain.usecase.SeedRegionsUseCase
import com.auggie.scratch_map_app.domain.usecase.SeedWaypointsUseCase
import com.mapbox.common.MapboxOptions
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ScratchMapApp : Application() {

    @Inject
    lateinit var seedWaypointsUseCase: SeedWaypointsUseCase

    @Inject
    lateinit var seedRegionsUseCase: SeedRegionsUseCase

    @Inject
    lateinit var seedAchievementsUseCase: SeedAchievementsUseCase

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

        appScope.launch {
            seedWaypointsUseCase()
            seedRegionsUseCase()
            seedAchievementsUseCase()
        }
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
     * Each time a user is first seen (app start while signed in, or a fresh sign-in): pull cloud
     * progress down, push local tiles up, and update the profile summary.
     */
    private suspend fun restoreOnSignIn() {
        var lastRestoredUid: String? = null
        authRepository.observeAuthState().collect { user ->
            if (user == null || user.uid == lastRestoredUid) return@collect
            lastRestoredUid = user.uid
            Timber.i("Auth: signed in as %s", user.uid)
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
