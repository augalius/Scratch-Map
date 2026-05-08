package com.auggie.scratch_map_app.data.repository

import com.auggie.scratch_map_app.data.local.converter.TrailMapper
import com.auggie.scratch_map_app.data.local.dao.TrailDao
import com.auggie.scratch_map_app.data.local.entity.TrailCheckpointEntity
import com.auggie.scratch_map_app.data.local.entity.TrailEntity
import com.auggie.scratch_map_app.domain.model.Trail
import com.auggie.scratch_map_app.domain.repository.TrailRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrailRepositoryImpl @Inject constructor(
    private val dao: TrailDao
) : TrailRepository {

    override fun observeAll(): Flow<List<Trail>> =
        dao.observeAll().flatMapLatest { trails ->
            if (trails.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val checkpointFlows = trails.map { trail -> dao.observeCheckpoints(trail.id) }
            combine(checkpointFlows) { allCheckpoints ->
                trails.mapIndexed { i, trail ->
                    TrailMapper.entityToDomain(trail, allCheckpoints[i])
                }
            }
        }

    override fun observeTrail(trailId: String): Flow<Trail?> =
        dao.observeCheckpoints(trailId).map { checkpoints ->
            val entity = dao.getById(trailId) ?: return@map null
            TrailMapper.entityToDomain(entity, checkpoints)
        }



    override suspend fun insertTrails(trails: List<Trail>) {
        trails.forEach { trail ->
            dao.insertTrail(
                TrailEntity(
                    id = trail.id,
                    name = trail.name,
                    description = trail.description,
                    difficulty = trail.difficulty.name,
                    estimatedDistanceKm = trail.estimatedDistanceKm
                )
            )
            val entities = trail.checkpoints.map { cp ->
                TrailCheckpointEntity(
                    id = cp.id,
                    trailId = cp.trailId,
                    name = cp.name,
                    description = cp.description,
                    latitude = cp.latitude,
                    longitude = cp.longitude,
                    checkpointOrder = cp.order,
                    unlockRadiusMeters = cp.unlockRadiusMeters
                )
            }
            dao.updateTrailContent(
                trail.id,
                trail.name,
                trail.description,
                trail.difficulty.name,
                trail.estimatedDistanceKm.toFloat()
            )
            dao.insertCheckpoints(entities)
            entities.forEach {
                dao.updateCheckpointContent(
                    it.id,
                    it.name,
                    it.description,
                    it.latitude,
                    it.longitude,
                    it.checkpointOrder,
                    it.unlockRadiusMeters
                )
            }
        }
    }

    override suspend fun markCheckpointReached(checkpointId: String) {
        dao.markCheckpointReached(checkpointId)
    }


    override suspend fun markAllCheckpointsReached(trailId: String) {
        dao.markAllCheckpointsReached(trailId)
    }

    override suspend fun markTrailCompleted(trailId: String) {
        dao.markTrailCompleted(trailId)
    }

    override suspend fun deleteNotIn(ids: List<String>) = dao.deleteNotIn(ids)
}
