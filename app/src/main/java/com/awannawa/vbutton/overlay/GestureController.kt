package com.awannawa.vbutton.overlay

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * Whether a fling opens the panel, decoupled from Android APIs so it runs in JVM unit
 * tests (AGENTS.md). The rule implements FR-4's "a swipe to the left": the fling must
 * be leftward ([velocityX] < 0), horizontally dominant (|velocityX| > |velocityY|),
 * fast enough ([minFlingVelocityPx]) and traveled far enough horizontally
 * ([touchSlopPx]). Both thresholds are the view's scaled ViewConfiguration values at
 * event time — never fixed px, never cached across configuration changes
 * (01-ui-spec.md section 4).
 *
 * GestureDetector fires onFling when EITHER axis exceeds its minimum fling velocity,
 * so without the dominance check a vertical list scroll that merely drifts left while
 * starting on the 24dp handle strip would open the panel; FR-4 wants a horizontal
 * swipe, not any gesture with a leftward component.
 */
internal fun shouldOpenOnFling(
    velocityX: Float,
    velocityY: Float,
    horizontalTravelPx: Float,
    touchSlopPx: Float,
    minFlingVelocityPx: Float,
): Boolean {
    if (velocityX >= 0f) return false // FR-4: a swipe to the LEFT opens; rightward never does
    // Dominance is strict >, so |velocityX| == |velocityY| stays closed: equality is
    // not a leftward swipe, and a false-open is worse than a missed one.
    if (abs(velocityX) <= abs(velocityY)) return false
    if (abs(velocityX) < minFlingVelocityPx) return false
    return abs(horizontalTravelPx) >= touchSlopPx
}

/**
 * Gesture logic for the handle (AGENTS.md: gestures live here, not in Views). Wraps a
 * [GestureDetector]: a short tap or a left fling opens the panel (PRD FR-4) through
 * the [onOpen] callback the owning OverlayView supplies. The controller never touches
 * overlay state or the service itself.
 *
 * Long press is deliberately left at the detector's no-op default — drag mode is T05
 * and must not be started from here.
 */
class GestureController(
    private val context: Context,
    private val onOpen: () -> Unit,
) : GestureDetector.SimpleOnGestureListener() {

    private val detector = GestureDetector(context, this)

    /**
     * Entry point the [HandleView] forwards its touch stream to. The handle consumes
     * every event itself, so the return value only reports what the detector made of
     * the gesture.
     */
    fun onTouch(event: MotionEvent): Boolean = detector.onTouchEvent(event)

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        // Open immediately on UP: no confirmed-tap delay (PRD NFR-4, under 100 ms).
        onOpen()
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ): Boolean {
        // Travel is measured from the real down event; the platform may deliver a null
        // e1 (gesture begun before the detector saw it), and without it travel is
        // unknowable, so the fling stays closed.
        val down = e1 ?: return false
        // Read the scaled thresholds at event time: density or display size can change
        // between gestures, so px values are never cached (01-ui-spec.md section 4).
        val config = ViewConfiguration.get(context)
        val opens = shouldOpenOnFling(
            velocityX = velocityX,
            velocityY = velocityY,
            horizontalTravelPx = e2.x - down.x,
            touchSlopPx = config.scaledTouchSlop.toFloat(),
            minFlingVelocityPx = config.scaledMinimumFlingVelocity.toFloat(),
        )
        if (opens) onOpen()
        return opens
    }
}
