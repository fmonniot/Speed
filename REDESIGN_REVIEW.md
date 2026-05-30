# Speed Redesign — Code Review Findings (spec vs. implementation)

> Review of the `big-reimplementation` branch against `spec/design-spec.md` +
> `spec/component-reference.md`. `REDESIGN_PLAN.md` marks every task (A1–G1) done (☑); this doc
> re-checks those checkmarks and lists the gaps as independent, file-referenced tasks (R1–R8) that
> can each be implemented in a single session. Pick the highest-severity unblocked item.

## What's genuinely solid (verified, no action needed)
- Design-system layer is faithful: `Color.kt` matches §2.1 hexes exactly; `Type.kt` matches the §2.2
  scale; `Dimens.kt`/`Shapes.kt` match §2.4; `WavyLine` reproduces the component-reference algorithm
  (amp 3, period 14, stroke 3, dot r4, height 16) precisely. No hard-coded hex/radii in screens.
- Navigation graph (`MainActivity.kt`) covers all routes; bottom-nav hide/show + "Segments keeps Stats
  highlighted" are correct; tab back-stack state is preserved.
- Domain layer is real and unit-tested: `SessionStatsComputer`, `RideAggregates`, `SegmentMatcher`,
  `GpxExporter`, `FitExporter`, `ExportManager`, `UnitFormat`/`Conversions`. The service finalize hook
  computes + persists session aggregates and writes segment attempts.
- Stats overview (D6), Summary (D3), Segment list/detail (D7/D8), Settings (D9), Export+scope (D10/F5),
  Map (F1/F2), Segment creation (E6) are fully backfilled with real data.
- **Build/tests are green:** `./gradlew testDebugUnitTest` → `BUILD SUCCESSFUL` (DoD holds).

**Meta-finding:** several acceptance boxes are ticked in `REDESIGN_PLAN.md` whose behavior is actually a
**no-op stub** (search, help, rename/share, date-range picker) or a **placeholder left un-backfilled**
after its dependency landed (Live-HUD lean/lateral-G, Trips distance). The screens render and don't
crash, but the §4 interaction/content contract isn't fully met.

| # | Severity | Finding | Spec |
|---|---|---|---|
| R1 | High | Live HUD still shows placeholder lateral-G + lean despite E1 landing | §4.2 |
| R2 | High | Trips list shows `—` for per-trip distance despite the data being available | §4.4 |
| R3 | High | Sampling settings (GPS/IMU rate, auto-pause) are stored + displayed but **inert** | §4.9 |
| R4 | Medium | "Opens" actions are no-op stubs: search, help, segment rename/share, date-range | §4.4/4.7/4.8/4.9/4.10 |
| R5 | Medium | Stats month-bar tap opens generic Trips, not "that month's trips" | §4.6 |
| R6 | Low | Summary hero → Trace doesn't land on the top-speed moment | §4.3 |
| R7 | Low | "NEW PB" badge can flash while session/sessions load | §4.3 |
| R8 | Low | Known data gaps: session names are weekday-derived; battery % is `—` | §4.1 |

---

## R1 — Backfill Live HUD lateral-G and lean from the E1 fields (High)

**Why.** E1 added `ServiceState.currentLateralG` and `ServiceState.currentLeanDeg`
(`service/RaceRecordingService.kt:39-41`), but `LiveHudScreen` was never updated:
- G-FORCE tile uses `serviceState.currentG` (total accel magnitude) as a stand-in — labelled `g lat`
  but it is **not** lateral G (`ui/LiveHudScreen.kt:265`).
- LEAN tile is hard-coded `value = "—"` with unit `"right"` (`ui/LiveHudScreen.kt:296-303`).
- Hero footer shows `avg —` (`ui/LiveHudScreen.kt:241`).

**Do.** In `ui/LiveHudScreen.kt`:
- G-FORCE → `UnitFormat.lateralGValue(serviceState.currentLateralG)`.
- LEAN → `UnitFormat.leanValue(serviceState.currentLeanDeg)` for the value and
  `UnitFormat.leanDirection(serviceState.currentLeanDeg)` for the unit word (`leanDirection` already
  returns `"right"`/`"left"`/`""` — `util/UnitFormat.kt:71`).
