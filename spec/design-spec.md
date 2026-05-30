# Speed — Design Specification

> Source of truth: `spec/design-artifacts/` (Material 3 Expressive storyboard). This document is a written distillation of those mock-ups for implementation. The mock-ups are React/Babel; the app is Android (Jetpack Compose + Material 3). Where the two disagree, **this document wins** and the artifact should be re-rendered.

## 1. Product overview

Speed is a motorcycle/vehicle race-data logger. It records speed, acceleration, lean, GPS and IMU data at high precision, then lets the rider relive, analyse and export each ride. The redesign organises the whole app around four tasks, each a tab in the bottom navigation:

| Tab | Label | Icon | Purpose |
|---|---|---|---|
| 1 | **Ride** | `home` | Pre-ride home + live recording HUD |
| 2 | **Trips** | `route` | Browse, search and open past rides |
| 3 | **Stats** | `leaderboard` | Trends, records and tracked segments |
| 4 | **Settings** | `settings` | Capture/display preferences + data export |

Four primary flows span these tabs:

1. **Record a ride** — Ready/Home → Live HUD → Ride summary
2. **Open a specific ride** — Trips list → Trace / detail
3. **See your statistics** — Overview → Segments list → Segment detail
4. **Tune & export your data** — Settings → Export screen

## 2. Design system

The visual language is **Material 3 (Expressive)**: tonal surfaces, large rounded shapes, an extended FAB, a bottom navigation bar with an active "pill", Material Symbols (Rounded) icons, and the wavy progress indicator.

### 2.0 Units & dimensions

The artifacts are drawn in a CSS-pixel canvas **320 wide × 680 tall**. Treat that as a **320 dp reference frame**: all sizes below are **proportional design intent, not pixel-exact requirements**, and should be expressed in Compose as:

