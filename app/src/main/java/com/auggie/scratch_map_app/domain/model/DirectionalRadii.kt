package com.auggie.scratch_map_app.domain.model

/**
 * Scratch radii for [NUM_SECTORS] compass sectors, starting at north and running clockwise.
 *
 * Each sector's radius is derived from the distance to the nearest public road in that direction,
 * so that walking every road in an area reveals all of it.
 */
data class DirectionalRadii(val sectors: IntArray) {

    init {
        require(sectors.size == NUM_SECTORS) {
            "DirectionalRadii needs exactly $NUM_SECTORS sectors, got ${sectors.size}"
        }
    }

    /** Radius at an arbitrary bearing, linearly interpolated between the two adjacent sectors. */
    fun radiusAtBearing(bearing: Double): Double {
        val sectorSize = 360.0 / sectors.size
        val shifted = ((bearing + sectorSize / 2) % 360)
        val exactSector = shifted / sectorSize
        val lower = exactSector.toInt() % sectors.size
        val upper = (lower + 1) % sectors.size
        val t = exactSector - exactSector.toInt()
        return sectors[lower] * (1 - t) + sectors[upper] * t
    }

    fun maxRadius(): Int = sectors.max()

    override fun toString(): String =
        sectors.mapIndexed { i, r -> "${SECTOR_LABELS[i]}=${r}m" }.joinToString(", ")

    // IntArray uses identity equality, so both accessors need overriding.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DirectionalRadii) return false
        return sectors.contentEquals(other.sectors)
    }

    override fun hashCode() = sectors.contentHashCode()

    companion object {
        const val NUM_SECTORS = 16

        private val SECTOR_LABELS = arrayOf(
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
        )

        fun uniform(radius: Int) = DirectionalRadii(IntArray(NUM_SECTORS) { radius })
    }
}
