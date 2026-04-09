package com.auggie.scratch_map_app.data.seed

import com.auggie.scratch_map_app.domain.model.Region
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local starter region, used until region boundaries are prepared with
 * `tools/prepare_region.py` and served from a backend.
 */
@Singleton
class RegionSeeder @Inject constructor() {

    fun fetchRegions(): List<Region> = listOf(
        Region(
            id = "kaunas",
            name = "Kaunas",
            description = "The old capital at the confluence of the Nemunas and the Neris",
            minLat = 54.8350,
            maxLat = 54.9700,
            minLng = 23.7650,
            maxLng = 24.0550,
            goalTiles = 320_000
        )
    )
}
