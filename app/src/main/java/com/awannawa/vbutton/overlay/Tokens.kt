package com.awannawa.vbutton.overlay

import android.view.View

/**
 * Single source of truth for every size, color, and duration used by the overlay
 * (AGENTS.md maintainability rules). Every value below is copied from DESIGN.md
 * section 2 (locked visual tokens) or 01-ui-spec.md sections 2-3 (handle geometry,
 * size presets); the comment on each value names its source. A value that is not
 * defined in one of those documents must not be added here — ask the orchestrator,
 * who updates DESIGN.md first. Derived arithmetic on two locked values (for example
 * half of a locked border width) is allowed but must be written as derivation, not
 * as a new literal.
 */
object Tokens {

    /**
     * One row of the preset table (01-ui-spec.md section 3). Presets are data, not
     * scattered constants, so T07/T08 can make the preset selectable without
     * touching the views.
     */
    data class SizePreset(
        val buttonDp: Int,
        val iconDp: Int,
        val panelWidthDp: Int,
        val panelHeightDp: Int,
    )

    // 01-ui-spec.md section 3: presets S, M, L.
    val PRESET_S = SizePreset(buttonDp = 48, iconDp = 22, panelWidthDp = 56, panelHeightDp = 164)
    val PRESET_M = SizePreset(buttonDp = 52, iconDp = 24, panelWidthDp = 60, panelHeightDp = 176)
    val PRESET_L = SizePreset(buttonDp = 56, iconDp = 26, panelWidthDp = 64, panelHeightDp = 188)

    /** 01-ui-spec.md section 3 names M the default; T07/T08 make the preset selectable. */
    val activePreset: SizePreset = PRESET_M

    // 01-ui-spec.md section 2: handle geometry. The 24dp touch area is not a separate
    // constant because it is the collapsed window width itself (section 3: the window
    // is 24dp while collapsed); HandleView simply fills the window.
    const val HANDLE_BAR_WIDTH_DP = 5
    const val HANDLE_HEIGHT_DP = 72

    // 01-ui-spec.md section 3: window width while collapsed.
    const val COLLAPSED_WINDOW_WIDTH_DP = 24

    // DESIGN.md section 2, color: no other colors are allowed in the overlay.
    const val HANDLE_FILL = 0xFF202124.toInt()
    const val HANDLE_BORDER = 0x66FFFFFF.toInt()
    const val PANEL_FILL = 0xCC202124.toInt()
    const val PANEL_BORDER = 0x33FFFFFF.toInt()
    const val ICON = 0xFFFFFFFF.toInt()
    const val PRESSED_HIGHLIGHT = 0x33FFFFFF.toInt()

    // DESIGN.md section 2, shape and spacing.
    const val PANEL_CORNER_RADIUS_DP = 16
    const val PANEL_PADDING_VERTICAL_DP = 6
    const val PANEL_PADDING_HORIZONTAL_DP = 4
    const val PANEL_GAP_DP = 4

    /** DESIGN.md section 2: both overlay borders are 1dp. */
    const val BORDER_WIDTH_DP = 1

    /** 01-ui-spec.md section 2: default handle alpha; the opacity setting arrives in T08. */
    const val HANDLE_ALPHA_DEFAULT = 0.6f

    /** DESIGN.md section 2, motion: open and close duration. */
    const val EXPAND_COLLAPSE_MS = 150L

    /** DESIGN.md section 2, motion: the pressed highlight fades out on release. */
    const val PRESS_FADE_OUT_MS = 100L
}

/**
 * dp to px with the current density for the overlay views. Uses Math.round instead
 * of truncation so fractional intermediate values (borders on scaled densities)
 * never lose half a pixel. px values are never cached across configuration changes
 * (01-ui-spec.md section 4). Receiver is View because that is what every overlay
 * class is (a View is not a Context); VButtonService keeps its own Context-based
 * helper for the window params it creates before any view exists.
 */
internal fun View.dp(value: Int): Int =
    Math.round(value * resources.displayMetrics.density)
