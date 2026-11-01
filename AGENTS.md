# AGENTS.md: virtual-buttons

Read by the orchestrator and every worker. Place it at the project root, next to the `docs/` folder.

## About the project

- A native Android app (Kotlin) that provides virtual Volume +/- and Power buttons as an AccessibilityService overlay on the right edge of the screen. Target device: Vivo T1 Pro (Android 12 or newer), minSdk 28.
- Sources of truth: `DESIGN.md` at the project root (locked visual tokens and anti-slop rules), and in `docs/virtual-buttons/`: `00-main-task.md` (PRD), `01-ui-spec.md` (icons, sizes, responsive rules), `README.md` (worker guide, status, handover), and `tasks/T01` through `T11`.
- If documents conflict: `DESIGN.md` wins for look and feel, `01-ui-spec.md` wins for icons, sizes, and responsive rules, the task file wins for work steps, and the PRD wins for scope.
- Product principles that must never be broken: no `INTERNET` permission, no analytics or ads, never read screen content, low resource usage, no Compose, and the only runtime dependency allowed is `androidx.core:core-ktx` (optional).

## Roles

- Orchestrator: splits and delegates tasks, monitors workers, audits results independently, commits, and reports to the project owner.
- Worker: completes exactly one task in a fresh session, then reports.

## Orchestrator flow

- One task per worker, fresh session, delegated sequentially in the order given under "Implementation order" in `README.md`.
- Every worker prompt must contain: role, path of the task file, required reading, required skills, file boundaries, acceptance criteria, prohibitions, and the report format (see Worker prompt template).
- Inject the skill `/home/awan/.agents/skills/andrej-karpathy-skills/SKILL.md` into every worker prompt if that file exists.
- For UI tasks (T03, T05, T07, T08, T11) also inject the `hallmark` skill (see Skills and design rules) and tell the worker to read `DESIGN.md`.
- Monitor workers with a heartbeat every 30 seconds. If there is no new output, inspect the process tree (`ps -ef`). If there is no response for 5 minutes, send a follow-up asking what is blocking the worker, then check again.
- After a worker finishes, audit independently: run `./gradlew compileDebugKotlin lintDebug testDebugUnitTest` yourself, read `git diff`, compare against the task's acceptance criteria, check the maintainability rules below, and confirm the manifest has no `INTERNET` permission.
- For UI tasks, also run the design check in `DESIGN.md` section 5 and a Hallmark-style audit of the changed UI files. Fix real violations before moving on, and ignore findings that only apply to web pages.
- Security, error-handling, and lifecycle findings are fixed before the next task starts. Do not defer them to UI or release tasks.
- After the audit passes: commit (one commit per task, message `T0X: short summary`), then update the status and handover entry in `README.md`.
- Send a per-task report to the project owner, then start the next task immediately without waiting for a reply.
- Device testing happens only if `adb` and a device are available. Otherwise mark "needs device test" in the report and never claim it passed.

## Worker rules

- Before writing code, read `00-main-task.md`, `01-ui-spec.md`, your task file, and the "Cross-task integration points" section below. For UI tasks, also read `DESIGN.md` and load the skills listed under "Skills and design rules".
- Work only within the task scope. Problems in other tasks go into the Notes section of the task file and must not be fixed silently.
- Change only the files listed under "Related files" in your task, plus new files that are genuinely needed. Do not refactor files owned by other tasks without orchestrator approval.
- Allowed commands: `./gradlew compileDebugKotlin`, `lintDebug`, `testDebugUnitTest`, `assembleDebug`.
- Forbidden outside T10: `assembleRelease`, `bundleRelease`, and touching signing or the keystore.
- Forbidden in every task: adding runtime dependencies, changing Gradle, AGP, or Kotlin versions, and adding manifest permissions without approval.
- Forbidden: timers, polling, or receivers that run while idle, accessibility event subscriptions, `GlobalScope`, and new threads without a written reason.
- Done means meeting the Definition of Done below. Tick a status in `README.md` only after verification passes.

## Skills and design rules

- All tasks: `andrej-karpathy-skills` (path in the Orchestrator flow section).
- UI tasks (T03, T05, T07, T08, T11): also `hallmark`, an anti-AI-slop design skill (upstream `github.com/Nutlope/hallmark`, installed with `npx skills add nutlope/hallmark`). The orchestrator fills in the install path that matches the worker tool (for example `.codex/skills/hallmark/SKILL.md` for Codex or `~/.claude/skills/hallmark/` for Claude Code).
- Hallmark is written for web UI. Apply the principles that fit a native Android overlay and settings screen: restraint, locked tokens with no improvised values, no decorative filler, no invented content, and no re-drawn system chrome. Ignore its web-only parts (landing-page structures, theme picking, HTML or CSS output).
- `DESIGN.md` is the locked design system for this project. If Hallmark and `DESIGN.md` disagree, `DESIGN.md` wins.
- If you need a value that `DESIGN.md` does not define, stop and ask the orchestrator. The orchestrator updates `DESIGN.md` first, then the worker continues.