- Hero `avg`: there is no running-average field on `ServiceState`. Either accumulate it locally in the
  composable (sum/count of `currentSpeedMs` while recording) or add an `avgSpeedMs` to `ServiceState`.

**Acceptance.** Cornering shows a non-zero `g lat`; lean reads e.g. `38° right` and flips sign with
direction; remove the `TODO(E1)` comments. `./gradlew assembleDebug` passes.

## R2 — Show the real per-trip distance on the Trips list (High)

**Why.** `SessionSummary` now carries `distanceM` (persisted at finalize, `DataPointDao.kt:20`,
`Session.kt:31`) and the Ride-home/Summary screens already read it — but the Trips row still hard-codes
`meta = "$dateStr · —"` with a stale `TODO(E2)` (`ui/TripsScreen.kt:158`).

**Do.** Build the meta as `"$dateStr · ${session.distanceM?.let { UnitFormat.distance(it, units) } ?: "—"}"`.

**Acceptance.** Trip rows read `"26 May · 54.8 km"` (Imperial → `mi`); rows for not-yet-finalized
sessions still degrade to `—`.

## R3 — Wire sampling settings into the capture pipeline (High; confirm scope)

**Why.** §4.9 describes GPS rate / IMU rate / Auto-pause as functional capture controls, and Ride-home
prints `"sampling at $gpsRateHz Hz"`. But the settings are **never read by the recorder**:
- `sensor/GpsCollector.kt:66` hard-codes `LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, 100L)` (10 Hz).
- `sensor/ImuCollector.kt:46-47` uses `SENSOR_DELAY_GAME` (~50 Hz) — and the **default `imu_rate_hz`
  is 100**, so the Settings value is wrong even at defaults.
- `auto_pause` is stored but referenced nowhere in `service/` or `sensor/`.

So changing any of these in Settings has no effect; the GPS-rate subtext is only accidentally correct
at the 10 Hz default.

**Do.** Thread `gps_rate_hz` / `imu_rate_hz` / `auto_pause` from `SettingsRepository` into
`RaceRecordingService` and on into `GpsCollector` (interval = `1000/hz` ms) and `ImuCollector`
(`samplingPeriodUs = 1_000_000/hz`); implement auto-pause using the existing stationary/ZUPT signal.

**Acceptance.** Changing GPS rate changes the fix cadence; IMU default display matches the real rate;
auto-pause halts capture when stationary. **Note:** this was never an explicit plan task (C1 only
required exposing the flows) — confirm whether it's in scope before implementing, or down-scope to
"make the Settings labels honest" (e.g. default IMU to 50 Hz to match `SENSOR_DELAY_GAME`).

## R4 — Implement (or descope) the no-op "Opens" actions (Medium)

**Why.** Several §4 "Opens" interactions are wired to `{}` in `MainActivity.kt`, yet their acceptance
boxes are ticked:
- Trips search — `onSearch = {}` (`MainActivity.kt:270`); §4.4 #1.
- Segments search — `onSearch = {}` (`MainActivity.kt:315`); §4.7 #3.
- Settings help — `onHelp = {}` (`MainActivity.kt:377`); §4.9 #1.
- Export help — `onHelp = {}` (`MainActivity.kt:388`); §4.10 #2.
- Segment overflow **rename** + **share** — `onRename = {}`, `onShare = {}` (`MainActivity.kt:341,347`);
  §4.8 #2. (delete + set-as-goal are wired.)
- Stats date-range picker — `onDateRange = {}` (`MainActivity.kt:303`); §4.6 #1.

The search bars and overflow menu items are present and tappable but do nothing.

**Do.** Pick per item: implement a minimal overlay/dialog (search filter sheet, help/about dialog,
rename `AlertDialog` writing a new name field, share via the existing `exportSession` path, M3
`DatePicker`), **or** consciously descope and untick the relevant boxes in `REDESIGN_PLAN.md` so the
tracker is honest. These are independent and can be separate sub-tasks.

