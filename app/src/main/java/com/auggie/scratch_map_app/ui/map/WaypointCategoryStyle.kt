package com.auggie.scratch_map_app.ui.map

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Fort
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Room
import androidx.compose.ui.graphics.vector.ImageVector
import com.auggie.scratch_map_app.domain.model.WaypointCategory

/**
 * Single source of truth for how a [WaypointCategory] is rendered (icon + color).
 * Used by both the Compose map UI and the bitmap marker generator so the two can never drift.
 */

val WaypointCategory.icon: ImageVector
    get() = when (this) {
        WaypointCategory.CHURCH -> Icons.Default.Church
        WaypointCategory.MONUMENT -> Icons.Default.AccountBalance
        WaypointCategory.PARK -> Icons.Default.Park
        WaypointCategory.VIEWPOINT -> Icons.Default.Landscape
        WaypointCategory.MUSEUM -> Icons.Default.Museum
        WaypointCategory.HISTORIC -> Icons.Default.HistoryEdu
        WaypointCategory.LANDMARK -> Icons.Default.Place
        WaypointCategory.FORT -> Icons.Default.Fort
        WaypointCategory.OTHER -> Icons.Default.Room
    }

/** Marker color as a `#RRGGBB` hex string (parse with `android.graphics.Color.parseColor`). */
val WaypointCategory.colorHex: String
    get() = when (this) {
        WaypointCategory.CHURCH -> "#9C27B0"
        WaypointCategory.MONUMENT -> "#FF5722"
        WaypointCategory.PARK -> "#4CAF50"
        WaypointCategory.VIEWPOINT -> "#2196F3"
        WaypointCategory.MUSEUM -> "#FF9800"
        WaypointCategory.HISTORIC -> "#795548"
        WaypointCategory.LANDMARK -> "#E91E63"
        WaypointCategory.FORT -> "#546E7A"
        WaypointCategory.OTHER -> "#607D8B"
    }
