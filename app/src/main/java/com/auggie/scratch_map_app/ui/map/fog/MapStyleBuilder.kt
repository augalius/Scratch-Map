package com.auggie.scratch_map_app.ui.map.fog

import com.auggie.scratch_map_app.ui.theme.FogColor
import com.auggie.scratch_map_app.ui.theme.toHexString

/**
 * Builds the Mapbox style document for the scratch map.
 *
 * Split out of [FogLayerManager] so that file stays about GeoJSON: this is a ~200 line document
 * with a handful of interpolated values, and mixing it into logic made both harder to read.
 */
internal object MapStyleBuilder {

    /** Colour set for one theme. Grouping these keeps the light/dark pairs side by side. */
    private data class Palette(
        val background: String,
        val water: String,
        val park: String,
        val building: String,
        val roadMajor: String,
        val roadMajorCase: String,
        val roadMinor: String,
        val roadMinorCase: String,
        val label: String,
        val labelHalo: String
    )

    private val dark = Palette(
        background = FogColor.toHexString(),
        water = "#1a3a5c",
        park = "#1a3a2a",
        building = "#2a2a3e",
        roadMajor = "#c8a84a",
        roadMajorCase = "#8a6a2a",
        roadMinor = "#3a3a50",
        roadMinorCase = "#2a2a3a",
        label = "#c0c0d0",
        labelHalo = FogColor.toHexString()
    )

    private val light = Palette(
        background = "#f0ece2",
        water = "#4a90d9",
        park = "#b8d9a0",
        building = "#d4c8b8",
        roadMajor = "#e8a840",
        roadMajorCase = "#c08020",
        roadMinor = "#d8d0c0",
        roadMinorCase = "#b8b0a0",
        label = "#444444",
        labelHalo = "#ffffff"
    )

    fun build(
        fogJson: String,
        waypointJson: String,
        fogSourceId: String,
        fogLayerId: String,
        fogColor: String,
        isDark: Boolean
    ): String {
        val p = if (isDark) dark else light

        return """
        {
          "version": 8,
          "glyphs": "mapbox://fonts/mapbox/{fontstack}/{range}.pbf",
          "sources": {
            "mapbox-streets": {
              "type": "vector",
              "url": "mapbox://mapbox.mapbox-streets-v8"
            },
            "$fogSourceId": {
              "type": "geojson",
              "data": $fogJson
            },
            "waypoint-source": {
              "type": "geojson",
              "data": $waypointJson
            }
          },
          "layers": [
            {
              "id": "background",
              "type": "background",
              "paint": { "background-color": "${p.background}" }
            },
            {
              "id": "landuse-park",
              "type": "fill",
              "source": "mapbox-streets",
              "source-layer": "landuse",
              "filter": ["in", "class", "park", "cemetery", "glacier", "pitch", "sand"],
              "paint": { "fill-color": "${p.park}", "fill-opacity": 0.8 }
            },
            {
              "id": "water",
              "type": "fill",
              "source": "mapbox-streets",
              "source-layer": "water",
              "paint": { "fill-color": "${p.water}" }
            },
            {
              "id": "building",
              "type": "fill",
              "source": "mapbox-streets",
              "source-layer": "building",
              "paint": { "fill-color": "${p.building}", "fill-opacity": 0.7 }
            },
            {
              "id": "road-minor-case",
              "type": "line",
              "source": "mapbox-streets",
              "source-layer": "road",
              "filter": ["in", "class", "street", "street_limited", "service", "track", "pedestrian", "path"],
              "paint": { "line-color": "${p.roadMinorCase}", "line-width": 4 }
            },
            {
              "id": "road-minor",
              "type": "line",
              "source": "mapbox-streets",
              "source-layer": "road",
              "filter": ["in", "class", "street", "street_limited", "service", "track", "pedestrian", "path"],
              "paint": { "line-color": "${p.roadMinor}", "line-width": 2 }
            },
            {
              "id": "road-major-case",
              "type": "line",
              "source": "mapbox-streets",
              "source-layer": "road",
              "filter": ["in", "class", "primary", "secondary", "tertiary", "motorway", "trunk", "motorway_link", "trunk_link"],
              "paint": { "line-color": "${p.roadMajorCase}", "line-width": 6 }
            },
            {
              "id": "road-major",
              "type": "line",
              "source": "mapbox-streets",
              "source-layer": "road",
              "filter": ["in", "class", "primary", "secondary", "tertiary", "motorway", "trunk", "motorway_link", "trunk_link"],
              "paint": { "line-color": "${p.roadMajor}", "line-width": 3, "line-opacity": 0.9 }
            },
            {
              "id": "road-label",
              "type": "symbol",
              "source": "mapbox-streets",
              "source-layer": "road",
              "filter": ["in", "class", "primary", "secondary", "tertiary", "motorway", "trunk", "street"],
              "layout": {
                "text-field": ["get", "name"],
                "text-font": ["DIN Pro Regular", "Arial Unicode MS Regular"],
                "text-size": 12,
                "symbol-placement": "line"
              },
              "paint": {
                "text-color": "${p.label}",
                "text-halo-color": "${p.labelHalo}",
                "text-halo-width": 1.5
              }
            },
            {
              "id": "place-label",
              "type": "symbol",
              "source": "mapbox-streets",
              "source-layer": "place_label",
              "layout": {
                "text-field": ["get", "name"],
                "text-font": ["DIN Pro Medium", "Arial Unicode MS Regular"],
                "text-size": ["interpolate", ["linear"], ["zoom"], 5, 10, 12, 14, 16, 18]
              },
              "paint": {
                "text-color": "${p.label}",
                "text-halo-color": "${p.labelHalo}",
                "text-halo-width": 1.5
              }
            },
            {
              "id": "$fogLayerId",
              "type": "fill",
              "source": "$fogSourceId",
              "paint": { "fill-color": "$fogColor", "fill-antialias": true }
            },
            {
              "id": "waypoint-unlocked",
              "type": "symbol",
              "source": "waypoint-source",
              "filter": ["==", ["get", "unlocked"], true],
              "minzoom": 11,
              "layout": {
                "icon-image": ["concat", "waypoint-", ["get", "category"]],
                "icon-size": 0.8,
                "icon-anchor": "bottom",
                "icon-allow-overlap": true,
                "icon-ignore-placement": true,
                "symbol-avoid-edges": false,
                "text-field": ["get", "name"],
                "text-font": ["DIN Pro Medium", "Arial Unicode MS Regular"],
                "text-size": 11,
                "text-offset": [0, 0.3],
                "text-anchor": "top",
                "text-allow-overlap": false,
                "text-ignore-placement": false
              },
              "paint": {
                "text-color": ["get", "color"],
                "text-halo-color": "${p.labelHalo}",
                "text-halo-width": 1,
                "text-opacity": ["interpolate", ["linear"], ["zoom"], 15.5, 0, 16, 1]
              }
            },
            {
              "id": "waypoint-locked",
              "type": "symbol",
              "source": "waypoint-source",
              "filter": ["==", ["get", "unlocked"], false],
              "minzoom": 11,
              "layout": {
                "icon-image": ["concat", "waypoint-locked-", ["get", "category"]],
                "icon-size": 1.0,
                "icon-anchor": "bottom",
                "icon-allow-overlap": true,
                "icon-ignore-placement": true,
                "symbol-avoid-edges": false
              }
            }
          ]
        }
        """.trimIndent()
    }
}
