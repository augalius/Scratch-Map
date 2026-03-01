package com.auggie.scratch_map_app.di

import com.auggie.scratch_map_app.data.repository.ScratchTileRepositoryImpl
import com.auggie.scratch_map_app.domain.repository.ScratchTileRepository
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
}
