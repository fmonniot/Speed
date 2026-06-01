# Testing strategy & remediation plan

## Assessment (2026-05-31)

Verified against the code, build config, and a live test run — not taken on faith from the
earlier write-up (parts of which were inaccurate; see "Corrections" below).

**The unit layer is genuinely good — keep it.**
- ~2,900 LOC of JVM/Robolectric tests across `domain`, `fusion`, `export`, `util`.
- `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL, all green.
- `VelocityCalibrationTests` drives the Kalman filter against 6 real CSV traces in
  `src/test/resources/raw_traces/`. This is a real asset. Pure computation like this is
  correctly unit-tested and should stay that way.

**The instrumented layer is the problem — and worse than "stale tests":**

| Issue | Reality |
|---|---|
| No CI | There is no `.github/workflows`, no CI of any kind. Nothing runs automatically. |
| No device in the loop | `adb` isn't even installed locally. `androidTest` is effectively write-only. |
| `NavigationTest` stale | Asserts `"START RACE"`, `"Past Sessions"`, `onNodeWithContentDescription("Sessions")`. Real nav is `Ride/Trips/Stats/Settings`, home heading `"Ready to ride"`. Compiles, but every assertion would fail on first run. |
| Duplicate test | `FusionIntegrationTest.kt` exists in both `test/` (Robolectric, runs) and `androidTest/` (instrumented, never runs), testing the same path. |
| `RaceRecordingServiceTest` | Never asserts satellites/accuracy reset (the `a3bade2` bug); uses bare `Thread.sleep(1000)`. |
| `Example*Test` stubs | Both boilerplate, never removed. |

**The real coverage gap:** the entire behavior layer is untested — `RaceViewModel` (301 LOC:
`createSegment`+haversine, export orchestration, toggle/record logic) and every Compose screen.
This is the code most likely to break on refactor.

### Corrections to the earlier write-up
- It claimed `ui-test-junit4` "just needs to be added to `androidTestImplementation`." It is
  already present (`app/build.gradle.kts:101`, plus `ui-test-manifest:103`). The infra is wired
  up and unused, not missing.
- It implied "CI would show it passing." There is no CI; the tests simply never execute.

## Strategic direction

Two test layers exist but only one *runs*. In a single-dev, no-CI, no-emulator setup, any test
that needs a device is dead weight. So "prefer end-to-end" here means: push integration-level
tests **down into the JVM/Robolectric suite** that runs on every `./gradlew test`, and keep the
device-bound `androidTest` set as small as possible — then make even that small set runnable in CI.

Robolectric gives a real `Application`; Room has an in-memory builder; Compose screens take plain
params — so real ViewModel + real Room + real screen rendering can be tested on the JVM without a
device. That is genuine end-to-end confidence that executes for free on every build.

---

## Plan

### Phase 1 — Cleanup (hygiene, zero risk)
1. Delete `ExampleUnitTest`, `ExampleInstrumentedTest`.
2. Delete the instrumented `FusionIntegrationTest` (keep the Robolectric one that runs).
3. Delete the stale `NavigationTest` (rewritten as a Robolectric test in Phase 2, where it runs).
4. Fix `RaceRecordingServiceTest`: add satellites/accuracy-reset assertions; replace `Thread.sleep`
   with a poll helper (`awaitCondition { ... }`).

### Phase 2 — Build the missing behavior layer (JVM/Robolectric, runs on every build)
These live in `src/test/` so they execute in the fast, free suite:
1. **ViewModel + Room integration tests** (highest leverage, the "E2E-ish" win): real in-memory
   Room + real `RaceRepository` + real `RaceViewModel`. Cover create-segment, delete-session,
   stats computation, export wiring.
2. **Robolectric Compose screen tests** via `createComposeRule()` — establish the pattern.
   First: `RideHomeScreen` stop-chip (`StopChip`, `contentDescription = "Stop sensors"`, visible
   only when `isSensorsEnabled`, fires `onStop`). Then a nav smoke test replacing the deleted
   `NavigationTest`.

### Phase 3 — Shrink `androidTest` to only what truly needs a device
Move device-independent tests to `src/test/` (Robolectric):
1. `RaceDatabaseTest` / `SegmentDaoTest` → Robolectric + `Room.inMemoryDatabaseBuilder()`.
2. Remove the redundant instrumented `FusionIntegrationTest` (done in Phase 1).
3. Leave behind in `androidTest/` only what genuinely needs a device:
   `RaceRecordingServiceTest` (foreground service, wakelock, real `Binder`) and any real GPS/IMU
   sensor-pipeline tests.

### Phase 4 — CI (makes all the above durable)
Without CI, every fix above rots again. Two-job GitHub Actions workflow:

1. **`unit` job** (every push, ~1 min, free): `./gradlew :app:test :app:lint`. After Phases 2–3
   this covers Room, fusion, ViewModel+repository, and Compose/nav behavior.
2. **`instrumented` job** — Gradle Managed Devices with an ATD (Automated Test Device) image on a
   KVM-accelerated `ubuntu-latest` runner, for the handful of true device tests. Add to
   `app/build.gradle.kts`:
   ```kotlin
   android.testOptions.managedDevices.localDevices {
       create("pixel30atd") {
           device = "Pixel 6"
           apiLevel = 30
           systemImageSource = "aosp-atd"   // headless, faster, lower flake
       }
   }
   ```
   CI runs `./gradlew pixel30atdDebugAndroidTest` after enabling KVM:
   ```yaml
   - run: |
       echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666"' | sudo tee /etc/udev/rules.d/99-kvm4all.rules
       sudo udevadm control --reload-rules && sudo udevadm trigger --name-match=kvm
   ```
   Because the device set is small, run it on every PR; gate to `main`/nightly/`workflow_dispatch`
   if PR speed matters. (Firebase Test Lab is the cloud-device alternative but needs GCP + cost —
   overkill here.)

## Sequencing note
Phase 1 first (quick hygiene). Phases 2 and 3 are the bulk of the value. Phase 4 should land
*with or right after* Phase 2 so the new JVM suite is actually enforced. Deliberately not
investing in more instrumented Compose/nav tests — they won't add confidence the JVM suite
doesn't already give, and they cost emulator time.
