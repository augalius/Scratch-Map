package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.repository.RegionContentSource
import com.auggie.scratch_map_app.domain.repository.RegionRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedRegionsUseCase @Inject constructor(
    private val regionRepository: RegionRepository,
    private val seeder: RegionContentSource
) {
    suspend operator fun invoke() {
        val regions = seeder.fetchRegions()
        if (regions.isEmpty()) return

        regionRepository.insertAll(regions)
        regionRepository.deleteNotIn(regions.map { it.id })
        Timber.i("Synced %d regions from Firestore", regions.size)
    }
}
