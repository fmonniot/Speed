# Speed

A motorcycle/vehicle race-data logger for Android. Records GPS + IMU data at high precision, fuses them with a Kalman filter, and lets you review, analyse and export each ride.

## What it does

- Records speed, acceleration, lean angle, lateral G and GPS track at up to 10 Hz (GPS) / 100 Hz (IMU).
- Fuses GPS and IMU into a smooth Kalman-filtered speed estimate with zero-velocity updates (ZUPT) during standstills.
- Stores every ride in a local Room database (no cloud, fully offline).
- Lets you define **segments** (user-defined road stretches) that are automatically timed on every matching ride.
- Exports rides as **CSV**, **GPX** or **FIT** (single-session share or bulk ZIP).

## Screens

| Tab | Screen | Purpose |
|---|---|---|
| Ride | Home | Sensor status, last ride card, record FAB |
| Ride | Live HUD | Glanceable during a ride — speed hero, G / lean / accel / altitude tiles |
| Ride | Summary | Post-ride stats, PB badge, segment PB count |
| Trips | List | Browse and filter all past rides |
| Trips | Trace | Speed + lateral-G charts, scrub playhead, GPS map |
| Stats | Overview | Distance trend, records grid, 6-month bar chart |
| Stats | Segments | Timed road segments, best times, trend arrows |
| Stats | Segment detail | Attempt history with relative-bar progress |
| Settings | Settings | GPS/IMU rates, auto-pause, units, dark theme |
| Settings | Export | Bulk export: format × scope × include toggles |

## Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 (green seed scheme, light + dark)
- **Architecture:** MVVM + foreground `LifecycleService`
- **Concurrency:** Kotlin Coroutines + Flow
- **Database:** Room v6 (`race_db`)
- **Settings:** DataStore Preferences
- **Location:** Google Play Services Fused Location Provider
- **Map:** MapLibre Android SDK (open-source, no API key)
- **Build:** Gradle Kotlin DSL, KSP

## Building

Requires Android Studio (or the command-line Gradle wrapper). No API keys needed.

```bash
# Debug APK
./gradlew :app:assembleDebug

# Unit tests
./gradlew :app:test

# Instrumented tests (device/emulator required)
./gradlew :app:connectedAndroidTest

# Lint
./gradlew :app:lint
```

- minSdk 26 · targetSdk 34 · JVM toolchain 17
- Needs `ACCESS_FINE_LOCATION` and (Android 13+) `POST_NOTIFICATIONS` — requested at runtime.

## Exporting data

From the **Settings → Export** screen, choose CSV / GPX / FIT, select which trips and data streams to include, then tap Export. The result is shared as a ZIP via the system share sheet.

### CSV columns (processed)

| Column | Description |
|---|---|
| `elapsed_ms` | Milliseconds since session start |
| `wall_clock_iso` | ISO-8601 wall-clock time |
| `lat`, `lon`, `altitude_m` | GPS position |
| `gps_speed_ms` | Raw GPS speed (m/s) |
| `gps_accuracy_m` | GPS horizontal accuracy (m) |
| `accel_x/y/z` | World-frame linear acceleration (m/s²) |
| `accel_magnitude` | Acceleration vector magnitude |
| `derived_speed_ms` | Kalman-fused speed (m/s) |
| `derived_accel_ms2` | Signed acceleration from fused speed |

### Manual extraction via adb

Raw sensor traces (when "Record raw traces" is on):
```bash
adb pull /sdcard/Android/data/eu.monniot.speed/files/raw_traces/ .
```

Temporary export cache (debug builds only):
```
/data/data/eu.monniot.speed/cache/
```

## Design reference

Visual design is specified in [`spec/design-spec.md`](spec/design-spec.md) (Material 3 Expressive, green seed).  
Component layout details are in [`spec/component-reference.md`](spec/component-reference.md).