**Acceptance.** Each listed control either opens its sheet/dialog/overlay or is explicitly marked
out-of-scope in the tracker.

## R5 — Stats month-bar should open that month's trips (Medium)

**Why.** §4.6 #3 says a month bar navigates to "that month's trips", but `MonthBars` calls a single
`onOpenTrips` that routes to bare `Routes.TRIPS` with no month scope (`ui/StatsScreen.kt:411`,
`MainActivity.kt:305`).

**Do.** Pass the tapped `MonthBar`'s start/end to the callback and extend the Trips route to accept a
date window (today it only supports `?filter=all|week` — `MainActivity.kt:110,254`). Add a month-scoped
filter mode to `TripsScreen`/`TripsFilter`.

**Acceptance.** Tapping a bar opens Trips showing only that month's rides.

## R6 — Summary hero should land on the top-speed moment in the Trace (Low)

**Why.** §4.3 #3: "Jumps to the top-speed moment on the Trace." Today the hero navigates to
`trace/{id}` with no playhead position (`MainActivity.kt:250`); the Trace opens with no scrub.

**Do.** Compute the max-speed index when loading Summary and pass it through the route (e.g.
`trace/{id}?at={index}`); have `TraceScreen` seed `playheadIndex` from it.

**Acceptance.** Opening the Trace from the Summary hero positions the playhead/marker at peak speed;
opening from the Trips row or download path is unaffected.

## R7 — Guard the "NEW PB" badge during async load (Low)

**Why.** `isNewPb = topSpeed != null && sessions.filter{…}.all{…}` (`MainActivity.kt:232-234`). The
`sessions` flow starts as `emptyList()` and loads asynchronously, so `.all {}` over an empty list is
vacuously `true` — the badge can flash "NEW PB" before the comparison set is loaded.

**Do.** Require `sessions.isNotEmpty()` (or gate on both `session` and `sessions` being loaded) before
evaluating the PB comparison.

**Acceptance.** The badge only appears once the comparison set is loaded and the session truly beats it.

## R8 — Known data gaps to close later (Low)

**Why / Do.** Acknowledged in the plan notes but worth tracking as real spec gaps:
- **Session names** are derived from the weekday everywhere (`"EEEE 'ride'"`) — `RideHomeScreen.kt:284`,
  `SummaryScreen.kt:102`, `TripsScreen.kt:62`. §4.1/§4.3 want a user-given or location-derived name.
  Add a `name` column to `Session` + a rename affordance (pairs naturally with the R4 segment-rename
  dialog work and `RaceViewModel.updateSessionNotes`'s pattern).
- **Battery % chip** shows `"—%"` (`RideHomeScreen.kt:132`). `ServiceState` exposes
  wattage/capacity/time-remaining but no percentage. Add a battery-% field to `ServiceState` and read
  `BatteryManager.EXTRA_LEVEL/EXTRA_SCALE`.

**Acceptance.** Rides show a real/editable name; the GPS-chip row shows a live battery %.

---

## Out of scope / non-issues (reviewed, intentionally not tasks)
- `SpeedSwitch` uses M3's built-in `Switch` colors rather than the exact 52×32 thumb geometry —
  component-reference explicitly allows "M3's built-in Switch is close."
- Trace one-tap **Download** is CSV/zip only (format choice lives in the Export screen) — this is a
  documented design decision; not a bug.
- WavyLine/month-bar use `onPrimaryContainer`-derived colors instead of literal `primary` over a
  `primaryContainer` card — a sensible contrast adaptation, still role-based.
- `material3-adaptive-navigation-suite` is still a dependency but `NavigationSuiteScaffold` is no longer
  referenced (grep-clean). Removing it is optional dead-dep cleanup, not a correctness issue.

## Verification (per future task, when implemented)
- `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` stay green.
- R1: record a ride, corner → Live HUD `g lat`/lean update; R2: Trips rows show km/mi; R3: change GPS
  rate and confirm the fix cadence changes; R4/R5/R6: tap each control and confirm it opens/navigates as
  the §4 table specifies.
