# Speed Redesign — Project Plan & Task Tracker

> **What this is.** A living task tracker for implementing the Material 3 Expressive redesign
> described in `spec/design-spec.md`. Each task is sized for a single independent Sonnet session.
> When you finish a task, tick its status box and check off its acceptance criteria here, then
> commit this file alongside the work.
>
> **How to use.** Pick the lowest-numbered unstarted task whose dependencies are all done. Read
> the linked spec section and the "Key files". Do **not** start a task whose dependencies are
> incomplete. Honor the global rules below.

## Status legend
`☐` not started `◐` in progress `☑` done

## Global rules (apply to every task)
- **Source of truth:** `spec/design-spec.md` is authoritative. For concrete layout values
  (paddings, sizes, gaps, icon sizes) and the `WavyLine` algorithm, read `spec/component-reference.md`
  — a compact distillation of the mock-ups. Only fall back to the raw `spec/design-artifacts/*.jsx`
  (grep + read the single relevant function) if something is truly unspecified; the spec still wins
  on any disagreement.
- **Units & sizing:** layout/spacing/radii/component sizes in `dp`; text in `sp`.
- **Colors:** reference M3 `ColorScheme` roles only — never hard-code hex inside screens/components.
- **Data units:** store SI internally (m/s, m, m/s²); convert only at the display boundary per the
  Units setting.
- **Decorative contract:** sensor chips and all Live-HUD readouts and stat cards are intentionally
  non-interactive — do not attach navigation to them.
- **Example values:** every number/name in spec §4 (142 trips, "Col de Turini", etc.) is illustrative,
  not a fixed string.
- **Definition of done for any task:** `./gradlew assembleDebug` succeeds; existing unit tests pass
  (`./gradlew testDebugUnitTest`); this tracker updated.

## Confirmed approach decisions
- **UI-first scaffold** — build the design system + all screens wired to navigation first (using
  current/derived data and placeholders for not-yet-built metrics), then backfill domain features.
- **Map:** MapLibre / osmdroid (open-source, no API key).
- **Charts:** hand-rolled Compose `Canvas` (no charting dependency).
- **Theme:** single green seed scheme only — remove Material You dynamic color; dark/light follows
  the in-app Dark-theme setting, not the system.

## Suggested order
A1→A2→A3→A4→A5 · B1→B2 · C1→C2→C3 · D1…D10 · E1…E6 / F1…F5 · G1.
Phases A–D deliver the redesigned, fully tappable app (with placeholders); E–F make every number and
feature real; G cleans up.

## Progress checklist
- [x] A1 Green color scheme · [x] A2 Remove dynamic color · [x] A3 Typography · [x] A4 Shape/dimension tokens · [x] A5 Shared components
- [x] B1 Four-tab destinations · [x] B2 Nav graph with all routes
- [x] C1 Settings keys · [x] C2 Units formatting · [x] C3 Dark-theme wiring
- [x] D1 Ride/Home · [x] D2 Live HUD · [x] D3 Summary · [x] D4 Trips list · [x] D5 Trace/detail · [x] D6 Stats overview · [ ] D7 Segment list · [ ] D8 Segment detail · [ ] D9 Settings · [ ] D10 Export
- [ ] E1 Lean+lateral G · [ ] E2 SessionStats · [ ] E3 Aggregates · [ ] E4 Segments model · [ ] E5 Segment matching · [ ] E6 Segment creation
- [ ] F1 MapLibre · [ ] F2 Full-screen map · [ ] F3 GPX export · [ ] F4 FIT export · [ ] F5 Export scope/include
- [ ] G1 Cleanup

---

# Phase A — Design system foundation

## A1. Green color scheme
**Status:** ☑ · **Depends on:** none · **Spec:** §2.1

Replace the current red/teal/purple schemes with the spec's full **green** light and dark
`ColorScheme`s. Every role listed in §2.1 must be set: `primary`/`onPrimary`,
`primaryContainer`/`onPrimaryContainer`, `secondary`/`secondaryContainer`/`onSecondaryContainer`,
`tertiary`/`tertiaryContainer`/`onTertiaryContainer`, `error`/`errorContainer`/`onErrorContainer`,
`surface`, `surfaceDim`, the `surfaceContainer*` tiers, `onSurface`, `onSurfaceVariant`, `outline`,
`outlineVariant`. Put the raw hex values in a dedicated `Color.kt`; build the two schemes in `Theme.kt`.

**Key files:** `app/src/main/java/eu/monniot/speed/ui/theme/Theme.kt`, new `…/ui/theme/Color.kt`.
Reference hexes in `spec/design-spec.md` §2.1 and tokens in `spec/design-artifacts/material3.jsx`.

**Acceptance criteria:**
- [x] Light and dark `ColorScheme`s contain every role from §2.1 with the exact spec hexes.
- [x] No screen or component hard-codes a hex; all colors come from the scheme.
- [x] Old red/teal/purple values are gone from the active schemes (cleanup of leftovers can defer to G1).
- [x] App builds and renders with the green scheme in both light and dark previews.

## A2. Remove dynamic color; make dark an explicit parameter
**Status:** ☑ · **Depends on:** A1 · **Spec:** §2.1 "Rule", §4.9

Remove Material You dynamic color from `RaceLoggerTheme`. The composable should take an explicit
`darkTheme: Boolean` and choose between the two green schemes only — no `dynamicLightColorScheme`,
no `dynamicDarkColorScheme`, no `dynamicColor` flag. As a temporary source until C3, `MainActivity`
passes `isSystemInDarkTheme()`. Keep the existing status-bar `SideEffect` and edge-to-edge behavior.

