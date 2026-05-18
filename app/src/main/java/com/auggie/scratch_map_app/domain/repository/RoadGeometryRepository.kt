package com.auggie.scratch_map_app.domain.repository

import com.auggie.scratch_map_app.domain.model.DirectionalRadii

/**
 * Supplies the directional scratch radii for a position, derived from nearby road geometry.
 *
 * Implementations are expected to cache aggressively and to degrade to a sensible default
 * rather than throwing — callers run this on every location fix.
 */
interface RoadGeometryRepository {
    suspend fun getDirectionalRadii(lat: Double, lng: Double): DirectionalRadii
}
