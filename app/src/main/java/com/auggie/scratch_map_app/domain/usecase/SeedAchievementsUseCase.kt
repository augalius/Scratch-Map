package com.auggie.scratch_map_app.domain.usecase

import com.auggie.scratch_map_app.data.seed.AchievementSeeder
import com.auggie.scratch_map_app.domain.repository.AchievementRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedAchievementsUseCase @Inject constructor(
    private val achievementRepository: AchievementRepository,
    private val seeder: AchievementSeeder
) {
    suspend operator fun invoke() {
        val definitions = seeder.fetchAchievements()
        if (definitions.isEmpty()) return

        achievementRepository.insertDefinitions(definitions)
        achievementRepository.deleteDefinitionsNotIn(definitions.map { it.id })
        Timber.i("Seeded %d achievement definitions", definitions.size)
    }
}
