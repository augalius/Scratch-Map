package com.auggie.scratch_map_app.di

import com.auggie.scratch_map_app.data.remote.FirestoreSyncService
import com.auggie.scratch_map_app.data.repository.AuthRepositoryImpl
import com.auggie.scratch_map_app.data.repository.ContentVersionRepositoryImpl
import com.auggie.scratch_map_app.data.repository.RoadGeometryRepositoryImpl
import com.auggie.scratch_map_app.data.repository.ScratchTileRepositoryImpl
import com.auggie.scratch_map_app.data.seed.AchievementSeeder
import com.auggie.scratch_map_app.data.seed.RegionSeeder
import com.auggie.scratch_map_app.data.seed.TrailSeeder
import com.auggie.scratch_map_app.data.seed.WaypointSeeder
import com.auggie.scratch_map_app.domain.repository.AchievementContentSource
import com.auggie.scratch_map_app.domain.repository.ContentVersionRepository
import com.auggie.scratch_map_app.domain.repository.RegionContentSource
import com.auggie.scratch_map_app.domain.repository.RoadGeometryRepository
import com.auggie.scratch_map_app.domain.repository.SyncRepository
import com.auggie.scratch_map_app.domain.repository.AccountStatusRepository
import com.auggie.scratch_map_app.data.remote.FirestoreAccountStatusRepository
import com.auggie.scratch_map_app.domain.repository.TrailContentSource
import com.auggie.scratch_map_app.domain.repository.WaypointContentSource
import com.auggie.scratch_map_app.data.repository.SettingsRepositoryImpl
import com.auggie.scratch_map_app.data.repository.AchievementRepositoryImpl
import com.auggie.scratch_map_app.data.repository.RegionRepositoryImpl
import com.auggie.scratch_map_app.data.repository.TrailRepositoryImpl
import com.auggie.scratch_map_app.data.repository.WaypointRepositoryImpl
import com.auggie.scratch_map_app.domain.repository.AuthRepository
import com.auggie.scratch_map_app.domain.repository.AchievementRepository
import com.auggie.scratch_map_app.domain.repository.RegionRepository
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
import com.auggie.scratch_map_app.domain.repository.SettingsRepository
import com.auggie.scratch_map_app.domain.repository.TrailRepository
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
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindWaypointRepository(impl: WaypointRepositoryImpl): WaypointRepository

    @Binds @Singleton
    abstract fun bindRegionRepository(impl: RegionRepositoryImpl): RegionRepository

    @Binds @Singleton
    abstract fun bindAchievementRepository(impl: AchievementRepositoryImpl): AchievementRepository

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindTrailRepository(impl: TrailRepositoryImpl): TrailRepository

    @Binds @Singleton
    abstract fun bindSyncRepository(impl: FirestoreSyncService): SyncRepository

    @Binds @Singleton
    abstract fun bindAccountStatusRepository(
        impl: FirestoreAccountStatusRepository
    ): AccountStatusRepository

    @Binds @Singleton
    abstract fun bindRoadGeometryRepository(impl: RoadGeometryRepositoryImpl): RoadGeometryRepository

    @Binds @Singleton
    abstract fun bindContentVersionRepository(
        impl: ContentVersionRepositoryImpl
    ): ContentVersionRepository

    // Authored-content sources — the Firestore seeders, behind domain-owned interfaces.

    @Binds @Singleton
    abstract fun bindRegionContentSource(impl: RegionSeeder): RegionContentSource

    @Binds @Singleton
    abstract fun bindWaypointContentSource(impl: WaypointSeeder): WaypointContentSource

    @Binds @Singleton
    abstract fun bindTrailContentSource(impl: TrailSeeder): TrailContentSource

    @Binds @Singleton
    abstract fun bindAchievementContentSource(impl: AchievementSeeder): AchievementContentSource
}