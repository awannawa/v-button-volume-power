package com.awannawa.vbutton.overlay

import android.annotation.SuppressLint
import android.view.Gravity

/**
 * The physical screen edge the overlay hugs. Right is the historical and default side;
 * Left mirrors every behaviour. Uses Gravity.RIGHT/LEFT rather than START/END so the
 * overlay stays on the physical edge under RTL — the same reason the original right-only
 * window used Gravity.RIGHT (PRD section 10, 01-ui-spec.md section 4).
 */
enum class ScreenSide {
    Right,
    Left;

    companion object {
        /** The exact string Prefs stores for [Left]; every other stored value maps to [Right]. */
        const val STORED_LEFT = "left"
        const val STORED_RIGHT = "right"
    }
}

/** Pure: the only place a stored string becomes a [ScreenSide]. Prefs delegates here. */
fun ScreenSide.Companion.fromStoredOr(value: String?): ScreenSide =
    if (value == ScreenSide.STORED_LEFT) ScreenSide.Left else ScreenSide.Right

/** Pure: window gravity for a side. TOP is constant; only the left/right anchor flips. */
@SuppressLint("RtlHardcoded") // physical LEFT/RIGHT are required; END/START would flip under RTL
fun gravityFor(side: ScreenSide): Int =
    (if (side == ScreenSide.Left) Gravity.LEFT else Gravity.RIGHT) or Gravity.TOP

/**
 * Pure: the window's `x` for a side. With Gravity.RIGHT, x is the inward distance from the
 * right edge; with Gravity.LEFT it is the inward distance from the left edge — so each side
 * offsets by the inset on its OWN edge (nav bar / cutout).
 */
fun inwardOffsetPx(side: ScreenSide, leftInsetPx: Int, rightInsetPx: Int): Int =
    if (side == ScreenSide.Left) leftInsetPx else rightInsetPx
