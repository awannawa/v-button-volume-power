package com.awannawa.vbutton.overlay

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
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
 * (PRD FR-3). Owns the collapsed/expanded/dragging switch: visibility plus the
 * window width (24dp collapsed, active preset's panel width expanded, 01-ui-spec.md
 * section 3) through WindowManager.updateViewLayout, and — while dragging — the
 * window's Y position, clamped to the safe area (PRD FR-6).
 *
 * T07 adds the screen-fit pass ([recomputeLayout], run on configuration changes and
 * through [applyConfig]): insets via WindowMetrics, compact mode, the window's X
 * offset from the right inset, and restoring the position from the stored yRatio
 * (01-ui-spec.md section 4). It also excludes the whole window from the system back
 * gesture ([onLayout], PRD R1), so a leftward swipe on the handle opens the panel.
 *
 * Expanding comes from [GestureController] (tap or left fling on the handle, FR-4);
 * dragging comes from the same controller (handle long press, FR-6). Collapsing
 * comes from the auto-collapse timer or a tap outside the panel (ACTION_OUTSIDE,
 * FR-5). Settings reach the overlay only through [applyConfig], called by the
 * service's Prefs listener (T08). The panel buttons act through the
 * [Actions] instance the service passes in (T06); this view never touches
 * AudioManager or the service itself (AGENTS.md).
 */