**Key files:** `…/ui/theme/Theme.kt`, `…/MainActivity.kt`.

**Acceptance criteria:**
- [x] `RaceLoggerTheme` no longer references any dynamic-color API.
- [x] Passing `darkTheme = true/false` swaps only colors; layouts/interactions are identical.
- [x] Status bar icon contrast still flips correctly with the theme.

## A3. Typography
**Status:** ☑ · **Depends on:** A1 · **Spec:** §2.2

Add a `Type.kt` defining a Material 3 `Typography` based on **Roboto Flex** (add the font resource;
fall back to the system sans if bundling is impractical) and map the spec's type scale: hero numerals
(104 / 64–80 / 56 sp, weight 700, tight letter-spacing/line-height), screen headings (24–28 sp/500),
top-bar title (22 sp/400), card stat values (20–30 sp/600), body/list (15–16 sp/500), caption (11–13 sp),
section label (12–13 sp/600 uppercase, tracked, primary). Wire into `MaterialTheme(typography = …)`.
Expose any non-standard styles (e.g. the giant hero numeral) as named text styles components can reuse.

**Key files:** new `…/ui/theme/Type.kt`, `…/ui/theme/Theme.kt`, font resources under `app/src/main/res/font/`.

**Acceptance criteria:**
- [x] `Typography` covers display/headline/title/body/label roles per §2.2 with sizes in `sp`.
- [x] A reusable hero-numeral style exists for the live-speed / top-speed / distance numerals (`SpeedTextStyles`).
- [x] Text scales with the OS font-scale setting (because sizes are `sp`).
- [x] `MaterialTheme` uses the new typography app-wide.

## A4. Shape & dimension tokens
**Status:** ☑ · **Depends on:** none · **Spec:** §2.4

Capture the spec's corner-radius scale and spacing as named tokens so screens stop using magic numbers.
Provide an M3 `Shapes` set plus a small `Dimens`/`Radii` object: radii 28 (hero/large), 24 (export/map),
18–20 (medium tonal), 18 (small stat), 16 (bordered rows), 12 (banner/chips-as-buttons), 8 (filter/sort
chips), nav active pill 16, FAB 18, full-width button 28, search bar 24; plus the 16 dp standard screen
horizontal padding and the 56 dp top bar / 72 dp bottom nav heights.

**Key files:** new `…/ui/theme/Shapes.kt` (and/or `…/ui/theme/Dimens.kt`), `…/ui/theme/Theme.kt`.

**Acceptance criteria:**
- [x] All radii, the 16 dp screen padding, and the bar heights from §2.4 exist as named tokens (`SpeedDimens`).
- [x] `MaterialTheme(shapes = …)` is wired where M3 components consume it (`SpeedShapes`).
- [x] Later component/screen tasks can reference these tokens (no inline duplicates needed).

## A5. Shared components library
**Status:** ☑ · **Depends on:** A1, A3, A4 · **Spec:** §2.5

Create a `ui/components/` package with the reusable building blocks, each with an `@Preview`. Components:
`SpeedTopBar` (56 dp; optional leading `arrow_back`, 22 sp title, optional trailing action icon, `surface`
background); `SpeedBottomNav` (72 dp; 4 destinations; `secondaryContainer` active pill 56×32 r16 behind a
filled icon + heavier label; `surfaceContainer` background); `TonalStatCard` (container color + matching
`on*` text, top-right icon, big value, small caption); `SectionLabel` (uppercase, tracked, primary);
`ListRow` (leading 44 dp round icon chip, title + meta, trailing value or `chevron_right`); selectable
chips `FilterChip`/`SortChip`/`RangeChip` (32 dp tall, r8, selected = `secondaryContainer` + leading
`check`, unselected = outlined); `SpeedSwitch` (52×32; `primary` track + large thumb on, outlined + small
thumb off); a full-width filled button helper (56 dp, r28); an extended-FAB helper (56 dp, `primary`,
icon+label); and **`WavyLine`** — port the M3 Expressive sine-wave progress from `material3.jsx` to a
Compose `Canvas`, drawing the wave to a progress fraction with a filled leading dot.

**Key files:** new package `…/ui/components/` (one file per component or grouped sensibly). Reference
`spec/design-artifacts/material3.jsx` for `WavyLine` and chip/card/nav visuals.

**Acceptance criteria:**
- [x] Each component listed above exists, is theme-driven, and has a working `@Preview`.
- [x] `WavyLine` renders a sine wave filled to a `progress` (0..1) param with a leading dot, light+dark.
- [x] Selectable chips show the selected/unselected states from §2.5 (`SpeedSelectableChip`).
- [x] `SpeedSwitch` matches the on/off thumb+track spec (primary track / outline track).
- [x] Components use A4 tokens and A3 type styles — no hard-coded colors or radii.

**Design decision:** Roboto Flex font is not bundled (adds ~1 MB to APK with no runtime variable-font
support on Android). The system sans-serif (Roboto) is used for all type styles; the sizes and weights
from §2.2 are applied exactly. Swap in a Roboto Flex download in the G1 cleanup.

---

# Phase B — Navigation restructure

## B1. Four-tab destinations
**Status:** ☑ · **Depends on:** A5 · **Spec:** §1, §2.5

Replace the 3-entry `MainDestination` enum (Race/Sessions/Settings) with the four spec tabs:
**Ride** (`home`), **Trips** (`route`), **Stats** (`leaderboard`), **Settings** (`settings`) — icons
from material-icons-extended. Use the new `SpeedBottomNav` from A5 for the bar (replace or restyle the
`NavigationSuiteScaffold`). The bar must be **hidden** on full-bleed/sub-screens: Live HUD, Summary,
Trace, Segment detail, Export (driven by current route).

