package com.auggie.scratch_map_app.di

import com.auggie.scratch_map_app.data.repository.ScratchTileRepositoryImpl
import com.auggie.scratch_map_app.data.repository.AchievementRepositoryImpl
import com.auggie.scratch_map_app.data.repository.RegionRepositoryImpl
import com.auggie.scratch_map_app.data.repository.WaypointRepositoryImpl
import com.auggie.scratch_map_app.domain.repository.AchievementRepository
import com.auggie.scratch_map_app.domain.repository.RegionRepository
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
import com.auggie.scratch_map_app.domain.repository.WaypointRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindScratchTileRepository(impl: ScratchTileRepositoryImpl): ScratchTileRepository

    @Binds @Singleton
    abstract fun bindWaypointRepository(impl: WaypointRepositoryImpl): WaypointRepository

    @Binds @Singleton
    abstract fun bindRegionRepository(impl: RegionRepositoryImpl): RegionRepository

    @Binds @Singleton
    abstract fun bindAchievementRepository(impl: AchievementRepositoryImpl): AchievementRepository
}