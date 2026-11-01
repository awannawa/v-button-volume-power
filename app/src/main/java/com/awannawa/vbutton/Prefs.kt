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
     * in ms. T08's settings UI writes it; the collapse timer reads it at schedule
     * time (OverlayView.scheduleAutoCollapse), so a change applies from the next
     * expand without recreating anything.
     *
     * Ruled bound (T08): SeekBar 1-10 seconds, step 1 s — [AUTO_COLLAPSE_TIMEOUT_MIN_MS]
     * to [AUTO_COLLAPSE_TIMEOUT_MAX_MS]. The reader clamps to this bound so only
     * values the ruled UI can write ever reach the timer.
     */
    private const val KEY_AUTO_COLLAPSE_TIMEOUT_MS = "auto_collapse_timeout_ms"
    const val AUTO_COLLAPSE_TIMEOUT_MS_DEFAULT = 3000L
    const val AUTO_COLLAPSE_TIMEOUT_MIN_MS = 1000L
    const val AUTO_COLLAPSE_TIMEOUT_MAX_MS = 10000L

    /** Stored timeout clamped to the ruled 1-10 s bound, or the default (3000 ms). */
    fun autoCollapseTimeoutMs(context: Context): Long =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getLong(KEY_AUTO_COLLAPSE_TIMEOUT_MS, AUTO_COLLAPSE_TIMEOUT_MS_DEFAULT)
            .coerceIn(AUTO_COLLAPSE_TIMEOUT_MIN_MS, AUTO_COLLAPSE_TIMEOUT_MAX_MS)

    /** Writes the ruled-bound timeout. Only the settings UI (T08) writes this. */
    fun setAutoCollapseTimeoutMs(context: Context, timeoutMs: Long) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_AUTO_COLLAPSE_TIMEOUT_MS, timeoutMs)
            .apply()
    }

    /**
     * FR-9: short haptic feedback when a panel button is pressed. T08's settings UI
     * writes it; the overlay reads it at the moment of the press (T06).
     */
    private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    const val HAPTICS_ENABLED_DEFAULT = true

    /** FR-9 haptics setting, or the default (on) when nothing was stored yet. */
    fun hapticsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAPTICS_ENABLED, HAPTICS_ENABLED_DEFAULT)

    /** Writes the haptics toggle. Only the settings UI (T08) writes this. */
    fun setHapticsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAPTICS_ENABLED, enabled)
            .apply()
    }

    /**
     * T08 (FR-10): handle opacity as an Int percent, 30-100 — the number the opacity
     * SeekBar shows and writes (per-100 by definition of percent; the overlay converts
     * to an alpha fraction by dividing by 100). 01-ui-spec.md section 2: default 0.6
     * alpha, range 0.3-1.0, so the percent default is 60 and the bound 30-100. The
     * reader clamps to that bound; no other value is writable through the ruled UI.
     */
    private const val KEY_HANDLE_OPACITY_PERCENT = "handle_opacity_percent"
    const val HANDLE_OPACITY_PERCENT_DEFAULT = 60
    const val HANDLE_OPACITY_PERCENT_MIN = 30
    const val HANDLE_OPACITY_PERCENT_MAX = 100

    /** Stored opacity percent clamped to 30-100, or the default (60). */
    fun handleOpacityPercent(context: Context): Int =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getInt(KEY_HANDLE_OPACITY_PERCENT, HANDLE_OPACITY_PERCENT_DEFAULT)
            .coerceIn(HANDLE_OPACITY_PERCENT_MIN, HANDLE_OPACITY_PERCENT_MAX)

    /** Writes the opacity percent. Only the settings UI (T08) writes this. */
    fun setHandleOpacityPercent(context: Context, percent: Int) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_HANDLE_OPACITY_PERCENT, percent)
            .apply()
    }

    /**
     * T08 (FR-10): the size preset as one of [SIZE_PRESET_SMALL], [SIZE_PRESET_MEDIUM]
     * or [SIZE_PRESET_LARGE] — the exact strings 01-ui-spec.md section 3 names the
     * presets by; mapping to the locked Tokens preset data happens in the overlay
     * (same pattern as the y-ratio conversions living in SafeArea, not here). Compact
     * mode may step the rendered preset down for a short screen but never rewrites
     * this stored value: the user's choice is the desired preset, not the rendered one.
     */
    private const val KEY_SIZE_PRESET = "size_preset"
    const val SIZE_PRESET_SMALL = "S"
    const val SIZE_PRESET_MEDIUM = "M"
    const val SIZE_PRESET_LARGE = "L"
    const val SIZE_PRESET_DEFAULT = SIZE_PRESET_MEDIUM

    /** Stored preset string, or [SIZE_PRESET_DEFAULT] ("M") when nothing was stored yet. */
    fun sizePreset(context: Context): String =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getString(KEY_SIZE_PRESET, SIZE_PRESET_DEFAULT) ?: SIZE_PRESET_DEFAULT

    /** Writes the preset string. Only the settings UI (T08) writes this. */
    fun setSizePreset(context: Context, preset: String) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SIZE_PRESET, preset)
            .apply()
    }

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

    /**
     * Saves the position ratio once per drag release (FR-6), or the default from
     * T08's "Reset position" — the overlay's applyConfig re-clamps and repositions.
     */
    fun setYRatio(context: Context, ratio: Float) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_Y_RATIO, ratio)
            .apply()
    }
}
