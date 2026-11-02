package com.awannawa.vbutton.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the pure fling predicate (AGENTS.md: pure logic covered by JVM unit
 * tests). The thresholds are arbitrary px — the predicate is unit-agnostic; on the
 * device they come from ViewConfiguration at event time.
 */
class ShouldOpenOnFlingTest {

    private val touchSlopPx = 10f
    private val minFlingPx = 50f

    @Test
    fun `left fling with enough speed and travel opens`() {
        assertTrue(shouldOpenOnFling(ScreenSide.Right, -800f, 0f, -40f, touchSlopPx, minFlingPx))
    }

    @Test
    fun `rightward fling never opens`() {
        assertFalse(shouldOpenOnFling(ScreenSide.Right, 800f, 0f, 40f, touchSlopPx, minFlingPx))
    }

    @Test
    fun `zero velocity never opens`() {
        assertFalse(shouldOpenOnFling(ScreenSide.Right, 0f, 0f, -40f, touchSlopPx, minFlingPx))
    }

    @Test
    fun `leftward speed below the fling minimum does not open`() {
        assertFalse(shouldOpenOnFling(ScreenSide.Right, -(minFlingPx - 1), 0f, -40f, touchSlopPx, minFlingPx))
    }

    @Test
    fun `leftward speed without horizontal travel does not open`() {
        // onFling fires when EITHER axis is fast enough, so a quick flick can arrive
        // with real leftward speed but almost no horizontal displacement.
        assertFalse(shouldOpenOnFling(ScreenSide.Right, -800f, 0f, -(touchSlopPx - 1), touchSlopPx, minFlingPx))
    }

    @Test
    fun `boundaries are inclusive`() {
        assertTrue(shouldOpenOnFling(ScreenSide.Right, -minFlingPx, 0f, -touchSlopPx, touchSlopPx, minFlingPx))
    }

    @Test
    fun `horizontally dominant left fling opens even with some vertical motion`() {
        // A natural swipe is rarely perfectly horizontal; moderate vertical velocity
        // must not block it as long as the leftward component dominates (FR-4).
        assertTrue(shouldOpenOnFling(ScreenSide.Right, -800f, -200f, -40f, touchSlopPx, minFlingPx))
    }

    @Test
    fun `vertical dominant gesture with leftward drift does not open`() {
        // FR-4 wants a swipe to the left, not any gesture with a leftward component:
        // a vertical list scroll starting on the handle strip drifts left by a little
        // while being overwhelmingly vertical, and must stay closed. All other gates
        // pass here (speed and travel are enough); only dominance blocks it.
        assertFalse(shouldOpenOnFling(ScreenSide.Right, -150f, -3000f, -40f, touchSlopPx, minFlingPx))
    }

    @Test
    fun `equal leftward and vertical velocity does not open`() {
        // Knife-edge |velocityX| == |velocityY| is resolved CLOSED on purpose: the
        // dominance rule is strict (abs(vx) > abs(vy)), because equality is not a
        // leftward swipe and a false-open is worse than a missed one.
        assertFalse(shouldOpenOnFling(ScreenSide.Right, -800f, -800f, -40f, touchSlopPx, minFlingPx))
    }

    @Test
    fun `left side opens on a rightward fling`() {
        assertTrue(shouldOpenOnFling(ScreenSide.Left, 1200f, 0f, 60f, touchSlopPx, minFlingPx))
    }

    @Test
    fun `left side ignores a leftward fling`() {
        assertFalse(shouldOpenOnFling(ScreenSide.Left, -1200f, 0f, -60f, touchSlopPx, minFlingPx))
    }

    @Test
    fun `right side opens on a leftward fling`() {
        assertTrue(shouldOpenOnFling(ScreenSide.Right, -1200f, 0f, -60f, touchSlopPx, minFlingPx))
    }
}
