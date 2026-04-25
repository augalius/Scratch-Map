package com.auggie.scratch_map_app.domain.model

import com.auggie.scratch_map_app.domain.model.Achievement
import com.auggie.scratch_map_app.domain.model.AchievementStage
import org.junit.Assert.*
import org.junit.Test

class AchievementTest {

    private val noStage = Achievement(
        id = "a1", title = "Walker", description = "", iconName = "walk",
        targetValue = 100L, currentValue = 0L
    )

    private val stages =
        listOf(AchievementStage(25L), AchievementStage(50L), AchievementStage(100L))
    private val staged = Achievement(
        id = "a2", title = "Staged", description = "", iconName = "star",
        targetValue = 100L, currentValue = 0L, stages = stages
    )

    @Test
    fun `isUnlocked no stages returns true when current meets target`() {
        assertTrue(noStage.copy(currentValue = 100L).isUnlocked)
    }

    @Test
    fun `isUnlocked no stages returns false when current below target`() {
        assertFalse(noStage.copy(currentValue = 99L).isUnlocked)
    }

    @Test
    fun `isUnlocked with stages requires every stage to pass`() {
        assertFalse(staged.copy(currentValue = 50L).isUnlocked)
    }

    @Test
    fun `isUnlocked with stages returns true when all stages met`() {
        assertTrue(staged.copy(currentValue = 100L).isUnlocked)
    }

    @Test
    fun `progress no stages is proportional`() {
        assertEquals(0.5f, noStage.copy(currentValue = 50L).progress, 0.001f)
    }

    @Test
    fun `progress no stages is clamped to 1 when exceeded`() {
        assertEquals(1f, noStage.copy(currentValue = 200L).progress, 0.001f)
    }

    @Test
    fun `progress with stages shows progress toward next stage`() {
        // currentValue=37: past stage 25, next stage 50 → (37-25)/(50-25) = 0.48
        assertEquals(0.48f, staged.copy(currentValue = 37L).progress, 0.01f)
    }

    @Test
    fun `progress with stages returns 1 when all stages complete`() {
        assertEquals(1f, staged.copy(currentValue = 100L).progress, 0.001f)
    }

    @Test
    fun `progress with stages is 0 before first stage`() {
        // currentValue=10: no prev stage → prevTarget=0, range=25, (10-0)/25 = 0.4
        assertEquals(0.4f, staged.copy(currentValue = 10L).progress, 0.01f)
    }

    @Test
    fun `completedStageCount counts stages where current meets target`() {
        assertEquals(2, staged.copy(currentValue = 60L).completedStageCount)
    }


    @Test
    fun `completedStageCount returns all when fully complete`() {
        assertEquals(3, staged.copy(currentValue = 100L).completedStageCount)
    }


    @Test
    fun `overallProgress ignores stage boundaries`() {
        assertEquals(0.75f, staged.copy(currentValue = 75L).overallProgress, 0.001f)
    }


    @Test
    fun `milestoneFractions is empty when no stages`() {
        assertTrue(noStage.milestoneFractions.isEmpty())
    }


    @Test
    fun `milestoneFractions subsequent fractions are proportional to target`() {
        val fractions = staged.milestoneFractions
        assertEquals(3, fractions.size)
        assertEquals(0f, fractions[0], 0.001f)
        assertEquals(0.5f, fractions[1], 0.001f)
        assertEquals(1.0f, fractions[2], 0.001f)
    }
}
