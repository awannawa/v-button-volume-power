# T03: Handle and panel UI

Status: TODO
Dependencies: T02
Required reading: `DESIGN.md`, `docs/virtual-buttons/01-ui-spec.md`
Required skills: andrej-karpathy-skills, hallmark (see `AGENTS.md`)

## Goal

- Replace the placeholder with an `OverlayView` containing a thin handle and a vertical button panel, with both states.

## Scope

- In scope: visuals, icons, collapsed and expanded states, animation, window size.
- Out of scope: gestures (T04), button actions (T06), drag (T05).

## Implementation steps

- Read `01-ui-spec.md`, then copy the icons from `assets/icons/android/` into `app/src/main/res/drawable/`.
- Create `OverlayView : FrameLayout` containing a `HandleView` and a `PanelView`.
- `HandleView`: a custom View that draws a 5dp x 72dp bar (fill, border, and corners per `DESIGN.md`) on the right edge, with `alpha` from the settings (default 0.6) and a 24dp wide touch area.
- `PanelView`: a vertical LinearLayout with 3 buttons (Vol +, Vol -, Power) sized by the S, M, or L preset, a card background per `DESIGN.md`, and the icons `ic_volume_up`, `ic_volume_down`, `ic_power` colored through `imageTintList`.
- Define the panel buttons as a list of `ButtonSpec` (see `AGENTS.md`), not as hardcoded views.
- State: `setExpanded(Boolean)` changes visibility and the `LayoutParams` size (24dp while collapsed, the preset's panel width while expanded) and calls `updateViewLayout`.
- Provide `applyConfig()` so size, opacity, and other settings can be applied later without recreating the view.
- Animate `translationX` for about 150 ms with `ViewPropertyAnimator`. No continuous animators.
- Add a `contentDescription` to every button.
- Express every size through a `dp()` helper.

## Related files

- `overlay/OverlayView.kt`, `overlay/HandleView.kt`, `overlay/PanelView.kt`, `res/drawable/ic_*.xml`

## Acceptance criteria

- Collapsed shows only a thin bar on the right edge.
- Expanded shows 3 vertical buttons attached to the right edge at the same Y position as the handle.
- Switching state is smooth and the window covers nothing beyond its own width.

## Verification

- Run the design check in `DESIGN.md` section 5 and record the result under Notes.
- Call `setExpanded` from a temporary debug button and check both states in portrait.
- Layout Inspector or a screenshot: the window width matches the spec.
- Remove the temporary debug button before marking the task done.

## Notes

- (to be filled in by the worker)
