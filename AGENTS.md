# AGENTS.md — Race Data Logger

Behavioral guidance for AI agents working on this codebase.

---

## General Principles

- **Work methodically**: Step through changes one file at a time. Verify dependencies and side effects (like breaking Previews or Fusion logic) before proceeding.
- **Ask before assuming**: If a requirement is ambiguous or a system constraint is unclear, ask the user for clarification instead of guessing.
- **Respect established patterns**: Follow the existing architecture (e.g., stateless UI content, world-frame fusion, monotonic timing) even if you prefer a different approach.
- **Safety first**: Never use shell commands like `sed` or `awk` to edit files. Use the provided buffer-safe tools to ensure the IDE stays in sync.
- **Minimize Scope**: Keep bug fix actions strictly contained to the fix itself. Do not perform unrelated refactoring or "clean up" unless explicitly requested.
- **Preserve Context**: Do not modify or delete comments that are not directly related to the code you are changing.
- **Plan Mode**: When the user ask you to plan something, do not modify any files. Only provide the execution plan.

---

## Workflow Protocols (STRICT COMPLIANCE REQUIRED)

To prevent unrequested refactors and maintain extreme precision, the following protocols are **MANDATORY**. Failure to follow these will result in immediate rejection of the work.

### 1. Plan-then-Confirm (The "Two-Turn" Rule)
For any change that isn't a single-line trivial fix, you **MUST** follow this sequence:
1. **Phase 1 (Plan)**: Describe the proposed changes in plain English. List the exact files, methods, and line numbers you intend to touch. **PROHIBITION**: You must not invoke any write tools (write_file, replace_text, etc.) during this turn.
2. **Phase 2 (Review)**: Wait for the user to provide a "Green Light" or specific feedback.
3. **Phase 3 (Execute)**: Apply only the agreed-upon changes using the most surgical tool available.

### 2. Surgical Precision via `replace_text`
- **STRICT PREFERENCE**: You **MUST** prefer `replace_text` over `write_file`. Overwriting an entire file is considered a "heavy-handed" action and is the primary cause of accidental context loss.
- **Technical Constraint**: Using `replace_text` forces you to match existing strings exactly, which technically prevents you from "cleaning up" unrelated code or deleting comments.
- **Additive by default**: Refactors and logic changes should be additive. Never delete or "simplify" existing comments, statistics, or documentation unless explicitly commanded to "Refactor for Brevity".

---

## Domain Knowledge

### Units and sign conventions — be explicit, never assume

This codebase mixes several unit systems. Always check which one a variable is in before using it. When adding new variables, always include the unit in the name or a comment.

| Quantity | Internal unit | Display unit | Notes |
|---|---|---|---|
| Speed | m/s | km/h | Convert only at the UI boundary (`* 3.6f`) |
| Acceleration | m/s² | m/s² or G | 1G = 9.81 m/s² |
| Time intervals | nanoseconds (Long) | seconds (Float) | GPS/IMU use Nanos; Fusion uses Seconds (dt) |
| Distance | meters | km | |

Acceleration is **signed** in the fusion layer. Positive = speeding up, negative = braking. The `derivedAccelMs2` in `FusedVelocity` explicitly tracks this scalar change in speed.

### Coordinate frames — always state which frame you're in

There are two acceleration coordinate systems in play:

- **Device frame**: Raw axes from `Sensor.TYPE_LINEAR_ACCELERATION`. Relative to the phone's physical orientation.
- **World frame**: Axes fixed to the Earth (North/East/Up). This is what the Kalman filter uses.

The `ImuCollector` rotates data from device → world frame using `TYPE_ROTATION_VECTOR`. 
- `accelX` in world frame = East
- `accelY` in world frame = North
- `accelZ` in world frame = Up

### Timing — monotonic clocks only

All timing logic must use `SystemClock.elapsedRealtimeNanos()` (monotonic) or the timestamps provided by the sensors themselves (also monotonic). 

- **NEVER** use `System.currentTimeMillis()` for duration or interval calculations.
- Use `System.currentTimeMillis()` **ONLY** for storing the "wall clock" start time of a session.

---

## Velocity Fusion & Kalman Filter

The project uses a custom `VelocityFusion` engine (see `VelocitiFusion.kt`) to combine GPS and IMU data.

- **Dual 1D Kalman Filters**: One for the North axis, one for the East axis.
- **Predict Phase**: Uses IMU world-frame acceleration to advance the velocity estimate.
- **Update Phase**: Uses GPS velocity (decomposed into N/E via bearing) to correct the estimate.
- **ZUPT (Zero Velocity Update)**: When both GPS and IMU agree the vehicle is stopped, a 0m/s measurement is injected with very low noise (`0.01f`) to kill IMU drift.

**Rules for modification:**
1. **GPS Gating**: Do not update the filter with GPS fixes older than 300ms or with accuracy worse than 20m.
2. **ZUPT Logic**: Do not relax the "both GPS and IMU must agree" requirement without a very strong reason. 
3. **DT is Dynamic**: Always calculate the actual `dt` (in seconds) between fusion ticks using monotonic timestamps.

---

## UI and Previews

The app uses a modern Compose-based UI with a Navigation Suite (Bottom Bar / Rail).

- **Stateless Content**: Compose screens must be split into a stateful wrapper (handling ViewModels) and a stateless `Content` composable (taking raw data and lambdas).
- **Previews**: All main screens are previewable in `Previews.kt`. They should be wrapped in `SpeedAppShell` to visualize the navigation context.
- **MANDATORY**: You must check and update the corresponding `@Preview` in `ScreenPreviews.kt` after any UI change. Always use the `render_compose_preview` tool to verify the visual result.
- **Theme**: Use `RaceLoggerTheme` for all previews to ensure correct color schemes.

---

## Android Lifecycle & Services

- **Foreground Service**: `RaceRecordingService` handles sensor collection. It must remain active even when the screen is off.
- **WakeLock**: A `PARTIAL_WAKE_LOCK` is used during recording. Ensure it is acquired on start and released on stop.
- **Permissions**: The app requires `ACCESS_FINE_LOCATION` and `POST_NOTIFICATIONS` (on API 33+). These are requested in `MainActivity`.

---

## Data Persistence

- **Room Database**: All `DataPoint` entries are stored in Room.
- **Sessions**: A session is defined by a unique `sessionId` (UUID).
- **Export**: Data is exported to CSV via `RaceRepository` and shared using `FileProvider`.

---

## When Making Changes

- **Before changing a threshold** (ZUPT, GPS age, Kalman Q/R): Read the detailed comments in `VelocitiFusion.kt` explaining why those values were chosen.
- **Before adding a sensor**: Ensure its coordinate frame is handled correctly (device vs. world).
- **UI Changes**: Verify that the changes don't break the `Previews.kt` visual tests. Always run `render_compose_preview` to see the actual UI.