class OverlayView(
    context: Context,
    private val windowManager: WindowManager,
    private val actions: Actions,
) : FrameLayout(context) {

    /**
     * One explicit state, per AGENTS.md. [State.Dragging] is entered only from a
     * stable Collapsed/Expanded state (FR-6) and always returns to the state that
     * preceded the drag, so expand/collapse transitions never fight an active drag.
     */
    private enum class State { Collapsed, Expanded, Dragging }

    /**
     * FR-5 auto-collapse: at most one pending callback, and only while expanded
     * (NFR-1). Removed when collapsed, when reset (remove-before-post), and on
     * detach, so no callback can outlive an expanded, attached overlay.
     */
    private val autoCollapseHandler = Handler(Looper.getMainLooper())
    private val autoCollapseRunnable = Runnable { setExpanded(false) }

    // The controller is created first: HandleView forwards its touch stream to it.
    private val gestureController = GestureController(
        context,
        onOpen = { setExpanded(true) },
        onDragStart = { downRawY -> startDrag(downRawY) },
        onDragMove = { rawY -> dragTo(rawY) },
        onDragEnd = { endDrag() },
    )
    private val handleView = HandleView(context, gestureController)

    /**
     * The v1 buttons (PRD FR-7/FR-8: volume holds repeat, power does not). Built once
     * for the overlay's lifetime: compact mode (T07) rebuilds the PanelView on a
     * preset change, and rebuilding with fresh specs would register a new
     * press-canceller in Actions on every rotation — reusing one spec list keeps
     * Actions' press registry at one entry per button for the overlay's life. Each
     * spec declares its semantic actions (tapAction/longPressAction — also the
     * TalkBack activation path) and gets its physical press lifecycle from
     * [Actions.pressHandler]; T11 adds items to this list only.
     */
    private val buttonSpecs: List<ButtonSpec> = defaultButtons()

    /**
     * The preset the overlay currently renders with: the user's stored preset (Prefs,
     * T08) stepped down by compact mode (01-ui-spec.md section 4) when this screen is
     * too short for the panel. Recomputed on every configuration pass
     * ([recomputeLayout]); compact mode only shrinks, never widens, and never rewrites
     * the stored choice. Read by VButtonService for the window it creates before this
     * view is attached.
     */
    var effectivePreset: Tokens.SizePreset = compactPresetForCurrentScreen()
        private set

    private var panelView = PanelView(context, buttonSpecs, effectivePreset)
    private var state = State.Collapsed

    /**
     * The stable state the drag restores on release (Collapsed or Expanded — the
     * task file's "while the panel is expanded, dragging still moves the whole
     * panel": release returns the panel to whatever it was doing). Rewritten if a
     * collapse or expand request lands mid-drag; the drag itself never changes it.
     */
    private var preDragState = State.Collapsed

    /**
     * Per-drag bookkeeping, captured once at drag start so the move path is pure
     * arithmetic on primitives plus one updateViewLayout — no lookups, no
     * allocations per event (T09 audit).
     */
    private var dragRange = SafeArea.YRange(0, 0)
    private var grabOffsetPx = 0f

    /** The handle alpha the release transition restores (whatever applyConfig had set). */
    private var preDragHandleAlpha = Tokens.HANDLE_ALPHA_DEFAULT

    /**
     * Reusable rect + list for the back-gesture exclusion ([onLayout]): the
     * allocation-free pattern the platform documents — mutate the rect, re-assign the
     * list, so layout passes never allocate.
     */
    private val gestureExclusionRect = Rect()
    private val gestureExclusionRectList = listOf(gestureExclusionRect)

    // DESIGN.md section 2, motion locks one curve for open and close.
    private val motionInterpolator = DecelerateInterpolator()

    init {
        addView(handleView, LayoutParams(LayoutParams.MATCH_PARENT, dp(Tokens.HANDLE_HEIGHT_DP)))
        addView(panelView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        panelView.visibility = GONE
        // Park the panel fully outside the window so the first expand animates a real
        // slide-in from the right instead of popping into place. Parked at the
        // effective (compact) preset's width, matching showCollapsed's target.
        panelView.translationX = dp(effectivePreset.panelWidthDp).toFloat()
        applyConfig()
    }

    /**
     * Idempotent (AGENTS.md): repeated calls with the same target do nothing, and a
     * call during a running animation simply re-targets it (ViewPropertyAnimator
     * replaces the previous animation on the same view).
     *
     * A request landing mid-drag never fights the drag (T05 ruling): the drag keeps
     * moving the window and only the state the drag will restore changes. The timer's
     * collapse also plays its visuals right away — the panel's expiry must be visible
     * even with the finger still down — while an expand request just waits for the
     * drag to end.
     */
    fun setExpanded(expanded: Boolean) {
        val target = if (expanded) State.Expanded else State.Collapsed
        if (state == State.Dragging) {
            preDragState = target
            if (!expanded) showCollapsed()
            return
        }
        if (state == target) return
        state = target
        if (expanded) showExpanded() else showCollapsed()
    }

    /** The expanded visuals: reveal the panel, widen the window, restart the FR-5 countdown. */
    private fun showExpanded() {
        handleView.visibility = GONE
        panelView.visibility = VISIBLE
        resizeWindow(dp(effectivePreset.panelWidthDp))
        panelView.animate()
            .translationX(0f)
            .setDuration(Tokens.EXPAND_COLLAPSE_MS)
            .setInterpolator(motionInterpolator)
        scheduleAutoCollapse()
    }

    /**
     * The collapsed visuals: reveal the handle, slide the panel out, and narrow the
     * window when the slide finishes. The end-action guard is "not Expanded" rather
     * than "== Collapsed" so it also runs after a mid-drag collapse, where the state
     * is still Dragging (T05).
     */
    private fun showCollapsed() {
        cancelAutoCollapse()
        // The handle sits under the panel (same dark fill, panel is on top), so it
        // can become visible now and is only revealed as the panel slides right.
        handleView.visibility = VISIBLE
        panelView.animate()
            .translationX(dp(effectivePreset.panelWidthDp).toFloat())
            .setDuration(Tokens.EXPAND_COLLAPSE_MS)
            .setInterpolator(motionInterpolator)
            .withEndAction {
                // Guard: a re-expand mid-animation cancels this one and suppresses
                // the end action, but the guard keeps the swap correct even if a
                // platform build ever ran it.
                if (state != State.Expanded) {
                    panelView.visibility = GONE
                    resizeWindow(dp(Tokens.COLLAPSED_WINDOW_WIDTH_DP))
                }
            }
    }

    /**
     * Enters drag mode (FR-6), from a stable state only. Captures everything the move
     * path needs once — the valid Y range and where the finger grabbed the window —
     * and applies the DESIGN.md section 2 drag visuals instantly (the release
     * transition is the one duration the tokens define; entry has none). The haptic
     * (FR-9) sits behind the same haptics gate the panel buttons use (T06).
     */
    private fun startDrag(downRawY: Float) {
        if (state == State.Dragging) return
        val params = layoutParams as? WindowManager.LayoutParams ?: return
        // Fresh insets at drag start (SafeArea reads WindowMetrics each call), so a
        // drag clamps against the configuration it runs in — including system-bar
        // visibility changes that no onConfigurationChanged announces. The effective
        // (compact) preset's height is the tallest content this configuration renders.
        dragRange = SafeArea.compute(context, dp(effectivePreset.panelHeightDp))
        grabOffsetPx = downRawY - params.y
        preDragState = state
        preDragHandleAlpha = handleView.alpha
        state = State.Dragging
        // DESIGN.md section 2, motion — drag state: scaleX 1.5, alpha 1.0. The pivot is
        // the screen edge so the bar grows inward and never off the window surface.
        handleView.pivotX = handleView.width.toFloat()
        handleView.pivotY = handleView.height / 2f
        handleView.scaleX = Tokens.HANDLE_DRAG_SCALE_X
        handleView.alpha = Tokens.HANDLE_DRAG_ALPHA
        if (Prefs.hapticsEnabled(context)) {
            handleView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * One drag move (FR-6): the window's new top is the finger's rawY minus the grab
     * offset, clamped to the range captured at drag start, written into the EXISTING
     * WindowManager.LayoutParams — handle and panel share the window, so the whole
     * panel follows by itself, and the X axis is locked because only y is ever
     * written. No allocations and no lookups per event (T09 audit).
     */
    private fun dragTo(rawY: Float) {
        if (state != State.Dragging) return
        val params = layoutParams as? WindowManager.LayoutParams ?: return
        params.y = Math.round(SafeArea.clampY(rawY - grabOffsetPx, dragRange))
        updateWindowLayout()
    }

    /**
     * Leaves drag mode: saves the position as a ratio of the range it was dragged in
     * (never px — 01-ui-spec.md section 4 — so rotation, display-size changes, and
     * reboots restore a sensible position), restores the state the drag started from,
     * and returns the handle to normal over DESIGN.md's 100 ms release transition.
     * Restoring Expanded replays the expanded visuals (idempotent) and, per FR-5,
     * restarts the no-interaction countdown — the drag's release is an interaction.
     * Collapsed needs no visuals: the drag never changed them, and a mid-drag timer
     * expiry already played the collapse.
     */
    private fun endDrag() {
        if (state != State.Dragging) return
        (layoutParams as? WindowManager.LayoutParams)?.let { params ->
            Prefs.setYRatio(context, SafeArea.yRatioFromY(params.y.toFloat(), dragRange))
        }
        state = preDragState
        handleView.animate()
            // 1f is the identity scale, not a token — the drag scale (1.5) is the token.
            .scaleX(1f)
            .alpha(preDragHandleAlpha)
            .setDuration(Tokens.DRAG_RELEASE_MS)
        if (state == State.Expanded) showExpanded()
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

    /**
     * Back-gesture exclusion (PRD R1): the whole window is excluded from the system
     * back gesture, so a leftward swipe on the handle opens the panel (FR-4) instead
     * of navigating back. The rect is in this view's local coordinates and covers the
     * entire view, which is exactly the window (the view fills it) — expand/collapse
     * resizes re-layout the view, so re-setting here tracks every window size.
     *
     * Height limit: the system honors at most ~200dp of exclusion height per screen
     * edge. The handle is 72dp and the tallest panel is preset L at 188dp —
     * 01-ui-spec.md section 3 deliberately keeps every preset under 200dp, so the
     * whole-view rect is always honored. Deliberate assertion by spec, not runtime
     * detection.
     *
     * API guard: setSystemGestureExclusionRects exists from API 29. API 28 is a no-op
     * by design — gesture navigation itself shipped in Android 10 (API 29), so on
     * API 28 there is no system gesture to conflict with (AGENTS.md: guarded calls
     * never crash).
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        gestureExclusionRect.set(0, 0, width, height)
        systemGestureExclusionRects = gestureExclusionRectList
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
     * recreating views. Idempotent. Reads every setting it renders from Prefs — the
     * handle opacity (01-ui-spec.md section 2: alpha follows the setting) and, through
     * the full screen-fit pass, the size preset (01-ui-spec.md section 4: recompute
     * "whenever a setting changes") — so T08 needs no other path into the overlay.
     * The timeout needs no read here: the collapse timer reads it at schedule time.
     */
    fun applyConfig() {
        // Percent (per hundred) to alpha fraction; Prefs owns the 30-100 bound.
        val alpha = Prefs.handleOpacityPercent(context) / 100f
        if (state == State.Dragging) {
            // T05 handover: a setting change mid-drag must neither stomp the drag
            // visuals (DESIGN.md motion: alpha 1.0 while dragging) nor let endDrag
            // restore a stale preDragHandleAlpha — retarget the restore value instead.
            preDragHandleAlpha = alpha
        } else {
            handleView.alpha = alpha
        }
        recomputeLayout()
    }

    /**
     * The screen-fit pass (01-ui-spec.md section 4: "Recompute everything above in
     * onConfigurationChanged and whenever a setting changes"): fresh insets, compact
     * mode, and the window's x/y, then one layout update. Called by
     * VButtonService.onConfigurationChanged and [applyConfig].
     *
     * Position rule (01-ui-spec.md section 4): at rest the stored yRatio is the source
     * of truth — the y is re-converted from it against the fresh range, so rotation,
     * a display-size change, or a preset change keeps the position sensible instead of
     * carrying stale px over. A drag in progress is the exception: the finger is the
     * truth there, so the current y is only re-clamped into the fresh range and the
     * drag keeps moving against it (the release then saves the ratio for the new
     * geometry).
     *
     * Before the window exists (construction, via [applyConfig] in init) it only
     * settles [effectivePreset]; VButtonService reads that preset and the same insets
     * to place the window before addView.
     */
    fun recomputeLayout() {
        val screen = SafeArea.readScreenInsets(context)
        val marginPx = dp(Tokens.SCREEN_EDGE_MARGIN_DP)
        val preset = selectCompactPreset(screen, marginPx)
        if (preset != effectivePreset) applyPreset(preset)
        effectivePreset = preset

        val params = layoutParams as? WindowManager.LayoutParams ?: return
        val range = SafeArea.compute(screen, dp(effectivePreset.panelHeightDp), marginPx)
        dragRange = range
        // X offset (01-ui-spec.md section 4): with Gravity.RIGHT, x is the inward
        // distance from the right edge — the navigation bar sitting there in landscape
        // with 3-button navigation, or a cutout — so the overlay never covers system
        // areas on the x axis.
        params.x = screen.rightInsetPx
        params.y = when (state) {
            State.Dragging -> Math.round(SafeArea.clampY(params.y.toFloat(), range))
            else -> Math.round(SafeArea.yFromRatio(Prefs.yRatio(context), range))
        }
        updateWindowLayout()
    }

    /**
     * Compact-mode decision for one inset reading (pure math in
     * SafeArea.selectFittingPreset, JVM-tested): the active preset stepped down while
     * its panel is taller than the space between the system bars and the screen-edge
     * margins. The active preset is the user's stored choice (T08), not a constant.
     */
    private fun selectCompactPreset(
        screen: SafeArea.ScreenInsets,
        marginPx: Int,
    ): Tokens.SizePreset = SafeArea.selectFittingPreset(
        availableHeightPx = SafeArea.availableHeightPx(screen, marginPx),
        activePreset = sizePresetFromPref(Prefs.sizePreset(context)),
        density = resources.displayMetrics.density,
    )

    /** [selectCompactPreset] for the current configuration (used at construction). */
    private fun compactPresetForCurrentScreen(): Tokens.SizePreset =
        selectCompactPreset(SafeArea.readScreenInsets(context), dp(Tokens.SCREEN_EDGE_MARGIN_DP))

    /**
     * Swaps the panel to a new preset (compact mode stepped down; a future setting
     * changed it). PanelView sizes its buttons at construction, so a preset change
     * replaces the instance. The visual state carries over: a visible panel stays in
     * place and keeps its window width, a hidden one stays parked outside for the next
     * slide-in. The outgoing instance's press timers are cancelled here (its touch
     * stream dies with the swap, and the child views' own detach cleanup only stops
     * their animations), and the specs are shared ([buttonSpecs]), so Actions' press
     * registry never grows across rebuilds. An unchanged preset never reaches here
     * (recomputeLayout guards).
     */
    private fun applyPreset(preset: Tokens.SizePreset) {
        val panelVisible = panelView.visibility == VISIBLE
        actions.cancelPendingPresses()
        removeView(panelView)
        panelView = PanelView(context, buttonSpecs, preset)
        addView(panelView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        panelView.visibility = if (panelVisible) VISIBLE else GONE
        panelView.translationX = if (panelVisible) 0f else dp(preset.panelWidthDp).toFloat()
        if (panelVisible) resizeWindow(dp(preset.panelWidthDp))
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
        updateWindowLayout()
    }

    /**
     * The single guarded updateViewLayout path (AGENTS.md error handling), shared by
     * the width switch ([resizeWindow]) and the drag's y updates ([dragTo]).
     */
    private fun updateWindowLayout() {
        val params = layoutParams as? WindowManager.LayoutParams ?: return
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

/**
 * Maps the stored Prefs preset string to the locked preset data (01-ui-spec.md
 * section 3), falling back to the compiled default (Tokens.activePreset, preset M)
 * for the absent/unset case. Pure and JVM-tested (AGENTS.md): no Android types.
 */
internal fun sizePresetFromPref(value: String): Tokens.SizePreset = when (value) {
    Prefs.SIZE_PRESET_SMALL -> Tokens.PRESET_S
    Prefs.SIZE_PRESET_LARGE -> Tokens.PRESET_L
    else -> Tokens.activePreset // "M" and anything unreadable: the default preset
}
