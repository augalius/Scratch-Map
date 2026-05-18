package com.auggie.scratch_map_app.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.auggie.scratch_map_app.domain.model.DirectionalRadii
import com.auggie.scratch_map_app.util.TileCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Queries OpenStreetMap Overpass API for road/path geometry and computes
 * directional scratch radii using a "midpoint to next road" algorithm.
 *
 * For each compass direction, the scratch extends ~65% of the distance to
 * the nearest public road/path in that direction. This guarantees that
 * walking ALL roads in an area reveals 100% of the map — adjacent roads'
 * scratch zones overlap in the middle.
 *
 * Private roads (access=private/no) are excluded from the analysis, so
 * private areas get covered by the scratch from surrounding public roads.
 *
 * Cache hierarchy:
 *   1. In-memory ConcurrentHashMap (TTL = 30 min)
 *   2. SharedPreferences on disk (TTL = 7 days) — survives restarts and offline
 *   3. Overpass API (requires network)
 *   4. uniform(DEFAULT_RADIUS) as last resort
 */
@Singleton
class OverpassRoadDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val memCache = ConcurrentHashMap<String, CachedResult>()
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("road_density_cache_v3", Context.MODE_PRIVATE) // Private roads are excluded
    }

    companion object {
        private const val QUERY_RADIUS_METERS = 1000
        private const val GRID_SIZE = 0.001
        private const val MEM_CACHE_TTL_MS  = 30 * 60 * 1000L // 30 minutes
        private const val DISK_CACHE_TTL_MS = 12 * 60 * 60 * 1000L // 12 hours
        // Short TTL for "no road data" results: long enough that a coverage gap or an Overpass
        // outage cannot cause a network round-trip on every location fix, short enough to recover.
        private const val FALLBACK_CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
        // The grid key space is unbounded, so cap the on-disk store.
        private const val MAX_DISK_ENTRIES = 2_000
        private const val OVERPASS_URL = "https://overpass-api.de/api/interpreter"
        private const val TIMEOUT_MS = 5_000
        private const val NUM_SECTORS = DirectionalRadii.NUM_SECTORS
        private const val SECTOR_SIZE_DEG = 360.0 / NUM_SECTORS

        // Points closer than this are on the user's own road — skip them.
        private const val SELF_ROAD_THRESHOLD_M = 8.0
        // Scratch extends this fraction of the distance to the next road.
        private const val COVERAGE_FACTOR = 0.65
        private const val MIN_RADIUS = 20
        private const val DEFAULT_RADIUS = 60 // Fallback when no road data
        private const val MAX_RADIUS = 500
        private val PRIVATE_ACCESS = setOf("private", "no")
    }

    /**
     * @param isFallback true when [radii] is the DEFAULT_RADIUS guess used because no road data
     *   could be obtained. Cached briefly so failures are not retried on every location fix.
     */
    private data class CachedResult(
        val radii: DirectionalRadii,
        val timestamp: Long,
        val isFallback: Boolean = false
    )

    private fun CachedResult.isFresh(now: Long): Boolean =
        now - timestamp < if (isFallback) FALLBACK_CACHE_TTL_MS else MEM_CACHE_TTL_MS

    /**
     * Returns directional scratch radii
     * Uses cache hierarchy: memory -> disk -> Overpass -> default.
     */
    suspend fun getDirectionalRadii(lat: Double, lng: Double): DirectionalRadii {
        val key = gridKey(lat, lng)
        val now = System.currentTimeMillis()

        // In-memory cache (fallback entries expire sooner — see CachedResult.isFresh)
        memCache[key]?.let { cached ->
            if (cached.isFresh(now)) return cached.radii
        }

        // Disk cache — only ever holds real road-derived results
        loadFromDisk(key)?.let { cached ->
            if (now - cached.timestamp < DISK_CACHE_TTL_MS) {
                memCache[key] = cached
                return cached.radii
            }
        }

        // Overpass API
        return try {
            val roadPoints = queryPublicRoadGeometry(lat, lng)

            if (roadPoints.isEmpty()) {
                Timber.w("Overpass returned 0 road points at (%.4f, %.4f) — fallback cached", lat, lng)
                return cacheFallback(key, now)
            }

            // A genuinely open area (points exist but none survive filtering) legitimately
            // resolves to uniform MAX_RADIUS, and is cached like any other real result.
            val radii = computeRadiiFromRoadPoints(lat, lng, roadPoints)
            val result = CachedResult(radii, now)
            memCache[key] = result
            saveToDisk(key, result)

            // Prefill surrounding grid cells with the same road points so the
            // user doesn't hit a slow Overpass call when moving into an adjacent cell.
            prefillNearbyCells(lat, lng, roadPoints, now)

            radii
        } catch (e: Exception) {
            // Offline or Overpass down: stale real data beats a guess.
            loadFromDisk(key)?.let { stale ->
                memCache[key] = stale
                return stale.radii
            }
            Timber.w(e, "No road data available at (%.4f, %.4f) — fallback cached", lat, lng)
            cacheFallback(key, now)
        }
    }

    /** Caches and returns the default radius, so a failure is not retried on every fix. */
    private fun cacheFallback(key: String, now: Long): DirectionalRadii {
        val radii = DirectionalRadii.uniform(DEFAULT_RADIUS)
        memCache[key] = CachedResult(radii, now, isFallback = true)
        return radii
    }

    // ── Prefill ───────────────────────────────────────────────────────

    /**
     * Uses already fetched road points to compute
     * and cache directional radii for the surrounding grid cells.
     */
    private fun prefillNearbyCells(
        centerLat: Double, centerLng: Double,
        roadPoints: List<Pair<Double, Double>>,
        timestamp: Long
    ) {
        val steps = 2
        for (dLat in -steps..steps) {
            for (dLng in -steps..steps) {
                if (dLat == 0 && dLng == 0) continue
                val nearLat = centerLat + dLat * GRID_SIZE
                val nearLng = centerLng + dLng * GRID_SIZE
                val nearKey = gridKey(nearLat, nearLng)
                // Don't overwrite a cell that already holds fresh, real road data.
                val existing = memCache[nearKey]
                if (existing != null && !existing.isFallback && existing.isFresh(timestamp)) continue
                memCache[nearKey] = CachedResult(
                    computeRadiiFromRoadPoints(nearLat, nearLng, roadPoints), timestamp
                )
            }
        }
    }

    private fun saveToDisk(key: String, result: CachedResult) {
        val value = result.radii.sectors.joinToString(",") + "|" + result.timestamp
        prefs.edit().putString(key, value).apply()
        pruneDiskCacheIfLarge()
    }

    /** Grid keys are unbounded, so evict expired entries once the store grows past the cap. */
    private fun pruneDiskCacheIfLarge() {
        val all = prefs.all
        if (all.size <= MAX_DISK_ENTRIES) return
        val now = System.currentTimeMillis()
        val editor = prefs.edit()
        all.forEach { (k, v) ->
            val timestamp = (v as? String)?.substringAfterLast('|')?.toLongOrNull()
            if (timestamp == null || now - timestamp >= DISK_CACHE_TTL_MS) editor.remove(k)
        }
        editor.apply()
    }

    private fun loadFromDisk(key: String): CachedResult? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching {
            val (sectorsStr, tsStr) = raw.split("|")
            val sectors = sectorsStr.split(",").map { it.toInt() }.toIntArray()
            if (sectors.size != NUM_SECTORS) return null
            CachedResult(DirectionalRadii(sectors), tsStr.toLong())
        }.getOrNull()
    }

    private suspend fun queryPublicRoadGeometry(
        lat: Double, lng: Double
    ): List<Pair<Double, Double>> = withContext(Dispatchers.IO) {
        // Include all highway types — primary roads down, forest tracks, footpaths...
        val query = """
            [out:json][timeout:10];
            way["highway"~"^(motorway|trunk|primary|secondary|tertiary|unclassified|residential|service|track|footway|path|cycleway|pedestrian|steps|living_street)${'$'}"](around:$QUERY_RADIUS_METERS,$lat,$lng);
            out geom tags;
        """.trimIndent()

        val url = URL(OVERPASS_URL)
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.outputStream.bufferedWriter().use { it.write("data=${java.net.URLEncoder.encode(query, "UTF-8")}") }

            if (connection.responseCode != 200)
                throw RuntimeException("Overpass returned ${connection.responseCode}")

            parsePublicRoadPoints(connection.inputStream.bufferedReader().readText())
        } finally {
            connection.disconnect()
        }
    }

    private fun parsePublicRoadPoints(json: String): List<Pair<Double, Double>> {
        val result = mutableListOf<Pair<Double, Double>>()
        try {
            val elements = JSONObject(json).getJSONArray("elements")
            for (i in 0 until elements.length()) {
                val elem = elements.getJSONObject(i)
                if (elem.has("tags")) {
                    val access = elem.getJSONObject("tags").optString("access", "")
                    if (access in PRIVATE_ACCESS) continue
                }
                if (elem.has("geometry")) {
                    val geom = elem.getJSONArray("geometry")
                    for (j in 0 until geom.length()) {
                        val node = geom.getJSONObject(j)
                        result.add(node.getDouble("lat") to node.getDouble("lon"))
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse Overpass road geometry")
        }
        return result
    }

    private fun computeRadiiFromRoadPoints(
        userLat: Double, userLng: Double,
        roadPoints: List<Pair<Double, Double>>
    ): DirectionalRadii {
        val nearestDist = DoubleArray(NUM_SECTORS) { Double.MAX_VALUE }

        for ((ptLat, ptLng) in roadPoints) {
            val dist = TileCalculator.haversineDistance(userLat, userLng, ptLat, ptLng)
            if (dist < SELF_ROAD_THRESHOLD_M) continue
            if (dist > QUERY_RADIUS_METERS) continue
            val sector = bearingToSector(TileCalculator.bearing(userLat, userLng, ptLat, ptLng))
            if (dist < nearestDist[sector]) nearestDist[sector] = dist
        }

        // -1 = no road point found in this sector
        val rawRadii = IntArray(NUM_SECTORS) { sector ->
            if (nearestDist[sector] == Double.MAX_VALUE) -1
            else (nearestDist[sector] * COVERAGE_FACTOR).roundToInt().coerceIn(MIN_RADIUS, MAX_RADIUS)
        }

        // If no data at all (open field), return uniform MAX_RADIUS
        if (rawRadii.none { it >= 0 }) return DirectionalRadii.uniform(MAX_RADIUS)

        // Interpolate empty sectors from their nearest neighbors that have data so that
        // it won't scratch MAX_RADIUS into the missing side
        val filled = IntArray(NUM_SECTORS) { i ->
            if (rawRadii[i] >= 0) return@IntArray rawRadii[i]
            var leftDist = 0; var leftVal = -1
            var rightDist = 0; var rightVal = -1
            for (d in 1 until NUM_SECTORS) {
                if (leftVal < 0) {
                    val idx = (i - d + NUM_SECTORS) % NUM_SECTORS
                    if (rawRadii[idx] >= 0) { leftDist = d; leftVal = rawRadii[idx] }
                }
                if (rightVal < 0) {
                    val idx = (i + d) % NUM_SECTORS
                    if (rawRadii[idx] >= 0) { rightDist = d; rightVal = rawRadii[idx] }
                }
                if (leftVal >= 0 && rightVal >= 0) break
            }
            when {
                leftVal >= 0 && rightVal >= 0 ->
                    (leftVal * rightDist + rightVal * leftDist) / (leftDist + rightDist)
                leftVal >= 0 -> leftVal
                else -> rightVal
            }
        }
        return DirectionalRadii(filled)
    }

    /**
     * Returns the bearing from (lat1, lng1) to (lat2, lng2)
     */
    private fun bearingToSector(bearing: Double): Int {
        val shifted = (bearing + SECTOR_SIZE_DEG / 2) % 360
        return (shifted / SECTOR_SIZE_DEG).toInt().coerceIn(0, NUM_SECTORS - 1)
    }

    private fun gridKey(lat: Double, lng: Double): String {
        val qLat = (lat / GRID_SIZE).roundToInt()
        val qLng = (lng / GRID_SIZE).roundToInt()
        return "$qLat:$qLng"
    }
}
