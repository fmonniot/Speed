# Speed — Component Reference

> **Purpose.** A compact distillation of the React mock-ups in `design-artifacts/` so
> implementation sessions don't have to read the large `.jsx` files. It captures the concrete
> layout values (paddings, sizes, gaps, icon sizes) and the `WavyLine` algorithm that
> `design-spec.md` leaves implicit.
>
> **Authority.** `design-spec.md` wins on anything it states. This file fills the gaps the spec
> leaves to "visual reference" and translates the artifact's CSS-pixel values into the 320 dp
> reference frame (so **px → dp** for layout, **px → sp** for text; see spec §2.0). Numbers here
> are *design intent*, not pixel-exact requirements. Colors are M3 `ColorScheme` roles — never
> hard-code hex.
>
> Once a component is ported to Kotlin (`ui/components/`, `ui/theme/`), that Kotlin file becomes
> the reference and this doc is no longer needed for it.

## Role-name mapping (artifact → Compose)

The artifacts use shorthand for two roles; map them to the standard Compose `ColorScheme` names:

| Artifact token | Compose `ColorScheme` role |
|---|---|
| `onSurfaceVar` | `onSurfaceVariant` |
| `outlineVar` | `outlineVariant` |

All other tokens (`primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `secondary`,
`secondaryContainer`, `onSecondaryContainer`, `tertiary`, `tertiaryContainer`, `onTertiaryContainer`,
`error`, `errorContainer`, `onErrorContainer`, `surface`, `surfaceDim`, `surfaceContainerLow`,
`surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest`, `onSurface`, `outline`) map
1:1. Note the artifacts omit `onSecondary`/`onTertiary`/`onError` and `surfaceBright`/`surfaceContainerLowest`
— pick reasonable M3 values for those if a component needs them.

> The white text on the `error` Stop button and `tertiary` "Export all" button is `#fff` in the
> artifact — in Compose use `onError` / `onTertiary` respectively (both white-ish in this scheme).

## Icon conventions (Material Symbols Rounded → Compose)

- Fill axis: `fill 1` = active/selected/emphasis, `fill 0` = inactive. In Compose, swap between the
  filled and outlined variants of an `Icons.Rounded.*` glyph (or use a font with the FILL axis).
- Common sizes: nav 22, top-bar leading 24 / trailing 26, list-row leading-chip glyph 22,
  sensor-chip glyph 18, metric-card glyph 18, trend arrows 12–16, chevron 20–24.
- Default icon tint is `onSurfaceVariant`; accent glyphs use `primary` or the card's `on*` role.

---

# Shared primitives

## Status bar (`M3Status`) — 28 dp
OS status bar; in-app it's the system bar, not drawn. Reference values: height 28, horizontal
padding 14, time left + (5G · signal · battery) right, tinted `onSurface` at ~0.75 opacity, 11 sp.
In Compose this is handled by `WindowInsets`/edge-to-edge — don't draw it.

## Top app bar (`M3TopBar`) — 56 dp
- Height **56**, sits at top offset 28 (below status bar). Padding: left **12**, right **8**.
- Layout: `SpaceBetween`. Leading group (optional 40×40 round icon button, glyph 24, `onSurface`) +
  title; trailing 40×40 round icon button.
- Title: **22 sp / weight 400 / `onSurface`**. Gap between leading icon and title 6 (or 14 if no leading icon).
- Trailing action icon: size **26**, `fill 1`, color **`primary`**. Optional (Ride/Home has none).
- Background **`surface`**.

## Bottom navigation (`M3Nav`) — 72 dp
- Height **72**, background **`surfaceContainer`**, items spaced `SpaceAround`, top padding **12**.
- Four destinations, each a column (icon pill + label), width 64, vertical gap 4:
  | key (route) | label | icon |
  |---|---|---|
  | `ride` | Ride | `home` |
  | `trips` | Trips | `route` |
  | `stats` | Stats | `leaderboard` |
  | `settings` | Settings | `settings` |
- **Active pill**: 56×32, radius **16**, background `secondaryContainer`; glyph 22 `fill 1`
  `onSecondaryContainer`. Inactive: transparent pill, glyph `fill 0` `onSurfaceVariant`.
- Label **11 sp**, weight 600 active / 500 inactive; color `onSurface` active / `onSurfaceVariant`
  inactive; letter-spacing ~0.02em.

## WavyLine — M3 Expressive wavy progress
Used in the Live HUD speed hero (and optionally the segment PB card) to show a fraction (current
speed ÷ session top speed).

**Algorithm** (from the artifact; reproduce in a Compose `Canvas`):
- Inputs: `progress` (0..1), `color` (default `primary`), `track` (default `primaryContainer`).
- Canvas height 16 dp; the wave centerline is at y = 8.
- Wave: `y(x) = 8 + amp * sin((x / period) * π)` with **amplitude = 3**, **period = 14** (so the
  sine argument advances π every 14 dp ⇒ wavelength 28 dp). Sample x across the full width.
- Draw order: (1) full-width wave stroke in `track`, **stroke width 3**, round caps;
  (2) the same wave path clipped to `x ∈ [0, progress·width]` stroked in `color`, width 3;
  (3) a filled **circle radius 4** in `color` at `(progress·width, 8)` — the leading dot.
- Stretches to the container width (`preserveAspectRatio none` in the artifact ⇒ fill available width).

## Selectable chip (filter / sort / range)
- Height **32**, radius **8**, horizontal padding 12, inner gap 6.
- **Selected**: background `secondaryContainer`, no border, leading `check` glyph 16
  `onSecondaryContainer`, label `onSecondaryContainer`.
- **Unselected**: transparent background, 1 dp `outline` border, no check, label `onSurfaceVariant`.
- Label **13 sp / weight 500**. Single-select within a group.

## Switch (`SpeedSwitch`) — 52×32
- Track 52×32, radius 16.
- **On**: track `primary` (no border); thumb 24×24 radius 12 `onPrimary`, offset top 4 / left 24.
- **Off**: track `surfaceContainerHighest` with 2 dp `outline` border; thumb 16×16 `outline`,
  offset top 6 / left 6.
- (M3's built-in `Switch` is close; match these proportions or build a custom one.)

## Tonal stat card (`TonalStatCard`)
- Radius **18–20**, padding ~14×16. Background = a container role, text = its matching `on*` role.
- Optional top row: small label (12 sp, `on*` at ~0.8 opacity) on the left + icon (18, top-right).
- Value: big numeral, **20–30 sp / weight 600**, line-height 1, letter-spacing −0.02em, with an
  optional small unit suffix (12 sp, ~0.7 opacity) baseline-aligned.
- Caption under the value: 12 sp `on*` ~0.8 opacity.

## List row (`ListRow`)
- Padding ~12 vertical / 4 horizontal, inner gap 12–14. Optional 1 dp `outlineVariant` bottom divider.
- Leading **44×44 round chip** (radius 22), a container color; glyph 22 in the matching `on*`.
- Middle: title **15–16 sp / weight 500 / `onSurface`** (ellipsize), meta **12–13 sp / `onSurfaceVariant`**.
- Trailing: either a value (right-aligned, primary 16–18 sp/600 + small unit caption) or a
  `chevron_right` glyph 20–24 `onSurfaceVariant`, or both.

## Section label (`SectionLabel`)
- **13 sp / weight 600 / `primary`**, letter-spacing ~0.04em, left padding 4, often uppercase.
  Used to head grouped-list sections (Settings/Export). (Some hero labels use `on*Container` at
  ~0.8 opacity, 12 sp, letter-spacing 0.06–0.08em — see hero cards below.)

## Grouped-list container
- Background **`surfaceContainerLow`**, radius **20**. Rows inside separated by 1 dp `outlineVariant`
  dividers (no divider after the last row). Row: leading glyph 22 `onSurfaceVariant`, label 15 sp/500
  `onSurface` (flex), trailing value 14 sp `onSurfaceVariant` **or** a `SpeedSwitch`.

## Buttons & FAB
- **Extended FAB**: height **56**, radius **18**, background `primary`, padding left 18 / right 22,
  icon+label gap 10, subtle shadow. Icon 24 `fill 1` `onPrimary`; label 16 sp/600 `onPrimary`.
  (Ride/Home: `fiber_manual_record` + "Record", anchored bottom-right, ~16 from edges, ~88 above bottom.)
- **Full-width filled button**: height **56**, radius **28**, centered icon+label gap 10.
  Stop = `error` bg (icon `stop`, white text); Export = `primary` bg (icon `download`, `onPrimary`).
- **Inline tonal button** (e.g. "Export all" in the Settings card): height 48, radius 24, `tertiary`
  bg, centered icon 20 + label 15 sp/600 in white/`onTertiary`.

## Screen content frame
- Reference canvas 320×680. Content area is inset: **top 84** (28 status + 56 top bar), **bottom 72**
  when the nav bar is present (else 0), **horizontal 16**. Full-bleed screens (Live HUD) skip the top
  bar and place content from the status bar down.

---

# Per-screen layout notes

Each screen's content + interactions are specified in `design-spec.md` §4. Below are only the extra
layout specifics (spacing, hero sizes, grids) distilled from the artifact. Spec section in brackets.

## Ride · Home (`M3Idle`) [§4.1]
- Top bar "Speed", no trailing icon. Nav active = Ride.
- Heading "Ready to ride" **28 sp/500**, letter-spacing −0.01em; subtext 14 sp `onSurfaceVariant`, top margin 4.
- Sensor chips row (margin-top 16, gap 8): each a **bordered** chip (height 32, radius 8, 1 dp
  `outlineVariant`, `surface` bg, padding left 8 / right 12, gap 6) — glyph 18 `primary` `fill 1` +
  13 sp `onSurfaceVariant`/500 text. *Decorative.* (GPS+sat, IMU, battery%.)
- **Last-ride card**: margin-top 18, `primaryContainer`, radius **28**, padding ~20. Header row:
  label "LAST RIDE · {date}" 12 sp/600 `onPrimaryContainer` (letter-spacing 0.08em, ~0.8 opacity) +
  `two_wheeler` glyph 22. Title 24 sp/500 (margin-top 6). Three inline stats (gap 24, margin-top 16):
  value **30 sp/600** line-height 1 + caption 12 sp ~0.75 opacity. (top km/h · max g · km.)
- **Two stat cards** grid 1fr/1fr, gap 12, margin-top 12: "This week" (`secondaryContainer`),
  "Total" (`tertiaryContainer`); radius 20, padding 14×16; top icon 22, value 20 sp/600 (margin-top 10),
  caption 12 sp.
- Extended FAB "Record" bottom-right.

## Ride · Live HUD (`M3Live`) [§4.2]
- **No top bar, no nav.** Recording banner: top offset 28, margin 8×16, height **40**, radius **12**,
  `errorContainer`; left = `fiber_manual_record` 16 `error` + "Recording · {elapsed}" 13 sp/600
  `onErrorContainer`; right = "{Hz} · {pts}" 12 sp ~0.8 opacity.
- **Speed hero**: top 84, `primaryContainer`, radius **28**, padding ~18×22. Label "CURRENT SPEED"
  13 sp/600 (letter-spacing 0.06em, ~0.8 opacity). Numeral **104 sp / weight 700**, line-height 0.9,
  letter-spacing −0.04em, with unit "km/h" 18 sp/500 baseline-aligned. `WavyLine` (margin-top 8).
  Footer row `SpaceBetween` (margin-top 4): "avg {n}" / "top {n}" 12 sp ~0.7 opacity.
- **4 metric tiles**: grid 1fr/1fr gap 12 (artifact positions at top 360). Each radius 20, padding
  14×16, with a top row (label 13 sp/500 ~0.85 opacity + icon 18) and a value **30 sp/600** + unit 12 sp:
  - G-force → `tertiaryContainer`, value "0.84", unit "g lat", icon `speed`.
  - Lean → `secondaryContainer`, value "38°", unit "right", icon `motorcycle`.
  - Accel → `surfaceContainerHigh`/`onSurface`, value "+0.42", unit "g", icon `trending_up`.
  - Altitude → `surfaceContainerHigh`/`onSurface`, value "1247", unit "m", icon `terrain`.
  *All decorative.*
- **Stop button**: full-width filled `error`, bottom 24. Only interactive element.

## Ride · Summary (`M3Summary`) [§4.3]
- Top bar: `arrow_back` / "Trip summary" / `share`. No nav.
- Title 24 sp/500; meta 13 sp `onSurfaceVariant` (date · times · duration).
- **Top-speed hero**: `primary`, radius 28, padding ~20. Header: "TOP SPEED" 12 sp/600 `onPrimary`
  (0.85 opacity, letter-spacing 0.08em) + conditional **NEW PB badge** (pill: translucent white bg
  `rgba(255,255,255,0.2)`, radius 8, padding 3×8, `trophy` glyph 14 + "NEW PB" 11 sp/600 `onPrimary`).
  Numeral **80 sp/700** line-height 0.9 letter-spacing −0.04em + "km/h" 16 sp 0.85 opacity.
- **6-stat grid** 1fr/1fr gap 12: each `radius 18`, padding 12×14, label 12 sp 0.8 opacity + value
  **22 sp/600** (margin-top 4). Order: Max G lateral (`tertiaryContainer`), Max lean (`secondaryContainer`),
  Distance, Avg speed, Hard brake, Moving (last four `surfaceContainerHigh`/`onSurface`). *Decorative.*
- **Segment link row**: height 56, radius 16, 1 dp `outlineVariant` border, padding 0×18; label
  15 sp/500 + `chevron_right` 24.

## Trips · Trace (`M3Detail`) [§4.5]
- Top bar: `arrow_back` / "Trace" / `download`. No nav.
- **Map card**: radius **24**, height ~220, `surfaceContainerHigh`. Polyline start dot (`primary`,
  r7 with white r3 center), end dot (`error`, r7); floating top-left chip (`surface` bg, radius 8,
  padding 6×10) with `speed` glyph 16 `primary` + value 12 sp/600. *(Placeholder until MapLibre lands.)*
- **Speed chart card** (margin-top 12, `surfaceContainerLow`, radius **20**, padding 14×16): header
  row = "Speed" 14 sp/600 + range "0 – 187 km/h" 12 sp `onSurfaceVariant`. Chart: filled area in
  `primaryContainer` + line in `primary` stroke 2.5, ~64 dp tall.
- **G-lateral chart card** (same styling): header "G lateral" + "−1.18 / +1.24"; **zero baseline
  line** in `outlineVariant`; line in `tertiary` stroke 2.5, ~52 dp tall.
- Charts need a **drag scrub playhead** (not in the static artifact) — see spec §4.5 #4.

## Trips · List (`M3History`) [§4.4]
- Top bar: "Trips" / `search`. Nav active = Trips.
- **Search bar (pill)**: height **48**, radius **24**, `surfaceContainerHigh`, padding 0×16, gap 10;
  `search` glyph 22 `onSurfaceVariant` + placeholder "Search {N} trips" 15 sp `onSurfaceVariant`.
- Filter chips (margin-top 12, gap 8): All / This week (selectable-chip spec above).
- Trip rows: ListRow spec — 44 dp `tertiaryContainer` chip with `two_wheeler` `onTertiaryContainer`;
  name 16 sp/500 + "{date} · {dist}" 13 sp meta; trailing top-speed value 18 sp/600 + "km/h" 11 sp caption.

## Stats · Overview (`M3Stats`) [§4.6]
- Top bar: "Statistics" / `date_range`. Nav active = Stats.
- Range chips (gap 8): This year / 90 days / All time (selectable-chip spec).
- **Distance hero**: `primaryContainer`, radius 28, padding ~20 (margin-top 12). Header: "DISTANCE ·
  {scope}" 12 sp/600 (letter-spacing 0.08em, 0.8 opacity) + trend pill (`trending_up` 16 + "+18%"
  13 sp/600). Numeral **56 sp/700** line-height 0.9 letter-spacing −0.03em + "km" 16 sp.
  **6-month bar chart** (margin-top 18, height 64, gap 10): per month a column = bar (radius 6, height
  ∝ value, max bar ≈ 48 dp; current month `primary`, others `rgba(0,0,0,0.12)`) + month label 10 sp
  (current 700/0.95 opacity, others 500/0.6).
- **Records grid** 1fr/1fr gap 12: Top speed (`secondaryContainer`), Max lean (`tertiaryContainer`),
  Max g lat + Longest (`surfaceContainerHigh`). Card: top label 12 sp + icon 18; value **26 sp/600**
  + optional unit 12 sp.
- **Segments link row**: height 56, radius 16, 1 dp `outlineVariant`; left = `timer` 20 `primary` +
  "{N} tracked segments" 15 sp/500; right `chevron_right` 24.

## Stats · Segment list (`M3SegmentList`) [§4.7]
- Top bar: `arrow_back` / "Segments" / `add`. Nav active = Stats.
- Search bar (pill, as above, glyph 20) + sort chips (Best time / Most runs / Nearby) + count label
  "{N} SEGMENTS" 12 sp `onSurfaceVariant` (margin-top 14, left padding 2).
- Rows (gap 2, 1 dp `outlineVariant` divider between): leading **44 dp timer chip** — `primaryContainer`
  if starred/goal (glyph `onPrimaryContainer`), else `surfaceContainerHigh` (glyph `onSurfaceVariant`),
  `timer` glyph 22 `fill 1`. Middle: name 15 sp/500 (ellipsize) + "{dist} · {N} runs" 12 sp meta.
  Trailing column (right-aligned, gap 4): best time 16 sp/600 (`primary` if starred else `onSurface`) +
  trend row (glyph 12 + "{Δ}s" 11 sp). Then `chevron_right` 20.
- **Trend logic**: Δ<0 → faster → `arrow_downward` + `primary`; Δ=0 → unchanged → `remove` + "—" +
  `onSurfaceVariant`; Δ>0 → slower → `arrow_upward` + `error`.

## Stats · Segment detail (`M3Segment`) [§4.8]
- Top bar: `arrow_back` / "Segment" / `more_vert`. No nav.
- Title 24 sp/500 + meta "{dist} · {N} attempts" 13 sp.
- **PB card**: `primaryContainer`, radius 28, padding ~20. Header: "PERSONAL BEST" 12 sp/600
  (letter-spacing 0.06em, 0.8 opacity) + trend (`arrow_downward` 16 `primary` + "{Δ}s faster" 13 sp/600).
  Time numeral **64 sp/700** line-height 0.9 letter-spacing −0.03em (margin-top 4). Three inline stats
  (gap 22, margin-top 14): value 20 sp/600 + caption 11 sp 0.75 opacity (v.max · g lat · lean).
- **History list** under a "HISTORY" label (13 sp/600 `onSurfaceVariant`, letter-spacing 0.04em). Each
  attempt = grid `[60, 64, 1fr, 46]` columns, column-gap 10, padding 10 vertical, 1 dp `outlineVariant`
  divider: date 13 sp `onSurfaceVariant` · time 16 sp/600 (`primary` if best) · **relative bar**
  (track height 8 radius 4 `surfaceContainerHigh`; fill width = pct%, `primary` if best else `outline`) ·
  delta 13 sp right-aligned (`primary` if best). Best attempt highlighted.

## Settings (`M3Settings`) [§4.9]
- Top bar: "Settings" / `help`. Nav active = Settings.
- **SAMPLING** section (label + grouped-list container): rows GPS rate (`gps_fixed`, value "10 Hz"),
  IMU rate (`sensors`, "100 Hz"), Auto-pause (`pause_circle`, switch). 
- **DISPLAY** section (margin-top 18): Units (`straighten`, "Metric"), Dark theme (`dark_mode`, switch).
- **Raw data export card**: `tertiaryContainer`, radius **24**, padding ~18. Header: `database` glyph 24
  + "Raw data export" 16 sp/600. Summary line 13 sp ~0.85 opacity (trips · size · formats). Then the
  inline tonal "Export all" button (height 48, radius 24, `tertiary`, `download` 20 + label 15 sp/600).

## Settings · Export (`M3Export`) [§4.10]
- Top bar: `arrow_back` / "Export data" / `help`. No nav.
- Summary line 13 sp `onSurfaceVariant` (trips · size · resolution).
- **FORMAT** section: 3-up grid (gap 10) of cards CSV (`description`) / GPX (`map`) / FIT (`memory`).
  Card: radius **18**, padding 14×12, column-centered (icon 24 + label 14 sp/600, gap 8). **Selected**:
  `primaryContainer` bg, 2 dp `primary` border, `check_circle` 16 `primary` at top-right, icon `fill 1`.
  Unselected: `surfaceContainerLow` bg, 1 dp `outlineVariant` border. Single-select.
- **SCOPE** section: grouped-list with rows Trips (`route`, "All {N}") and Date range
  (`calendar_month`, "All time"), each with trailing value + `chevron_right` 20 (opens a picker).
- **INCLUDE** section: grouped-list of switch rows GPS track (`satellite_alt`), IMU · accel & gyro
  (`sensors`), Lean angle (`motorcycle`).
- **Export button**: full-width filled `primary`, radius 28, `download` 22 + label "Export {N} trips ·
  {size}" 16 sp/600 `onPrimary` (label reflects live scope/include selection).
