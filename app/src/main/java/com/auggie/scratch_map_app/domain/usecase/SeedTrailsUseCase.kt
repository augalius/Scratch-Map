package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.domain.repository.TrailContentSource
import com.auggie.scratch_map_app.domain.repository.TrailRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedTrailsUseCase @Inject constructor(
    private val seeder: TrailContentSource,
    private val repository: TrailRepository
) {
    suspend operator fun invoke() {
        val trails = seeder.fetchTrails()
        if (trails.isNotEmpty()) {
            repository.insertTrails(trails)
            repository.deleteNotIn(trails.map { it.id })
        }
    }
}
