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
}
