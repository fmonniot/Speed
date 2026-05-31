# Speed — Task Backlog

Items are grouped by area. Each task states **what** to do, **where** in the code to do it, and **acceptance criteria** so the result can be verified without running the app.

**Identifiers:** `B` = bug · `U` = UX/discoverability · `F` = feature · `R` = research  
**Statuses:** `todo` · `in-progress` · `blocked` · `done`

---

## Bugs

### B1 — Summary screen shows zero stats until Trace is opened
**Status:** `done`  
**Area:** `ui/SummaryScreen.kt`, `service/RaceRecordingService.kt`

**Problem:** After stopping a recording and navigating to the Summary screen, all stat cards (distance, avg speed, max lateral G, etc.) show `—` or zero. Opening the Trace screen and going back fixes it.

**Root cause:** `stopRecording()` in `RaceRecordingService` finalises the session (computes `SessionStatsComputer`, writes aggregate columns back to Room) inside a coroutine. The Summary screen's `LaunchedEffect(sessionId) { session = viewModel.getSession(sessionId) }` is a one-shot load that may run *before* that write completes, returning a `Session` with null aggregate fields.

**Acceptance criteria:**
- [ ] After tapping Stop and landing on the Summary screen, `distanceM`, `avgSpeedMs`, `maxSpeedMs`, `maxLateralG`, `maxLeanDeg`, `hardBrakeG`, `movingPercent` are all non-null and non-zero for any ride that had sensor data.
- [ ] No visible flash of zero values followed by correct values.

---

## UX / Discoverability

### U1 — Sensor chips look interactive but are decorative
**Status:** `done`  
**Area:** `ui/RideHomeScreen.kt` → `SensorChip` composable

**Problem:** The GPS / IMU / Battery chips use `MaterialTheme.colorScheme.primary` for icon tint. `primary` reads as "active / tappable" in M3. Per the design spec (§4.1), these chips are explicitly decorative.

**Change:** Swap the icon tint from `MaterialTheme.colorScheme.primary` to `MaterialTheme.colorScheme.onSurfaceVariant` to match the spec and the label colour.

**Acceptance criteria:**
- [ ] Sensor chip icons render in `onSurfaceVariant`, not `primary`.
- [ ] Chips have no click handler and show no ripple on touch.

---

### U2 — Top-speed hero on Summary screen has no visual tap affordance
**Status:** `done`  
**Area:** `ui/SummaryScreen.kt`

