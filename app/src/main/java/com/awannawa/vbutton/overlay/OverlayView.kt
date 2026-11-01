package com.awannawa.vbutton.overlay

import android.content.Context
import android.util.Log
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.awannawa.vbutton.R

private const val LOG_TAG = "VButton"

/**
 * Root of the overlay window: the [HandleView] while collapsed, the [PanelView]
 * while expanded, both top-aligned so the panel appears at the handle's Y position
 * (PRD FR-3). Owns the collapsed/expanded switch: visibility plus the window width
 * (24dp collapsed, active preset's panel width expanded, 01-ui-spec.md section 3)
 * through WindowManager.updateViewLayout.
 *
 * Gestures (T04) will call [setExpanded]; settings (T08) will call [applyConfig].
 */
class OverlayView(
    context: Context,
    private val windowManager: WindowManager,
) : FrameLayout(context) {

    /**
     * One explicit state, per AGENTS.md. Dragging joins this enum in T05; until then
     * a two-state enum keeps setExpanded free of interdependent booleans.
     */
    private enum class State { Collapsed, Expanded }

    private val handleView = HandleView(context)
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
        } else {
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
     * The v1 buttons (PRD FR-7/FR-8: volume holds repeat, power does not). The action
     * lambdas are no-ops until T06 wires real Actions through them; T11 adds items
     * to this list only.
     */
    private fun defaultButtons(): List<ButtonSpec> = listOf(
        ButtonSpec(
            id = "volume_up",
            iconRes = R.drawable.ic_volume_up,
            contentDescriptionRes = R.string.button_volume_up,
            tapAction = {},
            longPressAction = {},
            repeatable = true,
        ),
        ButtonSpec(
            id = "volume_down",
            iconRes = R.drawable.ic_volume_down,
            contentDescriptionRes = R.string.button_volume_down,
            tapAction = {},
            longPressAction = {},
            repeatable = true,
        ),
        ButtonSpec(
            id = "power",
            iconRes = R.drawable.ic_power,
            contentDescriptionRes = R.string.button_power,
            tapAction = {},
            longPressAction = {},
            repeatable = false,
        ),
    )
}
