package com.auggie.scratch_map_app.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import com.auggie.scratch_map_app.domain.model.WaypointCategory
import com.mapbox.maps.Style

object WaypointMarkerGenerator {

    private const val LOCKED_SIZE = 64
    private const val UNLOCKED_SIZE = 56

    fun addMarkerImages(style: Style) {
        for (category in WaypointCategory.entries) {
            val color = categoryColorInt(category)
            val unlocked = createMarkerBitmap(color, UNLOCKED_SIZE, category, locked = false)
            style.addImage("waypoint-${category.name}", unlocked)

            val locked = createMarkerBitmap(
                Color.parseColor("#3a3a52"), LOCKED_SIZE, category, locked = true
            )
            style.addImage("waypoint-locked-${category.name}", locked)
        }
    }

    private fun createMarkerBitmap(
        color: Int, size: Int, category: WaypointCategory, locked: Boolean
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, (size * 1.3f).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = size.toFloat()
        val h = bitmap.height.toFloat()

        val fullPath = Path().apply {
            moveTo(w * 0.1f, 0f + w * 0.08f)
            lineTo(w * 0.1f, h * 0.5f)
            lineTo(w * 0.5f, h)
            lineTo(w * 0.9f, h * 0.5f)
            lineTo(w * 0.9f, 0f + w * 0.08f)
            quadTo(w * 0.9f, 0f, w * 0.9f - w * 0.08f, 0f)
            lineTo(w * 0.1f + w * 0.08f, 0f)
            quadTo(w * 0.1f, 0f, w * 0.1f, 0f + w * 0.08f)
            close()
        }

        // Shadow
        canvas.save()
        canvas.translate(2f, 3f)
        canvas.drawPath(fullPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.argb(50, 0, 0, 0)
            style = Paint.Style.FILL
        })
        canvas.restore()

        // Fill
        canvas.drawPath(fullPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        })

        // Border
        canvas.drawPath(fullPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = if (locked) Color.parseColor("#5a5a70") else Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = size * 0.04f
        })

        // Draw the Material icon inside the pin head
        val iconColor = if (locked) Color.parseColor("#6a6a80") else Color.WHITE
        val iconSize = (w * 0.52f).toInt()
        val iconLeft = ((w - iconSize) / 2).toInt()
        val iconTop = ((h * 0.48f - iconSize) / 2).toInt()
        val iconBitmap = category.icon.toBitmap(iconSize, iconColor)
        canvas.drawBitmap(iconBitmap, iconLeft.toFloat(), iconTop.toFloat(), null)

        return bitmap
    }

    private fun ImageVector.toBitmap(size: Int, tintColor: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tintColor
            style = Paint.Style.FILL
        }

        canvas.scale(size / viewportWidth, size / viewportHeight)

        fun drawNode(node: VectorNode) {
            when (node) {
                is VectorGroup -> for (child in node) drawNode(child)
                is VectorPath -> {
                    val path = PathParser().addPathNodes(node.pathData).toPath().asAndroidPath()
                    canvas.drawPath(path, paint)
                }
            }
        }
        for (child in root) drawNode(child)
        return bitmap
    }

    private fun categoryColorInt(category: WaypointCategory): Int =
        Color.parseColor(category.colorHex)
}