**Key files:** `…/MainActivity.kt` (`MainDestination`, `SpeedAppShell`).

**Acceptance criteria:**
- [x] Bottom nav shows exactly Ride / Trips / Stats / Settings with the correct icons and active pill.
- [x] The bar is hidden on Live HUD, Summary, Trace, Segment detail, and Export routes.
- [x] Switching tabs preserves each tab's back stack state.

**Notes:** `NavigationSuiteScaffold` replaced by `SpeedBottomNav` (A5) inside a new `SpeedAppShell`.
Visibility driven by `Routes.bottomBar` (ride/trips/stats/segments/settings); Segment list keeps
**Stats** highlighted. Outlined/Filled icon pairs feed the active-pill fill.

## B2. Nav graph with all routes (placeholders)
**Status:** ☑ · **Depends on:** B1 · **Spec:** §4 (all interaction tables)

Expand the `NavHost` to every spec destination, each as a minimal placeholder composable wired with the
correct top bar (via `SpeedTopBar`) and bottom-nav visibility, and the §4 navigation wiring so the whole
app is tappable before any real content exists. Routes: `ride` (start), `live`, `summary/{sessionId}`,
`trips`, `trace/{sessionId}`, `stats`, `segments`, `segment/{segmentId}`, `settings`, `export`. Each
placeholder shows its screen title and buttons/links that navigate per the spec (e.g. Ride's Record FAB →
`live`; a trip row → `summary/{id}`; Stats segments row → `segments`; Settings "Export all" → `export`).

**Key files:** `…/MainActivity.kt` (`SpeedApp` NavHost), small placeholder composables (can live in
`ui/` temporarily, to be replaced by the D-tasks).

**Acceptance criteria:**
- [x] All ten routes exist and are reachable by following the spec's navigation interactions.
- [x] Back navigation (`arrow_back`) returns to the correct previous screen on every sub-screen.
- [x] Decorative elements present in placeholders are non-interactive.
- [x] No crash navigating any path; argument routes (`{sessionId}`, `{segmentId}`) parse correctly.

**Notes:** Routes + nav wiring live in `MainActivity` (`Routes`, `SpeedNavHost`); placeholders in
`ui/PlaceholderScreens.kt`. Live → Stop pops `live` inclusive then pushes `summary/{id}` so Back from
Summary doesn't return to the HUD. Ride & Live placeholders are replaced by the real screens in D1/D2.

---

# Phase C — Display/units plumbing

## C1. Settings keys
**Status:** ☑ · **Depends on:** none · **Spec:** §4.9

Extend `SettingsRepository` (DataStore) with the redesign's settings, mirroring the existing
flow+setter pattern: `gps_rate_hz` (int, default 10), `imu_rate_hz` (int, default 100),
`auto_pause` (bool, default off), `units` (enum Metric/Imperial, default Metric — store as string/int),
`dark_theme` (bool, default follows nothing — pick a sensible default, e.g. off). Expose each as a `Flow`
with a `suspend` setter. Keep the existing `auto_start_sensors` / `record_raw_traces`.

**Key files:** `…/data/SettingsRepository.kt`; a `Units` enum (new `…/data/Units.kt` or in the repo file).

**Acceptance criteria:**
- [x] Five new keys exposed as flows with setters and sensible defaults.
- [x] `units` round-trips through DataStore as a typed `Units` value.
- [x] Existing settings untouched and still working.

**Notes:** New `data/Units.kt` enum (stored by `name`, defaults METRIC). Keys added to
`SettingsRepository`: `gps_rate_hz` (10), `imu_rate_hz` (100), `auto_pause` (false), `units`,
`dark_theme` (false). `units`/`darkTheme`/`gpsRateHz` also surfaced on `RaceViewModel`.

## C2. Units-aware formatting
**Status:** ☑ · **Depends on:** C1 · **Spec:** §4 intro, §5 "Units boundary"

Add display-boundary conversion + formatting keyed off `Units`. Extend `util/Conversions.kt` and add
`util/UnitFormat.kt` with helpers to convert SI → display (km/h↔mph, km↔mi, m↔ft) and to format speed,
distance, altitude, lateral G, and lean strings with the right unit label. Provide a `LocalUnits`
CompositionLocal (or expose `units` from the ViewModel) so screens read the current unit without
threading it manually.

**Key files:** `…/util/Conversions.kt`, new `…/util/UnitFormat.kt`, ViewModel or a `LocalUnits` provider.

**Acceptance criteria:**
- [x] Conversion helpers for speed/distance/altitude both directions with unit tests.
- [x] Formatting helpers return correctly-labeled strings for Metric and Imperial.
- [x] Screens can obtain the active `Units` from one place; storage stays SI.

**Notes:** `Conversions` extended with mph/mi/ft both directions. New `util/UnitFormat.kt` formats
speed/distance/altitude/lateral-G/lean (value + unit + combined). `LocalUnits` CompositionLocal is
provided from `RaceViewModel.units` in `SpeedApp`. Covered by `UnitFormatTest`.

## C3. Dark-theme setting wiring
**Status:** ☑ · **Depends on:** A2, C1 · **Spec:** §4.9

Replace A2's temporary `isSystemInDarkTheme()` source: `MainActivity` collects the `dark_theme` flow and
passes it into `RaceLoggerTheme(darkTheme = …)`. Toggling the Settings switch (built in D9) must reskin
the whole app instantly.

**Key files:** `…/MainActivity.kt`.

