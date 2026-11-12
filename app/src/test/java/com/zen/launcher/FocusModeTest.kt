package com.zen.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar.FRIDAY
import java.util.Calendar.MONDAY
import java.util.Calendar.SATURDAY
import java.util.Calendar.SUNDAY
import java.util.Calendar.THURSDAY
import java.util.Calendar.TUESDAY
import java.util.Calendar.WEDNESDAY

class FocusModeTest {
    private val weekdays = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)

    private fun at(h: Int, m: Int = 0) = h * 60 + m

    @Test
    fun `day window locks from start up to but not including end, on chosen days only`() {
        val start = at(8)
        val end = at(17)
        assertTrue(isLocked(at(8), MONDAY, start, end, weekdays))
        assertTrue(isLocked(at(16, 59), FRIDAY, start, end, weekdays))
        assertFalse(isLocked(at(17), FRIDAY, start, end, weekdays))
        assertFalse(isLocked(at(7, 59), MONDAY, start, end, weekdays))
        assertFalse(isLocked(at(10), SATURDAY, start, end, weekdays))
    }

    @Test
    fun `overnight window belongs to the day it starts`() {
        val start = at(22)
        val end = at(7)
        // Friday 23:00 and Saturday 06:00 are both part of Friday night.
        assertTrue(isLocked(at(23), FRIDAY, start, end, weekdays))
        assertTrue(isLocked(at(6), SATURDAY, start, end, weekdays))
        // Saturday 23:00 starts Saturday night, which is not chosen; Monday 06:00 is Sunday night.
        assertFalse(isLocked(at(23), SATURDAY, start, end, weekdays))
        assertFalse(isLocked(at(6), MONDAY, start, end, weekdays))
        assertTrue(isLocked(at(6), MONDAY, start, end, weekdays + SUNDAY))
        assertFalse(isLocked(at(12), FRIDAY, start, end, weekdays))
    }

    @Test
    fun `same start and end locks 24 hours from the start time`() {
        // Monday 09:00 until Tuesday 09:00.
        assertTrue(isLocked(at(9), MONDAY, at(9), at(9), weekdays))
        assertTrue(isLocked(at(8, 59), TUESDAY, at(9), at(9), weekdays))
        // Sunday is not chosen: Monday before 09:00 is still free.
        assertFalse(isLocked(at(8, 59), MONDAY, at(9), at(9), weekdays))
        assertFalse(isLocked(at(12), SUNDAY, at(9), at(9), weekdays))
    }
}
