package com.awannawa.vbutton.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM tests for the pure position math (AGENTS.md: pure logic covered by JVM unit
 * tests, T05 task file: "put the clamping and ratio conversion in pure functions with
 * JVM unit tests"). All values are arbitrary px — the math is unit-agnostic; on the
 * device they come from DisplayMetrics, the bar-height resources, and the density.
 */
class SafeAreaTest {

    /**
     * A typical phone: 2400px screen, 100px status bar, 100px navigation bar, a 500px
     * panel and the 8dp margin at 3x density (24px). 01-ui-spec.md section 4:
     * valid Y from insetTop + margin to screenHeight - insetBottom - panelHeight - margin.
     */
    private val range = SafeArea.compute(
        screenHeightPx = 2400,
        insetTopPx = 100,
        insetBottomPx = 100,
        panelHeightPx = 500,
        marginPx = 24,
    )

    // SafeArea.compute: the valid range. ------------------------------------------------

    @Test
    fun `range follows the ui-spec formula`() {
        assertEquals(124, range.minPx) // insetTop + 8dp
        assertEquals(1776, range.maxPx) // screenHeight - insetBottom - panelHeight - 8dp
    }

    @Test
    fun `panel taller than the space collapses the range to one valid position`() {
        // max would be negative (800 - 100 - 700 - 24 = -24): the range must stay
        // inside the screen, never invert, so the overlay lands on min == max.
        val degenerate = SafeArea.compute(
            screenHeightPx = 800,
            insetTopPx = 100,
            insetBottomPx = 100,
            panelHeightPx = 700,
            marginPx = 24,
        )
        assertEquals(124, degenerate.minPx)
        assertEquals(124, degenerate.maxPx)
    }

    @Test
    fun `panel exactly as tall as the space fits gives min equals max`() {
        val exact = SafeArea.compute(
            screenHeightPx = 2400,
            insetTopPx = 100,
            insetBottomPx = 100,
            panelHeightPx = 2152,
            marginPx = 24,
        )
        assertEquals(124, exact.minPx)
        assertEquals(124, exact.maxPx)
    }

    // SafeArea.clampY: clamping. ---------------------------------------------------------

    @Test
    fun `clamp keeps a position inside the range unchanged`() {
        assertEquals(500f, SafeArea.clampY(500f, range))
        assertEquals(124f, SafeArea.clampY(124f, range)) // min boundary inclusive
        assertEquals(1776f, SafeArea.clampY(1776f, range)) // max boundary inclusive
    }

    @Test
    fun `clamp pushes a position above or below the range back inside`() {
        assertEquals(124f, SafeArea.clampY(-400f, range)) // under the status bar
        assertEquals(1776f, SafeArea.clampY(2400f, range)) // off the bottom
    }

    @Test
    fun `clamp on a degenerate range always returns the single position`() {
        val degenerate = SafeArea.YRange(124, 124)
        assertEquals(124f, SafeArea.clampY(0f, degenerate))
        assertEquals(124f, SafeArea.clampY(1000f, degenerate))
    }

    // SafeArea.yRatioFromY: y -> ratio. --------------------------------------------------

    @Test
    fun `range ends map to ratio 0 and 1`() {
        assertEquals(0f, SafeArea.yRatioFromY(124f, range))
        assertEquals(1f, SafeArea.yRatioFromY(1776f, range))
    }

    @Test
    fun `ratio is proportional between the ends`() {
        // 413 is exactly a quarter of the 1652px span.
        assertEquals(0.25f, SafeArea.yRatioFromY(537f, range))
        assertEquals(0.5f, SafeArea.yRatioFromY(950f, range))
    }

    @Test
    fun `ratio of a y outside the range is clamped into 0 to 1`() {
        // The stored ratio must stay in its documented domain even if a caller passes
        // an unclamped px value.
        assertEquals(0f, SafeArea.yRatioFromY(-100f, range))
        assertEquals(1f, SafeArea.yRatioFromY(3000f, range))
    }

    @Test
    fun `ratio on a degenerate range is the neutral 0_5`() {
        // T05 ruling on the division-by-zero guard: any ratio restores to the same
        // single position, so the least surprising value is stored.
        val degenerate = SafeArea.YRange(124, 124)
        assertEquals(0.5f, SafeArea.yRatioFromY(124f, degenerate))
    }

    // SafeArea.yFromRatio: ratio -> y. ---------------------------------------------------

    @Test
    fun `ratio 0 and 1 restore the range ends`() {
        assertEquals(124f, SafeArea.yFromRatio(0f, range))
        assertEquals(1776f, SafeArea.yFromRatio(1f, range))
    }

    @Test
    fun `a stored ratio restores its position`() {
        assertEquals(784.8f, SafeArea.yFromRatio(0.4f, range), 0.01f)
    }

    @Test
    fun `a ratio outside 0 to 1 restores a clamped position`() {
        // A ratio saved on a taller screen (or a corrupt value) must never push the
        // window under a system bar or off the screen.
        assertEquals(124f, SafeArea.yFromRatio(-0.2f, range))
        assertEquals(1776f, SafeArea.yFromRatio(1.5f, range))
    }

    @Test
    fun `any ratio on a degenerate range restores the single position`() {
        val degenerate = SafeArea.YRange(124, 124)
        assertEquals(124f, SafeArea.yFromRatio(0f, degenerate))
        assertEquals(124f, SafeArea.yFromRatio(0.5f, degenerate))
        assertEquals(124f, SafeArea.yFromRatio(1f, degenerate))
    }

    @Test
    fun `save and restore round-trip keeps the position`() {
        // Sub-pixel tolerance: the ratio is stored as a Float, so a restore can differ
        // from the dragged position by a fraction of a pixel, never visibly.
        for (y in intArrayOf(124, 200, 950, 1500, 1776)) {
            val restored = SafeArea.yFromRatio(SafeArea.yRatioFromY(y.toFloat(), range), range)
            assertEquals(y.toFloat(), restored, 0.5f)
        }
    }

    @Test
    fun `the default ratio stays the ruling value`() {
        // T05 ruling: the position default is 0.4 until the user drags (Prefs.kt).
        assertEquals(0.4f, com.awannawa.vbutton.Prefs.Y_RATIO_DEFAULT)
    }

    // Compact mode (T07): SafeArea.availableHeightPx + selectFittingPreset. -------------
    // Preset panel heights in px at density 2: S 328, M 352, L 376; at density 3:
    // S 492, M 528, L 564.

    @Test
    fun `available height subtracts both insets and both margins`() {
        val screen = SafeArea.ScreenInsets(
            screenHeightPx = 800,
            insetTopPx = 100,
            insetBottomPx = 100,
            rightInsetPx = 0,
        )
        assertEquals(552, SafeArea.availableHeightPx(screen, 24))
    }

    @Test
    fun `a panel of exactly the available height leaves a valid range`() {
        // The "fits" measure of compact mode must agree with SafeArea.compute: a panel
        // of exactly the available height gives max == min — non-inverted, so the
        // position restore has a (degenerate but valid) range to work with.
        val screen = SafeArea.ScreenInsets(800, 100, 100, 0)
        val available = SafeArea.availableHeightPx(screen, 24)
        val exact = SafeArea.compute(800, 100, 100, available, 24)
        assertEquals(124, exact.minPx)
        assertEquals(124, exact.maxPx)
    }

    @Test
    fun `compact keeps the active preset when it fits`() {
        // M (352px at density 2) fits in 552px — even though L (376px) would too:
        // compact mode never steps UP, the user's chosen size wins.
        assertEquals(Tokens.PRESET_M, SafeArea.selectFittingPreset(552, Tokens.PRESET_M, 2f))
    }

    @Test
    fun `compact never steps up on a tall screen`() {
        assertEquals(Tokens.PRESET_M, SafeArea.selectFittingPreset(2000, Tokens.PRESET_M, 2f))
        assertEquals(Tokens.PRESET_S, SafeArea.selectFittingPreset(2000, Tokens.PRESET_S, 2f))
    }

    @Test
    fun `compact steps down one preset to the first that fits`() {
        // M (352px) misses 340px, S (328px) lands: one step, not a jump to the floor.
        assertEquals(Tokens.PRESET_S, SafeArea.selectFittingPreset(340, Tokens.PRESET_M, 2f))
        // L (376px) misses 360px, M (352px) fits.
        assertEquals(Tokens.PRESET_M, SafeArea.selectFittingPreset(360, Tokens.PRESET_L, 2f))
    }

    @Test
    fun `compact steps through several presets when needed`() {
        // L (376px) and M (352px) both miss 340px; S (328px) lands.
        assertEquals(Tokens.PRESET_S, SafeArea.selectFittingPreset(340, Tokens.PRESET_L, 2f))
    }

    @Test
    fun `compact floors at preset S even when nothing fits`() {
        assertEquals(Tokens.PRESET_S, SafeArea.selectFittingPreset(100, Tokens.PRESET_M, 2f))
        assertEquals(Tokens.PRESET_S, SafeArea.selectFittingPreset(100, Tokens.PRESET_S, 2f))
    }

    @Test
    fun `compact fit boundary is exact after density scaling`() {
        // 176dp at density 3 is exactly 528px: fits. One pixel less does not, and the
        // step-down lands on S (164dp = 492px also misses 527px, so the floor holds).
        assertEquals(Tokens.PRESET_M, SafeArea.selectFittingPreset(528, Tokens.PRESET_M, 3f))
        assertEquals(Tokens.PRESET_S, SafeArea.selectFittingPreset(527, Tokens.PRESET_M, 3f))
    }

    @Test
    fun `insets overload computes the same range as the pure core`() {
        val screen = SafeArea.ScreenInsets(2400, 100, 100, 42)
        assertEquals(
            SafeArea.compute(2400, 100, 100, 500, 24),
            SafeArea.compute(screen, 500, 24),
        )
    }
}
