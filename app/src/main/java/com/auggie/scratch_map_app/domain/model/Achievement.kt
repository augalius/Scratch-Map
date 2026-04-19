package com.auggie.scratch_map_app.domain.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val targetValue: Long,
    val currentValue: Long
) {
    val isUnlocked: Boolean get() = currentValue >= targetValue

    val progress: Float get() =
        (currentValue.toFloat() / targetValue.coerceAtLeast(1)).coerceIn(0f, 1f)
}
