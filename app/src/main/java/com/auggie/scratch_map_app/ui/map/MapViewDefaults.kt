package com.auggie.scratch_map_app.ui.map

import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar

/**
 * Chrome for the scratch map: hide Mapbox's built-in overlays and set up the location puck.
 */
internal fun MapView.applyScratchMapChrome(locationEnabled: Boolean) {
    location.enabled = locationEnabled
    location.pulsingEnabled = true
    scalebar.enabled = false
    attribution.enabled = false
    logo.enabled = false
}

/** Fades the map in once its style has loaded, hiding the flash of unstyled tiles. */
internal fun MapView.fadeInWhenStyled() {
    animate().alpha(1f).setDuration(FADE_IN_MS).start()
}

private const val FADE_IN_MS = 200L
