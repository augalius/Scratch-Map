package com.auggie.scratch_map_app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.auggie.scratch_map_app.R
import com.auggie.scratch_map_app.domain.model.UserLocation
import com.auggie.scratch_map_app.domain.repository.SyncRepository
import com.auggie.scratch_map_app.domain.usecase.CheckWaypointProximityUseCase
import com.auggie.scratch_map_app.domain.usecase.RevealTilesUseCase
import com.auggie.scratch_map_app.di.ApplicationScope
import com.auggie.scratch_map_app.domain.location.LocationClient
import com.auggie.scratch_map_app.util.TileCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
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
    lateinit var checkWaypointProximityUseCase: CheckWaypointProximityUseCase

    @Inject
    lateinit var statusTracker: ServiceStatusTracker

    @Inject
    lateinit var syncRepository: SyncRepository

    /** Process-lifetime scope: the final flush must survive [stopSelf]. */
    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    private var locationJob: Job? = null
    private var syncJob: Job? = null
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

        // Every 5 minutes: if 50+ new tiles have accumulated since the last upload, flush
        // just those new tiles. If fewer than 50, keep counting — they roll into the next
        // interval. Everything remaining is flushed when tracking stops.
        syncJob = lifecycleScope.launch {
            var lastFlushedTileCount = 0
            while (true) {
                kotlinx.coroutines.delay(5 * 60_000)
                val newTilesSinceFlush = totalNewTiles - lastFlushedTileCount
                if (newTilesSinceFlush >= 50) {
                    syncRepository.flushTiles()
                    lastFlushedTileCount = totalNewTiles
                }
            }
        }

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
        syncJob?.cancel()
        syncJob = null
        // Must NOT use lifecycleScope: stopSelf() below destroys the service and cancels it
        // mid-flush, silently dropping every tile revealed since the last interval upload.
        appScope.launch { syncRepository.flushTiles() }
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

            // Check waypoint proximity and unlock nearby waypoints
            val unlocked = checkWaypointProximityUseCase(location)
            for (waypoint in unlocked) {
                statusTracker.onWaypointUnlocked(waypoint)
            }

            if (unlocked.isNotEmpty()) {
                vibrate()
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
            .setSmallIcon(R.drawable.ic_explore)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_explore, "Stop", stopIntent)
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

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    300,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
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
