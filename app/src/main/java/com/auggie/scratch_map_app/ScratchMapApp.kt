package com.auggie.scratch_map_app

import android.app.Application
import com.mapbox.common.MapboxOptions
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class ScratchMapApp : Application() {

    override fun onCreate() {
        super.onCreate()
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
