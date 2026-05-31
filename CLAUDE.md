# Speed — CLAUDE.md

## Build & run

```bash
# Assemble debug APK
./gradlew :app:assembleDebug

# Unit tests (JVM + Robolectric)
./gradlew :app:test

# Instrumented tests (requires a connected device/emulator)
./gradlew :app:connectedAndroidTest

# Lint
./gradlew :app:lint
```

- **minSdk** 26 · **targetSdk** 34 · **compileSdk** 36
- **JVM toolchain** 17 (set in `app/build.gradle.kts`)
- No API keys or `local.properties` entries required. MapLibre is open-source and needs no key.
- No signing config beyond the default debug keystore; `isMinifyEnabled = false` in release (no Proguard active).

## Architecture

**MVVM + Foreground Service**, single `:app` module.

```
Sensors (GPS / IMU)
  └── GpsCollector / ImuCollector  (eu.monniot.speed.sensor)
        └── DataFusion              (eu.monniot.speed.fusion) — 100 ms Kalman tick
              └── RaceRecordingService  (LifecycleService, foreground)
                    ├── persists DataPoints via RaceRepository → Room
                    └── exposes ServiceState as companion-object StateFlow (no binding needed)

RaceViewModel  (AndroidViewModel)
  ├── reads RaceRecordingService.state  (companion StateFlow)
  ├── reads RaceRepository flows        (sessionSummaries, segmentListItems)
  ├── reads SettingsRepository flows    (DataStore preferences)
  └── sends Intent actions to the service (start/stop sensors, start/stop recording)

UI screens (Compose, eu.monniot.speed.ui)
  └── single shared RaceViewModel, obtained via viewModel() in MainActivity
```

Data flows one-way: sensors → service state → ViewModel StateFlows → UI.  
No DI framework — all dependencies are constructed manually in `init {}` blocks.

## Package structure

| Package | Contents |
|---|---|
| `eu.monniot.speed` | `MainActivity`, `Routes`, `RaceLoggerApplication` |
| `.data` | Room entities, DAOs, `RaceRepository`, `SettingsRepository` |
| `.domain` | Pure Kotlin logic: `SegmentMatcher`, `SessionStats`, `RideAggregates` |
| `.fusion` | `VelocityFusion` (Kalman), `DataFusion`, `CoordinateTransformer`, `MotionMath` |
| `.sensor` | `GpsCollector`, `ImuCollector`, `RawSensorSink`, `FileRawSink` |
| `.service` | `RaceRecordingService`, `ServiceState` |
| `.ui` | One file per screen composable |
| `.ui.components` | Shared composables (`SpeedBottomNav`, `TonalStatCard`, `WavyLine`, …) |
| `.ui.theme` | `Color`, `Type`, `Theme`, `Dimens`, `Shapes` |
| `.viewmodel` | `RaceViewModel` |
| `.export` | `ExportManager`, `GpxExporter`, `FitExporter` |
| `.util` | `UnitFormat`, `FormatUtils`, `Conversions` |

## Key libraries

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose BOM | 2026.02.01 | UI toolkit |
| Material 3 | via BOM | Design system (green seed scheme, light + dark) |
| Navigation Compose | 2.9.7 | Single-activity nav graph (`Routes` object defines all routes) |
| Room + KSP | 2.8.4 | Local database (`race_db`, v6) |
| DataStore Preferences | 1.1.2 | Settings persistence (`SettingsRepository`) |
| Google Play Services Location | 21.3.0 | Fused location provider for GPS |
| Coroutines / Flow | 1.10.2 | All async work; sensor data as cold Flows |
| MapLibre Android SDK | 11.8.0 | GPS track map on Trace and FullScreenMap screens |
| Mockito + mockito-kotlin | 5.11 / 5.2 | Test doubles |
| Robolectric | 4.16 | Android-framework unit tests (JVM) |

**No Hilt / Koin** — manual dependency construction.  
**No Retrofit / network layer** — fully offline app.

## Code conventions

- **Units at the boundary:** all internal values are SI (m/s, m, m/s²). Convert only at the display layer via `eu.monniot.speed.util.UnitFormat`. Never store km/h.
- **`LocalUnits`** CompositionLocal is set in `SpeedApp` and consumed by any screen that needs the current unit preference.
- **New screen:** add a `@Composable fun FooScreen(…)` file in `eu.monniot.speed.ui`, add its route constant to `Routes`, wire it in `SpeedNavHost` inside `MainActivity.kt`, and expose any needed data from `RaceViewModel`.
- **New setting:** add a `Flow<T>` + setter in `SettingsRepository`, expose a `StateFlow` in `RaceViewModel`.
- **Theme:** follow the M3 green seed color scheme defined in `ui/theme/Color.kt`. Use `MaterialTheme.colorScheme.*` roles — never hard-code hex values.
- `ServiceState` is the single source of live sensor truth. Read it via `RaceRecordingService.state` (companion object `StateFlow`).

## Testing approach

**Unit tests** (`src/test/`)
- Domain logic (`SegmentMatcher`, `SessionStats`, `RideAggregates`) — plain JUnit4, no Android framework.
- Fusion math (`VelocityFusion`, `MotionMath`, `CoordinateTransformer`) — plain JUnit4.
- Export (`GpxExporter`, `FitExporter`, `ExportManager`) — plain JUnit4.
- Utility functions — plain JUnit4.
- `VelocityCalibrationTests` drives the Kalman filter against real CSV sensor traces stored in `src/test/resources/raw_traces/`. Add new traces there when tuning.
- Robolectric used only where Android APIs are unavoidable in unit scope.

**Instrumented tests** (`src/androidTest/`)
- Room DAO tests (`RaceDatabaseTest`, `SegmentDaoTest`) — run against an in-memory database.
- `FusionIntegrationTest` — end-to-end sensor pipeline on a real device.
- `RaceRecordingServiceTest` — service lifecycle on a real device.
- `NavigationTest` — Compose nav graph smoke test.

No custom test rules or utilities beyond standard `ActivityScenarioRule` / `AndroidJUnit4`.

## Common pitfalls

- **Destructive migrations:** `RaceDatabase` uses `fallbackToDestructiveMigration()` at v6. Any schema change silently drops all user data on upgrade. Add a proper `Migration` object before shipping schema changes.
- **`exportSchema = false`:** Room schema JSON is not generated; if you enable it, add the export dir to `.gitignore` or check it in deliberately.
- **`VelocityFusion` / `SimpleKalmanFilter` are not thread-safe.** All calls must come from the same coroutine. `DataFusion` enforces this via a single `Dispatchers.Default` loop.
- **`RaceRecordingService.state` is a companion-object `StateFlow`**, not an `IBinder`-bound interface. Don't try to call `bindService`; just read the static flow.
- **Dark theme is app-controlled** (DataStore `darkTheme` preference), not the system setting. `MainActivity` reads `viewModel.darkTheme` and passes it to `RaceLoggerTheme`.
- **No Proguard:** `isMinifyEnabled = false` in release. If you enable minification, Room entities and the MapLibre SDK will need keep rules.
- **No product flavors, no feature flags.**
- **KSP-generated Room code** (`*_Impl` classes) must not be edited by hand.
