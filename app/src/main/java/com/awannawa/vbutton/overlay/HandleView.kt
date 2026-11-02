package com.awannawa.vbutton.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View

/**
 * Draws the collapsed handle: the 5dp x 72dp bar on the anchored screen edge (left or
 * right), while the rest of the view (24dp wide, 01-ui-spec.md section 2) is the touch
 * area [GestureController] listens to. All values come from [Tokens]; the opacity comes
 * from the settings via OverlayView.applyConfig (View.alpha).
 */
class HandleView(
    context: Context,
    private val gestureController: GestureController,
    private val side: () -> ScreenSide,
) : View(context) {

    // One drawable per view instance, never shared, so no mutate() is needed.
    private val bar = GradientDrawable().apply {
        // DESIGN.md section 2: handle fill #FF202124, 1dp #66FFFFFF border, fully rounded ends.
        setColor(Tokens.HANDLE_FILL)
        setStroke(dp(Tokens.BORDER_WIDTH_DP), Tokens.HANDLE_BORDER)
        // Fully rounded ends: the radius is half the bar's own width (derivation, not a new token).
        cornerRadius = dp(Tokens.HANDLE_BAR_WIDTH_DP) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        // Bounds are set here, not in onSizeChanged, so a live side change (invalidate from
        // OverlayView.recomputeLayout) moves the bar to the new edge on the next frame. Canvas
        // coordinates run left-to-right in every locale, so 0 is the physical left bound and
        // width is the physical right bound (same reasoning as the window's physical Gravity,
        // PRD section 10).
        val barPx = dp(Tokens.HANDLE_BAR_WIDTH_DP)
        val left = handleBarLeftPx(side(), width, barPx)
        bar.setBounds(left, 0, left + barPx, height)
        bar.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Forward the raw stream to the gesture controller (AGENTS.md: gesture logic
        // lives in GestureController, not in Views) and always consume: no touch on
        // the handle may leak to the app underneath (task T04).
        gestureController.onTouch(event)
        return true
    }
}
