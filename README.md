# Android Race Data Logger

A production-quality Android application for recording race data (speed, acceleration, GPS) at 100ms precision.

## Features

- **High-Precision Logging:** 100ms (10Hz) data collection interval.
- **Sensor Fusion:** Combines GPS data with IMU (Linear Accelerometer + Rotation Vector) using a Kalman filter for smooth and accurate speed/acceleration estimation.
- **World-Frame Acceleration:** Automatically converts device-frame acceleration to world-frame (gravity removed), making it orientation-independent.
- **Foreground Service:** Continuous recording even when the app is in the background or the screen is off.
- **Data Management:** Uses Room (SQLite) for structured storage.
- **CSV Export:** Export your race sessions to CSV for external analysis.
- **Modern UI:** Built with Jetpack Compose and Material 3.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Architecture:** MVVM + Foreground Service
- **Concurrency:** Kotlin Coroutines & Flow
- **Location:** Google Play Services Fused Location Provider
- **Database:** Room
- **Build System:** Gradle Kotlin DSL

## Setup Instructions

1. **Clone/Copy Files:** Ensure all files are placed in their respective paths as generated.
2. **Sync Project:** Open the project in Android Studio and perform a Gradle Sync.
3. **Permissions:** The app requires the following permissions (requested at runtime):
    - `ACCESS_FINE_LOCATION`
    - `POST_NOTIFICATIONS` (on Android 13+)
4. **Google Play Services:** Ensure the device/emulator has Google Play Services installed for location tracking.
5. **Build & Run:** Hit 'Run' in Android Studio.

## Implementation Details

- **Clock Alignment:** All sensors and location updates are synced using `SystemClock.elapsedRealtimeNanos()`.
- **Kalman Filter:** A 1D Kalman filter integrates IMU acceleration with GPS speed measurements.
- **Battery Optimization:** Uses a `WakeLock` during recording to ensure consistent timing while allowing the screen to turn off.
- **Thread Safety:** Data fusion runs on `Dispatchers.Default`, while database operations use `Dispatchers.IO`.

## Exported CSV Format

The exported CSV contains the following columns:
- `elapsed_ms`: Time since the start of the session.
- `wall_clock_iso`: ISO-8601 formatted system time.
- `lat`, `lon`, `altitude_m`: GPS coordinates and altitude.
- `gps_speed_ms`: Raw speed from GPS (m/s).
- `gps_accuracy_m`: GPS horizontal accuracy (meters).
- `accel_x`, `accel_y`, `accel_z`: World-frame linear acceleration (m/s²).
- `accel_magnitude`: Vector magnitude of acceleration.
- `derived_speed_ms`: Kalman-filtered speed (m/s).
- `derived_accel_ms2`: Acceleration derived from the filtered speed.
