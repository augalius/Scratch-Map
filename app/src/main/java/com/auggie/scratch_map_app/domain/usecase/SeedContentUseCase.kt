package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.repository.ContentVersionRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedContentUseCase @Inject constructor(
    private val contentVersionRepository: ContentVersionRepository,
    private val seedRegions: SeedRegionsUseCase,
    private val seedWaypoints: SeedWaypointsUseCase,
    private val seedAchievements: SeedAchievementsUseCase
) {
    suspend operator fun invoke() {
        val remoteVersion = contentVersionRepository.getRemoteVersion()
        val localVersion = contentVersionRepository.getLocalVersion()

        if (remoteVersion <= localVersion) {
            Timber.d("Content up to date (v%d), skipping seed", localVersion)
            return
        }

        Timber.i("Content version %d is outdated, re-seeding to %d", localVersion, remoteVersion)
        seedRegions()
        seedWaypoints()
        seedAchievements()
        contentVersionRepository.saveLocalVersion(remoteVersion)
    }
}
