// annotations.jsx — interaction "redline" layer over the Speed storyboard.
// Numbered pins are overlaid on each (light-theme) screen; a paired table
// on the right says what each pin's element does when you interact with it.
// `proposed`-style flags have been retired — every interaction below is now
// a confirmed feature.

const ANN_MONO = 'JetBrains Mono, monospace';
const ANN_SANS = '"Roboto Flex", system-ui, sans-serif';

// Five interaction kinds. Toggles are treated as a flavour of "in place".
const ITYPE = {
  nav:     { color: '#2563eb', label: 'Navigates to another screen' },
  inplace: { color: '#0d9488', label: 'Updates this screen in place' },
  opens:   { color: '#7c3aed', label: 'Opens a sheet, picker or menu' },
  action:  { color: '#dc2626', label: 'Runs an action / system task' },
  deco:    { color: '#78716c', label: 'Decorative — not interactive' },
};

// ── pin overlaid on the screen ───────────────────────────────────────────
function Pin({ n, x, y, type }) {
  const c = ITYPE[type].color;
  return (
    <div style={{
      position: 'absolute', left: x, top: y, transform: 'translate(-50%,-50%)',
      width: 22, height: 22, borderRadius: 11, background: c, color: '#fff',
      border: '2px solid #fff', boxShadow: '0 1px 5px rgba(0,0,0,0.45)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontFamily: ANN_MONO, fontSize: 12, fontWeight: 700, zIndex: 30,
    }}>{n}</div>
  );
}

// ── one table row ─────────────────────────────────────────────────────────
function SpecRow({ n, type, el, gesture, result }) {
  const c = ITYPE[type].color;
  return (
    <div style={{
      display: 'flex', gap: 11, alignItems: 'flex-start', padding: '9px 13px',
      borderLeft: `3px solid ${c}`, background: '#fff', borderRadius: '0 10px 10px 0',
      boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
    }}>
      <div style={{
        flex: '0 0 auto', width: 21, height: 21, borderRadius: 11, background: c, color: '#fff',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontFamily: ANN_MONO, fontSize: 11.5, fontWeight: 700, marginTop: 1,
      }}>{n}</div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
          <span style={{ fontFamily: ANN_SANS, fontSize: 13.5, fontWeight: 600, color: '#1c1a17' }}>{el}</span>
          <span style={{ fontFamily: ANN_MONO, fontSize: 9.5, letterSpacing: '0.08em', textTransform: 'uppercase', color: '#a39d93' }}>{gesture}</span>
        </div>
        <div style={{ fontFamily: ANN_SANS, fontSize: 12.5, color: '#5c574f', marginTop: 2, lineHeight: 1.42 }}>{result}</div>
      </div>
    </div>
  );
}

// ── the table panel beside a screen ────────────────────────────────────────
function SpecPanel({ summary, rows }) {
  return (
    <div style={{
      width: 384, height: 708, background: '#f4f2ec', borderLeft: '1px solid #d8d4cb',
      padding: '20px 18px 16px', display: 'flex', flexDirection: 'column',
    }}>
      <div style={{ fontFamily: ANN_MONO, fontSize: 10, letterSpacing: '0.2em', color: '#a39d93', textTransform: 'uppercase' }}>Interactions</div>
      <div style={{ fontFamily: ANN_SANS, fontSize: 13, color: '#5c574f', marginTop: 6, lineHeight: 1.45 }}>{summary}</div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 7, marginTop: 14 }}>
        {rows.map((r) => <SpecRow key={r.n} {...r} />)}
      </div>
      <div style={{ flex: 1 }} />
      <div style={{ fontFamily: ANN_MONO, fontSize: 9.5, letterSpacing: '0.04em', color: '#a39d93', marginTop: 14, display: 'flex', alignItems: 'center', gap: 7 }}>
        <span style={{ width: 7, height: 7, borderRadius: 7, background: '#bcf0b4', border: '1px solid rgba(10,10,10,0.2)' }} />
        Light theme shown · dark behaves identically
      </div>
    </div>
  );
}

