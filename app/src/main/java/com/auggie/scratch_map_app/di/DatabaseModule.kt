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
