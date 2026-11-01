package com.awannawa.vbutton.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics

/**
 * The overlay's valid Y range, the yRatio <-> px conversions, and the compact-mode
 * preset decision (PRD FR-6, PRD section 9, 01-ui-spec.md section 4). One
 * responsibility: position and screen-fit math. Everything except [readScreenInsets]
 * and the [compute] context wrapper is pure and JVM-tested (AGENTS.md); T07 upgraded
 * the inset lookup (WindowMetrics + display cutout on API 30+) without changing any
 * signature T05 established, so no caller and no existing test changed with it.
 */
internal object SafeArea {

    /**
     * Valid top offsets for the overlay window, in px. The window has gravity TOP, so
     * a Y value is the window's top edge measured from the top of the screen.
     */
    data class YRange(val minPx: Int, val maxPx: Int)

    /**
     * One reading of the screen for the current configuration, in px: the height the Y
     * range is measured against, plus the insets the overlay must stay clear of on the
     * edges that matter. [rightInsetPx] feeds the window's X offset (01-ui-spec.md
     * section 4: the navigation bar on the right in landscape with 3-button
     * navigation, or a cutout — 0 when nothing intrudes there, as in portrait).
     */
    data class ScreenInsets(
        val screenHeightPx: Int,
        val insetTopPx: Int,
        val insetBottomPx: Int,
        val rightInsetPx: Int,
    )

    /**
     * Pure core (AGENTS.md: pure logic separated from Android APIs and covered by JVM
     * unit tests). 01-ui-spec.md section 4: valid Y runs from insetTop + margin to
     * screenHeight - insetBottom - panelHeight - margin — the whole window (the handle
     * while collapsed, the panel while expanded) stays clear of the status bar, the
     * navigation bar, and the screen edges.
     *
     * A screen too short for the panel (max < min) collapses to min == max instead of
     * returning an inverted range: every caller clamps against [YRange], so the
     * overlay still lands on one valid position. Compact mode ([selectFittingPreset])
     * steps the preset down first, so this degenerate case only remains for screens
     * where even preset S does not fit.
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
     * Reads [ScreenInsets] for the current configuration.
     *
     * API 30+ (01-ui-spec.md section 4): WindowMetrics — the metrics of the frame this
     * window actually occupies right now, so a rotation or a move into a multi-window
     * region (basic multi-window, task scope) re-reads the true height and insets.
     * systemBars and displayCutout are queried separately and combined per side with
     * the explicit max: the overlay must clear whichever type reaches further on each
     * edge. currentWindowMetrics (not maximum) because the overlay must fit the region
     * it is shown in.
     *
     * Fallback for API 28/29 (kept per 01-ui-spec.md section 4): DisplayMetrics plus
     * the status/navigation bar height resources. Which side the navigation bar sits
     * on in landscape is not exposed by those resources, so the right inset reads 0
     * there; the primary device (Android 12+) always takes the WindowMetrics path.
     */
    fun readScreenInsets(context: Context): ScreenInsets {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val manager = context.getSystemService(WindowManager::class.java)
            if (manager != null) {
                val metrics: WindowMetrics = manager.currentWindowMetrics
                val windowInsets = metrics.windowInsets
                val bars = windowInsets.getInsets(WindowInsets.Type.systemBars())
                val cutout = windowInsets.getInsets(WindowInsets.Type.displayCutout())
                return ScreenInsets(
                    screenHeightPx = metrics.bounds.height(),
                    insetTopPx = maxOf(bars.top, cutout.top),
                    insetBottomPx = maxOf(bars.bottom, cutout.bottom),
                    rightInsetPx = maxOf(bars.right, cutout.right),
                )
            }
        }
        val metrics = context.resources.displayMetrics
        return ScreenInsets(
            screenHeightPx = metrics.heightPixels,
            insetTopPx = systemDimenPx(context, "status_bar_height"),
            insetBottomPx = systemDimenPx(context, "navigation_bar_height"),
            rightInsetPx = 0,
        )
    }

    /**
     * Android wrapper, same signature T05 left it: fresh [ScreenInsets] plus the
     * locked screen-edge margin. Called per drag start (OverlayView.startDrag), so a
     * drag always clamps against the configuration it runs in — including system-bar
     * visibility changes that are not configuration changes (fullscreen apps).
     */
    fun compute(context: Context, panelHeightPx: Int): YRange {
        val screen = readScreenInsets(context)
        return compute(
            screen,
            panelHeightPx,
            marginPx = Math.round(Tokens.SCREEN_EDGE_MARGIN_DP * context.resources.displayMetrics.density),
        )
    }

    /** [compute] for callers that already hold a [ScreenInsets] reading. */
    fun compute(screen: ScreenInsets, panelHeightPx: Int, marginPx: Int): YRange =
        compute(screen.screenHeightPx, screen.insetTopPx, screen.insetBottomPx, panelHeightPx, marginPx)

    /**
     * The largest panel height that fits between the insets and the margins, in px —
     * the same measure SafeArea.compute uses: a panel of exactly this height leaves
     * max == min (still a valid, non-inverted range), one pixel more degenerates it.
     * Pure. This is compact mode's "fits" input.
     */
    fun availableHeightPx(screen: ScreenInsets, marginPx: Int): Int =
        screen.screenHeightPx - screen.insetTopPx - screen.insetBottomPx - 2 * marginPx

    /**
     * Compact mode (01-ui-spec.md section 4): when the active preset's panel does not
     * fit in [availableHeightPx], step down one preset at a time until one does,
     * preset S at minimum. Pure (presets are locked data in Tokens).
     *
     * "Fits" is the panel-fits-on-screen measure of [availableHeightPx]: compact mode
     * exists so the panel never has to hang off the screen (the case where compute
     * degenerates), not to guarantee a draggable span. When even S does not fit, S is
     * returned and compute's degenerate range does the clamping — the documented
     * floor.
     */
    fun selectFittingPreset(
        availableHeightPx: Int,
        activePreset: Tokens.SizePreset,
        density: Float,
    ): Tokens.SizePreset {
        var chosen = activePreset
        while (Math.round(chosen.panelHeightDp * density) > availableHeightPx) {
            chosen = smallerPreset(chosen) ?: break // S is the floor
        }
        return chosen
    }

    /** One step down [Tokens.PRESETS_SMALLEST_FIRST]; null when already at S. */
    private fun smallerPreset(preset: Tokens.SizePreset): Tokens.SizePreset? {
        val ladder = Tokens.PRESETS_SMALLEST_FIRST
        val index = ladder.indexOf(preset)
        return if (index > 0) ladder[index - 1] else null
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
     * does not exist — the formula accepts 0. Only reached by the API 28/29 fallback
     * in [readScreenInsets].
     */
    @SuppressLint("DiscouragedApi") // the only way to read a hidden platform dimen (28/29 fallback only)
    private fun systemDimenPx(context: Context, name: String): Int {
        val id = context.resources.getIdentifier(name, "dimen", "android")
        return if (id != 0) context.resources.getDimensionPixelSize(id) else 0
    }
}
