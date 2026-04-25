package com.auggie.scratch_map_app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MilestonesProgressBar(
    progress: Float,
    milestones: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "milestonesProgress"
    )

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    val dotRadius = height * 0.85f
    val totalHeight = height + dotRadius * 2

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
    ) {
        val barH = height.toPx()
        val barY = (size.height - barH) / 2f
        val barR = barH / 2f
        val dotR = dotRadius.toPx()

        drawRoundRect(
            color = track,
            topLeft = Offset(0f, barY),
            size = Size(size.width, barH),
            cornerRadius = CornerRadius(barR)
        )

        if (animated > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(primary, secondary),
                    startX = 0f,
                    endX = size.width
                ),
                topLeft = Offset(0f, barY),
                size = Size(size.width * animated, barH),
                cornerRadius = CornerRadius(barR)
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.White.copy(0.28f), Color.White.copy(0f)),
                    startX = 0f,
                    endX = size.width * animated
                ),
                topLeft = Offset(0f, barY),
                size = Size(size.width * animated, barH * 0.4f),
                cornerRadius = CornerRadius(barR)
            )
        }

        for (fraction in milestones) {
            val cx = size.width * fraction.coerceIn(0f, 1f)
            val cy = size.height / 2f
            val reached = animated >= fraction - 0.005f

            drawCircle(
                color = Color.White,
                radius = dotR,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = if (reached) secondary else track.copy(alpha = 0.4f),
                radius = dotR,
                center = Offset(cx, cy),
                style = Stroke(width = dotR * 0.55f)
            )
            if (reached) {
                drawCircle(
                    color = secondary.copy(alpha = 0.35f),
                    radius = dotR * 0.45f,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}
