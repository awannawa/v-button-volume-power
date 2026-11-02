# Left/Right Screen Side Setting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a settings option that lets the user put the overlay handle and panel on the right (current behaviour) or the left screen edge, applied live and remembered across restarts.

**Architecture:** One stored setting (`Prefs` key `screen_side`, values `"right"`/`"left"`, default `"right"`) is the single source of truth. It is read wherever the code hard-codes the right side today and mirrored at five decision points: window gravity and inward X offset (`VButtonService`), handle bar edge (`HandleView`), panel park/slide direction and drag pivot (`OverlayView`), opening-fling direction (`GestureController`), and left-vs-right inset selection (`SafeArea`). All branching lives in pure helpers (`ScreenSide`, `gravityFor`, `inwardOffsetPx`, `insetForSide`, side-aware `shouldOpenOnFling`) so it is JVM-testable; the views and service only feed them the stored side. Live updates reuse the existing `applyConfig()` path (already fired by the service's Prefs listener); persistence reuses the existing "read Prefs on connect" path in `VButtonService`.

**Tech Stack:** Kotlin, plain Views/Canvas (no Compose, no AndroidX), `android.accessibilityservice.AccessibilityService`, `WindowManager`, JUnit 4 for JVM tests.

**Spec:** No separate spec file. The short design lives in the "Design (short)" section below and records every decision the user made, so the plan stands alone. `DESIGN.md` and `docs/virtual-buttons/01-ui-spec.md` are the locked design sources it must obey.

---

## Design (short)

**Intent and why.** The owner's physical buttons are broken and they drive the phone one-handed; a handle glued to the right edge is out of reach for some grips. The user wants to keep the familiar overlay but move it to whichever edge their thumb reaches. Every behaviour must mirror exactly, so the mental model does not change, only the side.

**Goals.**
- A settings choice: handle and panel on the right (default) or the left screen edge.
- Full mirror: the handle bar flush to the chosen edge; the panel opens inward from it; the handle drag pivot sits on the chosen edge; the opening swipe mirrors (right side opens on a leftward swipe, left side opens on a rightward swipe); all other collapsed/expanded visuals identical.
- Applies live while the service runs, through the existing Prefs listener into `applyConfig()`; no restart.
- Persists across service restart and reboot (stored in `Prefs`, restored on connect).

**Non-goals.**
- No new permissions, no new runtime dependencies, no Compose.
- No change to vertical (Y) behaviour or to `yRatio` semantics.
- No per-app or automatic side detection; the user picks it.

**Anti-goals (failure even if it "works").**
- Two diverging code paths for left and right instead of one side-parameterised path.
- A hard-coded right-side constant left behind, so the left side is subtly wrong (bar on the wrong edge, panel sliding the wrong way, pivot on the wrong side).
- A default that changes the current right-side behaviour for existing users.

**Constraints (from the project).**
- No `INTERNET` permission and no new permissions (`AGENTS.md`).
- Runtime dependencies stay at zero; `junit` is test-only. No Gradle/AGP/Kotlin version changes (`AGENTS.md`, `build.gradle.kts`).
- All colours, sizes, durations come from `Tokens` / `DESIGN.md` / `01-ui-spec.md`. The side setting adds no visual token; it reuses existing geometry (`DESIGN.md` section 2).
- One explicit overlay state enum; pure logic separated and JVM-tested; `applyConfig()` is the single settings entry point (`AGENTS.md` maintainability).
- `Gravity.RIGHT`/`Gravity.LEFT` (physical sides), never `START`/`END` (which mirror under RTL). The current code already uses `RIGHT`; adding `LEFT` keeps that discipline (`PRD` section 10, `01-ui-spec.md` section 4).
- Settings copy: literal, sentence case, no emoji (`DESIGN.md` section 3).

**Approaches.**
- **A — One `screen_side` flag, side-aware logic in place (CHOSEN).** Store a `ScreenSide` enum in `Prefs`. Thread it through the five places that assume right today, backed by pure helpers with JVM tests. One code path parameterised by side. Trades a small multi-file touch for zero duplication and a single source of truth. Matches `AGENTS.md`.
- **B — Separate right and left overlay classes.** Rejected: duplicates nearly every file, breaks the maintainability rules, and creates two places to fix every future gesture or size change.

**Kept Y (confirmed with the user).** The valid Y range is computed from panel height and top/bottom insets and does not depend on the horizontal side, so the stored `yRatio` is reused unchanged when the side changes: the handle reappears at the same height. No reset, no new Y logic.

**Live and remembered (confirmed with the user).** The side is written to `Prefs`; the service's existing `OnSharedPreferenceChangeListener` calls `applyConfig()`, which recomputes gravity and X. On connect, `VButtonService` reads the stored side and applies it before `addView`, so the handle appears on the chosen edge after a restart or reboot. The default is `"right"`, so anyone who never opens the setting sees no change.

**Decisions left to the builder, settled here.**
- `ScreenSide` lives in `overlay/` with the other geometry types. `Prefs` imports it for serialisation; the enum carries no `Context`, so there is no leak.
- `GestureController` and `HandleView` receive a `side: () -> ScreenSide` provider rather than reading `Prefs` themselves, so they stay free of `Prefs` and pick up a live side change on the next gesture or layout.

---

## Global Constraints

- No `INTERNET` permission; no new manifest permissions.
- Zero runtime dependencies; `junit:junit:4.13.2` is test-only. No Gradle/AGP/Kotlin version changes.
- Visual values only from `Tokens`/`DESIGN.md`/`01-ui-spec.md`; the side setting adds no new colour, size, or duration token.
- Every `SharedPreferences` key and default lives only in `Prefs.kt`.
- Use `Gravity.RIGHT`/`Gravity.LEFT`, never `START`/`END`; `@SuppressLint("RtlHardcoded")` is the established pattern for that.
- All overlay UI work on the main thread; pure decision logic is JVM-tested; `applyConfig()` stays the single settings entry point.
- Default stored side is `"right"`, so existing users see no change.

## Review Focus

Conditions the spec implies that no happy-path test would catch; each is pinned by a test in the owning task.

1. **RTL locale plus left side.** A `START`-anchored window would flip to the wrong physical edge. Expected: `Gravity.LEFT` keeps the overlay on the physical left regardless of locale. Pinned in Task 1 (`gravityFor(Left) == Gravity.LEFT or Gravity.TOP`).
2. **Inward `x` offset on the left side.** On the right, `x` is the inward distance from the right edge (a landscape nav bar); on the left it must be the left inset, or the overlay covers a nav bar or cutout. Expected: `inwardOffsetPx(Left, leftInset, rightInset) == leftInsetPx`. Pinned in Task 1.
3. **Mirrored opening swipe.** The right side opens on a leftward fling; the left side must open on a rightward fling, or the handle opens on the wrong swipe. Expected: the opening-fling sign flips with side, dominance and speed unchanged. Pinned in Task 4.
4. **Live side switch mid-session.** Switching side while expanded must leave the panel on the new edge, still expanded, Y unchanged. Expected: gravity and X recomputed through `applyConfig()`/`recomputeLayout()`, `y` restored from `yRatio`. Pinned in Task 5 (compile plus full test run) and the Task 7 device QA step.
5. **Unreadable stored side.** A corrupted or absent value must not crash and must fall back to right. Expected: `fromStoredOr` returns `Right` for anything not exactly `"left"`. Pinned in Task 1.

---

## File Structure

- `app/src/main/java/com/awannawa/vbutton/overlay/ScreenSide.kt` — **create.** `ScreenSide` enum (`Right`, `Left`) with the two stored strings, plus pure `fromStoredOr`, `gravityFor`, `inwardOffsetPx`. One responsibility: which physical edge and how the window anchors to it.
- `app/src/main/java/com/awannawa/vbutton/Prefs.kt` — **modify.** Add `KEY_SCREEN_SIDE`, `SCREEN_SIDE_DEFAULT`, `screenSide(context)`, `setScreenSide(context, side)`.
- `app/src/main/java/com/awannawa/vbutton/overlay/SafeArea.kt` — **modify.** Add `leftInsetPx` to `ScreenInsets`; populate it in both `readScreenInsets` branches; add pure `insetForSide(screen, side)`.
- `app/src/main/java/com/awannawa/vbutton/VButtonService.kt` — **modify.** Derive gravity and inward `x` from the stored side in `attachOverlay()`.
- `app/src/main/java/com/awannawa/vbutton/overlay/HandleView.kt` — **modify.** Take a `side` provider and draw the bar flush to that edge of its bounds.
- `app/src/main/java/com/awannawa/vbutton/overlay/OverlayView.kt` — **modify.** Side-aware panel park/slide direction, drag pivot, and gravity/X in `recomputeLayout()`; pass the side provider to `GestureController` and `HandleView`.
- `app/src/main/java/com/awannawa/vbutton/overlay/GestureController.kt` — **modify.** `shouldOpenOnFling` gains a `side` parameter; the controller takes a `side` provider.
- `app/src/main/java/com/awannawa/vbutton/SettingsActivity.kt` + `res/layout/activity_settings.xml` + `res/values/strings.xml` — **modify.** New "Screen side" RadioGroup (Right/Left) wired to `Prefs.setScreenSide`, refreshed in `refreshUi()`.
- `app/src/test/java/com/awannawa/vbutton/overlay/ScreenSideTest.kt` — **create.** Pure tests for the enum mapping, gravity, inward offset.
- `app/src/test/java/com/awannawa/vbutton/overlay/SafeAreaTest.kt` — **modify.** `insetForSide` tests; add `leftInsetPx` to the existing `ScreenInsets` literal.
- `app/src/test/java/com/awannawa/vbutton/overlay/ShouldOpenOnFlingTest.kt` — **modify.** Add `side` to every existing call (Right); add left-side mirror tests.

`Prefs` has no JVM test today because it needs a `Context`, and this plan adds no dependency to change that. The serialisation boundary is instead extracted into the pure `fromStoredOr` (tested in `ScreenSideTest`), leaving `Prefs` a thin wrapper consistent with the rest of the file.

---

### Task 1: `ScreenSide` enum and pure helpers

**Files:**
- Create: `app/src/main/java/com/awannawa/vbutton/overlay/ScreenSide.kt`
- Test: `app/src/test/java/com/awannawa/vbutton/overlay/ScreenSideTest.kt`

**Interfaces:**
- Produces:
  - `enum class ScreenSide { Right, Left }` with companion `const val STORED_LEFT = "left"` and `const val STORED_RIGHT = "right"`.
  - `fun ScreenSide.Companion.fromStoredOr(value: String?): ScreenSide` — `"left"` (exact) maps to `Left`; anything else, including `null`, maps to `Right`.
  - `fun gravityFor(side: ScreenSide): Int`
  - `fun inwardOffsetPx(side: ScreenSide, leftInsetPx: Int, rightInsetPx: Int): Int`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.awannawa.vbutton.overlay

import android.view.Gravity
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenSideTest {

    @Test
    fun `stored left maps to Left`() {
        assertEquals(ScreenSide.Left, ScreenSide.fromStoredOr(ScreenSide.STORED_LEFT))
    }

    @Test
    fun `right, null, empty and garbage all map to Right`() {
        assertEquals(ScreenSide.Right, ScreenSide.fromStoredOr(ScreenSide.STORED_RIGHT))
        assertEquals(ScreenSide.Right, ScreenSide.fromStoredOr(null))
        assertEquals(ScreenSide.Right, ScreenSide.fromStoredOr(""))
        assertEquals(ScreenSide.Right, ScreenSide.fromStoredOr("LEFT")) // case-sensitive: only the exact stored value counts
        assertEquals(ScreenSide.Right, ScreenSide.fromStoredOr("end"))
    }

    @Test
    fun `gravity anchors to the physical edge and TOP`() {
        assertEquals(Gravity.RIGHT or Gravity.TOP, gravityFor(ScreenSide.Right))
        assertEquals(Gravity.LEFT or Gravity.TOP, gravityFor(ScreenSide.Left))
    }

    @Test
    fun `inward offset on the right uses the right inset`() {
        assertEquals(90, inwardOffsetPx(ScreenSide.Right, leftInsetPx = 40, rightInsetPx = 90))
    }

    @Test
    fun `inward offset on the left uses the left inset`() {
        assertEquals(40, inwardOffsetPx(ScreenSide.Left, leftInsetPx = 40, rightInsetPx = 90))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `source ~/tools/android-env.sh && ./gradlew testDebugUnitTest --tests "com.awannawa.vbutton.overlay.ScreenSideTest" --console=plain`
Expected: FAIL — `ScreenSide`, `fromStoredOr`, `gravityFor`, `inwardOffsetPx` unresolved.

- [ ] **Step 3: Implement `ScreenSide.kt`**

Signatures and pure bodies (gravity and inward offset are two-line `if`s the signature and tests fully determine):

```kotlin
package com.awannawa.vbutton.overlay

import android.view.Gravity

enum class ScreenSide {
    Right,
    Left;

    companion object {
        const val STORED_LEFT = "left"
        const val STORED_RIGHT = "right"
    }
}

fun ScreenSide.Companion.fromStoredOr(value: String?): ScreenSide =
    if (value == ScreenSide.STORED_LEFT) ScreenSide.Left else ScreenSide.Right

fun gravityFor(side: ScreenSide): Int =
    (if (side == ScreenSide.Left) Gravity.LEFT else Gravity.RIGHT) or Gravity.TOP

fun inwardOffsetPx(side: ScreenSide, leftInsetPx: Int, rightInsetPx: Int): Int =
    if (side == ScreenSide.Left) leftInsetPx else rightInsetPx
```

Add a short KDoc on the enum explaining why `RIGHT`/`LEFT` and not `START`/`END` (physical sides never flip under RTL; same reason the original right-only window used `Gravity.RIGHT`, PRD section 10). Keep it why-only per `AGENTS.md`.

- [ ] **Step 4: Run test to verify it passes**

Run: `source ~/tools/android-env.sh && ./gradlew testDebugUnitTest --tests "com.awannawa.vbutton.overlay.ScreenSideTest" --console=plain`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/awannawa/vbutton/overlay/ScreenSide.kt app/src/test/java/com/awannawa/vbutton/overlay/ScreenSideTest.kt
git commit -m "feat: add ScreenSide enum and pure side helpers"
```

---

### Task 2: `SafeArea` left inset and `insetForSide`

**Files:**
- Modify: `app/src/main/java/com/awannawa/vbutton/overlay/SafeArea.kt`
- Test: `app/src/test/java/com/awannawa/vbutton/overlay/SafeAreaTest.kt` (the `ScreenInsets` literal near line 171, plus new tests)

**Interfaces:**
- Consumes: `ScreenSide`, `ScreenSide.Left` (Task 1).
- Produces:
  - `SafeArea.ScreenInsets(screenHeightPx: Int, insetTopPx: Int, insetBottomPx: Int, leftInsetPx: Int, rightInsetPx: Int)` — new `leftInsetPx` field.
  - `fun SafeArea.insetForSide(screen: ScreenInsets, side: ScreenSide): Int` — `leftInsetPx` for `Left`, else `rightInsetPx`.

- [ ] **Step 1: Write the failing test**

Add this test to `SafeAreaTest.kt`:

```kotlin
@Test
fun `inset for side picks the matching edge`() {
    val screen = SafeArea.ScreenInsets(
        screenHeightPx = 2400,
        insetTopPx = 100,
        insetBottomPx = 100,
        leftInsetPx = 42,
        rightInsetPx = 90,
    )
    assertEquals(42, SafeArea.insetForSide(screen, ScreenSide.Left))
    assertEquals(90, SafeArea.insetForSide(screen, ScreenSide.Right))
}
```

Also update the existing `SafeArea.ScreenInsets(...)` constructor call in this test file (near line 171) to add `leftInsetPx = 0,`.

- [ ] **Step 2: Run test to verify it fails**

Run: `source ~/tools/android-env.sh && ./gradlew testDebugUnitTest --tests "com.awannawa.vbutton.overlay.SafeAreaTest" --console=plain`
Expected: FAIL — `ScreenInsets` has no `leftInsetPx` parameter; `insetForSide` unresolved.

- [ ] **Step 3: Implement**

In `SafeArea.kt`: add `leftInsetPx` to the `ScreenInsets` data class between `insetBottomPx` and `rightInsetPx`; in `readScreenInsets` add `leftInsetPx = maxOf(bars.left, cutout.left),` in the API 30+ branch and `leftInsetPx = 0,` in the DisplayMetrics fallback (one-line comment: same limitation as the existing right-inset `0` on API 28/29). Add the pure function:

```kotlin
fun insetForSide(screen: ScreenInsets, side: ScreenSide): Int =
    if (side == ScreenSide.Left) screen.leftInsetPx else screen.rightInsetPx
```

- [ ] **Step 4: Run test to verify it passes**

Run: `source ~/tools/android-env.sh && ./gradlew testDebugUnitTest --tests "com.awannawa.vbutton.overlay.SafeAreaTest" --console=plain`
Expected: PASS (all existing tests plus the new one).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/awannawa/vbutton/overlay/SafeArea.kt app/src/test/java/com/awannawa/vbutton/overlay/SafeAreaTest.kt
git commit -m "feat: read the left inset and add insetForSide"
```

---

### Task 3: `Prefs` screen-side key and service anchoring

**Files:**
- Modify: `app/src/main/java/com/awannawa/vbutton/Prefs.kt`
- Modify: `app/src/main/java/com/awannawa/vbutton/VButtonService.kt` (`attachOverlay`, lines ~122-177)

**Interfaces:**
- Consumes: `ScreenSide`, `ScreenSide.fromStoredOr`, `ScreenSide.STORED_LEFT`, `ScreenSide.STORED_RIGHT`, `gravityFor`, `inwardOffsetPx` (Task 1); `SafeArea.readScreenInsets` already returns the `ScreenInsets` with `leftInsetPx` (Task 2).
- Produces:
  - `Prefs.SCREEN_SIDE_DEFAULT: ScreenSide = ScreenSide.Right`
  - `fun Prefs.screenSide(context: Context): ScreenSide`
  - `fun Prefs.setScreenSide(context: Context, side: ScreenSide)`

- [ ] **Step 1: Write the failing test**

`Prefs` needs a `Context`, so there is no JVM test for it here (consistent with the project, which has none). The pure mapping it delegates to is already covered. Add this assertion to `ScreenSideTest` to document the default that `Prefs.SCREEN_SIDE_DEFAULT` must mirror:

```kotlin
@Test
fun `the documented default side is Right`() {
    assertEquals(ScreenSide.Right, ScreenSide.fromStoredOr(null))
}
```

- [ ] **Step 2: Run it (documents the default; no new production code is needed for this test)**

Run: `source ~/tools/android-env.sh && ./gradlew testDebugUnitTest --tests "com.awannawa.vbutton.overlay.ScreenSideTest" --console=plain`
Expected: PASS (the default is already `Right`). This step exists so the default is pinned before the `Prefs` wrapper references it.

- [ ] **Step 3: Implement the `Prefs` key**

Add `import com.awannawa.vbutton.overlay.ScreenSide` to `Prefs.kt`, then the key, default, and accessors:

```kotlin
private const val KEY_SCREEN_SIDE = "screen_side"
val SCREEN_SIDE_DEFAULT: ScreenSide = ScreenSide.Right

fun screenSide(context: Context): ScreenSide =
    ScreenSide.fromStoredOr(
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getString(KEY_SCREEN_SIDE, ScreenSide.STORED_RIGHT),
    )

fun setScreenSide(context: Context, side: ScreenSide) {
    context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_SCREEN_SIDE, if (side == ScreenSide.Left) ScreenSide.STORED_LEFT else ScreenSide.STORED_RIGHT)
        .apply()
}
```

- [ ] **Step 4: Implement service anchoring in `VButtonService.attachOverlay()`**

Import `gravityFor` and `inwardOffsetPx` from `overlay`. Read the side once next to the existing `screen` read, and use the helpers inside the `LayoutParams.apply { }` block:

```kotlin
val side = Prefs.screenSide(this)
// ...inside .apply { }:
gravity = gravityFor(side)
x = inwardOffsetPx(side, screen.leftInsetPx, screen.rightInsetPx)
```

Keep the `@SuppressLint("RtlHardcoded")` annotation. Update its comment to say the anchor follows the stored side and stays physical (`LEFT`/`RIGHT`, never `END`/`START`), so the existing rationale is not contradicted.

- [ ] **Step 5: Run compile and tests**

Run: `source ~/tools/android-env.sh && ./gradlew compileDebugKotlin testDebugUnitTest --console=plain`
Expected: BUILD SUCCESSFUL; all `ScreenSideTest` cases green.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/awannawa/vbutton/Prefs.kt app/src/main/java/com/awannawa/vbutton/VButtonService.kt app/src/test/java/com/awannawa/vbutton/overlay/ScreenSideTest.kt
git commit -m "feat: store the screen side and anchor the window by it"
```

---

### Task 4: `GestureController` side-aware opening fling

**Files:**
- Modify: `app/src/main/java/com/awannawa/vbutton/overlay/GestureController.kt`
- Modify: `app/src/test/java/com/awannawa/vbutton/overlay/ShouldOpenOnFlingTest.kt`

**Interfaces:**
- Consumes: `ScreenSide` (Task 1).
- Produces:
  - `internal fun shouldOpenOnFling(side: ScreenSide, velocityX: Float, velocityY: Float, horizontalTravelPx: Float, touchSlopPx: Float, minFlingVelocityPx: Float): Boolean` — the opening direction flips with side (Right opens on `velocityX < 0`; Left opens on `velocityX > 0`); dominance and speed rules unchanged.
  - `GestureController` gains `private val side: () -> ScreenSide` as its first constructor parameter and passes `side()` into `shouldOpenOnFling` in `onFling`.

- [ ] **Step 1: Write the failing test**

In `ShouldOpenOnFlingTest.kt`, add `ScreenSide.Right` as the first argument to every existing `shouldOpenOnFling(...)` call (assertions unchanged). Then add:

```kotlin
@Test
fun `left side opens on a rightward fling`() {
    assertTrue(shouldOpenOnFling(ScreenSide.Left, velocityX = 1200f, velocityY = 0f,
        horizontalTravelPx = 60f, touchSlopPx = 20f, minFlingVelocityPx = 400f))
}

@Test
fun `left side ignores a leftward fling`() {
    assertFalse(shouldOpenOnFling(ScreenSide.Left, velocityX = -1200f, velocityY = 0f,
        horizontalTravelPx = -60f, touchSlopPx = 20f, minFlingVelocityPx = 400f))
}

@Test
fun `right side opens on a leftward fling`() {
    assertTrue(shouldOpenOnFling(ScreenSide.Right, velocityX = -1200f, velocityY = 0f,
        horizontalTravelPx = -60f, touchSlopPx = 20f, minFlingVelocityPx = 400f))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `source ~/tools/android-env.sh && ./gradlew testDebugUnitTest --tests "com.awannawa.vbutton.overlay.ShouldOpenOnFlingTest" --console=plain`
Expected: FAIL — `shouldOpenOnFling` has no `side` parameter (and the existing calls no longer compile).

- [ ] **Step 3: Implement**

Change the pure function; the first guard becomes side-aware and the rest of the rule is unchanged:

```kotlin
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
    if (abs(velocityX) <= abs(velocityY)) return false
    if (abs(velocityX) < minFlingVelocityPx) return false
    return abs(horizontalTravelPx) >= touchSlopPx
}
```

Give `GestureController` a `private val side: () -> ScreenSide,` first parameter (before `onOpen`) and in `onFling` pass `side = side()` to `shouldOpenOnFling(...)`. Its KDoc should mention the side provider mirrors the opening direction.

- [ ] **Step 4: Update the `GestureController` call site in `OverlayView`**

The constructor now needs a side provider; `OverlayView` constructs it in Task 5. To keep this task compiling on its own, pass `side = { Prefs.screenSide(context) }` at the existing `GestureController(...)` construction in `OverlayView` now (this is the only change to `OverlayView` in this task).

- [ ] **Step 5: Run test to verify it passes**

Run: `source ~/tools/android-env.sh && ./gradlew compileDebugKotlin testDebugUnitTest --tests "com.awannawa.vbutton.overlay.ShouldOpenOnFlingTest" --console=plain`
Expected: PASS (existing tests plus 3 new) and BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/awannawa/vbutton/overlay/GestureController.kt app/src/main/java/com/awannawa/vbutton/overlay/OverlayView.kt app/src/test/java/com/awannawa/vbutton/overlay/ShouldOpenOnFlingTest.kt
git commit -m "feat: mirror the opening fling direction to the screen side"
```

---

### Task 5: `HandleView` and `OverlayView` full mirror

**Files:**
- Modify: `app/src/main/java/com/awannawa/vbutton/overlay/HandleView.kt`
- Modify: `app/src/main/java/com/awannawa/vbutton/overlay/OverlayView.kt`

**Interfaces:**
- Consumes: `Prefs.screenSide` (Task 3); `SafeArea.insetForSide` (Task 2); `gravityFor`, `inwardOffsetPx`, `ScreenSide` (Task 1); `GestureController(side = ...)` construction already updated in Task 4.
- Produces: no new public API. `HandleView` takes `side: () -> ScreenSide`; `OverlayView` re-reads the stored side each layout pass so `applyConfig()` applies a live side change.

- [ ] **Step 1: `HandleView` draws the bar on the side edge**

Add `private val side: () -> ScreenSide,` to the constructor (after `gestureController`). In `onSizeChanged`, replace the hard-coded right bounds with:

```kotlin
val barPx = dp(Tokens.HANDLE_BAR_WIDTH_DP)
val left = if (side() == ScreenSide.Left) 0 else width - barPx
bar.setBounds(left, 0, left + barPx, height)
```

- [ ] **Step 2: `OverlayView` side plumbing**

- Construct `HandleView(context, gestureController, side = { Prefs.screenSide(context) })`.
- Add a helper that reads the side once: `private fun currentSide(): ScreenSide = Prefs.screenSide(context)`.
- Add a park offset helper that mirrors with the side:

```kotlin
private fun parkOffsetPx(side: ScreenSide): Float {
    val width = dp(effectivePreset.panelWidthDp).toFloat()
    return if (side == ScreenSide.Left) -width else width
}
```

- Use it in the `init` block (initial park), `showCollapsed` (animate target), and `applyPreset` (hidden-park value). `showExpanded` keeps target `0f`.

- [ ] **Step 3: Drag pivot on the side edge**

In `startDrag`, set the pivot from the side so the 1.5x growth points inward:

```kotlin
handleView.pivotX = if (currentSide() == ScreenSide.Left) 0f else handleView.width.toFloat()
```

`pivotY` and the scale/alpha lines stay as they are.

- [ ] **Step 4: Gravity and inward `x` in `recomputeLayout()`**

Replace the hard-coded `params.x = screen.rightInsetPx` with side-aware values computed once at the top of the pass:

```kotlin
val side = currentSide()
// ...on the params:
params.gravity = gravityFor(side)
params.x = inwardOffsetPx(side, screen.leftInsetPx, screen.rightInsetPx)
```

Leave the existing `updateWindowLayout()` call as the single guarded update. `applyConfig()` already calls `recomputeLayout()`, so a side change from settings applies live; the `params.y` branch stays exactly as it is so `yRatio` still owns Y.

- [ ] **Step 5: Run compile and full tests**

Run: `source ~/tools/android-env.sh && ./gradlew compileDebugKotlin testDebugUnitTest --console=plain`
Expected: BUILD SUCCESSFUL; all unit tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/awannawa/vbutton/overlay/HandleView.kt app/src/main/java/com/awannawa/vbutton/overlay/OverlayView.kt
git commit -m "feat: mirror handle bar, panel slide, and drag pivot by screen side"
```

---

### Task 6: Settings UI "Screen side" row

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/layout/activity_settings.xml`
- Modify: `app/src/main/java/com/awannawa/vbutton/SettingsActivity.kt`

**Interfaces:**
- Consumes: `Prefs.screenSide`, `Prefs.setScreenSide` (Task 3); `ScreenSide` (Task 1).
- Produces: a `RadioGroup` id `side_group` with `side_right` and `side_left`, wired and refreshed.

- [ ] **Step 1: Add strings**

In `strings.xml`, near the size strings:

```xml
<string name="section_screen_side">Screen side</string>
<string name="side_right">Right</string>
<string name="side_left">Left</string>
```

- [ ] **Step 2: Add the row to the layout**

Immediately after the size `RadioGroup` (a plain row of platform widgets, sentence case, `DESIGN.md` section 3):

```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="16dp"
    android:text="@string/section_screen_side"
    android:fontFamily="sans-serif-medium"
    android:textSize="14sp" />

<RadioGroup
    android:id="@+id/side_group"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="4dp"
    android:orientation="horizontal">

    <RadioButton
        android:id="@+id/side_right"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/side_right"
        android:textSize="16sp" />

    <RadioButton
        android:id="@+id/side_left"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/side_left"
        android:textSize="16sp" />
</RadioGroup>
```

- [ ] **Step 3: Wire and refresh in `SettingsActivity`**

Add `import com.awannawa.vbutton.overlay.ScreenSide`. In `wireListeners()`:

```kotlin
findViewById<RadioGroup>(R.id.side_group).setOnCheckedChangeListener { _, checkedId ->
    val side = if (checkedId == R.id.side_left) ScreenSide.Left else ScreenSide.Right
    Prefs.setScreenSide(this, side)
}
```

In `refreshUi()`, next to the size group check:

```kotlin
findViewById<RadioGroup>(R.id.side_group).check(
    if (Prefs.screenSide(this) == ScreenSide.Left) R.id.side_left else R.id.side_right,
)
```

- [ ] **Step 4: Run compile and tests**

Run: `source ~/tools/android-env.sh && ./gradlew compileDebugKotlin testDebugUnitTest --console=plain`
Expected: BUILD SUCCESSFUL; all tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/layout/activity_settings.xml app/src/main/java/com/awannawa/vbutton/SettingsActivity.kt
git commit -m "feat: add the Screen side setting to the settings screen"
```

---

### Task 7: Docs, design check, and final verification

**Files:**
- Modify: `DESIGN.md`
- Modify: `docs/virtual-buttons/01-ui-spec.md`
- Modify: `docs/virtual-buttons/README.md`
- Modify: `docs/virtual-buttons/DEVICE-QA.md`

**Interfaces:** none (documentation only).

- [ ] **Step 1: Update `DESIGN.md`**

Add one line in section 2 (or near section 3) stating the handle/panel edge is a user setting: right by default, left mirrors it; still `Gravity.RIGHT`/`LEFT`, never `END`/`START`; no new token.

- [ ] **Step 2: Update `01-ui-spec.md` section 4**

Add: "Screen side is a setting (right default). On the left every rule mirrors: bar flush to the left bound, panel slides in from the left, opening fling is rightward, pivot at the left edge. `Gravity.LEFT` (physical), never `START`. The left inset comes from WindowMetrics on API 30+; the 28/29 fallback reads 0 on the left, the same limitation as the right inset."

- [ ] **Step 3: Update `README.md`**

Add the screen-side setting to the feature list, and append a handover entry (date, task, files changed, "needs device test: left-side mirror visuals and fling on device").

- [ ] **Step 4: Update `DEVICE-QA.md`**

Add a step: set the side to Left; verify the handle is flush left over both a white and a black app; the panel opens from the left at the same Y; a rightward swipe opens it while a leftward swipe and the Back gesture do not navigate; the drag pivot grows inward; rotation keeps it on the left; the setting survives service off/on and a reboot; then set it back to Right and confirm the original behaviour.

- [ ] **Step 5: Design check and full verification**

Run: `source ~/tools/android-env.sh && ./gradlew compileDebugKotlin lintDebug testDebugUnitTest --console=plain`
Expected: BUILD SUCCESSFUL; lint reports no new errors; all unit tests pass.

Answer the `DESIGN.md` section 5 checklist for this diff (no new colours or tokens, no gradient/shadow/blur, no emoji icons, overlay still text-free, physical `LEFT`/`RIGHT`, every button at least 48dp with a `contentDescription`) and record the answers in the handover entry.

Confirm the manifest is untouched: `git diff --stat app/src/main/AndroidManifest.xml` shows no change (still no `INTERNET`).

- [ ] **Step 6: Commit**

```bash
git add DESIGN.md docs/virtual-buttons/01-ui-spec.md docs/virtual-buttons/README.md docs/virtual-buttons/DEVICE-QA.md
git commit -m "docs: document the screen side setting"
```

---

## Self-Review

**1. Spec coverage.**
- Right/left option: Task 1 (enum) + Task 3 (Prefs) + Task 6 (UI).
- Full mirror: bar edge (T5), panel slide (T5), pivot (T5), opening fling (T4), gravity and offset (T3), left inset (T2).
- Live while active: `applyConfig()` already fired by the Prefs listener; T5 `recomputeLayout` reads the side every pass.
- Remembered after restart/reboot: Prefs key restored on connect (T3).
- Keep Y: no Y change; `yRatio` reused (T5 leaves the Y branch untouched).
- Design check, no `INTERNET`, no dependencies: T7.

**2. Step scan.** Every task has a test (or a documented reason the thin `Prefs` wrapper has none, matching the project), a failing run, an implement step, a passing run, and a commit. No "handle edge cases" filler. T7 steps are concrete doc edits with a real verification command. Task 4 Step 4 keeps the build green mid-plan by moving one `OverlayView` constructor argument; Task 5 does not repeat it.

**3. Type consistency.** `ScreenSide`, `fromStoredOr`, `STORED_LEFT`/`STORED_RIGHT`, `gravityFor`, `inwardOffsetPx`, `insetForSide`, `shouldOpenOnFling(side, ...)`, `Prefs.screenSide`/`setScreenSide`/`SCREEN_SIDE_DEFAULT`, and `ScreenInsets.leftInsetPx` are each introduced once (Tasks 1–4) and re-used with the same names and signatures later. The `side: () -> ScreenSide` provider is used consistently in `GestureController` and `HandleView`; `OverlayView` reads `Prefs` through `currentSide()`.

**4. Review Focus.** The five lines are each pinned: RTL gravity (T1), left inward offset (T1), mirrored swipe (T4), live switch (T5 compile/test + T7 device step), unreadable stored side (T1).

**5. Proportion.** The plan is about the size of the change. Code blocks are signatures, test bodies, and the two short pure helpers; the view edits are described as one-line target/side changes rather than transcribed bodies.

---

## Execution Handoff

Plan saved to `docs/superpowers/plans/2026-11-01-left-right-handle.md`.

Two execution approaches:

- **Subagent-driven** — a fresh subagent implements each task and a fresh reviewer checks it before the next starts, then a whole-branch review. Most thorough; costs a fresh context per task and per review.
- **Native** — one session implements every task, then a single fresh reviewer checks the whole branch. Fastest.

Recommendation: **Native**. The tasks form one tight chain (enum, then insets, then Prefs/service, then gesture, then views, then UI, then docs) where each task's interfaces feed the next, so a single continuous session keeps the names coherent with less handoff overhead than seven separate subagent contexts. The remaining risk (left-side visuals) is device-only and is captured in Task 7, not solved by more review contexts.
