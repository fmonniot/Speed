# Testing Plan — Speed App

This document outlines the strategy for verifying the core logic, sensor fusion, and UI of the Speed application. 

---

## 1. Unit Testing (Local JVM)
Focus on pure logic that is independent of the Android framework.

### 1.1. Velocity Fusion (`VelocitiFusion.kt`)
- [ ] **Kalman Filter Convergence**: Feed static GPS and IMU data to ensure the filter stabilizes at the expected velocity.
- [ ] **Predict Phase**: Verify that world-frame acceleration correctly advances the velocity estimate using dynamic `dt`.
- [ ] **Update Phase**: Verify that GPS velocity (decomposed into N/E) corrects the estimate within the 300ms gating window.
- [ ] **ZUPT (Zero Velocity Update)**: Mock scenarios where GPS and IMU both indicate < threshold movement. Verify the filter snaps to exactly 0 m/s with low noise.
- [ ] **Timing Logic**: Ensure `dt` calculations correctly handle monotonic nanosecond timestamps.

### 1.2. Coordinate Transformations
- [ ] **Device to World Frame**: Test `ImuCollector` rotation logic using known rotation vectors (e.g., identity, 90° tilt, 180° flip) to ensure East/North/Up axes are correctly assigned.

### 1.3. Unit Conversions
- [ ] **Speed**: m/s to km/h (multiplier 3.6).
- [ ] **Acceleration**: m/s² to G (divisor 9.81).

---

## 2. Instrumented Testing (On-Device)
Focus on Android components and hardware abstractions.

### 2.1. Database (`RaceDatabase.kt`)
- [ ] **DataPoint Persistence**: Verify that `DataPoint` entries are correctly saved and retrieved by `sessionId`.
- [ ] **Session Management**: Verify deletion of sessions and cascading deletes of associated data points.

### 2.2. Service & Lifecycle (`RaceRecordingService.kt`)
- [ ] **Service Start/Stop**: Verify foreground service transitions and notification visibility.
- [ ] **WakeLock Management**: Ensure `PARTIAL_WAKE_LOCK` is acquired during recording and released afterward.
- [ ] **Sensor Collection**: Verify that `GpsCollector` and `ImuCollector` successfully register listeners and emit data.

---

## 3. UI & Compose Testing
Focus on navigation and state-to-visual mapping.

### 3.1. Navigation
- [ ] **Bottom Bar/Rail**: Verify that clicking "Race", "Sessions", and "Settings" routes to the correct screens.
- [ ] **Session Detail**: Verify navigation to `session_detail/{sessionId}` passes the correct ID and loads data.

### 3.2. Visual Verification
- [ ] **Stateless Previews**: Use `Previews.kt` (with `SpeedAppShell` wrapper) to verify layouts for:
    - [ ] Idle state (Start button visible).
    - [ ] Recording state (Stop button, active timers).
    - [ ] Empty sessions list vs. populated list.
- [ ] **Permission Handling**: Verify the UI handles the absence of `ACCESS_FINE_LOCATION` gracefully.

---

## 4. Integration & "Field" Tests (Manual/ADB)
End-to-end verification of user flows.

### 4.1. The "Golden" Recording Flow
1. Start recording.
2. Simulate movement (via `adb shell geo fix` or Emulator extended controls).
3. Stop recording.
4. Verify session appears in history.
5. Export CSV and verify the `ACTION_SEND` intent contains the correct URI.

### 4.2. Static Drift Test
1. Place device on a flat surface.
2. Start recording for 5 minutes.
3. Verify that ZUPT logic prevents "phantom movement" (speed should stay at 0.0 km/h).

---

## 5. Raw Data Recording & Replay
High-fidelity logic verification using real-world sensor traces.

### 5.1. Raw Logging Infrastructure
- [ ] **Define `RawSensorSink` Interface**: To decouple sensor collectors from the storage mechanism.
- [ ] **Implement `FileRawSink`**: A high-performance logger (CSV or binary) to record raw high-frequency `ImuSample` (100Hz) and `Location` (1Hz) data.
- [ ] **Instrumentation**: Update `ImuCollector` and `GpsCollector` to optionally pipe data into a `RawSensorSink`.
- [ ] **Dev Toggle**: Add a "Record Raw Traces" switch in the Settings screen to enable/disable this logging.

### 5.2. Test Replayer
- [ ] **Implement `SensorReplayer`**: A test utility that reads raw trace files and emits them as `SharedFlow<ImuSample>` and `SharedFlow<Location>`.
- [ ] **"Black Box" Fusion Test**: A JUnit test that feeds a recorded "Braking" or "Acceleration" trace into `DataFusion` and asserts that the resulting `DataPoint` output matches expected physical bounds.

---

## Progress Tracking
| Phase | Task | Status |
|---|---|---|
| Unit | VelocitiFusion Kalman Logic | ⏳ Pending |
| Unit | Unit Conversions | ⏳ Pending |
| DB | Room CRUD | ⏳ Pending |
| UI | Navigation Suite | ⏳ Pending |
| E2E | Export Flow | ⏳ Pending |
| Raw | Raw Logging Infrastructure | ⏳ Pending |
| Raw | Sensor Replayer Logic | ⏳ Pending |