**Acceptance criteria:**
- [x] App theme follows the `dark_theme` preference, not the system setting.
- [x] Changing the preference at runtime re-skins immediately with no restart.

**Notes:** `MainActivity` collects `viewModel.darkTheme` (StateFlow over the C1 key) and passes it to
`RaceLoggerTheme(darkTheme = …)`; emitting a new value recomposes and re-skins. The Settings toggle
that writes the key is built in D9.

---

# Phase D — Screens

> Each task replaces the matching B2 placeholder with the real screen per its §4 Content + Interactions,
> reusing Phase A components and C2 formatting. Where a metric depends on an unbuilt domain feature
> (E-phase), show a clear placeholder ("—") and leave a `TODO` referencing the E-task. Keep decorative
> elements non-interactive.

## D1. Ride · Ready/Home (`M3Idle`)
**Status:** ☑ · **Depends on:** A5, B2, C2 · **Spec:** §4.1

Build the pre-ride home: "Ready to ride" heading; status subtext combining sensor-readiness with the
configured GPS rate (from C1); three **decorative** sensor chips (GPS fix + satellite count, IMU state,
battery %) fed from `RaceViewModel.serviceState`; a last-ride card (most recent `SessionSummary`: label
"LAST RIDE · date", name, inline top speed / max lateral G / distance); two summary stat cards
("This week" rides count, "Total" lifetime distance — from E3 aggregates, placeholder until then);
and the **Record** extended FAB. Wire interactions per §4.1: last-ride card → `summary/{id}`; This-week →
`trips` pre-filtered to this week; Total → `stats`; FAB → start recording → `live`; bottom nav switches.

**Key files:** new `…/ui/RideHomeScreen.kt`; `…/viewmodel/RaceViewModel.kt`; DAO aggregates (stub→E3).

**Acceptance criteria:**
- [x] All §4.1 content present; sensor chips reflect live `serviceState` and are non-interactive.
- [x] Each interaction from the §4.1 table navigates/acts correctly (FAB starts recording → Live HUD).
- [x] Metrics not yet computed show a clear placeholder, not a fake number.
- [x] Speeds/distances formatted via C2 per the Units setting.

**Notes:** `ui/RideHomeScreen.kt`. Takes plain state (`ServiceState`, `gpsRateHz`, `lastRide`,
`thisWeekCount`) + nav lambdas; the parent (`SpeedNavHost`) collects flows and derives `lastRide`
(newest `SessionSummary`) and `thisWeekCount` (calendar-week filter — placeholder until E3). Placeholders
"—" with TODO(E2/E3) for max lateral G, session distance, and lifetime total distance. Battery % chip is
"—" (not yet in `ServiceState`). Last-ride name derived from the weekday until a real name field exists.

## D2. Ride · Live HUD (`M3Live`)
**Status:** ☑ · **Depends on:** A5, B2, C2 · **Spec:** §4.2

Full-bleed glanceable HUD, **no top bar or bottom nav**, only Stop interactive. Recording banner
(`errorContainer`: record dot + "Recording · elapsed", right side sampling rate + points captured);
speed hero (`primaryContainer`: "CURRENT SPEED" label, giant numeral of fused speed, `WavyLine` =
current/top-speed fraction, footer avg/top); four **decorative** metric tiles (lateral G, lean angle +
direction, signed longitudinal accel in G, GPS altitude); full-width `error` Stop button → ends & saves →
`summary/{id}`. Read everything from `serviceState`. Lean and lateral-G use the new fields from E1 once
available; until then read from current accel/placeholder.

**Key files:** new `…/ui/LiveHudScreen.kt`; `RaceViewModel.serviceState`; `WavyLine` from A5.

**Acceptance criteria:**
- [x] Banner, hero (with working `WavyLine` progress), four tiles, and Stop button all present.
- [x] Only Stop is interactive; tiles and hero are decorative.
- [x] Stop ends the recording and navigates to that session's Summary.
- [x] Elapsed time, point count, rate, and speed update live from `serviceState`.
- [x] No top bar / bottom nav shown on this route.

**Notes:** `ui/LiveHudScreen.kt`, full-bleed `systemBarsPadding`. `WavyLine` fraction = current ÷
top-speed-so-far (tracked locally since `ServiceState` has no running top). G-force tile uses
`currentG` (total accel magnitude in G) as a stand-in — TODO(E1) for true lateral G; lean tile and the
hero "avg" footer are "—" pending E1/E2. Accel & altitude use real `ServiceState`/`latestPoint` data.
Stop: parent captures `serviceState.sessionId` before `stopRecording()`, then pops `live` and pushes
`summary/{id}`. Lean icon falls back to `Icons.Filled.TwoWheeler` (no `Rounded.Motorcycle`).

## D3. Ride · Summary (`M3Summary`)
**Status:** ☑ · **Depends on:** A5, B2, C2 · **Spec:** §4.3 · **Real numbers need:** E2, E5

Post-ride summary. Top bar: `arrow_back` leading, "Trip summary" title, `share` trailing; no bottom nav.
Content: session name title; meta (date, start→end, duration); top-speed hero (`primary`) with conditional
**"NEW PB"** badge (shown only when this session beats the all-time best); 6-stat grid (max lateral G,
max lean, distance, avg speed, hard brake, moving %); segment-PB link row (count of segments where this
ride set a PB). Interactions: back; share → share sheet; hero tap → Trace at the top-speed moment;
segment-PB row → this ride's segments. Consume `SessionStats` (E2) and segment PB count (E5); placeholder
"—" until those exist.