// ── screen + pins + panel, as one artboard body ────────────────────────────
function AnnotatedScreen({ Comp, spec, scheme }) {
  return (
    <div style={{ display: 'flex', width: 320 + 384, height: 708, background: '#eceae5' }}>
      <div style={{ position: 'relative', width: 320, height: 708 }}>
        {/* slim caption band so the phone screen aligns with the panel padding */}
        <div style={{ height: 28 }} />
        <div style={{ position: 'relative', width: 320, height: 680 }}>
          <M3Theme scheme={scheme || M3_GREEN}><Comp /></M3Theme>
          {spec.pins.map((p) => <Pin key={p.n} {...p} />)}
        </div>
      </div>
      <SpecPanel summary={spec.summary} rows={spec.rows} />
    </div>
  );
}

// ── "how to read this" legend ──────────────────────────────────────────────
function HowToRead() {
  return (
    <div style={{ width: 360, height: 708, background: '#f7fbf1', color: '#181d17', padding: '30px 26px', fontFamily: ANN_SANS, display: 'flex', flexDirection: 'column', borderRight: '1px solid #c1c9bd' }}>
      <div style={{ fontFamily: ANN_MONO, fontSize: 11, letterSpacing: '0.18em', color: '#3b6939', fontWeight: 500 }}>INTERACTION REVIEW</div>
      <div style={{ fontSize: 30, fontWeight: 700, letterSpacing: '-0.03em', lineHeight: 1, marginTop: 12 }}>What does each<br />element do?</div>
      <div style={{ fontSize: 13, color: '#424940', marginTop: 12, lineHeight: 1.55 }}>
        Every interactive element on every screen is pinned with a number. The table beside each screen says what that pin does when you tap, drag or toggle it — and which elements are deliberately decorative.
      </div>

      <div style={{ height: 1, background: '#c1c9bd', margin: '20px 0 16px' }} />
      <div style={{ fontFamily: ANN_MONO, fontSize: 10, letterSpacing: '0.16em', color: '#3b6939', fontWeight: 500 }}>PIN COLOURS</div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 9, marginTop: 12 }}>
        {Object.entries(ITYPE).map(([k, v]) => (
          <div key={k} style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
            <span style={{ flex: '0 0 auto', width: 20, height: 20, borderRadius: 10, background: v.color, border: '2px solid #fff', boxShadow: '0 1px 4px rgba(0,0,0,0.3)' }} />
            <span style={{ fontSize: 13, color: '#2b302a', fontWeight: 500 }}>{v.label}</span>
          </div>
        ))}
      </div>

      <div style={{ marginTop: 18, background: '#ebefe5', borderRadius: 14, padding: '13px 15px' }}>
        <div style={{ fontSize: 12.5, color: '#424940', lineHeight: 1.5 }}>
          Every interaction shown is a confirmed feature of the app. Decorative (grey) elements are intentionally non-interactive.
        </div>
      </div>

      <div style={{ flex: 1 }} />
      <div style={{ fontSize: 11.5, color: '#72796f', lineHeight: 1.55 }}>
        Double-click any board to open it full-screen. Behaviour is the spec only — the screens themselves are still static mock-ups.
      </div>
    </div>
  );
}

