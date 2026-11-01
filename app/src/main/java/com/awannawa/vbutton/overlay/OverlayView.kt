package com.awannawa.vbutton.overlay

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.awannawa.vbutton.Actions
import com.awannawa.vbutton.Prefs
import com.awannawa.vbutton.R

private const val LOG_TAG = "VButton"

/**
 * Root of the overlay window: the [HandleView] while collapsed, the [PanelView]
 * while expanded, both top-aligned so the panel appears at the handle's Y position
 * (PRD FR-3). Owns the collapsed/expanded switch: visibility plus the window width
 * (24dp collapsed, active preset's panel width expanded, 01-ui-spec.md section 3)
 * through WindowManager.updateViewLayout.
 *
 * Expanding comes from [GestureController] (tap or left fling on the handle, FR-4).
 * Collapsing comes from the auto-collapse timer or a tap outside the panel
 * (ACTION_OUTSIDE, FR-5). Settings (T08) will call [applyConfig]. The panel buttons
 * act through the [Actions] instance the service passes in (T06); this view never
 * touches AudioManager or the service itself (AGENTS.md).
 */
class OverlayView(
    context: Context,
    private val windowManager: WindowManager,
    private val actions: Actions,
) : FrameLayout(context) {

    /**
     * One explicit state, per AGENTS.md. Dragging joins this enum in T05; until then
     * a two-state enum keeps setExpanded free of interdependent booleans.
     */
    private enum class State { Collapsed, Expanded }

    /**
     * FR-5 auto-collapse: at most one pending callback, and only while expanded
     * (NFR-1). Removed when collapsed, when reset (remove-before-post), and on
     * detach, so no callback can outlive an expanded, attached overlay.
     */
    private val autoCollapseHandler = Handler(Looper.getMainLooper())
    private val autoCollapseRunnable = Runnable { setExpanded(false) }

    // The controller is created first: HandleView forwards its touch stream to it.
    private val gestureController = GestureController(context) { setExpanded(true) }
    private val handleView = HandleView(context, gestureController)
    private val panelView = PanelView(context, defaultButtons(), Tokens.activePreset)
    private var state = State.Collapsed

    // DESIGN.md section 2, motion locks one curve for open and close.
    private val motionInterpolator = DecelerateInterpolator()

    init {
        addView(handleView, LayoutParams(LayoutParams.MATCH_PARENT, dp(Tokens.HANDLE_HEIGHT_DP)))
        addView(panelView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        panelView.visibility = GONE
        // Park the panel fully outside the window so the first expand animates a real
        // slide-in from the right instead of popping into place.
        panelView.translationX = dp(Tokens.activePreset.panelWidthDp).toFloat()
        applyConfig()
    }

    /**
     * Idempotent (AGENTS.md): repeated calls with the same target do nothing, and a
     * call during a running animation simply re-targets it (ViewPropertyAnimator
     * replaces the previous animation on the same view).
     */
    fun setExpanded(expanded: Boolean) {
        val target = if (expanded) State.Expanded else State.Collapsed
        if (state == target) return
        state = target
        if (expanded) {
            handleView.visibility = GONE
            panelView.visibility = VISIBLE
            resizeWindow(dp(Tokens.activePreset.panelWidthDp))
            panelView.animate()
                .translationX(0f)
                .setDuration(Tokens.EXPAND_COLLAPSE_MS)
                .setInterpolator(motionInterpolator)
            scheduleAutoCollapse()
        } else {
            cancelAutoCollapse()
            // The handle sits under the panel (same dark fill, panel is on top), so it
            // can become visible now and is only revealed as the panel slides right.
            handleView.visibility = VISIBLE
            panelView.animate()
                .translationX(dp(Tokens.activePreset.panelWidthDp).toFloat())
                .setDuration(Tokens.EXPAND_COLLAPSE_MS)
                .setInterpolator(motionInterpolator)
                .withEndAction {
                    // Guard: a re-expand mid-animation cancels this one and suppresses
                    // the end action, but the guard keeps the swap correct even if a
                    // platform build ever ran it.
                    if (state == State.Collapsed) {
                        panelView.visibility = GONE
                        resizeWindow(dp(Tokens.COLLAPSED_WINDOW_WIDTH_DP))
                    }
                }
        }
    }

    /**
     * Restarts the auto-collapse countdown (FR-5). Called from this view's own
     * ButtonSpec wiring on every volume action (T06) so the panel does not close
     * while the user is still holding a volume button. A no-op while collapsed so no
     * callback can ever be scheduled outside an expanded overlay (NFR-1). (Power
     * actions need no reset: the panel touch itself already pushed the timer back on
     * DOWN, and a power tap locks the screen.)
     */
    fun resetAutoCollapse() {
        if (state != State.Expanded) return
        scheduleAutoCollapse()
    }

    /** At most one pending callback; remove-before-post makes rescheduling idempotent. */
    private fun scheduleAutoCollapse() {
        autoCollapseHandler.removeCallbacks(autoCollapseRunnable)
        // Read at schedule time, so a timeout change from T08 applies from the next
        // expand onward without recreating anything.
        autoCollapseHandler.postDelayed(autoCollapseRunnable, Prefs.autoCollapseTimeoutMs(context))
    }

    private fun cancelAutoCollapse() {
        autoCollapseHandler.removeCallbacks(autoCollapseRunnable)
    }

    /**
     * Every touch on the panel passes through here on its way to the buttons, so this
     * is the one hook that sees "any touch on the panel" (FR-5) and restarts the
     * countdown. Never intercepts: the children keep their own touch handling. While
     * collapsed the state guard makes handle touches a no-op here — opening the panel
     * must never schedule or reset a collapse timer (task T04).
     */
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (state == State.Expanded) resetAutoCollapse()
        return false
    }

    /**
     * Only events with no child target reach this view — in practice ACTION_OUTSIDE,
     * delivered because VButtonService ORs FLAG_WATCH_OUTSIDE_TOUCH into the window
     * flags. Collapse only while expanded: while collapsed an outside touch must be a
     * no-op, so the handle never collapse-flickers on touches in the app underneath
     * (task T04).
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_OUTSIDE && state == State.Expanded) {
            setExpanded(false)
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        // No scheduled callback may outlive the view (AGENTS.md lifecycle): the
        // service detaches the overlay on unbind/destroy, so this kills any pending
        // collapse when the service is turned off with the panel open.
        autoCollapseHandler.removeCallbacks(autoCollapseRunnable)
        // A finger can still be holding a panel button when the service is turned
        // off: stop its repeat/long-press timers too, so no press handler keeps
        // running after the view is gone (T06 acceptance criteria).
        actions.cancelPendingPresses()
        super.onDetachedFromWindow()
    }

    /**
     * Single entry point for settings (AGENTS.md): T08 pushes changes here instead of
     * recreating views. Only the handle opacity has a source today; T08 extends this
     * to read Prefs for the preset, opacity, and anything else. Idempotent.
     */
    fun applyConfig() {
        handleView.alpha = Tokens.HANDLE_ALPHA_DEFAULT
    }

    /**
     * Window width switch for [setExpanded]. layoutParams is the WindowManager
     *.LayoutParams the service attached us with, so editing it in place and calling
     * updateViewLayout keeps gravity and flags untouched.
     */
    private fun resizeWindow(widthPx: Int) {
        val params = layoutParams as? WindowManager.LayoutParams ?: return
        if (params.width == widthPx) return // idempotent: no redundant IPC
        params.width = widthPx
        try {
            windowManager.updateViewLayout(this, params)
        } catch (e: IllegalArgumentException) {
            // Throws if the window is already gone (service tearing down); not fatal.
            Log.e(LOG_TAG, "updateViewLayout failed", e)
        }
    }

    /**
     * The v1 buttons (PRD FR-7/FR-8: volume holds repeat, power does not). Each spec
     * declares its semantic actions (tapAction/longPressAction — also the TalkBack
     * activation path) and gets its physical press lifecycle from
     * [Actions.pressHandler]; T11 adds items to this list only.
     */
    private fun defaultButtons(): List<ButtonSpec> {
        /** One button row: the actions are wired once and shared by both paths. */
        fun button(
            id: String,
            iconRes: Int,
            contentDescriptionRes: Int,
            tap: () -> Unit,
            longPress: () -> Unit,
            repeatable: Boolean,
        ) = ButtonSpec(
            id = id,
            iconRes = iconRes,
            contentDescriptionRes = contentDescriptionRes,
            tapAction = tap,
            longPressAction = longPress,
            repeatable = repeatable,
            pressAction = actions.pressHandler(tap, longPress, repeatable),
        )

        return listOf(
            button(
                id = "volume_up",
                iconRes = R.drawable.ic_volume_up,
                contentDescriptionRes = R.string.button_volume_up,
                // T06: every volume action pushes the auto-collapse back so the panel
                // does not close while the user is still pressing.
                tap = {
                    actions.volumeUp()
                    resetAutoCollapse()
                },
                longPress = {}, // volume has no long-press semantics (FR-7: hold repeats)
                repeatable = true,
            ),
            button(
                id = "volume_down",
                iconRes = R.drawable.ic_volume_down,
                contentDescriptionRes = R.string.button_volume_down,
                tap = {
                    actions.volumeDown()
                    resetAutoCollapse()
                },
                longPress = {},
                repeatable = true,
            ),
            button(
                id = "power",
                iconRes = R.drawable.ic_power,
                contentDescriptionRes = R.string.button_power,
                tap = { actions.powerTap() },
                longPress = { actions.powerLongPress() },
                repeatable = false,
            ),
        )
    }
}
