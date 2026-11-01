package com.awannawa.vbutton

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.awannawa.vbutton.overlay.ButtonPress

private const val LOG_TAG = "VButton"

/**
 * All system interaction behind the panel buttons (AGENTS.md: a View never calls
 * AudioManager or the service directly). Takes the [AccessibilityService] — which is
 * the Context the T06 task file's "takes a Context and the AccessibilityService"
 * collapses into, because [AccessibilityService] extends Context and
 * [AccessibilityService.performGlobalAction] needs the service itself.
 *
 * The public interface is stable (AGENTS.md): T11 builds its extra buttons on
 * [volumeUp]/[volumeDown]/[powerTap]/[powerLongPress] and on [pressHandler], which
 * turns any pair of actions into the DOWN/UP/CANCEL press lifecycle the panel views
 * deliver through [ButtonSpec.pressAction]. One instance is created per overlay
 * attach in VButtonService and shared by every button of that overlay.
 *
 * Main thread only (touch events and the main-looper Handler below); the Handler
 * holds no posts while no button is held, so the service stays idle (NFR-1).
 */
class Actions(private val service: AccessibilityService) {

    private val audioManager: AudioManager =
        service.getSystemService(AudioManager::class.java)

    private val handler = Handler(Looper.getMainLooper())

    /**
     * One canceller per press handler created by [pressHandler] (three in v1, created
     * once per overlay attach). [cancelPendingPresses] runs them all when the overlay
     * detaches, so no timer can outlive the view (T06 acceptance criteria).
     */
    private val pressCancellers = mutableListOf<() -> Unit>()

    // FR-7: volume up/down on the currently active stream, with the system volume
    // indicator. ------------------------------------------------------------------

    /** Raises the volume of the currently active stream and shows the indicator (FR-7). */
    fun volumeUp() = adjustVolume(AudioManager.ADJUST_RAISE)

    /** Lowers the volume of the currently active stream and shows the indicator (FR-7). */
    fun volumeDown() = adjustVolume(AudioManager.ADJUST_LOWER)

    /**
     * One call per direction (T06 task file): USE_DEFAULT_STREAM_TYPE makes the system
     * pick the stream the user is actually interacting with (music while playing,
     * ringer otherwise, PRD verification), and FLAG_SHOW_UI surfaces the system volume
     * indicator. Nothing to check: adjustSuggestedStreamVolume returns void.
     */
    private fun adjustVolume(direction: Int) {
        audioManager.adjustSuggestedStreamVolume(
            direction,
            AudioManager.USE_DEFAULT_STREAM_TYPE,
            AudioManager.FLAG_SHOW_UI,
        )
    }

    // FR-8: power tap and long press. ----------------------------------------------

    /** Locks the screen (PRD S3). */
    fun powerTap() = performGlobal(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, "lock screen")

    /** Opens the system power menu (PRD S3). */
    fun powerLongPress() = performGlobal(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG, "power menu")

    /**
     * AGENTS.md error handling: the boolean result of performGlobalAction is checked
     * and logged. A false result means the system refused the action (R6: this can
     * differ on Vivo) — a real log warning, not an exception.
     */
    private fun performGlobal(action: Int, what: String) {
        val performed = service.performGlobalAction(action)
        if (!performed) Log.w(LOG_TAG, "performGlobalAction($what) returned false")
    }

    // Press lifecycle (T06): hold-to-repeat and the power tap/long-press split. ----

    /**
     * Builds the DOWN/UP/CANCEL handler for one panel button. This is the seam T11
     * uses for its extra buttons: repeatable buttons get hold-to-repeat (FR-7, PRD
     * S5), non-repeatable buttons get the tap-or-long-press split (FR-8). The timing
     * decisions live in [PressPolicy]; the handlers here only apply them to the
     * Handler.
     *
     * Each call returns an independent handler with its own state, so two buttons can
     * be held at once (e.g. Vol+ and Vol-) without interfering.
     */
    fun pressHandler(
        tap: () -> Unit,
        longPress: () -> Unit,
        repeatable: Boolean,
    ): (ButtonPress) -> Unit =
        if (repeatable) repeatingHandler(tap) else tapOrLongPressHandler(tap, longPress)

