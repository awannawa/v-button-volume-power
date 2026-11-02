package com.awannawa.vbutton.overlay

import android.view.Gravity
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenSideTest {

    @Test
    fun `stored left maps to Left`() {
        assertEquals(ScreenSide.Left, ScreenSide.fromStoredOr(ScreenSide.STORED_LEFT))
    }

    @Test
    fun `right, null, empty and garbage all map to Right`() {
        assertEquals(ScreenSide.Right, ScreenSide.fromStoredOr(ScreenSide.STORED_RIGHT))
        assertEquals(ScreenSide.Right, ScreenSide.fromStoredOr(null))
        assertEquals(ScreenSide.Right, ScreenSide.fromStoredOr(""))
        assertEquals(ScreenSide.Right, ScreenSide.fromStoredOr("LEFT")) // case-sensitive: only the exact stored value counts
        assertEquals(ScreenSide.Right, ScreenSide.fromStoredOr("end"))
    }

    @Test
    fun `gravity anchors to the physical edge and TOP`() {
        assertEquals(Gravity.RIGHT or Gravity.TOP, gravityFor(ScreenSide.Right))
        assertEquals(Gravity.LEFT or Gravity.TOP, gravityFor(ScreenSide.Left))
    }

    @Test
    fun `inward offset on the right uses the right inset`() {
        assertEquals(90, inwardOffsetPx(ScreenSide.Right, leftInsetPx = 40, rightInsetPx = 90))
    }

    @Test
    fun `inward offset on the left uses the left inset`() {
        assertEquals(40, inwardOffsetPx(ScreenSide.Left, leftInsetPx = 40, rightInsetPx = 90))
    }

    @Test
    fun `the documented default side is Right`() {
        // Prefs.SCREEN_SIDE_DEFAULT mirrors this; keep them in sync.
        assertEquals(ScreenSide.Right, ScreenSide.fromStoredOr(null))
    }

    @Test
    fun `handle bar hugs the anchored edge`() {
        // 24dp touch width at 1px/dp, 5px bar: left edge -> 0, right edge -> 19.
        assertEquals(0, handleBarLeftPx(ScreenSide.Left, widthPx = 24, barWidthPx = 5))
        assertEquals(19, handleBarLeftPx(ScreenSide.Right, widthPx = 24, barWidthPx = 5))
    }
}
