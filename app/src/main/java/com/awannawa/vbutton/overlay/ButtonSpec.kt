package com.awannawa.vbutton.overlay

/**
 * One panel button, in the shape AGENTS.md fixes ("Panel buttons are defined as a
 * list of ButtonSpec (id, icon, contentDescription, tap action, long-press action,
 * repeatable)"). PanelView renders whatever list it is given and knows nothing
 * about specific buttons, so T06 swaps the no-op action lambdas for real Actions
 * and T11 adds list items without modifying PanelView.
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
)
