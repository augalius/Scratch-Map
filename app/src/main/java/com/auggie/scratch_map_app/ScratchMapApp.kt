package com.auggie.scratch_map_app

import android.app.Application
import com.auggie.scratch_map_app.di.ApplicationScope
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
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        appScope.launch { seedWaypointsUseCase() }
    }
}