// ── per-screen interaction specs ────────────────────────────────────────────
// pin x/y are in the 320×680 screen coordinate space.
const SPECS = {
  idle: {
    summary: 'Home base before a ride — sensor check, last ride and the Record button.',
    pins: [
      { n: 1, x: 52,  y: 168, type: 'deco' },
      { n: 2, x: 160, y: 276, type: 'nav' },
      { n: 3, x: 85,  y: 403, type: 'nav' },
      { n: 4, x: 235, y: 403, type: 'nav' },
      { n: 5, x: 238, y: 564, type: 'action' },
      { n: 6, x: 40,  y: 644, type: 'nav' },
    ],
    rows: [
      { n: 1, type: 'deco',   el: 'Sensor chips',      gesture: '—',   result: 'GPS / IMU / battery status — glanceable only, not tappable.' },
      { n: 2, type: 'nav',    el: 'Last-ride card',    gesture: 'tap', result: 'Opens the full summary for Col de Turini.' },
      { n: 3, type: 'nav',    el: '“4 rides this week”', gesture: 'tap', result: 'Opens Trips, pre-filtered to this week.' },
      { n: 4, type: 'nav',    el: '“8 412 km total”',  gesture: 'tap', result: 'Opens Statistics (lifetime totals).' },
      { n: 5, type: 'action', el: 'Record FAB',        gesture: 'tap', result: 'Starts a new recording → Live HUD.' },
      { n: 6, type: 'nav',    el: 'Bottom nav',        gesture: 'tap', result: 'Switches Ride · Trips · Stats · Settings.' },
    ],
  },

  live: {
    summary: 'The during-ride HUD. Deliberately glanceable — only Stop is interactive.',
    pins: [
      { n: 1, x: 160, y: 56,  type: 'deco' },
      { n: 2, x: 160, y: 170, type: 'deco' },
      { n: 3, x: 85,  y: 401, type: 'deco' },
      { n: 4, x: 160, y: 628, type: 'action' },
    ],
    rows: [
      { n: 1, type: 'deco',    el: 'Recording banner', gesture: '—',   result: 'Elapsed time, rate & point count. Live status, not tappable.' },
      { n: 2, type: 'deco',    el: 'Speed hero',       gesture: '—',   result: 'Live speed + progress toward top speed. Not tappable.' },
      { n: 3, type: 'deco',    el: 'Metric tiles ×4',  gesture: '—', result: 'Live G / lean / accel / altitude — glanceable readouts, not tappable.' },
      { n: 4, type: 'action',  el: 'Stop button',      gesture: 'tap', result: 'Ends & saves the ride → Summary.' },
    ],
  },

  summary: {
    summary: 'Post-ride payoff screen. Mostly read-only, with two ways deeper.',
    pins: [
      { n: 1, x: 28,  y: 54,  type: 'nav' },
      { n: 2, x: 294, y: 54,  type: 'opens' },
      { n: 3, x: 160, y: 200, type: 'nav' },
      { n: 4, x: 85,  y: 315, type: 'deco' },
      { n: 5, x: 160, y: 520, type: 'nav' },
    ],
    rows: [
      { n: 1, type: 'nav',   el: 'Back',                gesture: 'tap', result: 'Returns to the previous screen.' },
      { n: 2, type: 'opens', el: 'Share',               gesture: 'tap', result: 'Opens the share sheet (image / GPX / link).' },
      { n: 3, type: 'nav',   el: 'Top-speed hero',      gesture: 'tap', result: 'Jumps to the 187 km/h moment on the Trace.' },
      { n: 4, type: 'deco',  el: 'Stat cards ×6',       gesture: '—',   result: 'Read-only ride metrics. Not tappable.' },
      { n: 5, type: 'nav',   el: '“3 personal bests”',  gesture: 'tap', result: 'Opens this ride’s segments → Segment list.' },
    ],
  },

  history: {
    summary: 'Your trip log — search, filter and open any ride.',
    pins: [
      { n: 1, x: 160, y: 108, type: 'opens' },
      { n: 2, x: 60,  y: 160, type: 'inplace' },
      { n: 3, x: 38,  y: 226, type: 'nav' },
      { n: 4, x: 120, y: 644, type: 'nav' },
    ],
    rows: [
      { n: 1, type: 'opens',   el: 'Search bar',    gesture: 'tap',        result: 'Opens search across all 142 trips.' },
      { n: 2, type: 'inplace', el: 'Filter chips',  gesture: 'tap',        result: 'Filters the list in place (All / This week).' },
      { n: 3, type: 'nav',     el: 'Trip row',      gesture: 'tap',        result: 'Opens that ride’s Summary.' },
      { n: 4, type: 'nav',     el: 'Bottom nav',    gesture: 'tap',        result: 'Switches section.' },
    ],
  },

  detail: {
    summary: 'One ride’s trace — map plus scrubbable charts.',
    pins: [
      { n: 1, x: 28,  y: 54,  type: 'nav' },
      { n: 2, x: 294, y: 54,  type: 'action' },
      { n: 3, x: 160, y: 194, type: 'opens' },
      { n: 4, x: 160, y: 366, type: 'inplace' },
    ],
    rows: [
      { n: 1, type: 'nav',     el: 'Back',              gesture: 'tap',  result: 'Returns to Trips.' },
      { n: 2, type: 'action',  el: 'Download',          gesture: 'tap',  result: 'Exports just this ride (CSV / GPX / FIT).' },
      { n: 3, type: 'opens',   el: 'Map card',          gesture: 'tap',  result: 'Opens the full-screen interactive map.' },
      { n: 4, type: 'inplace', el: 'Speed & G charts ×2', gesture: 'drag', result: 'Scrub a playhead; map + values follow your finger.' },
    ],
  },

  stats: {
    summary: 'Trends and records — everything re-scopes by range.',
    pins: [
      { n: 1, x: 294, y: 54,  type: 'opens' },
      { n: 2, x: 60,  y: 102, type: 'inplace' },
      { n: 3, x: 160, y: 262, type: 'nav' },
      { n: 4, x: 85,  y: 362, type: 'nav' },
      { n: 5, x: 160, y: 522, type: 'nav' },
      { n: 6, x: 200, y: 644, type: 'nav' },
    ],
    rows: [
      { n: 1, type: 'opens',   el: 'Date range',          gesture: 'tap', result: 'Opens a custom date-range picker.' },
      { n: 2, type: 'inplace', el: 'Range chips',         gesture: 'tap', result: 'Re-scopes all stats in place (year / 90 d / all).' },
      { n: 3, type: 'nav',     el: 'Month bars',          gesture: 'tap', result: 'Tap a bar → that month’s trips.' },
      { n: 4, type: 'nav',     el: 'Record cards ×4',     gesture: 'tap', result: 'Opens the ride that holds each record.' },
      { n: 5, type: 'nav',     el: '“14 tracked segments”', gesture: 'tap', result: 'Opens the Segment list.' },
      { n: 6, type: 'nav',     el: 'Bottom nav',          gesture: 'tap', result: 'Switches section.' },
    ],
  },

  segmentlist: {
    summary: 'All tracked segments — sortable and searchable.',
    pins: [
      { n: 1, x: 28,  y: 54,  type: 'nav' },
      { n: 2, x: 294, y: 54,  type: 'opens' },
      { n: 3, x: 160, y: 108, type: 'opens' },
      { n: 4, x: 60,  y: 160, type: 'inplace' },
      { n: 5, x: 38,  y: 244, type: 'nav' },
      { n: 6, x: 200, y: 644, type: 'nav' },
    ],
    rows: [
      { n: 1, type: 'nav',     el: 'Back',         gesture: 'tap', result: 'Returns to Statistics.' },
      { n: 2, type: 'opens',   el: 'Add',          gesture: 'tap', result: 'New segment — draw on the map or pick from a ride.' },
      { n: 3, type: 'opens',   el: 'Search bar',   gesture: 'tap', result: 'Searches your 14 segments.' },
      { n: 4, type: 'inplace', el: 'Sort chips',   gesture: 'tap', result: 'Re-sorts the list in place (time / runs / nearby).' },
      { n: 5, type: 'nav',     el: 'Segment row',  gesture: 'tap', result: 'Opens that segment’s detail & attempts.' },
      { n: 6, type: 'nav',     el: 'Bottom nav',   gesture: 'tap', result: 'Switches section.' },
    ],
  },

  segment: {
    summary: 'One segment’s record and its attempt history.',
    pins: [
      { n: 1, x: 28,  y: 54,  type: 'nav' },
      { n: 2, x: 294, y: 54,  type: 'opens' },
      { n: 3, x: 160, y: 212, type: 'nav' },
      { n: 4, x: 90,  y: 366, type: 'nav' },
    ],
    rows: [
      { n: 1, type: 'nav',   el: 'Back',                gesture: 'tap', result: 'Returns to the Segment list.' },
      { n: 2, type: 'opens', el: 'Overflow ⋮',          gesture: 'tap', result: 'Menu: rename, set as goal, delete, share.' },
      { n: 3, type: 'nav',   el: 'Personal-best card',  gesture: 'tap', result: 'Opens the trace of the attempt that set it.' },
      { n: 4, type: 'nav',   el: 'Attempt rows',        gesture: 'tap', result: 'Opens that attempt’s ride / trace.' },
    ],
  },

  settings: {
    summary: 'Capture & display preferences, plus the export entry point.',
    pins: [
      { n: 1, x: 294, y: 54,  type: 'opens' },
      { n: 2, x: 160, y: 130, type: 'opens' },
      { n: 3, x: 272, y: 230, type: 'inplace' },
      { n: 4, x: 272, y: 369, type: 'inplace' },
      { n: 5, x: 160, y: 528, type: 'nav' },
      { n: 6, x: 280, y: 644, type: 'nav' },
    ],
    rows: [
      { n: 1, type: 'opens',   el: 'Help',              gesture: 'tap', result: 'Opens help & about.' },
      { n: 2, type: 'opens',   el: 'Rate & Units rows', gesture: 'tap', result: 'Open a picker (GPS Hz, IMU Hz, units).' },
      { n: 3, type: 'inplace', el: 'Auto-pause switch', gesture: 'tap', result: 'Toggles instantly, no navigation.' },
      { n: 4, type: 'inplace', el: 'Dark-theme switch', gesture: 'tap', result: 'Switches the app theme instantly.' },
      { n: 5, type: 'nav',     el: 'Export-all button', gesture: 'tap', result: 'Opens the Export sheet.' },
      { n: 6, type: 'nav',     el: 'Bottom nav',        gesture: 'tap', result: 'Switches section.' },
    ],
  },

  export: {
    summary: 'The new raw-data export flow — format, scope and contents.',
    pins: [
      { n: 1, x: 28,  y: 54,  type: 'nav' },
      { n: 2, x: 294, y: 54,  type: 'opens' },
      { n: 3, x: 160, y: 180, type: 'inplace' },
      { n: 4, x: 160, y: 300, type: 'opens' },
      { n: 5, x: 272, y: 440, type: 'inplace' },
      { n: 6, x: 160, y: 571, type: 'action' },
    ],
    rows: [
      { n: 1, type: 'nav',     el: 'Back',              gesture: 'tap', result: 'Returns to Settings.' },
      { n: 2, type: 'opens',   el: 'Help',              gesture: 'tap', result: 'Opens help about the formats.' },
      { n: 3, type: 'inplace', el: 'Format CSV/GPX/FIT', gesture: 'tap', result: 'Single-select; pick one output format.' },
      { n: 4, type: 'opens',   el: 'Scope rows',        gesture: 'tap', result: 'Open pickers (which trips, which dates).' },
      { n: 5, type: 'inplace', el: 'Include switches',  gesture: 'tap', result: 'Toggle data streams on / off in place.' },
      { n: 6, type: 'action',  el: 'Export button',     gesture: 'tap', result: 'Starts the export → progress, then share.' },
    ],
  },
};

Object.assign(window, { ITYPE, Pin, SpecRow, SpecPanel, AnnotatedScreen, HowToRead, SPECS });
