package com.awannawa.vbutton

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.awannawa.vbutton.overlay.OverlayView
import com.awannawa.vbutton.overlay.Tokens

/**
 * Owns the overlay window: attaches it when the system binds the service and
 * removes it on unbind/destroy. The config requests no accessibility events,
 * so the service is idle (and costs nothing) unless its overlay is touched.
 * All overlay UI lives in OverlayView and its children.
 */
class VButtonService : AccessibilityService() {

    companion object {
        private const val LOG_TAG = "VButton"

        /**
         * The only mutable global state AGENTS.md allows. T08 reads it from
         * SettingsActivity to detect whether the service is alive.
         */
        @SuppressLint("StaticFieldLeak") // intentional and sanctioned; nulled in onDestroy
        var instance: VButtonService? = null
            private set
    }

    private var windowManager: WindowManager? = null
    private var overlayView: OverlayView? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WindowManager::class.java)
        attachOverlay()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        detachOverlay()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        detachOverlay()
        instance = null
        super.onDestroy()
    }

    // Deliberately empty: the config omits accessibilityEventTypes, so no event is
    // ever delivered (NFR-1).
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    /**
     * Idempotent (AGENTS.md): adds the overlay window once, even if called again.
     * The window starts collapsed (24dp wide) and only OverlayView.setExpanded
     * changes its width afterwards. Actions is created here too, one instance per
     * overlay, so its press-handler registry never grows across rebinds.
     */
    @SuppressLint("RtlHardcoded") // RIGHT is required, see the gravity comment below
    private fun attachOverlay() {
        if (overlayView != null) return
        val manager = windowManager ?: return

        val overlay = OverlayView(this, manager, Actions(this))

        val params = WindowManager.LayoutParams(
            dp(Tokens.COLLAPSED_WINDOW_WIDTH_DP),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // WATCH_OUTSIDE_TOUCH: OverlayView collapses on ACTION_OUTSIDE (FR-5, a tap
            // outside the panel). ORed with NOT_FOCUSABLE, never a replacement.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply {
            // RIGHT, not END: END mirrors to the left edge in RTL locales, while this
            // overlay must stay on the physical right edge (PRD section 10).
            gravity = Gravity.RIGHT or Gravity.TOP
        }

        try {
            manager.addView(overlay, params)
            overlayView = overlay
        } catch (e: IllegalArgumentException) {
            Log.e(LOG_TAG, "addView failed", e)
        }
    }

    /**
     * Idempotent: drops the view reference first so a repeated call is a no-op even
     * if the system had already torn the window down (which makes removeView throw).
     */
    private fun detachOverlay() {
        val view = overlayView ?: return
        overlayView = null
        try {
            windowManager?.removeView(view)
        } catch (e: IllegalArgumentException) {
            Log.w(LOG_TAG, "removeView failed", e)
        }
    }

    /**
     * Converts dp to px with the current density; px values are never cached across
     * config changes. Rounds like the overlay's Tokens.dp (no truncation, so a
     * fractional density from display-size zoom never loses the last pixel).
     */
    private fun dp(value: Int): Int =
        Math.round(value * resources.displayMetrics.density)
}
