package com.awannawa.vbutton.overlay

import android.annotation.SuppressLint
import android.content.Context

/**
 * The overlay's valid Y range and the yRatio <-> px conversions (PRD FR-6, PRD
 * section 9, 01-ui-spec.md section 4). One responsibility: position math. T07
 * improves the inset lookup inside the Android [compute] wrapper (WindowMetrics and
 * display-cutout insets, compact mode) without changing any signature in this file,
 * so no caller and no test changes with it.
 */
internal object SafeArea {

    /**
     * Valid top offsets for the overlay window, in px. The window has gravity TOP, so
     * a Y value is the window's top edge measured from the top of the screen.
     */
    data class YRange(val minPx: Int, val maxPx: Int)

    /**
     * Pure core (AGENTS.md: pure logic separated from Android APIs and covered by JVM
     * unit tests). 01-ui-spec.md section 4: valid Y runs from insetTop + margin to
     * screenHeight - insetBottom - panelHeight - margin — the whole window (the handle
     * while collapsed, the panel while expanded) stays clear of the status bar, the
     * navigation bar, and the screen edges.
     *
     * A screen too short for the panel (max < min, e.g. a short landscape screen
     * before T07's compact mode) collapses to min == max instead of returning an
     * inverted range: every caller clamps against [YRange], so the overlay still lands
     * on one valid position.
     */
    fun compute(
        screenHeightPx: Int,
        insetTopPx: Int,
        insetBottomPx: Int,
        panelHeightPx: Int,
        marginPx: Int,
    ): YRange {
        val min = insetTopPx + marginPx
        val max = screenHeightPx - insetBottomPx - panelHeightPx - marginPx
        return if (max < min) YRange(min, min) else YRange(min, max)
    }

    /**
     * Android wrapper: the T05 (API 28/29) inset way — screen height from
     * DisplayMetrics, the status bar height resource for the top inset, the
     * navigation bar height resource for the bottom, read defensively (an unknown
     * resource, e.g. under gesture navigation, reads as 0, which the formula
     * accepts). T07 replaces these internals with WindowMetrics/insets.
     */
    fun compute(context: Context, panelHeightPx: Int): YRange {
        val metrics = context.resources.displayMetrics
        return compute(
            screenHeightPx = metrics.heightPixels,
            insetTopPx = systemDimenPx(context, "status_bar_height"),
            insetBottomPx = systemDimenPx(context, "navigation_bar_height"),
            panelHeightPx = panelHeightPx,
            marginPx = Math.round(Tokens.SCREEN_EDGE_MARGIN_DP * metrics.density),
        )
    }

    /**
     * Clamps a window Y into [range]. Pure. Used on every drag move and on restore,
     * so neither the finger nor a stored ratio can push the window under a system bar
     * or off the screen.
     */
    fun clampY(yPx: Float, range: YRange): Float = when {
        yPx < range.minPx -> range.minPx.toFloat()
        yPx > range.maxPx -> range.maxPx.toFloat()
        else -> yPx
    }

    /**
     * The stored position ratio for a window Y, yRatio = (y - min) / (max - min)
     * (01-ui-spec.md section 4). Pure. The input is clamped first so the stored ratio
     * always stays inside 0.0..1.0. A degenerate range (min == max) has no span to
     * divide by; it stores the neutral 0.5 (T05 ruling on the division-by-zero guard:
     * any ratio would restore to the same single position, and 0.5 is the least
     * surprising value to keep around).
     */
    fun yRatioFromY(yPx: Float, range: YRange): Float {
        if (range.maxPx <= range.minPx) return 0.5f
        val clamped = clampY(yPx, range)
        return (clamped - range.minPx) / (range.maxPx - range.minPx)
    }

    /**
     * Restores a window Y from the stored ratio, y = min + ratio * (max - min), then
     * clamped (01-ui-spec.md section 4). Pure. The clamp absorbs a ratio saved on a
     * taller screen or under a different preset, so a restore always lands inside the
     * current range.
     */
    fun yFromRatio(yRatio: Float, range: YRange): Float {
        if (range.maxPx <= range.minPx) return range.minPx.toFloat()
        val y = range.minPx + yRatio * (range.maxPx - range.minPx)
        return clampY(y, range)
    }

    /**
     * A framework-internal dimen by name (the status/navigation bar heights are not
     * public resources, so there is no R entry to read them by); 0 when the resource
     * does not exist — the formula accepts 0, and T07 replaces this lookup with real
     * insets anyway.
     */
    @SuppressLint("DiscouragedApi") // the only way to read a hidden platform dimen; T07 moves to insets
    private fun systemDimenPx(context: Context, name: String): Int {
        val id = context.resources.getIdentifier(name, "dimen", "android")
        return if (id != 0) context.resources.getDimensionPixelSize(id) else 0
    }
}
