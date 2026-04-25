package com.auggie.scratch_map_app.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auggie.scratch_map_app.domain.model.RegionStats
import com.auggie.scratch_map_app.ui.components.AchievementCard
import com.auggie.scratch_map_app.ui.components.ExplorationProgressBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionDetailScreen(
    regionId: String,
    onBack: () -> Unit,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
    viewModel: RegionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose { onBottomBarVisibilityChanged(true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.stats?.region?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.stats != null -> {
                RegionDetailContent(
                    stats = uiState.stats!!,
                    onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun RegionDetailContent(
    stats: RegionStats,
    onBottomBarVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -5f) onBottomBarVisibilityChanged(false)
                else if (available.y > 5f) onBottomBarVisibilityChanged(true)
                return Offset.Zero
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RegionHeaderCard(stats)
        }

        item {
            Text(
                "Achievements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        items(stats.achievements) { achievement ->
            AchievementCard(achievement = achievement)
        }
    }
}

@Composable
private fun RegionHeaderCard(stats: RegionStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stats.region.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (stats.region.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stats.region.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            ExplorationProgressBar(progress = stats.progress)

            Spacer(Modifier.height(6.dp))

            Text(
                "${(stats.progress * 100).toInt()}% explored",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip(
                    label = "Waypoints",
                    value = "${stats.waypointsUnlocked}/${stats.waypointsTotal}"
                )
                StatChip(
                    label = "Achievements",
                    value = "${stats.achievements.count { it.isUnlocked }}/${stats.achievements.size}"
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
