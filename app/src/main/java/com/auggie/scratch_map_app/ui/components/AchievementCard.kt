package com.auggie.scratch_map_app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auggie.scratch_map_app.domain.model.Achievement

private val RANK_COLORS = listOf(
    Color(0xFFCD7F32), // I — Bronze
    Color(0xFFA8A9AD), // II — Silver
    Color(0xFFFFD700), // III — Gold
    Color(0xFF4FC3F7), // IV — Platinum
    Color(0xFFCE93D8)  // V — Diamond
)

private val RANK_NUMERALS = listOf("I", "II", "III", "IV", "V")

private fun rankColor(completedCount: Int): Color? =
    if (completedCount <= 0) null else RANK_COLORS.getOrElse(completedCount - 1) { RANK_COLORS.last() }

private fun rankNumeral(completedCount: Int): String? =
    if (completedCount <= 0) null else RANK_NUMERALS.getOrElse(completedCount - 1) { RANK_NUMERALS.last() }

@Composable
fun AchievementCard(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    val hasStages = achievement.stages.isNotEmpty()
    val rank = if (hasStages) achievement.completedStageCount
    else if (achievement.isUnlocked) 1 else 0
    val color = rankColor(rank)
    val numeral = rankNumeral(rank)

    val iconBg = color?.copy(alpha = 0.18f)
        ?: if (achievement.isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    val iconTint = color
        ?: if (achievement.isUnlocked) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked)
                (color?.copy(alpha = 0.07f)
                    ?: MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconBg,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = achievementIcon(achievement.iconName),
                            contentDescription = null,
                            modifier = Modifier
                                .size(26.dp)
                                .alpha(if (rank > 0) 1f else 0.5f),
                            tint = iconTint
                        )
                    }
                }
                if (numeral != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = color!!,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                    ) {
                        Text(
                            text = numeral,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (rank > 0) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                if (hasStages) {
                    MilestonesProgressBar(
                        progress = achievement.overallProgress,
                        milestones = achievement.milestoneFractions,
                        height = 6.dp
                    )
                } else {
                    ExplorationProgressBar(
                        progress = achievement.progress,
                        height = 6.dp
                    )
                }
            }

            if (achievement.isUnlocked && !hasStages) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Unlocked",
                    tint = color ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "${(achievement.overallProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = color ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun achievementIcon(name: String): ImageVector = when (name) {
    "directions_walk" -> Icons.AutoMirrored.Filled.DirectionsWalk
    "explore" -> Icons.Default.Explore
    "map" -> Icons.Default.Map
    "hiking" -> Icons.Default.Hiking
    "public" -> Icons.Default.Public
    "language" -> Icons.Default.Language
    "directions_run" -> Icons.Default.DirectionsRun
    "location_city" -> Icons.Default.LocationCity
    "military_tech" -> Icons.Default.MilitaryTech
    "stars" -> Icons.Default.Stars
    "church" -> Icons.Default.Church
    "account_balance" -> Icons.Default.AccountBalance
    "park" -> Icons.Default.Park
    "history_edu" -> Icons.Default.HistoryEdu
    "place" -> Icons.Default.Place
    "pin_drop" -> Icons.Default.PinDrop
    "collections" -> Icons.Default.Collections
    "museum" -> Icons.Default.Museum
    "fort" -> Icons.Default.Fort
    else -> Icons.Default.Star
}
