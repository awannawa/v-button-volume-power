package com.awannawa.vbutton.overlay

/**
 * One panel button, in the shape AGENTS.md fixes ("Panel buttons are defined as a
 * list of ButtonSpec (id, icon, contentDescription, tap action, long-press action,
 * repeatable)"). PanelView renders whatever list it is given and knows nothing
 * about specific buttons, so T06 swaps the no-op action lambdas for real Actions
 * and T11 adds list items without modifying PanelView.
 *
 * T06 adds [pressAction] next to the declared fields: the DOWN/UP/CANCEL lifecycle
 * that lets [com.awannawa.vbutton.Actions] drive hold-to-repeat and the power
 * long-press (orchestrator ruling on T06 wiring). tapAction/longPressAction stay the
 * semantic actions — the physical press path invokes them through the press handler,
 * and accessibility activation (TalkBack's performClick/performLongClick) invokes
 * them directly.
 *
 * Resource ids are plain Ints (no androidx annotations exist in this project; the
 * only allowed runtime dependency is core-ktx, which is not present).
 */
data class ButtonSpec(
    val id: String,
    val iconRes: Int,
    val contentDescriptionRes: Int,
    val tapAction: () -> Unit,
    val longPressAction: () -> Unit,
    val repeatable: Boolean,
    val pressAction: (ButtonPress) -> Unit,
)

/**
 * The touch lifecycle of one press on a panel button, abstracted away from
 * MotionEvent so the policy that consumes it (Actions/PressPolicy) stays free of
 * Android view APIs and JVM-testable.
 */
enum class ButtonPress { Down, Up, Cancel }
