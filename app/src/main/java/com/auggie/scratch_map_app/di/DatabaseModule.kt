package com.auggie.scratch_map_app.di

import android.content.Context
import androidx.room.Room
import com.auggie.scratch_map_app.data.local.ScratchMapDatabase
import com.auggie.scratch_map_app.data.local.dao.AchievementDefinitionDao
import com.auggie.scratch_map_app.data.local.dao.RegionDao
import com.auggie.scratch_map_app.data.local.dao.ScratchTileDao
import com.auggie.scratch_map_app.data.local.dao.TrailDao
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
                ScratchMapDatabase.MIGRATION_2_3,
                ScratchMapDatabase.MIGRATION_3_4,
                ScratchMapDatabase.MIGRATION_4_5,
                ScratchMapDatabase.MIGRATION_5_6,
                ScratchMapDatabase.MIGRATION_6_7,
                ScratchMapDatabase.MIGRATION_7_8,
                ScratchMapDatabase.MIGRATION_8_9,
                ScratchMapDatabase.MIGRATION_9_10,
                ScratchMapDatabase.MIGRATION_10_11,
                ScratchMapDatabase.MIGRATION_11_12
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

    @Provides
    @Singleton
    fun provideRegionDao(database: ScratchMapDatabase): RegionDao =
        database.regionDao()

    @Provides
    @Singleton
    fun provideAchievementDefinitionDao(database: ScratchMapDatabase): AchievementDefinitionDao =
        database.achievementDefinitionDao()

    @Provides
    @Singleton
    fun provideTrailDao(database: ScratchMapDatabase): TrailDao =
        database.trailDao()
}