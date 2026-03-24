package com.auggie.scratch_map_app.util

import com.auggie.scratch_map_app.domain.model.ScratchTile
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.sinh

/**
 * Turns a set of revealed tiles into smooth lat/lng outlines.
 *
 * Pure geometry with no rendering dependency: the caller converts the resulting rings into
 * whatever geometry type its map library wants.
 *
 * The pipeline is dilate → trace boundary edges → chain them into rings → smooth.
 */
object TileContourTracer {

    /**
     * @param dilationRadius tiles to grow the revealed set by, closing single-tile gaps.
     * @param smoothIterations Chaikin passes; each doubles the point count and rounds corners.
     * @return one closed ring per outline, as (latitude, longitude) pairs.
     */
    fun traceSmoothedOutlines(
        tiles: List<ScratchTile>,
        zoom: Int,
        dilationRadius: Int,
        smoothIterations: Int
    ): List<List<Pair<Double, Double>>> {
        if (tiles.isEmpty()) return emptyList()

        return traceContours(dilateTiles(tiles, dilationRadius)).mapNotNull { contour ->
            var ring = contour.map { (gx, gy) -> gridCornerToLatLng(gx, gy, zoom) }
            repeat(smoothIterations) { ring = chaikinSmooth(ring) }
            ring.takeIf { it.size >= 3 }
        }
    }

    private fun dilateTiles(tiles: List<ScratchTile>, radius: Int): Set<Long> {
        val radiusSq = radius * radius
        val result = HashSet<Long>(tiles.size * (2 * radius + 1))
        for (tile in tiles) {
            for (dx in -radius..radius) {
                for (dy in -radius..radius) {
                    if (dx * dx + dy * dy <= radiusSq) {
                        result.add(TileCalculator.tileKey(tile.tileX + dx, tile.tileY + dy))
                    }
                }
            }
        }
        return result
    }

    /** Marching-squares style boundary walk: collect outward edges, then chain them into rings. */
    private fun traceContours(cells: Set<Long>): List<List<Pair<Int, Int>>> {
        data class Edge(val x1: Int, val y1: Int, val x2: Int, val y2: Int)

        val edges = mutableListOf<Edge>()
        for (key in cells) {
            val tx = TileCalculator.tileKeyX(key)
            val ty = TileCalculator.tileKeyY(key)
            if (!cells.contains(TileCalculator.tileKey(tx, ty - 1))) edges.add(Edge(tx, ty, tx + 1, ty))
            if (!cells.contains(TileCalculator.tileKey(tx + 1, ty))) edges.add(Edge(tx + 1, ty, tx + 1, ty + 1))
            if (!cells.contains(TileCalculator.tileKey(tx, ty + 1))) edges.add(Edge(tx + 1, ty + 1, tx, ty + 1))
            if (!cells.contains(TileCalculator.tileKey(tx - 1, ty))) edges.add(Edge(tx, ty + 1, tx, ty))
        }

        val edgesByStart = HashMap<Long, MutableList<Edge>>(edges.size)
        for (edge in edges) {
            edgesByStart.getOrPut(TileCalculator.tileKey(edge.x1, edge.y1)) { mutableListOf() }
                .add(edge)
        }

        val used = HashSet<Edge>(edges.size)
        val contours = mutableListOf<List<Pair<Int, Int>>>()
        for (startEdge in edges) {
            if (startEdge in used) continue
            val corners = mutableListOf<Pair<Int, Int>>()
            var current = startEdge
            while (current !in used) {
                used.add(current)
                corners.add(current.x1 to current.y1)
                val next = edgesByStart[TileCalculator.tileKey(current.x2, current.y2)]
                current = next?.firstOrNull { it !in used } ?: break
            }
            if (corners.size >= 3) contours.add(corners)
        }
        return contours
    }

    /** Top-left corner of a tile in the Slippy Map grid, as (latitude, longitude). */
    private fun gridCornerToLatLng(x: Int, y: Int, zoom: Int): Pair<Double, Double> {
        val n = 1 shl zoom
        val lng = x.toDouble() / n * 360.0 - 180.0
        val lat = Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * y / n))))
        return lat to lng
    }

    /** One Chaikin corner-cutting pass over a closed ring. */
    private fun chaikinSmooth(points: List<Pair<Double, Double>>): List<Pair<Double, Double>> {
        if (points.size < 3) return points
        val n = points.size
        val result = ArrayList<Pair<Double, Double>>(n * 2)
        for (i in 0 until n) {
            val (lat0, lng0) = points[i]
            val (lat1, lng1) = points[(i + 1) % n]
            result.add(lat0 * 0.75 + lat1 * 0.25 to lng0 * 0.75 + lng1 * 0.25)
            result.add(lat0 * 0.25 + lat1 * 0.75 to lng0 * 0.25 + lng1 * 0.75)
        }
        return result
    }
}
