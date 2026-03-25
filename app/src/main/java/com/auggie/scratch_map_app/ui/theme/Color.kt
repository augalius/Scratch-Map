package com.auggie.scratch_map_app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * `#RRGGBB` form, for Mapbox style JSON and paint properties — they cannot take a Compose [Color],
 * so these two helpers keep the palette defined once here instead of duplicated as string literals.
 */
fun Color.toHexString(): String = String.format("#%06X", 0xFFFFFF and toArgb())

/** `rgba(r, g, b, a)` form, for Mapbox paint properties that need transparency. */
fun Color.toRgbaString(alpha: Double): String =
    "rgba(${(red * 255).toInt()}, ${(green * 255).toInt()}, ${(blue * 255).toInt()}, $alpha)"

val Primary = Color(0xFF2563EB)
val OnPrimary = Color.White
val Secondary = Color(0xFF10B981)
val OnSecondary = Color.White
val Tertiary = Color(0xFFF59E0B)
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkSurfaceVariant = Color(0xFF334155)
val DarkOnBackground = Color(0xFFF1F5F9)
val DarkOnSurface = Color(0xFFE2E8F0)
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color.White
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightOnBackground = Color(0xFF0F172A)
val LightOnSurface = Color(0xFF1E293B)
val Error = Color(0xFFEF4444)
val OnError = Color.White
val FogColor = Color(0xFF1A1A2E)
