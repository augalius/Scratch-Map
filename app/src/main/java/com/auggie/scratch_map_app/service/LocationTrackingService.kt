package com.auggie.scratch_map_app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.auggie.scratch_map_app.domain.model.UserLocation
import com.auggie.scratch_map_app.domain.usecase.RevealTilesUseCase
import com.auggie.scratch_map_app.domain.location.LocationClient
import com.auggie.scratch_map_app.util.TileCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service that continuously tracks the user's GPS position and feeds
 * each location fix through [RevealTilesUseCase] to scratch the map.
 */
@AndroidEntryPoint
class LocationTrackingService : LifecycleService() {
    @Inject
    lateinit var locationClient: LocationClient

    @Inject
    lateinit var revealTilesUseCase: RevealTilesUseCase

    @Inject
    lateinit var statusTracker: ServiceStatusTracker

    private var locationJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var totalNewTiles = 0
    private var lastLocation: UserLocation? = null

    companion object {
        const val CHANNEL_ID = "scratchmap_tracking"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.scratchmap.action.START_TRACKING"
        const val ACTION_STOP = "com.scratchmap.action.STOP_TRACKING"
        private const val WAKELOCK_TAG = "ScratchMap::LocationTracking"
        private const val INTERVAL_MS = 5_000L

        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent =
                Intent(context, LocationTrackingService::class.java).apply { action = ACTION_START }
            context.startForegroundService(
                intent
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(
                    context,
                    LocationTrackingService::class.java
                ).apply { action = ACTION_STOP })
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (isRunning) {
            return
        }

        isRunning = true
        totalNewTiles = 0
        lastLocation = null
        statusTracker.onTrackingStarted()
        startForeground(NOTIFICATION_ID, createNotification("Starting..."))
        acquireWakeLock()

        locationJob = lifecycleScope.launch {
            locationClient.getLocationUpdates(
                intervalMs = INTERVAL_MS,
                minDistanceMeters = 5f
            )
                .onEach { location -> processLocation(location) }
                .catch { e -> handleLocationError(e) }
                .launchIn(this)
        }
    }

    private fun stopTracking() {
        isRunning = false
        statusTracker.onTrackingStopped()
        locationJob?.cancel()
        locationJob = null
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun processLocation(location: UserLocation) {
        try {
            val newTiles = revealTilesUseCase(location)
            totalNewTiles += newTiles

            if (newTiles > 0) {
                Timber.d("Revealed %d tiles (session total: %d", newTiles, totalNewTiles)
            }

            // Update the notification with live stats
            lastLocation?.let { prev ->
                val distance = TileCalculator.haversineDistance(
                    prev.latitude, prev.longitude,
                    location.latitude, location.longitude
                )
                if (distance in 1.0..500.0) {
                    statusTracker.onDistanceUpdated(distance)
                }
            }
            lastLocation = location
            updateNotification(location)
        } catch (e: Exception) {
            Timber.e(e, "Location processing error")
        }
    }

    private fun handleLocationError(error: Throwable) {
        when (error) {
            is LocationClient.PermissionMissingException -> {
                statusTracker.onError("Location permission revoked")
                stopTracking()
            }

            is LocationClient.GpsDisabledException -> {
                statusTracker.onError("GPS disabled - enable location services")
            }

            else -> {
                Timber.e(error, "Location error: %s", error.message)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when ScratchMap is tracking your location"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(statusText: String = "Tracking your exploration"): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, LocationTrackingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Exploring the map")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(location: UserLocation) {
        val speedKmh = ((location.speed ?: 0f) * 3.6f)
        val distKm = statusTracker.sessionDistanceMeters.value / 1000.0
        val text = if (speedKmh > 1f) {
            "%.1f km/h \u00b7 %.1f km".format(speedKmh, distKm)
        } else {
            "%.1f km walked".format(distKm)
        }
        updateNotificationText(text)
    }

    private fun updateNotificationText(text: String) {
        val notification = createNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
            .apply {
                // Timeout after 4 hours for safety
                acquire(4 * 60 * 60 * 1000L)
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        locationJob?.cancel()
        releaseWakeLock()
    }
}
