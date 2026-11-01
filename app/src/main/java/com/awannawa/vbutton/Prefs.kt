package com.awannawa.vbutton

import android.content.Context

/**
 * Single source of truth for every SharedPreferences key and its default (AGENTS.md).
 * T05 (position) and T08 (settings) add their keys here only.
 */
object Prefs {
    const val PREFS_FILE = "vbutton_prefs"

    /**
     * FR-5: seconds without interaction before the expanded panel auto-collapses,
     * in ms. The write path (settings UI) arrives in T08; until then the default
     * always applies because there is nothing to store a different value.
     */
    private const val KEY_AUTO_COLLAPSE_TIMEOUT_MS = "auto_collapse_timeout_ms"
    const val AUTO_COLLAPSE_TIMEOUT_MS_DEFAULT = 3000L

    /** Stored timeout, or the FR-5 default (3000 ms) when nothing was stored yet. */
    fun autoCollapseTimeoutMs(context: Context): Long =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getLong(KEY_AUTO_COLLAPSE_TIMEOUT_MS, AUTO_COLLAPSE_TIMEOUT_MS_DEFAULT)

    /**
     * FR-9: short haptic feedback when a panel button is pressed. The write path
     * (settings toggle) arrives in T08; until then the default always applies.
     */
    private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    const val HAPTICS_ENABLED_DEFAULT = true

    /** FR-9 haptics setting, or the default (on) when nothing was stored yet. */
    fun hapticsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAPTICS_ENABLED, HAPTICS_ENABLED_DEFAULT)

    /**
     * FR-6: the overlay's vertical position, stored as a ratio (0.0 to 1.0) of the
     * valid safe-area range — never px (PRD section 9, 01-ui-spec.md section 4), so
     * the position survives rotation, a display-size change, and reboots. The px
     * conversions live in SafeArea; this file only owns the key and its default.
     */
    private const val KEY_Y_RATIO = "y_ratio"
    const val Y_RATIO_DEFAULT = 0.4f

    /** Saved position ratio, or the default (0.4, upper-middle area) when nothing was stored yet. */
    fun yRatio(context: Context): Float =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getFloat(KEY_Y_RATIO, Y_RATIO_DEFAULT)

    /** Saves the position ratio once per drag release (FR-6). Written only by the overlay. */
    fun setYRatio(context: Context, ratio: Float) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_Y_RATIO, ratio)
            .apply()
    }
}
