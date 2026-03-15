package com.auggie.scratch_map_app.domain.model

import com.auggie.scratch_map_app.domain.model.UserLocation
import org.junit.Assert.*
import org.junit.Test

class UserLocationTest {

    @Test
    fun `isSufficientlyAccurate returns true when accuracy is within threshold`() {
        val location = UserLocation(54.6872, 25.2797, accuracy = 30f, speed = null)
        assertTrue(location.isSufficientlyAccurate(50f))
    }

    @Test
    fun `isSufficientlyAccurate returns false when accuracy exceeds threshold`() {
        val location = UserLocation(54.6872, 25.2797, accuracy = 80f, speed = null)
        assertFalse(location.isSufficientlyAccurate(50f))
    }

    @Test
    fun `isSufficientlyAccurate returns true when accuracy equals threshold exactly`() {
        val location = UserLocation(54.6872, 25.2797, accuracy = 50f, speed = null)
        assertTrue(location.isSufficientlyAccurate(50f))
    }

    @Test
    fun `isSufficientlyAccurate returns false for zero threshold`() {
        val location = UserLocation(54.6872, 25.2797, accuracy = 1f, speed = null)
        assertFalse(location.isSufficientlyAccurate(0f))
    }
}
