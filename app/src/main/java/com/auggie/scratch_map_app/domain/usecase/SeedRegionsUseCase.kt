package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.data.seed.RegionSeeder
import com.auggie.scratch_map_app.domain.repository.RegionRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedRegionsUseCase @Inject constructor(
    private val regionRepository: RegionRepository,
    private val seeder: RegionSeeder
) {
    suspend operator fun invoke() {
        val regions = seeder.fetchRegions()
        if (regions.isEmpty()) return

        regionRepository.insertAll(regions)
        regionRepository.deleteNotIn(regions.map { it.id })
        Timber.i("Seeded %d regions", regions.size)
    }
}
