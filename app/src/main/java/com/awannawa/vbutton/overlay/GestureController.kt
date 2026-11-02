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
    side: ScreenSide,
    velocityX: Float,
    velocityY: Float,
    horizontalTravelPx: Float,
    touchSlopPx: Float,
    minFlingVelocityPx: Float,
): Boolean {
    // Opening travel is INWARD from the edge: right side -> leftward (<0), left side -> rightward (>0).
    val opensInward = if (side == ScreenSide.Left) velocityX > 0f else velocityX < 0f
    if (!opensInward) return false
    // Dominance is strict >, so |velocityX| == |velocityY| stays closed: equality is
    // not an inward swipe, and a false-open is worse than a missed one.
    if (abs(velocityX) <= abs(velocityY)) return false
    if (abs(velocityX) < minFlingVelocityPx) return false
    return abs(horizontalTravelPx) >= touchSlopPx
}

/**
 * Gesture logic for the handle (AGENTS.md: gestures live here, not in Views). Wraps
 * a [GestureDetector]: a short tap or a left fling opens the panel (PRD FR-4) and a
 * long press enters drag mode (PRD FR-6), each reported through the callback the
 * owning OverlayView supplies. The controller never touches overlay state or the
 * service itself — it only recognizes gestures and reports them.
 */
class GestureController(
    private val context: Context,
    private val side: () -> ScreenSide,
    private val onOpen: () -> Unit,
    private val onDragStart: (downRawY: Float) -> Unit,
    private val onDragMove: (rawY: Float) -> Unit,
    private val onDragEnd: () -> Unit,
) : GestureDetector.SimpleOnGestureListener() {

    private val detector = GestureDetector(context, this)

    /**
     * Which gesture the CURRENT touch stream belongs to — the raw-event bookkeeping
     * below, not overlay state (the overlay keeps its own enum). Set by [onLongPress],
     * cleared on UP/CANCEL and on the next DOWN, so it can never leak across streams.
     */
    private var dragInProgress = false

    /**
     * Entry point the [HandleView] forwards its touch stream to. The handle consumes
     * every event itself, so the return value only reports what the controller made of
     * the gesture.
     *
     * The drag rides the raw stream, not the detector: once [onLongPress] has fired,
     * moves and the final UP/CANCEL are handled here and deliberately NOT forwarded to
     * the detector, because a drag is neither a tap nor a fling candidate and a release
     * flick must never open the panel (FR-6). The X axis is locked by only ever
     * reporting rawY — horizontal movement is ignored entirely.
     */
    fun onTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN ->
                // A new stream: never inherit a drag from an abandoned one (e.g. a
                // stream that ended when the window was removed mid-drag).
                dragInProgress = false
            MotionEvent.ACTION_MOVE ->
                if (dragInProgress) {
                    onDragMove(event.rawY)
                    return true
                }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                if (dragInProgress) {
                    dragInProgress = false
                    onDragEnd()
                    return true
                }
        }
        return detector.onTouchEvent(event)
    }

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
            side = side(),
            velocityX = velocityX,
            velocityY = velocityY,
            horizontalTravelPx = e2.x - down.x,
            touchSlopPx = config.scaledTouchSlop.toFloat(),
            minFlingVelocityPx = config.scaledMinimumFlingVelocity.toFloat(),
        )
        if (opens) onOpen()
        return opens
    }

    override fun onLongPress(e: MotionEvent) {
        // FR-6: the long press is the drag-mode entry. The overlay decides whether the
        // state allows it (stable Collapsed/Expanded) and supplies haptic and visuals —
        // the controller only reports where the finger grabbed the screen.
        dragInProgress = true
        onDragStart(e.rawY)
    }
}
