package com.auggie.scratch_map_app.util

import android.content.Context
import com.auggie.scratch_map_app.domain.model.UserLocation
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DEBUG ONLY: appends every location fix to `files/location_log.csv` so tracking sessions can
 * be replayed while tuning reveal radii. Remove before release.
 */
object LocationLogger {

    private const val FILE_NAME = "location_log.csv"
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun log(context: Context, location: UserLocation) {
        runCatching {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) file.writeText("time,lat,lng,accuracy,speed\n")
            file.appendText(
                "${timeFormat.format(Date())},${location.latitude},${location.longitude}," +
                    "${location.accuracy},${location.speed ?: ""}\n"
            )
        }
    }
}