- **Layout, spacing, corner radii, component sizes → `dp`.**
- **Text sizes → `sp`** (so they respect the user's font-scale accessibility setting). Where this document lists a type size, read it as sp.

### 2.1 Color

The app ships a single **green seed** scheme with full light and dark variants. All colors are standard M3 roles — implement as a Compose `ColorScheme`, not as hard-coded hex.

**Green · Light (default)**

| Role | Hex | Role | Hex |
|---|---|---|---|
| primary | `#3b6939` | onPrimary | `#ffffff` |
| primaryContainer | `#bcf0b4` | onPrimaryContainer | `#002106` |
| secondary | `#52634f` | secondaryContainer | `#d5e8cf` |
| onSecondaryContainer | `#101f10` | tertiary | `#38656a` |
| tertiaryContainer | `#bcebf0` | onTertiaryContainer | `#002023` |
| error | `#ba1a1a` | errorContainer | `#ffdad6` |
| onErrorContainer | `#410002` | surface | `#f7fbf1` |
| surfaceDim | `#d7dbd2` | surfaceContainerLow | `#f1f5eb` |
| surfaceContainer | `#ebefe5` | surfaceContainerHigh | `#e5e9df` |
| surfaceContainerHighest | `#e0e4da` | onSurface | `#181d17` |
| onSurfaceVar | `#424940` | outline | `#72796f` |
| outlineVar | `#c1c9bd` | | |

**Green · Dark**

| Role | Hex | Role | Hex |
|---|---|---|---|
| primary | `#a1d39a` | onPrimary | `#0a390f` |
| primaryContainer | `#235024` | onPrimaryContainer | `#bcf0b4` |
| secondary | `#b9ccb4` | secondaryContainer | `#3a4b38` |
| onSecondaryContainer | `#d5e8cf` | tertiary | `#a0cfd4` |
| tertiaryContainer | `#1e4d52` | onTertiaryContainer | `#bcebf0` |
| error | `#ffb4ab` | errorContainer | `#93000a` |
| onErrorContainer | `#ffdad6` | surface | `#10140f` |
| surfaceContainerLow | `#181d17` | surfaceContainer | `#1c211b` |
| surfaceContainerHigh | `#272b25` | surfaceContainerHighest | `#313630` |
| onSurface | `#e0e4da` | onSurfaceVar | `#c1c9bd` |
| outline | `#8b9286` | outlineVar | `#424940` |

> **Rule:** dark theme behaves identically to light — same layouts, same interactions. Only the color roles swap. Dark/light follows the **Dark theme** setting (§4.9).

### 2.2 Typography

The single app typeface is **Roboto Flex** (`"Roboto Flex", system-ui, sans-serif`). The design leans on large, tight display numerals for hero metrics (speed, top speed, time). Sizes below are in **sp** (see §2.0).

| Use | Size (sp) | Weight | Notes |
|---|---|---|---|
| Hero numeral (live speed) | 104 | 700 | letter-spacing −0.04em, line-height 0.9 |
| Hero numeral (summary/segment) | 64–80 | 700 | |
| Hero numeral (stats distance) | 56 | 700 | |
| Screen heading | 24–28 | 500 | e.g. "Ready to ride", a ride name |
| Top-bar title | 22 | 400 | |
| Card stat value | 20–30 | 600 | |
| Body / list primary | 15–16 | 500 | |
| Caption / meta | 11–13 | 400–500 | onSurfaceVar |
| Section label | 12–13 | 600 | uppercase, letter-spacing, primary color |

### 2.3 Iconography

Material Symbols **Rounded**, with the fill axis used to denote active/emphasis state (`fill 1` = active/selected, `fill 0` = inactive). Sizes 16–26 dp depending on context. Notable glyphs: `home`, `route`, `leaderboard`, `settings`, `gps_fixed`, `sensors`, `battery_full`, `two_wheeler`, `fiber_manual_record`, `stop`, `speed`, `motorcycle`, `trending_up`, `terrain`, `trophy`, `timer`, `search`, `add`, `date_range`, `download`, `share`, `help`, `more_vert`, `chevron_right`, `arrow_back`, `check`, `check_circle`, `arrow_upward`/`arrow_downward`/`remove` (trend), `database`, `satellite_alt`, `calendar_month`, `pause_circle`, `dark_mode`, `straighten`.

### 2.4 Shape & spacing

Rounded, generous corners are a defining trait (all values dp):

| Element | Corner radius (dp) |
|---|---|
| Hero / large cards | 28 |
| Export & map cards | 24 |
| Medium tonal cards | 18–20 |
| Small stat cards | 18 |
| Bordered link rows | 16 |
| Recording banner / small chips-as-buttons | 12 |
| Filter/sort/range chips | 8 |
| Search bar (pill) | 24 (height 48) |
| Bottom-nav active pill | 16 (56×32) |
| FAB / full-width buttons | 18 (FAB) / 28 (full-width) |

Standard screen horizontal padding is **16 dp**. Content area sits between a 28 dp status bar + 56 dp top bar at the top and a 72 dp bottom nav (when present).

### 2.5 Shared components

- **Status bar** (28 dp): time left, signal/battery glyphs right. Tinted `onSurface`. (This is the OS status bar — in-app it's the system bar, not a drawn element.)
- **Top app bar** (56 dp, below status bar): optional leading icon (`arrow_back`), title (22 sp), optional trailing action icon (e.g. `search`, `share`, `download`, `help`, `date_range`, `more_vert`, `add`). Background = `surface`.
- **Bottom navigation bar** (72 dp): four destinations (Ride/Trips/Stats/Settings). Active item shows a `secondaryContainer` pill behind a filled icon plus a heavier label. Background = `surfaceContainer`.
- **Extended FAB**: 56 dp tall, `primary` background, icon + label, e.g. "Record".
- **Full-width filled button**: 56 dp, 28 radius (Stop = `error`, Export = `primary`).
- **Tonal stat card**: container color + matching `on*` text, icon top-right, big value, small caption.
- **Filter / sort / range chips**: 32 dp tall, 8 radius. Selected = `secondaryContainer` with a leading `check`; unselected = outlined.
- **Switch**: 52×32 dp, `primary` track when on with a large white thumb; outlined track with a small thumb when off.
- **Wavy progress line** (`WavyLine`): M3 Expressive sine-wave progress with a filled leading dot — used in the live speed hero to show progress toward top speed.
- **List row**: leading round 44 dp icon chip, title + meta, trailing value or `chevron_right`.

## 3. Interaction model

Every interactive element is classified into one of five kinds (from the redline layer). The implementation must honor these as the contract for what each element does:

| Kind | Meaning |
|---|---|
| **Navigates** | Pushes/switches to another screen |
| **Updates in place** | Mutates current screen state, no navigation (filters, toggles) |
| **Opens** | Opens a sheet, picker, menu or search overlay |
| **Action** | Runs a system task (start/stop recording, export) |
| **Decorative** | Intentionally non-interactive; glanceable only |

## 4. Screens

For each screen the **Content** describes what every element represents (the *e.g.* values are taken from the mock-up and are illustrative). The **Interactions** table is the behavioural contract. Display values respect the **Units** setting (§4.9): speed in km/h or mph, distance in km or mi, etc. — internal storage is SI (m/s, m, m/s²).

### 4.1 Ride · Ready / Home (`M3Idle`)

Home base before a ride. Background `surface`, top bar title "Speed" (no trailing icon), bottom nav active = **Ride**.

**Content:**
- **Heading** — static "Ready to ride".
- **Status subtext** — sensor-readiness summary + the configured **GPS sample rate** from Settings. *e.g.* "All sensors locked · sampling at 10 Hz" (10 Hz = current GPS rate setting).
- **Sensor chips** (live device/sensor status, decorative):
  - *GPS* — fix state + satellite count in use. *e.g.* "GPS 12" = fix acquired, 12 satellites.
  - *IMU* — IMU availability/active state.
  - *Battery* — device battery level %. *e.g.* "94%".
- **Last-ride card** — the most recently recorded session:
  - Label = "LAST RIDE · " + that session's date.
  - Title = the session's name (user-given or auto-derived from location).
  - Three inline stats = that session's **top speed** (km/h), **max lateral G**, **distance** (km).
- **Two summary stat cards**:
  - *This week* — count of sessions recorded in the current calendar week. *e.g.* "4 rides".
  - *Total* — lifetime cumulative distance across all sessions. *e.g.* "8 412 km".
- **Extended FAB** — "Record".

**Interactions:**

| # | Element | Gesture | Kind | Result |
|---|---|---|---|---|
| 1 | Sensor chips | — | Decorative | GPS/IMU/battery status, not tappable |
| 2 | Last-ride card | tap | Navigates | Opens the full Summary for that ride |
| 3 | This-week card | tap | Navigates | Opens Trips, pre-filtered to this week |
| 4 | Total card | tap | Navigates | Opens Statistics (lifetime totals) |
| 5 | Record FAB | tap | Action | Starts a new recording → Live HUD |
| 6 | Bottom nav | tap | Navigates | Switches Ride · Trips · Stats · Settings |

### 4.2 Ride · Live HUD (`M3Live`)

During-ride heads-up display. Deliberately glanceable — **only Stop is interactive**. No top bar or bottom nav; full-bleed content with the recording banner at top.

**Content:**
- **Recording banner** (`errorContainer`):
  - Record dot + "Recording · " + **elapsed time** since start (monotonic clock). *e.g.* "12:47".
  - Right side = current **sampling rate** (GPS Hz) + **points captured** so far this session. *e.g.* "10 Hz · 7 642 pts".
- **Speed hero** (`primaryContainer`):
  - "CURRENT SPEED" label.
  - Giant numeral = current **Kalman-fused speed** in display units. *e.g.* "142 km/h".
  - **WavyLine** = current speed as a fraction of this session's top speed so far. *e.g.* ≈64%.
  - Footer = running **average speed** and **session top speed**. *e.g.* "avg 118 / top 187".
- **Metric tiles ×4** (live readouts, decorative):
  - *G-force* — current **lateral G** (cornering load). *e.g.* "0.84 g lat".
  - *Lean* — current **lean angle + direction** (from rotation vector). *e.g.* "38° right".
  - *Accel* — current **signed longitudinal acceleration** in G (+ = speeding up, − = braking). *e.g.* "+0.42 g".
  - *Altitude* — current **GPS altitude**. *e.g.* "1247 m".
- **Stop button** — full-width `error`.

**Interactions:**

| # | Element | Gesture | Kind | Result |
|---|---|---|---|---|
| 1 | Recording banner | — | Decorative | Elapsed time, rate, point count |
| 2 | Speed hero | — | Decorative | Live speed + progress to top speed |
| 3 | Metric tiles ×4 | — | Decorative | Live G / lean / accel / altitude |
| 4 | Stop button | tap | Action | Ends & saves the ride → Summary |

### 4.3 Ride · Summary (`M3Summary`)

Post-ride payoff. Top bar: `arrow_back` leading, "Trip summary" title, `share` trailing. No bottom nav.

**Content:**
- **Title** — the session's name. *e.g.* "Col de Turini".
- **Meta** — session date, start→end wall-clock times, total duration. *e.g.* "26 May · 14:32 → 15:19 · 47:12".
- **Top-speed hero** (`primary`):
  - "TOP SPEED" + the session **max speed**. *e.g.* "187 km/h".
  - **"NEW PB" badge** — shown only when this session's top speed beats the previous all-time best (conditional).
- **Stat grid ×6** — read-only session metrics: **Max lateral G**, **Max lean**, **Distance**, **Average speed**, **Hard brake** (most-negative longitudinal G), **Moving %** (share of session time in motion vs stopped). *e.g.* 1.24 g / 52° / 54.8 km / 69.7 km/h / −0.92 g / 93%.
- **Segment link row** — count of segments where this ride set a personal best. *e.g.* "3 personal bests by segment".

**Interactions:**

| # | Element | Gesture | Kind | Result |
|---|---|---|---|---|
| 1 | Back | tap | Navigates | Returns to the previous screen |
| 2 | Share | tap | Opens | Share sheet (image / GPX / link) |
| 3 | Top-speed hero | tap | Navigates | Jumps to the top-speed moment on the Trace |
| 4 | Stat cards ×6 | — | Decorative | Read-only ride metrics |
| 5 | Segment-PB row | tap | Navigates | This ride's segments → Segment list |

### 4.4 Trips · List (`M3History`)

Top bar: "Trips" title, `search` trailing. Bottom nav active = **Trips**.

**Content:**
- **Search bar** — placeholder shows the **total session count**. *e.g.* "Search 142 trips".
- **Filter chips** — list scope. *e.g.* "All" (selected) / "This week"; extensible.
- **Trip rows** — one per session, newest first: leading vehicle-type icon, **name**, "**date · distance**", trailing **session top speed** in km/h. *e.g.* "Col de Turini · 26 May · 54.8 km · 187".

**Interactions:**

| # | Element | Gesture | Kind | Result |
|---|---|---|---|---|
| 1 | Search bar | tap | Opens | Search across all trips |
| 2 | Filter chips | tap | Updates in place | Filters list (All / This week) |
| 3 | Trip row | tap | Navigates | Opens that ride's Summary |
| 4 | Bottom nav | tap | Navigates | Switches section |

### 4.5 Trips · Trace / detail (`M3Detail`)

Top bar: `arrow_back` leading, "Trace" title, `download` trailing. No bottom nav.

**Content:**
- **Map card** — the session's **GPS track** as a polyline; start marker (primary), end marker (error); a floating chip calling out the **top-speed value** (and its point on the route). *e.g.* "187 km/h".
- **Speed chart card** — **speed over the ride** (time/distance axis); header shows the **min–max range**. *e.g.* "0 – 187 km/h".
- **G-lateral chart card** — **lateral G over the ride**, zero baseline; header shows **min/max**. *e.g.* "−1.18 / +1.24".

**Interactions:**

| # | Element | Gesture | Kind | Result |
|---|---|---|---|---|
| 1 | Back | tap | Navigates | Returns to Trips |
| 2 | Download | tap | Action | Exports just this ride (CSV / GPX / FIT) |
| 3 | Map card | tap | Opens | Full-screen interactive map |
| 4 | Speed & G charts ×2 | drag | Updates in place | Scrub a playhead; map + values follow finger |

### 4.6 Stats · Overview (`M3Stats`)

Top bar: "Statistics" title, `date_range` trailing. Bottom nav active = **Stats**. **All figures re-scope to the selected range.**

**Content:**
- **Range chips** — active scope. *e.g.* "This year" (selected) / "90 days" / "All time".
- **Distance hero** (`primaryContainer`):
  - Label = the scope. *e.g.* "DISTANCE · 2026".
  - **Trend** = % change vs the previous comparable period. *e.g.* "+18%".
  - Big numeral = **total distance in scope**. *e.g.* "3 412 km".
  - **6-month bar chart** = per-month distance over the trailing 6 months; current month highlighted.
- **Records grid ×4** — bests within scope: **Top speed**, **Max lean**, **Max lateral G**, **Longest ride** (distance). *e.g.* 187 km/h / 52° / 1.24 g / 312 km.
- **Segments link row** — count of tracked segments. *e.g.* "14 tracked segments".

**Interactions:**

| # | Element | Gesture | Kind | Result |
|---|---|---|---|---|
| 1 | Date range | tap | Opens | Custom date-range picker |
| 2 | Range chips | tap | Updates in place | Re-scopes all stats (year / 90 d / all) |
| 3 | Month bars | tap | Navigates | That month's trips |
| 4 | Record cards ×4 | tap | Navigates | Opens the ride holding each record |
| 5 | Segments row | tap | Navigates | Opens the Segment list |
| 6 | Bottom nav | tap | Navigates | Switches section |

### 4.7 Stats · Segment list (`M3SegmentList`)

Top bar: `arrow_back` leading, "Segments" title, `add` trailing. Bottom nav active = **Stats**.

A **segment** is a user-defined stretch of road that the app times across every ride that covers it.

**Content:**
- **Search bar** — "Search segments…".
- **Sort chips** — active sort. *e.g.* "Best time" (selected) / "Most runs" / "Nearby".
- **Count label** — total tracked segments. *e.g.* "14 SEGMENTS".
- **Segment rows** — per segment: leading timer chip (highlighted = favourite/goal), **name**, "**distance · N runs**" (attempts), **best time**, and a **trend** vs the previous attempt (down/green = faster, up/error = slower, dash = unchanged) with the **delta in seconds**. *e.g.* "Ascent, west · 2.84 km · 7 runs · 2:14.8 · 3.2s faster".

**Interactions:**

| # | Element | Gesture | Kind | Result |
|---|---|---|---|---|
| 1 | Back | tap | Navigates | Returns to Statistics |
| 2 | Add | tap | Opens | New segment — draw on map or pick from a ride |
| 3 | Search bar | tap | Opens | Searches segments |
| 4 | Sort chips | tap | Updates in place | Re-sorts (time / runs / nearby) |
| 5 | Segment row | tap | Navigates | Opens that segment's detail & attempts |
| 6 | Bottom nav | tap | Navigates | Switches section |

### 4.8 Stats · Segment detail (`M3Segment`)

Top bar: `arrow_back` leading, "Segment" title, `more_vert` trailing. No bottom nav.

**Content:**
- **Title** + meta = segment **name** and "**distance · N attempts**". *e.g.* "Ascent, west · 2.84 km · 7 attempts".
- **Personal-best card** (`primaryContainer`):
  - "PERSONAL BEST" + the **best time**. *e.g.* "2:14.8".
  - **Trend** vs the previous PB. *e.g.* "3.2s faster".
  - Three inline stats from the PB run: **max speed**, **max lateral G**, **max lean**. *e.g.* 142 / 0.91 g / 38°.
- **History list** — every attempt, newest first: **date**, **time**, a **relative bar** (length ∝ how this attempt compares to the best), and **delta vs best**; the best attempt is highlighted.

**Interactions:**

| # | Element | Gesture | Kind | Result |
|---|---|---|---|---|
| 1 | Back | tap | Navigates | Returns to the Segment list |
| 2 | Overflow ⋮ | tap | Opens | Menu: rename, set as goal, delete, share |
| 3 | Personal-best card | tap | Navigates | Opens the trace of the attempt that set it |
| 4 | Attempt rows | tap | Navigates | Opens that attempt's ride / trace |

### 4.9 Settings (`M3Settings`)

Top bar: "Settings" title, `help` trailing. Bottom nav active = **Settings**.

**Content (grouped lists, section labels in `primary`):**
- **SAMPLING**:
  - *GPS rate* — capture frequency for GPS fixes (picker). *e.g.* "10 Hz".
  - *IMU rate* — capture frequency for the IMU (picker). *e.g.* "100 Hz".
  - *Auto-pause* — pause recording automatically when stopped (switch).
- **DISPLAY**:
  - *Units* — Metric or Imperial; governs every displayed speed/distance/altitude (picker). *e.g.* "Metric".
  - *Dark theme* — light/dark color scheme (switch).
- **Raw data export card** (`tertiaryContainer`) — entry point to bulk export. Summary = **dataset size**: total trips, total bytes at full capture resolution, and available formats. *e.g.* "142 trips · 2.8 GB at full 100 ms resolution. CSV, GPX or FIT." + "Export all" button.

**Interactions:**

| # | Element | Gesture | Kind | Result |
|---|---|---|---|---|
| 1 | Help | tap | Opens | Help & about |
| 2 | GPS / IMU / Units rows | tap | Opens | Picker (GPS Hz, IMU Hz, units) |
| 3 | Auto-pause switch | tap | Updates in place | Toggles instantly |
| 4 | Dark-theme switch | tap | Updates in place | Switches app theme instantly |
| 5 | Export-all button | tap | Navigates | Opens the Export screen |
| 6 | Bottom nav | tap | Navigates | Switches section |

### 4.10 Settings · Export (`M3Export`)

Top bar: `arrow_back` leading, "Export data" title, `help` trailing. No bottom nav.

**Content:**
- **Summary line** — dataset size at full resolution. *e.g.* "142 trips · 2.8 GB at full 100 ms resolution".
- **FORMAT** — single-select among **CSV / GPX / FIT** (3-up cards); selected card outlined in `primary` with a `check_circle`.
- **SCOPE**:
  - *Trips* — which trips to include (picker). *e.g.* "All 142".
  - *Date range* — time window (picker). *e.g.* "All time".
- **INCLUDE** — per-stream toggles for what goes in the file: **GPS track**, **IMU (accel & gyro)**, **Lean angle**. *e.g.* GPS on / IMU on / Lean off.
- **Export button** — full-width `primary`; label reflects the **resulting count + estimated size** from the current scope/include selection. *e.g.* "Export 142 trips · 2.6 GB".

**Interactions:**

| # | Element | Gesture | Kind | Result |
|---|---|---|---|---|
| 1 | Back | tap | Navigates | Returns to Settings |
| 2 | Help | tap | Opens | Help about the formats |
| 3 | Format CSV/GPX/FIT | tap | Updates in place | Single-select output format |
| 4 | Scope rows | tap | Opens | Pickers (which trips, which dates) |
| 5 | Include switches | tap | Updates in place | Toggle data streams on/off |
| 6 | Export button | tap | Action | Starts export → progress, then share |

## 5. Notes for implementation

- **Decorative vs interactive:** sensor chips and all Live-HUD readouts are deliberately non-interactive — do not add navigation to them.
- **Example values:** every number/name in §4 (142 trips, 2.8 GB, "Col de Turini", segment names, etc.) is an illustrative *e.g.* of the described content, not a fixed string or a hard requirement.
- **Units boundary:** store SI internally (m/s, m, m/s²); convert only at the display boundary per the Units setting (§4.9).
- **Charts & map:** the artifacts draw static SVG; real screens need live charting and a real map provider, including the scrub-playhead interaction (§4.5 #4) that drives the map marker and chart read-outs together.
