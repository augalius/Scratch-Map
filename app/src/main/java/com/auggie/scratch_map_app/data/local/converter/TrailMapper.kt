package com.auggie.scratch_map_app.data.local.converter

import com.auggie.scratch_map_app.data.local.entity.TrailCheckpointEntity
import com.auggie.scratch_map_app.data.local.entity.TrailEntity
import com.auggie.scratch_map_app.domain.model.Trail
import com.auggie.scratch_map_app.domain.model.TrailCheckpoint
import com.auggie.scratch_map_app.domain.model.TrailDifficulty

object TrailMapper {

    fun entityToDomain(entity: TrailEntity, checkpoints: List<TrailCheckpointEntity>): Trail =
        Trail(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            difficulty = runCatching { TrailDifficulty.valueOf(entity.difficulty) }.getOrDefault(
                TrailDifficulty.EASY
            ),
            estimatedDistanceKm = entity.estimatedDistanceKm,
            checkpoints = checkpoints.map { checkpointEntityToDomain(it) },
            isCompleted = entity.isCompleted
        )

    private fun checkpointEntityToDomain(entity: TrailCheckpointEntity): TrailCheckpoint =
        TrailCheckpoint(
            id = entity.id,
            trailId = entity.trailId,
            name = entity.name,
            description = entity.description,
            latitude = entity.latitude,
            longitude = entity.longitude,
            order = entity.checkpointOrder,
            unlockRadiusMeters = entity.unlockRadiusMeters,
            isReached = entity.isReached,
            question = entity.question,
            answer = entity.answer,
            isPendingAnswer = entity.isPendingAnswer
        )
}
