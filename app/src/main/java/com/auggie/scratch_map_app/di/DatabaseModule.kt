package com.auggie.scratch_map_app.di

import android.content.Context
import androidx.room.Room
import com.auggie.scratch_map_app.data.local.ScratchMapDatabase
import com.auggie.scratch_map_app.data.local.dao.ScratchTileDao
import com.auggie.scratch_map_app.data.local.dao.WaypointDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): ScratchMapDatabase {
        return Room.databaseBuilder(context, ScratchMapDatabase::class.java, "scratchmap.db")
            .addMigrations(
                ScratchMapDatabase.MIGRATION_2_3
            )
            // No 1→2 migration exists, so v1 installs are recreated from scratch. Everything
            // from v2 onward migrates in place via the chain above.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideScratchTileDao(database: ScratchMapDatabase): ScratchTileDao =
        database.scratchTileDao()

    @Provides
    @Singleton
    fun provideWaypointDao(database: ScratchMapDatabase): WaypointDao =
        database.waypointDao()
}
