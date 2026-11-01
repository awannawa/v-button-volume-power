package com.awannawa.vbutton.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.StateSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import com.awannawa.vbutton.Prefs

/**
 * The vertical button panel. Renders whatever [ButtonSpec] list it is given in the
 * given size preset — it knows nothing about volume or power (AGENTS.md), so T11
 * adds buttons without touching this file. The actions live in the spec lambdas
 * ([ButtonSpec.pressAction] drives hold-to-repeat and the power long-press through
 * Actions, T06), and no View here ever reaches the service or AudioManager.
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
            addView(PanelButton(context, spec, preset), buttonParams(preset, index > 0))
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
}

/**
 * One panel button: the T03 rendering (spec icon, tint, pressed highlight, a11y
 * listeners) extended with the physical press lifecycle (T06). The raw touch stream
 * is translated to [ButtonPress] events for the spec's pressAction, so Actions can
 * drive hold-to-repeat and the tap-or-long-press split; the haptic on press (FR-9)
 * happens here because only a View can perform haptic feedback.
 *
 * Click and long-click stay wired to the spec's tap/long-press actions for
 * accessibility: TalkBack activates through performClick/performLongClick rather
 * than a raw touch stream, which onTouchEvent below consumes — so the two paths can
 * never double-fire.
 */
private class PanelButton(
    context: Context,
    private val spec: ButtonSpec,
    preset: Tokens.SizePreset,
) : ImageView(context) {

    init {
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
        // DESIGN.md section 2: pressed highlight is a #33FFFFFF circle behind the icon;
        // it appears instantly (set by hand in onTouchEvent) and fades out on release
        // over Tokens.PRESS_FADE_OUT_MS (same section, motion) via pressFadeOut below.
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

    /**
     * DESIGN.md section 2, motion: on release the highlight fades out over 100 ms.
     * The fade animates the background drawable's alpha while the pressed state is
     * held, so the circle fades out behind the icon (a view-level fade would dim the
     * icon too, and a foreground overlay would draw over it); the pressed state
     * clears when the fade ends. Main thread only, 100 ms, nothing runs while idle.
     */
    private val pressFadeOut = ValueAnimator.ofInt(255, 0).apply {
        duration = Tokens.PRESS_FADE_OUT_MS
        interpolator = LinearInterpolator()
        addUpdateListener { animation -> background?.alpha = animation.animatedValue as Int }
        addListener(object : AnimatorListenerAdapter() {
            private var cancelled = false

            override fun onAnimationCancel(animation: Animator) {
                cancelled = true
            }

            override fun onAnimationEnd(animation: Animator) {
                // Full strength again so the next press appears instantly.
                background?.alpha = 255
                // Only a natural end clears the pressed state: a cancel means a new
                // press started mid-fade and the instant-appear must win (DESIGN.md).
                if (!cancelled) isPressed = false
                cancelled = false
            }
        })
    }

    /**
     * Consumes the whole touch stream (nothing may leak to the app underneath, task
     * T04) and forwards the press lifecycle to the spec. Pressed-state highlighting
     * is set by hand: the platform sets it in the default touch handling that these
     * overrides replace, and the pressed circle must still light up while held; on
     * release it fades out ([pressFadeOut]) instead of clearing instantly.
     *
     * The physical path deliberately does not route through performClick: the action
     * already fires on Down (T06 ruling, NFR-4), so a click on Up would double-fire
     * tapAction. Accessibility activation still works — TalkBack's ACTION_CLICK runs
     * the T03 click listener via the default performClick — so the remaining
     * ClickableViewAccessibility warning is the same class of debt the T04 notes
     * deferred to T07/T08, not a broken activation path.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Cancel a running fade first: the instant-appear must win over a
                // release fade still in flight (DESIGN.md motion).
                pressFadeOut.cancel()
                isPressed = true
                // FR-9: short haptic when a button is pressed, honoring the setting
                // read at press time so a T08 toggle applies without any wiring.
                if (Prefs.hapticsEnabled(context)) {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
                spec.pressAction(ButtonPress.Down)
            }
            MotionEvent.ACTION_UP -> {
                // Fade out over PRESS_FADE_OUT_MS instead of clearing instantly; the
                // pressed state is held until the fade ends (DESIGN.md motion).
                if (isPressed) pressFadeOut.start()
                spec.pressAction(ButtonPress.Up)
            }
            MotionEvent.ACTION_CANCEL -> {
                if (isPressed) pressFadeOut.start()
                spec.pressAction(ButtonPress.Cancel)
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        // No animation may outlive the view (AGENTS.md lifecycle); cancel is a no-op
        // when idle, and its end listener only resets the drawable alpha and state.
        pressFadeOut.cancel()
        super.onDetachedFromWindow()
    }
}
