package com.auggie.scratch_map_app.data.local.converter

import com.auggie.scratch_map_app.data.local.converter.AchievementMapper
import com.auggie.scratch_map_app.domain.model.StageTarget
import org.junit.Assert.*
import org.junit.Test

class AchievementMapperTest {

    @Test
    fun `percent stages survive a round trip`() {
        val stages = listOf(StageTarget.Percent(25), StageTarget.Percent(100))
        assertEquals(stages, AchievementMapper.decodeStages(AchievementMapper.encodeStages(stages)))
    }

    /** Regression: absolute targets were dropped on encode, silently unstaging the achievement. */
    @Test
    fun `absolute stages survive a round trip`() {
        val stages = listOf(StageTarget.Absolute(25L), StageTarget.Absolute(100L))
        assertEquals(stages, AchievementMapper.decodeStages(AchievementMapper.encodeStages(stages)))
    }

    @Test
    fun `mixed stage forms survive a round trip`() {
        val stages = listOf(StageTarget.Percent(50), StageTarget.Absolute(500L))
        assertEquals(stages, AchievementMapper.decodeStages(AchievementMapper.encodeStages(stages)))
    }

    @Test
    fun `empty stage list encodes to an empty array`() {
        assertEquals("[]", AchievementMapper.encodeStages(emptyList()))
    }

    @Test
    fun `blank and empty json decode to no stages`() {
        assertTrue(AchievementMapper.decodeStages("").isEmpty())
        assertTrue(AchievementMapper.decodeStages("[]").isEmpty())
    }

    @Test
    fun `malformed json decodes to no stages instead of throwing`() {
        assertTrue(AchievementMapper.decodeStages("not json").isEmpty())
    }

    @Test
    fun `stage entries with neither target key are skipped`() {
        assertTrue(AchievementMapper.decodeStages("""[{"nonsense":1}]""").isEmpty())
    }
}