**Problem:** The top-speed `primaryContainer` card navigates to the Trace on tap (per spec §4.3 #3 and the wired `onOpenTrace` callback), but there is no visual cue that it is interactive — no `chevron_right`, no ripple hint, nothing. Users don't discover it.

**Acceptance criteria:**
- [ ] The top-speed hero card displays a clear interactive affordance (e.g. a `chevron_right` icon at the bottom-right corner, or a visible ripple, consistent with how other tappable cards look in the app).
- [ ] Tapping anywhere on the card still navigates to the Trace at the peak-speed moment.

---

### U3 — Segment concept is hidden
**Status:** `blocked`  
**Blocker:** This task needs a UX product decision before implementation: *which* additional Segments entry point to add (and exactly where). The two candidates have meaningfully different scope and visual implications, so I am not choosing on the user's behalf. Unblock by picking one of the options in **Findings** below (or specifying another).
**Area:** Navigation, `ui/StatsScreen.kt`, potentially `ui/TripsScreen.kt`

**Problem:** Users don't discover that segments exist. The Stats → Segments link row is the only entry point; it is easy to miss.

**Investigation needed before implementing:**
- Would a shortcut chip on the Trips list help?
- Would a contextual prompt on the Summary screen ("Define a segment from this ride?") help?

**Acceptance criteria (to be refined after UX investigation):**
- [ ] At least one additional, prominent path to the Segments list exists beyond the Stats overview.

**Findings:**

Current state: the only route to the Segments list (`Routes.SEGMENTS`) is the bordered link row at the bottom of Stats · Overview (spec §4.6 #5). The Summary screen has a *segment-PB* row, but it routes to `Routes.SEGMENTS` only conceptually filtered to "this ride's segments" — and it only reads as meaningful once a user already has segments defined. So for a user who has never created a segment, there is effectively no discoverable entry to the feature.

Two candidate entry points were considered:

1. **Shortcut/destination on the Trips screen.** Trips is the most-visited browsing surface. Options range from a non-scoping shortcut chip in the filter-chip row (cheap, but mixes a *navigation* affordance into a row whose other chips are *update-in-place* filters — inconsistent with the §4.4 interaction contract) to a top-bar trailing action. A top-bar action is cleaner but the Trips bar already carries `search` (§4.4); adding a second trailing icon for a secondary concept is questionable.

2. **Contextual prompt on the Summary screen** ("Define a segment from this ride?"). This is the strongest *teaching* moment — the user has just finished a ride and has a concrete track to turn into a segment. It also dovetails with the existing segment-PB row. Cost: it touches the segment-creation flow (the §4.7 "Add" path: draw on map / pick from a ride), which is a larger change than a pure navigation link, and the "pick from a ride" creation path must exist for the prompt to lead anywhere useful.

**Recommendation:** Add the contextual prompt on the Summary screen (option 2) — it is the highest-intent, most teachable surface and reinforces rather than clutters the existing interaction model — **but** confirm with the user first, because it depends on the segment-from-ride creation flow being in scope; if only a lightweight discoverability nudge is wanted, fall back to a single Segments destination reachable from the Stats tab area rather than overloading the Trips filter row.

---

### U4 — Trip list rows don't show departure time
**Status:** `done`  
**Area:** `ui/TripsScreen.kt`

**Problem:** The date format is `SimpleDateFormat("d MMM", ...)`. For users who record multiple trips per day, there is no way to tell them apart at a glance.

**Change:** Extend the date format to include hour and minute, e.g. `"d MMM · HH:mm"`.

**Acceptance criteria:**
- [ ] Each trip row in the Trips list shows the date **and** the start time (hour + minute in local timezone).
- [ ] The extra text fits within the existing `ListRow` layout without truncating the trip name on typical screen widths.

---

## Features

### F1 — Delete trips from the UI
**Status:** `todo`  
**Area:** `ui/TripsScreen.kt` and/or `ui/SummaryScreen.kt`

**Note:** The backend is already complete. `RaceViewModel.deleteSession()` calls `RaceRepository.deleteSession()` which calls `dao.deleteFullSession()` and removes the raw trace file from external storage. This is purely a UI task.

**Acceptance criteria:**
- [ ] The user can delete a trip from at least one screen (trip list or summary).
- [ ] A confirmation dialog or undo action is shown before permanent deletion.
- [ ] After deletion the trip no longer appears in the Trips list or Stats aggregates.
- [ ] If the deleted session had a raw trace file on disk it is also removed.

---

### F2 — Add a Stop action to the foreground notification
**Status:** `todo`  
**Area:** `service/RaceRecordingService.kt` → `createNotification()`

**Problem:** When the app is backgrounded and the screen is off, the only way to stop recording or kill the service is to re-open the app. The notification has no interactive action.

**Change:** Add a `NotificationCompat.Action` to the notification that sends `ACTION_STOP_SENSORS` (already defined as a constant) to the service via a `PendingIntent`. The action label should be "Stop" (or equivalent).

**Acceptance criteria:**
- [ ] While the service is running, the notification shows a "Stop" action button.
- [ ] Tapping the action button stops sensor collection and recording, and dismisses the notification — identical to calling `stopSensors()` from the app.
- [ ] The action button is present whether sensors are idle or actively recording.

---

### F3 — Pre-warm GPS fix before the user taps Record
**Status:** `todo`  
**Area:** `service/RaceRecordingService.kt`, `ui/RideHomeScreen.kt`, `viewmodel/RaceViewModel.kt`

**Problem:** GPS cold-start takes 15–60 seconds. If a user opens the app and immediately taps Record, the first portion of the ride has no GPS fix.

**Context:** A `autoStartSensors` DataStore preference and the corresponding `SettingsRepository.autoStartSensors` flow already exist. `RaceViewModel.init` already reads it and calls `startSensors()` on launch if enabled. This feature is partially plumbed — it just isn't surfaced to the user in Settings.

**Acceptance criteria:**
- [ ] Settings screen exposes the "Auto-start sensors on launch" toggle (the `autoStartSensors` preference).
- [ ] When enabled, GPS and IMU start acquiring when the app is opened, before the user taps Record. The Home screen sensor chips reflect the live GPS fix state.
- [ ] When disabled (default), sensors only start when the user taps Record.

---

### F4 — Dark theme: add system-follow option
**Status:** `todo`  
**Area:** `data/SettingsRepository.kt`, `viewmodel/RaceViewModel.kt`, `ui/SettingsScreen.kt`, `MainActivity.kt`

**Problem:** The dark theme setting is a `Boolean` (`DARK_THEME` `booleanPreferencesKey`). There is no way to follow the system theme.

**Change required:**
1. Replace `DARK_THEME: booleanPreferencesKey` with a string/enum key holding `LIGHT | DARK | SYSTEM`.
2. Update `SettingsRepository`, `RaceViewModel`, and `SettingsScreen` to expose and set the three-value option.
3. In `MainActivity`, map `SYSTEM` → read `isSystemInDarkTheme()` and pass it to `RaceLoggerTheme`.

**Acceptance criteria:**
- [ ] Settings exposes three choices: Light, Dark, Follow system.
- [ ] Selecting "Follow system" makes the app theme change automatically when the OS theme changes, without restarting the app.
- [ ] Existing users who had `darkTheme = false` are migrated to "Light"; those with `darkTheme = true` to "Dark". (DataStore migration or a default fallback is acceptable.)

---

### F5 — Auto-pause: add explanatory text
**Status:** `todo`  
**Area:** `ui/SettingsScreen.kt`

**Problem:** The "Auto-pause" toggle has no supporting text. It is not obvious that it causes stationary points to be excluded from the recording, affecting distance and moving-time stats.

**Change:** Add a one-line supporting text below the toggle label, e.g.: *"Stationary points are not recorded; distance and moving-time stats reflect riding time only."*

**Acceptance criteria:**
- [ ] The Auto-pause row in Settings shows a subtitle/supporting text explaining the effect.
- [ ] The text is consistent with the actual threshold (`AUTO_PAUSE_SPEED_THRESHOLD_MS = 0.5 m/s` in `RaceRecordingService`).

---

### F6 — Rename app references from "Race Logger" to "Speed"
**Status:** `todo`  
**Area:** `service/RaceRecordingService.kt`

**Note:** `strings.xml` already has `<string name="app_name">Speed</string>`. The remaining stale reference is in the service notification: `createNotification()` hardcodes the title `"Race Logger"`.

**Acceptance criteria:**
- [ ] The foreground notification title reads "Speed", not "Race Logger".
- [ ] No other hardcoded "Race Logger" strings remain in the codebase (verify with `grep -r "Race Logger" app/src`).

---

### F7 — Debug build: separate package and display name
**Status:** `todo`  
**Area:** `app/build.gradle.kts` → `buildTypes { debug { … } }`

**Change:** Add `applicationIdSuffix ".debug"` and `versionNameSuffix " (debug)"` to the debug build type so that debug and release builds can coexist on the same device.

**Acceptance criteria:**
- [ ] Debug builds install under `eu.monniot.speed.debug` and show as "Speed (debug)" in the launcher.
- [ ] Release builds are unaffected (`eu.monniot.speed`, "Speed").
- [ ] Both variants can be installed simultaneously on the same device.

---

### F8 — Auto-populate trip name from location
**Status:** `todo`  
**Area:** `service/RaceRecordingService.kt` → `stopRecording()`, or `viewmodel/RaceViewModel.kt`

**Problem:** New trips are named by the weekday at display time (e.g. "Monday ride"). A location-based name (e.g. start/end locality from reverse geocoding) would be more meaningful and make trips easier to identify.

**Potential approach:** After session finalisation in `stopRecording()`, perform a reverse-geocode lookup for the first and/or last GPS coordinate of the session using the Android `Geocoder` API (no extra dependency), and set `Session.name` if the user hasn't provided one.

**Acceptance criteria:**
- [ ] Trips recorded with a GPS fix receive an auto-generated name from location data (e.g. city or locality name).
- [ ] Trips where no GPS fix was obtained during the session fall back to the weekday label.
- [ ] User-renamed trips are never overwritten by this logic.
- [ ] The lookup runs off the main thread and does not block session finalisation.

---

## Investigation / Research

### R1 — Samsung: battery optimisation impact on GPS accuracy
**Status:** `todo`  
**Area:** No code change required initially — investigation only.

**Question:** On Samsung devices, setting the app's battery mode to "Optimised" (vs "Unrestricted") may throttle background sensor/location access when the screen is off. Determine:
1. What Android API, if any, lets the app detect which battery mode it is under.
2. Whether GPS update frequency is reduced in "Optimised" mode while the screen is off.
3. Whether the app should prompt the user to switch to "Unrestricted" (and when/how).

**Output:** A written note in this file (or a new `spec/` doc) summarising findings and a concrete recommendation. Only then should a code task be created.
