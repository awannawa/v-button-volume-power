package com.awannawa.vbutton.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.view.View

/**
 * Draws the collapsed handle: the 5dp x 72dp bar on the RIGHT edge of its bounds,
 * while the rest of the view (24dp wide, 01-ui-spec.md section 2) is touch area for
 * the gestures T04 adds. All values come from [Tokens]; the opacity comes from the
 * settings via OverlayView.applyConfig (View.alpha).
 */
class HandleView(context: Context) : View(context) {

    // One drawable per view instance, never shared, so no mutate() is needed.
    private val bar = GradientDrawable().apply {
        // DESIGN.md section 2: handle fill #FF202124, 1dp #66FFFFFF border, fully rounded ends.
        setColor(Tokens.HANDLE_FILL)
        setStroke(dp(Tokens.BORDER_WIDTH_DP), Tokens.HANDLE_BORDER)
        // Fully rounded ends: the radius is half the bar's own width (derivation, not a new token).
        cornerRadius = dp(Tokens.HANDLE_BAR_WIDTH_DP) / 2f
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        // The bar hugs the view's right edge. Canvas coordinates run left-to-right in
        // every locale, so this stays on the physical right edge in RTL too (same
        // reasoning as the window's Gravity.RIGHT, PRD section 10).
        bar.setBounds(
            width - dp(Tokens.HANDLE_BAR_WIDTH_DP),
            0,
            width,
            height,
        )
    }

    override fun onDraw(canvas: Canvas) {
        bar.draw(canvas)
    }
}
