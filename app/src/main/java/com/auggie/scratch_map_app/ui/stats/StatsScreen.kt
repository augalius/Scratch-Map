package com.auggie.scratch_map_app.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auggie.scratch_map_app.domain.model.RegionStats
import com.auggie.scratch_map_app.ui.components.ExplorationProgressBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onRegionClick: (String) -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gs = uiState.globalStats

    Scaffold(
        topBar = { TopAppBar(title = { Text("Exploration Stats") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Map,
                        label = "Area scratched",
                        value = "${"%.2f".format(gs.totalAreaKm2)} km²"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Place,
                        label = "Waypoints found",
                        value = "${gs.totalWaypointsUnlocked}"
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        label = "Trails completed",
                        value = "${gs.totalTrailsCompleted}"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Public,
                        label = "Regions visited",
                        value = "${gs.regionsVisited}"
                    )
                }
            }
            item {
                Text(
                    "Regions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }
            items(uiState.regionStats) { stats ->
                RegionCard(stats = stats, onClick = { onRegionClick(stats.region.id) })
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RegionCard(stats: RegionStats, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                stats.region.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${"%.2f".format(stats.revealedAreaKm2)} km² · ${stats.waypointsUnlocked}/${stats.waypointsTotal} waypoints · " +
                        "${stats.achievements.count { it.isUnlocked }}/${stats.achievements.size} achievements",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            ExplorationProgressBar(progress = stats.progress, height = 8.dp)
            Spacer(Modifier.height(4.dp))
            Text(
                "${(stats.progress * 100).toInt()}% explored",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