## Maintainability rules (mandatory)

The goal is code that stays correct when combined with later tasks, not code that merely works today, so that audit and fix cycles are not repeated.

- Single source of truth: all sizes, colors, and durations live in one place (`Tokens` or resources). No magic numbers scattered across files. Values must come from `DESIGN.md` and `01-ui-spec.md`. Never invent a value that is not defined there. Ask the orchestrator instead.
- All SharedPreferences keys and their default values live only in `Prefs.kt`.
- Overlay state is one explicit enum (`Collapsed`, `Expanded`, `Dragging`), not several interdependent booleans.
- Separation of concerns: rendering in Views, gesture logic in `GestureController`, system actions in `Actions`. A View must never call `AudioManager` or the service directly.
- Panel buttons are defined as a list of `ButtonSpec` (id, icon, contentDescription, tap action, long-press action, repeatable) starting in T03, so T11 only adds items without modifying `PanelView`.
- Configuration from settings enters through a single `applyConfig()` function on the overlay, so T08 does not have to rewrite T03 through T07.
- Pure logic (position clamping, `yRatio` conversion, compact preset selection) is separated from Android APIs and covered by JVM unit tests.
- Lifecycle: every view, Handler callback, listener, and receiver that is created must be cleaned up in `onUnbind` or `onDestroy`. `attach`, `detach`, and `setExpanded` must be idempotent.
- Threading: all UI work runs on the main thread.
- Error handling: `addView`, `removeView`, and `updateViewLayout` are wrapped in specific try-catch blocks with log tag `VButton`. The boolean result of `performGlobalAction` is checked and logged.
- API levels: calls above minSdk are wrapped in `Build.VERSION.SDK_INT` guards with a fallback. They must never crash.
- No mutable global state except `VButtonService.instance`, and no singleton that holds a `Context` or `View` without releasing it.
- One file, one responsibility. Keep functions small. Follow the names in the PRD project structure (section 11). Comments explain why (for example, why `Gravity.RIGHT` and not `END`), not what the code does.
- Remove temporary code (debug buttons, excessive logging) before marking a task done.

## Cross-task integration points

- T02 provides `VButtonService.instance` and the overlay attach and detach functions, used by T03 and T08.
- T03 provides `OverlayView.setExpanded()`, the `ButtonSpec` list, and `applyConfig()`, used by T04, T06, T08, and T11.
- T04 provides the function that resets the auto-collapse timer, called by T06 on every button action.
- T05 creates a single `SafeArea.compute()` function (simple first version) and stores `yRatio` in `Prefs`. T07 improves the same function (insets, compact mode) without changing its callers.
- T06 wraps system actions in `Actions` behind a stable interface, which T11 uses for extra buttons.
- T08 only reads and writes `Prefs`. Changes reach the overlay through `applyConfig()`.

## Definition of Done

- The task's acceptance criteria pass. Anything that requires a device is marked "needs device test".
- `compileDebugKotlin`, `lintDebug` (no new errors), and `testDebugUnitTest` pass.
- The manifest has no `INTERNET` permission, there are no new runtime dependencies, and there is no dead code or unexplained TODO.
- For UI tasks, the design check in `DESIGN.md` section 5 passes.
- `README.md` is updated: the task status and one handover entry.

## Report format

- Status: done, partial, or blocked.
- Files changed.
- Checks run (command and short result).
- Findings and caveats.
- What still needs device testing.

## Worker prompt template

```
Role: Android (Kotlin) implementation worker for the virtual-buttons project.
Task: <Tnn task name>. Read docs/virtual-buttons/tasks/<task file>.
Required reading first: AGENTS.md, docs/virtual-buttons/00-main-task.md, docs/virtual-buttons/01-ui-spec.md, docs/virtual-buttons/README.md, and DESIGN.md (UI tasks).
Required skills: andrej-karpathy-skills (all tasks) and hallmark (UI tasks T03, T05, T07, T08, T11). Skill paths: <paths>.
File boundaries: only <files from "Related files"> and new files that are genuinely needed.
Acceptance criteria: <copy from the task file>.
Prohibitions: no assembleRelease, no new runtime dependencies, no new permissions, no scope changes.
When done: run compileDebugKotlin, lintDebug, testDebugUnitTest (UI tasks: also the design check in DESIGN.md section 5). Update README.md (status and handover). Report using the format in AGENTS.md.
```
