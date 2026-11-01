package com.awannawa.vbutton

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the pure T06 press policy (AGENTS.md: pure logic covered by JVM unit
 * tests). The values assert the exact T06 task-file timings ("repeat about every
 * 150 ms after an initial delay of about 400 ms", "Power long press (about 500 ms)")
 * so the schedule cannot silently drift.
 */
class PressPolicyTest {

    @Test
    fun `first repeat tick waits the initial delay`() {
        assertEquals(400L, PressPolicy.repeatTickDelayMs(1))
        assertEquals(PressPolicy.REPEAT_INITIAL_DELAY_MS, PressPolicy.repeatTickDelayMs(1))
    }

    @Test
    fun `later repeat ticks wait one interval`() {
        assertEquals(150L, PressPolicy.repeatTickDelayMs(2))
        assertEquals(150L, PressPolicy.repeatTickDelayMs(3))
        assertEquals(150L, PressPolicy.repeatTickDelayMs(50))
    }

    @Test
    fun `task file timings are pinned`() {
        // T06 task file: initial delay about 400 ms, repeat about 150 ms, long press
        // about 500 ms. A change here must be an orchestrator decision, not a drift.
        assertEquals(400L, PressPolicy.REPEAT_INITIAL_DELAY_MS)
        assertEquals(150L, PressPolicy.REPEAT_INTERVAL_MS)
        assertEquals(500L, PressPolicy.LONG_PRESS_TIMEOUT_MS)
    }

    @Test
    fun `tap fires on up when the long press has not fired`() {
        assertTrue(PressPolicy.firesTapOnUp(longPressFired = false))
    }

    @Test
    fun `tap does not fire after the long press fired`() {
        // FR-8: one press must not do both — the long press consumes the release.
        assertFalse(PressPolicy.firesTapOnUp(longPressFired = true))
    }
}
