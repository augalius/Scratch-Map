package com.auggie.scratch_map_app.ui.components

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.auggie.scratch_map_app.util.PermissionUtil
import com.auggie.scratch_map_app.util.TrackingPermissionState

/**
 * Multi-step permission request flow
 */
@Composable
fun LocationPermissionHandler(onAllGranted: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var permissionState by remember {
        mutableStateOf(PermissionUtil.getPermissionState(context))
    }

    val refreshState = {
        permissionState = PermissionUtil.getPermissionState(context)
        if (permissionState == TrackingPermissionState.READY) {
            onAllGranted()
        }
    }

    // Location permission
    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted && coarseGranted) {
            refreshState()
        } else {
            permissionState = TrackingPermissionState.LOCATION_NEEDED
        }
    }

    // Notification permission
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        refreshState()
    }

    // Background location
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        refreshState()
    }

    when (permissionState) {
        TrackingPermissionState.READY -> {
            // All permissions graned, location enabled
            LaunchedEffect(Unit) {
                onAllGranted()
            }
        }

        // Approximate location was granted but precise was not — the grid is far too fine for
        // coarse fixes, so this gets its own copy rather than repeating the first-run prompt.
        TrackingPermissionState.FINE_LOCATION_NEEDED -> {
            PermissionCard(
                icon = Icons.Default.MyLocation,
                title = "Precise Location Needed",
                description = "ScratchMap currently has approximate location only. Revealing the " +
                        "map accurately needs precise location — choose \"Precise\" when prompted.",
                buttonText = "Grant Precise Location",
                onButtonClick = {
                    foregroundLocationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = modifier
            )
        }

        TrackingPermissionState.LOCATION_NEEDED -> {
            PermissionCard(
                icon = Icons.Default.LocationOn,
                title = "Location Permission Required",
                description = "This app needs access to your precise location to track your movement " +
                        "and reveal the map as you explore.",
                buttonText = "Grant Location Access",
                onButtonClick = {
                    foregroundLocationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = modifier
            )
        }

        TrackingPermissionState.NOTIFICATION_NEEDED -> {
            PermissionCard(
                icon = Icons.Default.Notifications,
                title = "Notification Permission",
                description = "ScratchMap shows a notification while tracking your location in the " +
                        "background. This is required by Android for GPS tracking.",
                buttonText = "Allow Notifications",
                onButtonClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                modifier = modifier
            )
        }

        TrackingPermissionState.BACKGROUND_LOCATION_NEEDED -> {
            PermissionCard(
                icon = Icons.Default.MyLocation,
                title = "Background Location",
                description = "To keep scratching the map when the app is in your pocket, " +
                        "ScratchMap needs \"Allow all the time\" location access. You can skip " +
                        "this and only track while app is open.",
                buttonText = "Allow Background Location",
                onButtonClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                },
                modifier = modifier
            )
        }

        TrackingPermissionState.LOCATION_DISABLED -> {
            PermissionCard(
                icon = Icons.Default.LocationOff,
                title = "Location Services Disabled",
                description = "Your device's location services are turned off. Please enable GPS " +
                        "in your device settings to start exploring.",
                buttonText = "Open Location Settings",
                onButtonClick = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onButtonClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = buttonText)
            }
        }
    }
}