    /**
     * Hold-to-repeat (T06): Down fires [action] once, then repeats it — first repeat
     * after the initial delay, then one repeat per interval — until Up or Cancel.
     * [longPress] is unused here: a repeatable volume button has no long-press
     * semantics (FR-7 says hold = repeat).
     */
    private fun repeatingHandler(action: () -> Unit): (ButtonPress) -> Unit {
        var ticksFired = 0
        val repeatTick = object : Runnable {
            override fun run() {
                action()
                ticksFired += 1
                handler.postDelayed(this, PressPolicy.repeatTickDelayMs(ticksFired + 1))
            }
        }
        pressCancellers.add { handler.removeCallbacks(repeatTick) }
        return { press ->
            when (press) {
                ButtonPress.Down -> {
                    // Remove-before-post (same pattern as the overlay's auto-collapse)
                    // keeps at most one chain alive even if a second Down arrives.
                    handler.removeCallbacks(repeatTick)
                    ticksFired = 0
                    action() // on ACTION_DOWN, run the action once (T06; NFR-4 latency)
                    handler.postDelayed(repeatTick, PressPolicy.repeatTickDelayMs(1))
                }
                ButtonPress.Up, ButtonPress.Cancel -> handler.removeCallbacks(repeatTick)
            }
        }
    }

    /**
     * Tap-or-long-press (T06 FR-8): Up within the long-press timeout fires [tap];
     * letting the timeout elapse fires [longPress] instead and consumes the
     * following Up, so one press never does both.
     */
    private fun tapOrLongPressHandler(tap: () -> Unit, longPress: () -> Unit): (ButtonPress) -> Unit {
        var longPressFired = false
        val longPressTimeout = Runnable {
            longPressFired = true
            longPress()
        }
        pressCancellers.add { handler.removeCallbacks(longPressTimeout) }
        return { press ->
            when (press) {
                ButtonPress.Down -> {
                    handler.removeCallbacks(longPressTimeout)
                    longPressFired = false
                    handler.postDelayed(longPressTimeout, PressPolicy.LONG_PRESS_TIMEOUT_MS)
                }
                ButtonPress.Up -> {
                    handler.removeCallbacks(longPressTimeout)
                    if (PressPolicy.firesTapOnUp(longPressFired)) tap()
                }
                ButtonPress.Cancel -> handler.removeCallbacks(longPressTimeout)
            }
        }
    }

    /**
     * Stops every pending press timer (repeat ticks and long-press timeouts).
     * OverlayView calls this from onDetachedFromWindow: a finger can still be holding
     * a button when the service is turned off, and no handler may keep running after
     * a button is released or the view is gone (T06 acceptance criteria, AGENTS.md
     * lifecycle). Cancelling only removes pending posts, so the buttons keep working
     * after a re-attach.
     */
    fun cancelPendingPresses() {
        pressCancellers.forEach { cancel -> cancel() }
    }
}

/**
 * Pure decision core of the T06 press interactions, kept free of android.os APIs so
 * it runs in JVM unit tests (AGENTS.md: pure logic separated from Android APIs).
 *
 * The three values below are the T06 task file's interaction timings ("repeat about
 * every 150 ms after an initial delay of about 400 ms", "Power long press (about
 * 500 ms)"). They are defined once, here — deliberately not in Tokens.kt, which holds
 * visual tokens from DESIGN.md/01-ui-spec.md, not interaction timings.
 */
internal object PressPolicy {

    /** T06: delay after ACTION_DOWN before the first repeat tick. */
    const val REPEAT_INITIAL_DELAY_MS = 400L

    /** T06: interval between repeat ticks after the first. */
    const val REPEAT_INTERVAL_MS = 150L

    /** T06: hold duration that turns a power tap into the power menu. */
    const val LONG_PRESS_TIMEOUT_MS = 500L

    /**
     * Delay before the [tick]-th repeat tick (1-based) after ACTION_DOWN: the first
     * tick waits the initial delay (so a short press does not stutter), every later
     * tick one interval.
     */
    fun repeatTickDelayMs(tick: Int): Long =
        if (tick <= 1) REPEAT_INITIAL_DELAY_MS else REPEAT_INTERVAL_MS

    /**
     * Whether ACTION_UP on a non-repeatable button still fires the tap: only when the
     * long-press timeout has not already fired — a long press must not also tap
     * (FR-8, the same rule PanelView's long-click listener has had since T03).
     */
    fun firesTapOnUp(longPressFired: Boolean): Boolean = !longPressFired
}
