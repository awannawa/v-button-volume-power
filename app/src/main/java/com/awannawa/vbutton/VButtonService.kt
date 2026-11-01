package com.awannawa.vbutton

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Owns the overlay window: attaches it when the system binds the service and
 * removes it on unbind/destroy. The config requests no accessibility events,
 * so the service is idle (and costs nothing) unless its overlay is touched.
 * T03 replaces the placeholder box with the real overlay UI.
 */
class VButtonService : AccessibilityService() {

    companion object {
        private const val LOG_TAG = "VButton"

        // DESIGN.md handle fill, so the placeholder already looks like the future handle.
        private const val PLACEHOLDER_COLOR = 0xFF202124.toInt()

        // 01-ui-spec section 3: collapsed window width is the handle's 24dp touch width.
        private const val WINDOW_WIDTH_DP = 24

        // 01-ui-spec section 2: handle height; the placeholder borrows it so the box
        // has a real size under a WRAP_CONTENT window.
        private const val HANDLE_HEIGHT_DP = 72

        /**
         * The only mutable global state AGENTS.md allows. T08 reads it from
         * SettingsActivity to detect whether the service is alive.
         */
        @SuppressLint("StaticFieldLeak") // intentional and sanctioned; nulled in onDestroy
        var instance: VButtonService? = null
            private set
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

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
     * T03 swaps the placeholder for OverlayView and extends the window params here.
     */
    @SuppressLint("RtlHardcoded") // RIGHT is required, see the gravity comment below
    private fun attachOverlay() {
        if (overlayView != null) return
        val manager = windowManager ?: return

        // A plain View would fill the whole WRAP_CONTENT window spec to screen height
        // (View.getDefaultSize returns the full AT_MOST size), so the placeholder pins
        // itself to the handle's locked geometry (24dp x 72dp, 01-ui-spec section 2).
        val placeholder = object : View(this) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                setMeasuredDimension(dp(WINDOW_WIDTH_DP), dp(HANDLE_HEIGHT_DP))
            }
        }
        placeholder.setBackgroundColor(PLACEHOLDER_COLOR)

        val params = WindowManager.LayoutParams(
            dp(WINDOW_WIDTH_DP),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            // RIGHT, not END: END mirrors to the left edge in RTL locales, while this
            // overlay must stay on the physical right edge (PRD section 10).
            gravity = Gravity.RIGHT or Gravity.TOP
        }

        try {
            manager.addView(placeholder, params)
            overlayView = placeholder
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

    /** Converts dp to px with the current density; px values are never cached across config changes. */
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
