package com.auggie.scratch_map_app.domain.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val targetValue: Long,
    val currentValue: Long,
    val stages: List<AchievementStage> = emptyList()
) {
    val isUnlocked: Boolean get() = if (stages.isEmpty()) {
        currentValue >= targetValue
    } else {
        stages.all { currentValue >= it.targetValue }
    }

    // Progress toward the next incomplete stage (or overall if no stages)
    val progress: Float get() {
        if (stages.isEmpty()) {
            return (currentValue.toFloat() / targetValue.coerceAtLeast(1)).coerceIn(0f, 1f)
        }
        val nextStage = stages.firstOrNull { currentValue < it.targetValue } ?: return 1f
        val prevTarget = stages.lastOrNull { currentValue >= it.targetValue }?.targetValue ?: 0L
        val range = (nextStage.targetValue - prevTarget).coerceAtLeast(1)
        return ((currentValue - prevTarget).toFloat() / range).coerceIn(0f, 1f)
    }

    val completedStageCount: Int get() = stages.count { currentValue >= it.targetValue }

    val overallProgress: Float get() =
        (currentValue.toFloat() / targetValue.coerceAtLeast(1)).coerceIn(0f, 1f)

    /**
     * Fractional positions (0–1) of each stage boundary on the overall bar.
     * The first milestone is always pinned to 0
     */
    val milestoneFractions: List<Float> get() =
        stages.mapIndexed { index, stage ->
            if (index == 0) 0f
            else (stage.targetValue.toFloat() / targetValue.coerceAtLeast(1)).coerceIn(0f, 1f)
        }
}