**Key files:** new `…/ui/SummaryScreen.kt`; `RaceViewModel.getSession`; `domain/SessionStats` (E2).

**Acceptance criteria:**
- [x] All §4.3 content present; stat cards are decorative; back/share/hero/segment-row interactions work.
- [x] "NEW PB" badge appears only when the session's top speed beats the previous best.
- [x] Hero tap routes to the Trace screen for this session.
- [x] Values formatted via C2; unbuilt metrics show placeholders with a TODO referencing E2/E5.

**Notes:** `ui/SummaryScreen.kt`. Takes the `Session` + per-stat nullable params + nav lambdas; parent
(`SpeedNavHost`) loads the session via `getSession` and computes `isNewPb` = top speed beats every other
recorded session (all-time top-speed PB for the badge; per-segment PB count is E5). All six grid stats and
the segment-PB count are "—" with TODO(E1/E2/E5) until those land. Share reuses `exportSession`. Hero/
segment-row navigate to `trace/{id}` / `segments`. Name derived from the weekday until a name field exists.

## D4. Trips · List (`M3History`)
**Status:** ☑ · **Depends on:** A5, B2, C2 · **Spec:** §4.4

Reworks `SessionsScreen`. Top bar: "Trips" title, `search` trailing; bottom nav active = Trips. Content:
search bar whose placeholder shows the total session count; filter chips (All selected / This week —
`Updates in place`); trip rows newest-first (leading vehicle-type icon, name, "date · distance", trailing
top speed). Interactions: search bar opens search; filter chips filter in place; row → `summary/{id}`;
bottom nav switches. Support arriving here pre-filtered to "this week" (from D1's This-week card).

**Key files:** new `…/ui/TripsScreen.kt` (replacing `SessionsScreen.kt`); `RaceViewModel.sessions`.

**Acceptance criteria:**
- [x] Search bar shows real total count; filter chips switch the list scope in place.
- [x] Rows show name/date/distance/top-speed and open the correct Summary on tap.
- [x] Deep-link/argument to open pre-filtered to "this week" works.
- [x] Distances/speeds formatted via C2.

**Notes:** `ui/TripsScreen.kt` (+ `TripsFilter` enum). Rows reuse `ListRow`; `isThisWeek` promoted to a
shared helper in `ui/DateTimeUtils.kt` (used by both Trips filter and the Ride home count). Deep-link:
route is `trips?filter={filter}` (default "all"); the Ride "This week" card navigates `trips?filter=week`,
and `SpeedAppShell` strips the query before bottom-nav matching so the bar still shows + highlights Trips.
Per-trip distance is "—" with TODO(E2); names derived from the weekday; search bar is tap-to-open (no-op
until a search overlay exists).

## D5. Trips · Trace/detail (`M3Detail`)
**Status:** ☑ · **Depends on:** A5, B2, C2 · **Spec:** §4.5 · **Map needs:** F1

Reworks `SessionDetailScreen`. Top bar: `arrow_back` leading, "Trace" title, `download` trailing; no
bottom nav. Content: map card (placeholder card until F1); speed chart card (hand-rolled Compose `Canvas`,
header shows min–max); G-lateral chart card (zero baseline, header min/max). Implement the **drag scrub
playhead** on the charts that moves a shared playhead and updates the read-out values (and later the map
marker via F1). Download trailing action exports just this ride (CSV now; GPX/FIT via F3/F4). Extend the
existing `SpeedChart` Canvas code as the starting point.

**Key files:** new/renamed `…/ui/TraceScreen.kt` (from `SessionDetailScreen.kt`); existing `SpeedChart`;
`RaceViewModel.getPointsForSession`.

**Acceptance criteria:**
- [x] Speed and lateral-G charts render from real session points with correct min/max headers.
- [x] Dragging on a chart moves a playhead and updates the displayed values for both charts.
- [x] Download triggers an export of this single ride.
- [x] Map area is a clear placeholder card pending F1; back returns to Trips.

**Notes:** `ui/TraceScreen.kt`. Parent loads points via `getPointsForSession`. A generic private
`TraceChart` (Compose `Canvas`) renders both cards; a single hoisted `playheadIndex` is shared so dragging
either chart updates both headers (speed value in `primary`, signed G in `tertiary`). Speed series =
`derivedSpeedMs ?: gpsSpeedMs`. **Lateral-G is a stand-in**: it plots signed longitudinal accel in G
(`Conversions.ms2ToG(derivedAccelMs2)`) with a TODO(E1) to swap to the real `DataPoint.lateralGz` once E1
lands. Map is a non-interactive placeholder (TODO F1/F2). Download reuses `exportSession` (CSV/zip today).
The old `SessionDetailScreen`/`SpeedChart` are untouched and removed in G1.

## D6. Stats · Overview (`M3Stats`)
**Status:** ☑ · **Depends on:** A5, B2, C2 · **Spec:** §4.6 · **Real numbers need:** E3

New screen. Top bar: "Statistics" title, `date_range` trailing; bottom nav active = Stats. All figures
re-scope to the selected range. Content: range chips (This year / 90 days / All time — `Updates in
place`); distance hero (`primaryContainer`: scope label, trend % vs previous comparable period, big
distance numeral, 6-month bar chart with current month highlighted — Canvas); records grid ×4 (top speed,
max lean, max lateral G, longest ride); segments link row (tracked-segment count). Interactions: date
range opens a picker; range chips re-scope; month bar → that month's trips; record card → the ride holding
that record; segments row → `segments`; bottom nav switches. Add scoped aggregate DAO queries (E3).

**Key files:** new `…/ui/StatsScreen.kt`; DAO aggregates (E3); a bar-chart Canvas component.

**Acceptance criteria:**
- [x] Range chips re-scope every figure on the screen.
- [x] Distance hero shows scoped total + trend % + a 6-month bar chart with the current month highlighted.
- [x] Records grid and month bars navigate to the correct ride/trips per §4.6.
- [x] Numbers come from E3 aggregates (placeholder until E3); formatted via C2.

**Notes:** `ui/StatsScreen.kt` (+ `StatsRange` enum). Range chips switch an internal scope (year/90d/all)
that re-scopes the label and the one derivable figure — the **Top-speed record**, computed from the scoped
`SessionSummary` list and wired to navigate to its holding ride. Everything else is deferred to E3 and shows
"—": scoped total distance + trend (TODO E2/E3), max lean/max-g-lat (TODO E1/E3), longest ride (TODO E2/E3),
segment count (TODO E4). The 6-month bar row renders the trailing-6-month labels with the current month
highlighted and zero-height placeholder bars (TODO E2/E3); bars + the three undeived record cards route to
trips/are inert until E3. Icon fallbacks: `Icons.Filled.Moving`/`Route` (no Rounded variants).

## D7. Stats · Segment list (`M3SegmentList`)
**Status:** ☐ · **Depends on:** A5, B2, C2 · **Spec:** §4.7 · **Data needs:** E4, E5

New screen. Top bar: `arrow_back` leading, "Segments" title, `add` trailing; bottom nav active = Stats.
Content: search bar; sort chips (Best time / Most runs / Nearby — `Updates in place`); count label;
segment rows (leading timer chip highlighted if favourite/goal, name, "distance · N runs", best time,
trend vs previous attempt with delta seconds — down/green faster, up/error slower, dash unchanged).
Interactions: back → Stats; add opens new-segment creation (E6); search; sort chips re-sort; row →
`segment/{id}`; bottom nav. Until E4/E5 land, show an empty-state with the chrome wired.

**Key files:** new `…/ui/SegmentListScreen.kt`; segment DAO/repo (E4); trend computation (E5).

**Acceptance criteria:**
- [ ] Search, sort chips, count label, and segment rows render per §4.7 (empty-state acceptable pre-E4).
- [ ] Trend indicator shows correct direction/color and delta seconds when data exists.
- [ ] Row → segment detail; add → creation flow; back → Stats.

## D8. Stats · Segment detail (`M3Segment`)
**Status:** ☐ · **Depends on:** A5, B2, C2 · **Spec:** §4.8 · **Data needs:** E4, E5

New screen. Top bar: `arrow_back` leading, "Segment" title, `more_vert` trailing; no bottom nav. Content:
title + meta (name, "distance · N attempts"); personal-best card (`primaryContainer`: "PERSONAL BEST" +
best time, trend vs previous PB, three inline stats from the PB run — max speed/max lateral G/max lean,
optionally a `WavyLine`); history list of every attempt newest-first (date, time, relative bar ∝ vs best,
delta vs best; best attempt highlighted). Interactions: back → segment list; overflow ⋮ opens a menu
(rename / set as goal / delete / share); PB card → trace of the PB attempt; attempt row → that attempt's
ride/trace.

**Key files:** new `…/ui/SegmentDetailScreen.kt`; segment DAO/repo (E4/E5).

**Acceptance criteria:**
- [ ] PB card, attempt history with relative bars + deltas, and the overflow menu all present.
- [ ] PB card and attempt rows navigate to the corresponding trace.
- [ ] Best attempt is visually highlighted; values formatted via C2.

## D9. Settings (`M3Settings`)
**Status:** ☐ · **Depends on:** A5, B2, C1 · **Spec:** §4.9

Reworks `SettingsScreen`. Top bar: "Settings" title, `help` trailing; bottom nav active = Settings.
Grouped lists with `primary` section labels: **SAMPLING** (GPS rate picker, IMU rate picker, Auto-pause
switch); **DISPLAY** (Units picker Metric/Imperial, Dark-theme switch); and a `tertiaryContainer`
**Raw data export card** summarizing dataset size + formats with an "Export all" button. All rows bound to
C1 keys; switches toggle instantly (Dark-theme drives C3). Interactions: help opens help/about; picker rows
open pickers; switches update in place; Export-all → `export`; bottom nav switches.

**Key files:** new `…/ui/SettingsScreen.kt` (replacing the old one); `SettingsRepository` (C1).

**Acceptance criteria:**
- [ ] GPS/IMU/Units pickers read & write the C1 settings; Auto-pause and Dark-theme switches toggle instantly.
- [ ] Dark-theme switch reskins the app live (via C3).
- [ ] Export-all navigates to the Export screen; help action opens help/about.
- [ ] Raw-data-export card summarizes trips count + size + available formats.

## D10. Settings · Export (`M3Export`)
**Status:** ☐ · **Depends on:** A5, B2, C1 · **Spec:** §4.10 · **Real export needs:** F3, F4, F5

New screen. Top bar: `arrow_back` leading, "Export data" title, `help` trailing; no bottom nav. Content:
summary line (dataset size at full resolution); **FORMAT** single-select 3-up cards CSV/GPX/FIT (selected
outlined in `primary` with `check_circle`); **SCOPE** rows (Trips picker, Date-range picker); **INCLUDE**
per-stream switches (GPS track, IMU, Lean angle); full-width `primary` Export button whose label reflects
the resulting count + estimated size from the current selection. Interactions: back → Settings; help;
format select in place; scope rows open pickers; include switches in place; Export runs the export
(F5 wiring) → progress → share.

**Key files:** new `…/ui/ExportScreen.kt`; export pipeline (F3/F4/F5).

**Acceptance criteria:**
- [ ] Format single-select, scope pickers, and include switches all function and update the button label.
- [ ] Export button label shows the live count + estimated size for the current scope/include.
- [ ] Export action produces a file in the selected format honoring the include toggles (via F-phase),
      then offers share.

---

# Phase E — Domain: session metrics & segments

## E1. Lean angle + lateral G in the pipeline
**Status:** ☐ · **Depends on:** none (parallel to D) · **Spec:** §4.2, §4.3, §4.5

Add `leanAngleDeg` (signed, + = right) and `lateralGz` to `DataPoint`. Derive lean from the rotation
vector already captured by `ImuCollector`; derive lateral G from the world-frame acceleration component
perpendicular to the direction of travel. Compute these in the fusion path (`DataFusion` /
`VelocitiFusion`), persist them, and bump the Room schema version (the DB already uses destructive
fallback). Keep `CoordinateTransformer` pure/JVM-testable and add unit tests for the new math.

**Key files:** `…/data/DataPoint.kt`, `…/fusion/DataFusion.kt`, `…/fusion/VelocitiFusion.kt`,
`…/fusion/CoordinateTransformer.kt`, `…/data/RaceDatabase.kt`.

**Acceptance criteria:**
- [ ] `DataPoint` persists `leanAngleDeg` and `lateralGz`; Room version bumped and migrates (or destroys) cleanly.
- [ ] Lean and lateral-G are computed each 10 Hz tick and exposed in `ServiceState` for the Live HUD.
- [ ] Unit tests cover the lean/lateral-G derivation in `CoordinateTransformer` (pure functions).

## E2. SessionStats computer
**Status:** ☐ · **Depends on:** E1 · **Spec:** §4.3, §4.1

Add `domain/SessionStats.kt` computing per-session: distance (GPS integration — currently missing),
average speed, max speed (top), max lateral G, max lean, hard brake (most-negative signed longitudinal
accel), and moving % (share of time not stationary — use the ZUPT/stationary signal). Either persist a
`SessionStats` row (recompute on session finalize) or compute-on-load with a cache. Expose via the
repository/ViewModel for D1/D3/D6.

**Key files:** new `…/domain/SessionStats.kt`; `…/data/RaceRepository.kt`; possibly a new entity/DAO;
`…/service/RaceRecordingService.kt` (finalize hook).

**Acceptance criteria:**
- [ ] All six+ metrics computed correctly from a session's `DataPoint`s (with unit tests on sample data).
- [ ] Distance integrates GPS track (validated against a known-distance fixture).
- [ ] Stats are available to the Summary/Home/Stats screens (replacing their placeholders).

## E3. Aggregate queries
**Status:** ☐ · **Depends on:** E2 · **Spec:** §4.1, §4.6

Add DAO queries for: rides recorded in the current calendar week, lifetime cumulative distance, and
range-scoped aggregates (top speed / max lean / max lateral G / longest ride and total distance for
This-year / 90-days / All-time, plus per-month distance for the trailing 6 months and trend vs the prior
comparable period). Feed D1's two summary cards and all of D6.

**Key files:** `…/data/DataPointDao.kt` (or a new stats DAO), `…/data/RaceRepository.kt`, ViewModel.

**Acceptance criteria:**
- [ ] This-week count and lifetime distance available to D1.
- [ ] Scoped records, scoped total distance, 6-month per-month series, and trend % available to D6.
- [ ] Queries covered by unit/instrumented tests on seeded data.

## E4. Segments data model
**Status:** ☐ · **Depends on:** none (parallel) · **Spec:** §4.7, §4.8

Introduce `Segment` (name, geometry/path, distance, optional favourite/goal flag) and `SegmentAttempt`
(segmentId, sessionId, time, date, the PB-run stats) entities with a DAO and repository methods. Bump
the Room schema. This is data-model only — matching/timing is E5.

**Key files:** new `…/data/Segment.kt`, `…/data/SegmentAttempt.kt`, `…/data/SegmentDao.kt`;
`…/data/RaceDatabase.kt`; repository additions.

**Acceptance criteria:**
- [ ] Entities + DAO + repository CRUD compile and migrate.
- [ ] Queries exist for: list segments (with best time, run count, last-attempt trend) and a segment's
      attempt history.
- [ ] DAO covered by tests.

## E5. Segment matching & timing
**Status:** ☐ · **Depends on:** E4, E2 · **Spec:** §4.7, §4.8, §4.3 (PB count)

After a ride is saved, detect which segments the session's GPS track crosses and record a
`SegmentAttempt` with its elapsed time; compute best time and the trend vs the previous attempt; expose
the count of segments where the ride set a PB (for the Summary segment row). Run as a post-ride job
(service finalize hook or a worker).

**Key files:** new `…/domain/SegmentMatcher.kt`; `…/service/RaceRecordingService.kt` finalize hook or a
`WorkManager` worker; segment repository.

**Acceptance criteria:**
- [ ] Re-crossing a defined segment on a new ride creates an attempt with a correct elapsed time.
- [ ] Best time and trend (faster/slower/unchanged + delta) computed and shown in D7/D8.
- [ ] Summary's "N personal bests by segment" reflects real PBs set by that ride.
- [ ] Matching logic unit-tested on a synthetic track fixture.

## E6. Segment creation flow
**Status:** ☐ · **Depends on:** E4, F1 · **Spec:** §4.7 (Add)

Implement D7's `add` action: create a new `Segment` either by drawing on the map or by picking a stretch
from an existing ride. Persist via the E4 repository; new segment then appears in the list and begins
being matched (E5) on future rides.

**Key files:** new creation composable/route; `MapTrackCard`/map from F1; segment repository.

**Acceptance criteria:**
- [ ] User can define a segment from a ride (and/or by drawing) and save it.
- [ ] The saved segment appears in the Segment list and is eligible for matching on subsequent rides.

---

# Phase F — Map & export formats

## F1. MapLibre integration
**Status:** ☐ · **Depends on:** D5 · **Spec:** §4.5, §5 "Charts & map"

Add the MapLibre Android SDK (or osmdroid) and a `MapTrackCard` composable: render the session's GPS
polyline with a start marker (primary) and end marker (error), and a floating chip calling out the
top-speed value at its point. Wire the D5 scrub playhead so dragging the chart moves a marker along the
track. Replace D5's map placeholder.

**Key files:** `app/build.gradle.kts` (dependency), new `…/ui/components/MapTrackCard.kt`, `…/ui/TraceScreen.kt`.

**Acceptance criteria:**
- [ ] Map renders OSM tiles with the session polyline + start/end markers and a top-speed chip.
- [ ] No API key/billing required (open-source provider).
- [ ] Scrubbing the D5 charts moves a marker along the polyline in sync with the read-outs.

## F2. Full-screen map
**Status:** ☐ · **Depends on:** F1 · **Spec:** §4.5 (map card → Opens)

Tapping D5's map card opens a full-screen interactive (pan/zoom) map route showing the same track.

**Key files:** new full-screen map route/composable; `…/MainActivity.kt` nav.

**Acceptance criteria:**
- [ ] Map card tap opens a full-screen interactive map of the ride; back returns to Trace.

## F3. GPX export
**Status:** ☐ · **Depends on:** none (parallel) · **Spec:** §4.5, §4.10, §5

Add a GPX writer alongside the existing CSV export (`RaceRepository.exportToCsv`). Produce valid GPX
(track points with lat/lon/elevation/time) for a session.

**Key files:** `…/data/RaceRepository.kt` (or a new `…/export/GpxExporter.kt`).

**Acceptance criteria:**
- [ ] GPX output validates against the schema and opens in a standard GPX viewer.
- [ ] Wired into the Download action (D5) and Export format choice (D10).

## F4. FIT export
**Status:** ☐ · **Depends on:** none (parallel) · **Spec:** §4.5, §4.10

Add a FIT writer (Garmin FIT SDK or a minimal encoder). Encode the session record stream into a valid
`.fit` file.

**Key files:** new `…/export/FitExporter.kt`; `app/build.gradle.kts` if using the FIT SDK.

**Acceptance criteria:**
- [ ] FIT output is readable by a standard FIT decoder/tool.
- [ ] Wired into the Export format choice (D10).

## F5. Export scope/include wiring
**Status:** ☐ · **Depends on:** F3, F4 · **Spec:** §4.10

Make D10 fully functional: honor SCOPE (which trips / date range) and INCLUDE toggles (GPS track / IMU /
Lean) when producing the chosen-format file; compute the estimated-size + count for the button label; run
as the Export action with progress, then offer share.

**Key files:** new `…/export/ExportManager.kt`; `…/ui/ExportScreen.kt`; `RaceViewModel`.

**Acceptance criteria:**
- [ ] Exported file contains only the selected streams for only the selected trips/date range.
- [ ] The Export button label's count + estimated size matches the actual export.
- [ ] Progress is shown and a share sheet appears on completion.

---

# Phase G — Cleanup

## G1. Remove dead code & refresh previews
**Status:** ☐ · **Depends on:** D1–D10 complete · **Spec:** —

Once the new screens replace the originals, delete the obsolete `RaceScreen`/`SessionsScreen`/old
`SessionDetailScreen`/old `SettingsScreen` and any leftover red/teal colors; refresh `ScreenPreviews.kt`
to cover the redesigned screens; drop the Material-2 `material` dependency if no longer referenced.

**Key files:** old `ui/*` screens, `…/ui/theme/Color.kt`, `…/ui/ScreenPreviews.kt`, `app/build.gradle.kts`.

**Acceptance criteria:**
- [ ] No references remain to the deleted screens; app builds and all routes work.
- [ ] `ScreenPreviews.kt` previews the redesigned screens in light and dark.
- [ ] Unused dependencies/colors removed; `./gradlew assembleDebug` and `testDebugUnitTest` pass.

---

## Verification reference (whole-app)
- **Per task:** `./gradlew assembleDebug`, then exercise the affected screen via the `/run` skill or an
  Android Studio preview; confirm the §4 interaction contract for that screen.
- **Theme (A1–A3, C3):** toggle Dark-theme; confirm both schemes match §2.1 and all screens re-skin with
  no hard-coded colors.
- **Navigation (B1–B2):** tap every element in each §4 Interactions table; confirm correct
  destination/in-place update/sheet, and that decorative elements do nothing.
- **Units (C1–C2):** switch Metric↔Imperial; every speed/distance/altitude updates while the DB stays SI.
- **Metrics (E1–E3):** record a short ride; verify distance/lateral G/lean/hard brake/moving % on the
  Summary; run `SessionStats`/`CoordinateTransformer` unit tests.
- **Segments (E4–E6):** create a segment, re-cross it on a new ride, confirm an attempt + trend appear.
- **Export (F3–F5):** export CSV/GPX/FIT with different INCLUDE toggles; open the files to confirm streams
  and that the button's count/size label matched.
- After any Room-schema change (E1/E2/E4) run `./gradlew testDebugUnitTest`.
