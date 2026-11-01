package com.awannawa.vbutton.overlay

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.StateSet
import android.widget.ImageView
import android.widget.LinearLayout

/**
 * The vertical button panel. Renders whatever [ButtonSpec] list it is given in the
 * given size preset — it knows nothing about volume or power (AGENTS.md), so T11
 * adds buttons without touching this file. Rendering only: the actions live in the
 * spec lambdas, and no View here ever reaches the service or AudioManager.
 */
class PanelView(
    context: Context,
    buttons: List<ButtonSpec>,
    preset: Tokens.SizePreset,
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        background = panelBackground()
        // DESIGN.md section 2, spacing: panel padding 6dp vertical, 4dp horizontal.
        val padH = dp(Tokens.PANEL_PADDING_HORIZONTAL_DP)
        val padV = dp(Tokens.PANEL_PADDING_VERTICAL_DP)
        setPadding(padH, padV, padH, padV)
        buttons.forEachIndexed { index, spec ->
            addView(buttonFor(spec, preset), buttonParams(preset, index > 0))
        }
    }

    /** DESIGN.md section 2: panel fill #CC202124, 16dp corner radius, 1dp #33FFFFFF border. */
    private fun panelBackground() = GradientDrawable().apply {
        setColor(Tokens.PANEL_FILL)
        setCornerRadius(dp(Tokens.PANEL_CORNER_RADIUS_DP).toFloat())
        setStroke(dp(Tokens.BORDER_WIDTH_DP), Tokens.PANEL_BORDER)
    }

    /** Square button of the preset size; every button after the first gets the 4dp gap. */
    private fun buttonParams(preset: Tokens.SizePreset, afterFirst: Boolean) =
        LayoutParams(dp(preset.buttonDp), dp(preset.buttonDp)).apply {
            if (afterFirst) topMargin = dp(Tokens.PANEL_GAP_DP)
        }

    private fun buttonFor(spec: ButtonSpec, preset: Tokens.SizePreset) = ImageView(context).apply {
        setImageResource(spec.iconRes)
        // 01-ui-spec.md section 1: icon files are white; the final color is applied
        // through imageTintList so T08 can customize it from settings.
        imageTintList = ColorStateList.valueOf(Tokens.ICON)
        // Padding shrinks the content box to the preset's icon size so the 24-grid
        // vector renders at iconDp instead of being scaled up to the button size
        // (FIT_CENTER fills whatever box it gets). The inset is derived:
        // (button - icon) / 2, no new token.
        val inset = dp((preset.buttonDp - preset.iconDp) / 2)
        setPadding(inset, inset, inset, inset)
        scaleType = ImageView.ScaleType.FIT_CENTER
        // DESIGN.md section 2: pressed highlight is a #33FFFFFF circle behind the icon,
        // shown instantly through the standard pressed state; the 100 ms fade-out is
        // T06 interaction polish.
        background = pressedHighlight()
        contentDescription = context.getString(spec.contentDescriptionRes)
        setOnClickListener { spec.tapAction() }
        setOnLongClickListener {
            spec.longPressAction()
            true // consumed, so a long press does not also fire the tap action
        }
    }

    /**
     * Fresh StateListDrawable per button: a shared instance would light up every
     * button when one is pressed. Default state is a plain transparent drawable.
     */
    private fun pressedHighlight() = StateListDrawable().apply {
        addState(
            intArrayOf(android.R.attr.state_pressed),
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Tokens.PRESSED_HIGHLIGHT)
            },
        )
        addState(StateSet.WILD_CARD, ColorDrawable(Color.TRANSPARENT))
    }
}
