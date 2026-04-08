package com.auggie.scratch_map_app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievement_unlocks")
data class AchievementUnlockEntity(
    @PrimaryKey
    @ColumnInfo(name = "achievement_id") val achievementId: String,
    @ColumnInfo(name = "unlocked_at_millis") val unlockedAtMillis: Long
)
