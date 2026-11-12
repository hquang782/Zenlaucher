package com.zen.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayTweaksTest {
    @Test
    fun `restores the user's own animation scale, or normal speed when it was already off`() {
        assertEquals(0.5f, restoredScale(0.5f))
        assertEquals(1f, restoredScale(0f))
    }

    @Test
    fun `gives back the user's color correction, but not a grayscale that was already on`() {
        assertEquals(true, restoresColorCorrection(wasEnabled = true, savedMode = 12))
        assertEquals(false, restoresColorCorrection(wasEnabled = true, savedMode = 0))
        assertEquals(false, restoresColorCorrection(wasEnabled = false, savedMode = 12))
    }
}
