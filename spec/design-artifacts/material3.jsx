// material3.jsx — MATERIAL 3 (Expressive) variation.
// Genuine M3 token system: tonal surfaces, green seed, big rounded shapes,
// extended FAB, nav bar w/ active pill, Material Symbols icons, wavy progress.

const M3_FONT = '"Roboto Flex", system-ui, sans-serif';
const ERR_LIGHT = { error: '#ba1a1a', errorContainer: '#ffdad6', onErrorContainer: '#410002' };
const ERR_DARK = { error: '#ffb4ab', errorContainer: '#93000a', onErrorContainer: '#ffdad6' };

const M3_GREEN = {
  name: 'Green', dark: false,
  primary: '#3b6939', onPrimary: '#ffffff', primaryContainer: '#bcf0b4', onPrimaryContainer: '#002106',
  secondary: '#52634f', secondaryContainer: '#d5e8cf', onSecondaryContainer: '#101f10',
  tertiary: '#38656a', tertiaryContainer: '#bcebf0', onTertiaryContainer: '#002023',
  ...ERR_LIGHT,
  surface: '#f7fbf1', surfaceDim: '#d7dbd2', surfaceContainerLow: '#f1f5eb', surfaceContainer: '#ebefe5',
  surfaceContainerHigh: '#e5e9df', surfaceContainerHighest: '#e0e4da',
  onSurface: '#181d17', onSurfaceVar: '#424940', outline: '#72796f', outlineVar: '#c1c9bd',
  font: M3_FONT,
};

const M3_GREEN_DARK = {
  name: 'Green · dark', dark: true,
  primary: '#a1d39a', onPrimary: '#0a390f', primaryContainer: '#235024', onPrimaryContainer: '#bcf0b4',
  secondary: '#b9ccb4', secondaryContainer: '#3a4b38', onSecondaryContainer: '#d5e8cf',
  tertiary: '#a0cfd4', tertiaryContainer: '#1e4d52', onTertiaryContainer: '#bcebf0',
  ...ERR_DARK,
  surface: '#10140f', surfaceDim: '#10140f', surfaceContainerLow: '#181d17', surfaceContainer: '#1c211b',
  surfaceContainerHigh: '#272b25', surfaceContainerHighest: '#313630',
  onSurface: '#e0e4da', onSurfaceVar: '#c1c9bd', outline: '#8b9286', outlineVar: '#424940',
  font: M3_FONT,
};

const M3_BLUE = {
  name: 'Blue', dark: false,
  primary: '#415f91', onPrimary: '#ffffff', primaryContainer: '#d7e3ff', onPrimaryContainer: '#001b3e',
  secondary: '#565f71', secondaryContainer: '#dae2f9', onSecondaryContainer: '#131c2b',
  tertiary: '#705574', tertiaryContainer: '#fad7fd', onTertiaryContainer: '#28132e',
  ...ERR_LIGHT,
  surface: '#f9f9ff', surfaceDim: '#d8d9e0', surfaceContainerLow: '#f3f3fa', surfaceContainer: '#ededf4',
  surfaceContainerHigh: '#e7e8ee', surfaceContainerHighest: '#e2e2e9',
  onSurface: '#191c20', onSurfaceVar: '#44474e', outline: '#74777f', outlineVar: '#c4c6d0',
  font: M3_FONT,
};

const M3_ORANGE = {
  name: 'Spiced', dark: false,
  primary: '#8f4c38', onPrimary: '#ffffff', primaryContainer: '#ffdbd1', onPrimaryContainer: '#3a0b01',
  secondary: '#77574e', secondaryContainer: '#ffdbd1', onSecondaryContainer: '#2c150f',
  tertiary: '#6c5d2f', tertiaryContainer: '#f6e1a6', onTertiaryContainer: '#231b00',
  ...ERR_LIGHT,
  surface: '#fff8f6', surfaceDim: '#e8d6d1', surfaceContainerLow: '#fff1ed', surfaceContainer: '#fceae6',
  surfaceContainerHigh: '#f6e4e0', surfaceContainerHighest: '#f0deda',
  onSurface: '#231917', onSurfaceVar: '#53433f', outline: '#85736e', outlineVar: '#d8c2bc',
  font: M3_FONT,
};

const M3_VIOLET = {
  name: 'Violet', dark: false,
  primary: '#65558f', onPrimary: '#ffffff', primaryContainer: '#e9ddff', onPrimaryContainer: '#21005d',
  secondary: '#625b71', secondaryContainer: '#e8def8', onSecondaryContainer: '#1e192b',
  tertiary: '#7e5260', tertiaryContainer: '#ffd9e3', onTertiaryContainer: '#31101d',
  ...ERR_LIGHT,
  surface: '#fef7ff', surfaceDim: '#ded8e0', surfaceContainerLow: '#f7f2fa', surfaceContainer: '#f3edf7',
  surfaceContainerHigh: '#ece6f0', surfaceContainerHighest: '#e6e0e9',
  onSurface: '#1d1b20', onSurfaceVar: '#49454f', outline: '#79747e', outlineVar: '#cac4d0',
  font: M3_FONT,
};

// default scheme used by any default-param fallbacks
const M3 = M3_GREEN;

const M3Ctx = React.createContext(M3_GREEN);
const useM3 = () => React.useContext(M3Ctx);
function M3Theme({ scheme = M3_GREEN, children }) {
  return <M3Ctx.Provider value={scheme}>{children}</M3Ctx.Provider>;
}

function MIcon({ name, size = 24, fill = 0, weight = 400, color, style }) {
  return (
    <span className="material-symbols-rounded" style={{
      fontSize: size, color, lineHeight: 1,
      fontVariationSettings: `'FILL' ${fill}, 'wght' ${weight}, 'opsz' ${size}`,
      ...style,
    }}>{name}</span>
  );
}

function M3Status() {
  const M3 = useM3();
  return <StatusBar time="14:32" color={M3.onSurface} opacity={0.75} font={M3.font} />;
}

// Bottom navigation bar (M3 signature)
function M3Nav({ active = 'home' }) {
  const M3 = useM3();
  const items = [
    ['home', 'Ride', 'home'],
    ['history', 'Trips', 'route'],
    ['stats', 'Stats', 'leaderboard'],
    ['settings', 'Settings', 'settings'],
  ];
  return (
    <div style={{
      position: 'absolute', bottom: 0, left: 0, right: 0, height: 72,
      background: M3.surfaceContainer,
      display: 'flex', alignItems: 'flex-start', justifyContent: 'space-around',
      paddingTop: 12,
    }}>
      {items.map(([k, label, icon]) => {
        const on = k === active;
        return (
          <div key={k} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, width: 64 }}>
            <div style={{
              width: 56, height: 32, borderRadius: 16,
              background: on ? M3.secondaryContainer : 'transparent',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <MIcon name={icon} size={22} fill={on ? 1 : 0} color={on ? M3.onSecondaryContainer : M3.onSurfaceVar} />
            </div>
            <span style={{ fontFamily: M3.font, fontSize: 11, fontWeight: on ? 600 : 500, color: on ? M3.onSurface : M3.onSurfaceVar, letterSpacing: '0.02em' }}>{label}</span>
          </div>
        );
      })}
    </div>
  );
}

function M3TopBar({ title, leading = null, trailing = 'account_circle' }) {
  const M3 = useM3();
  return (
    <div style={{
      position: 'absolute', top: 28, left: 0, right: 0, height: 56,
      padding: '0 8px 0 12px',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      background: M3.surface,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: leading ? 6 : 14 }}>
        {leading && (
          <div style={{ width: 40, height: 40, borderRadius: 20, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <MIcon name={leading} size={24} color={M3.onSurface} />
          </div>
        )}
        <span style={{ fontFamily: M3.font, fontSize: 22, color: M3.onSurface, fontWeight: 400 }}>{title}</span>
      </div>
      <div style={{ width: 40, height: 40, borderRadius: 20, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        {trailing && <MIcon name={trailing} size={26} fill={1} color={M3.primary} />}
      </div>
    </div>
  );
}

// Wavy progress line — M3 Expressive signature
function WavyLine({ color, track, pct = 64, width = 256 }) {
  const M3 = useM3();
  color = color || M3.primary;
  track = track || M3.primaryContainer;
  const w = width, amp = 3, period = 14;
  let d = `M0,8 `;
  for (let x = 0; x <= w; x += 2) {
    const y = 8 + amp * Math.sin((x / period) * Math.PI);
    d += `L${x},${y} `;
  }
  const fillW = (pct / 100) * w;
  return (
    <svg width="100%" height="16" viewBox={`0 0 ${w} 16`} preserveAspectRatio="none">
      <path d={d} stroke={track} strokeWidth="3" fill="none" strokeLinecap="round" />
      <clipPath id={`wc${pct}`}><rect x="0" y="0" width={fillW} height="16" /></clipPath>
      <path d={d} stroke={color} strokeWidth="3" fill="none" strokeLinecap="round" clipPath={`url(#wc${pct})`} />
      <circle cx={fillW} cy="8" r="4" fill={color} />
    </svg>
  );
}

// --- screen 1 · IDLE -----------------------------------------------------

function M3Idle() {
  const M3 = useM3();
  return (
    <ScreenFrame bg={M3.surface}>
      <M3Status />
      <M3TopBar title="Speed" trailing={null} />

      <div style={{ position: 'absolute', top: 84, left: 0, right: 0, bottom: 72, overflow: 'hidden', padding: '0 16px' }}>
        <div style={{ fontFamily: M3.font, fontSize: 28, color: M3.onSurface, fontWeight: 500, letterSpacing: '-0.01em', lineHeight: 1.1 }}>
          Ready to ride
        </div>
        <div style={{ fontFamily: M3.font, fontSize: 14, color: M3.onSurfaceVar, marginTop: 4 }}>
          All sensors locked · sampling at 10 Hz
        </div>

        {/* Sensor chips */}
        <div style={{ display: 'flex', gap: 8, marginTop: 16, flexWrap: 'wrap' }}>
          {[['gps_fixed', 'GPS 12'], ['sensors', 'IMU'], ['battery_full', '94%']].map(([ic, t]) => (
            <div key={t} style={{
              display: 'flex', alignItems: 'center', gap: 6,
              height: 32, padding: '0 12px 0 8px', borderRadius: 8,
              border: `1px solid ${M3.outlineVar}`, background: M3.surface,
            }}>
              <MIcon name={ic} size={18} color={M3.primary} fill={1} />
              <span style={{ fontFamily: M3.font, fontSize: 13, color: M3.onSurfaceVar, fontWeight: 500 }}>{t}</span>
            </div>
          ))}
        </div>

        {/* Last run — big primaryContainer card */}
        <div style={{
          marginTop: 18, background: M3.primaryContainer, borderRadius: 28,
          padding: '20px 20px 18px',
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontFamily: M3.font, fontSize: 12, color: M3.onPrimaryContainer, fontWeight: 600, letterSpacing: '0.08em', opacity: 0.8 }}>LAST RIDE · 26 MAY</span>
            <MIcon name="two_wheeler" size={22} color={M3.onPrimaryContainer} fill={1} />
          </div>
          <div style={{ fontFamily: M3.font, fontSize: 24, color: M3.onPrimaryContainer, fontWeight: 500, marginTop: 6 }}>
            Col de Turini
          </div>
          <div style={{ display: 'flex', gap: 24, marginTop: 16 }}>
            {[['187', 'km/h top'], ['1.24', 'max g'], ['54.8', 'km']].map(([v, k]) => (
              <div key={k}>
                <div style={{ fontFamily: M3.font, fontSize: 30, color: M3.onPrimaryContainer, fontWeight: 600, lineHeight: 1, letterSpacing: '-0.02em' }}>{v}</div>
                <div style={{ fontFamily: M3.font, fontSize: 12, color: M3.onPrimaryContainer, opacity: 0.75, marginTop: 2 }}>{k}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Two tonal stat cards */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: 12 }}>
          {[['route', 'This week', '4 rides', M3.secondaryContainer, M3.onSecondaryContainer],
            ['trending_up', 'Total', '8 412 km', M3.tertiaryContainer, M3.onTertiaryContainer]].map(([ic, k, v, bg, on]) => (
            <div key={k} style={{ background: bg, borderRadius: 20, padding: '14px 16px' }}>
              <MIcon name={ic} size={22} color={on} fill={1} />
              <div style={{ fontFamily: M3.font, fontSize: 20, color: on, fontWeight: 600, marginTop: 10, lineHeight: 1 }}>{v}</div>
              <div style={{ fontFamily: M3.font, fontSize: 12, color: on, opacity: 0.8, marginTop: 2 }}>{k}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Extended FAB */}
      <div style={{ position: 'absolute', right: 16, bottom: 88 }}>
        <div style={{
          height: 56, borderRadius: 18, background: M3.primary,
          display: 'flex', alignItems: 'center', gap: 10, padding: '0 22px 0 18px',
          boxShadow: '0 2px 6px rgba(0,0,0,0.18)',
        }}>
          <MIcon name="fiber_manual_record" size={24} color={M3.onPrimary} fill={1} />
          <span style={{ fontFamily: M3.font, fontSize: 16, color: M3.onPrimary, fontWeight: 600 }}>Record</span>
        </div>
      </div>

      <M3Nav active="home" />
    </ScreenFrame>
  );
}

// --- screen 2 · LIVE HUD -------------------------------------------------

function M3Live() {
  const M3 = useM3();
  return (
    <ScreenFrame bg={M3.surface}>
      <M3Status />

      {/* recording banner */}
      <div style={{
        position: 'absolute', top: 28, left: 0, right: 0,
        margin: '8px 16px 0', height: 40, borderRadius: 12,
        background: M3.errorContainer, display: 'flex', alignItems: 'center',
        justifyContent: 'space-between', padding: '0 14px',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <MIcon name="fiber_manual_record" size={16} color={M3.error} fill={1} />
          <span style={{ fontFamily: M3.font, fontSize: 13, color: M3.onErrorContainer, fontWeight: 600 }}>Recording · 12:47</span>
        </div>
        <span style={{ fontFamily: M3.font, fontSize: 12, color: M3.onErrorContainer, opacity: 0.8 }}>10 Hz · 7 642 pts</span>
      </div>

      {/* Speed hero card */}
      <div style={{
        position: 'absolute', top: 84, left: 16, right: 16,
        background: M3.primaryContainer, borderRadius: 28, padding: '18px 22px 20px',
      }}>
        <span style={{ fontFamily: M3.font, fontSize: 13, color: M3.onPrimaryContainer, fontWeight: 600, letterSpacing: '0.06em', opacity: 0.8 }}>CURRENT SPEED</span>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
          <div style={{ fontFamily: M3.font, fontSize: 104, color: M3.onPrimaryContainer, fontWeight: 700, lineHeight: 0.9, letterSpacing: '-0.04em' }}>142</div>
          <span style={{ fontFamily: M3.font, fontSize: 18, color: M3.onPrimaryContainer, fontWeight: 500 }}>km/h</span>
        </div>
        <div style={{ marginTop: 8 }}>
          <WavyLine pct={64} />
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4 }}>
          <span style={{ fontFamily: M3.font, fontSize: 12, color: M3.onPrimaryContainer, opacity: 0.7 }}>avg 118</span>
          <span style={{ fontFamily: M3.font, fontSize: 12, color: M3.onPrimaryContainer, opacity: 0.7 }}>top 187</span>
        </div>
      </div>

      {/* Metric tonal cards */}
      <div style={{ position: 'absolute', top: 360, left: 16, right: 16, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        {[
          ['G-force', '0.84', 'g lat', M3.tertiaryContainer, M3.onTertiaryContainer, 'speed'],
          ['Lean', '38°', 'right', M3.secondaryContainer, M3.onSecondaryContainer, 'motorcycle'],
          ['Accel', '+0.42', 'g', M3.surfaceContainerHigh, M3.onSurface, 'trending_up'],
          ['Altitude', '1247', 'm', M3.surfaceContainerHigh, M3.onSurface, 'terrain'],
        ].map(([k, v, u, bg, on, ic]) => (
          <div key={k} style={{ background: bg, borderRadius: 20, padding: '14px 16px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontFamily: M3.font, fontSize: 13, color: on, fontWeight: 500, opacity: 0.85 }}>{k}</span>
              <MIcon name={ic} size={18} color={on} />
            </div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 4, marginTop: 8 }}>
              <span style={{ fontFamily: M3.font, fontSize: 30, color: on, fontWeight: 600, lineHeight: 1, letterSpacing: '-0.02em' }}>{v}</span>
              <span style={{ fontFamily: M3.font, fontSize: 12, color: on, opacity: 0.7 }}>{u}</span>
            </div>
          </div>
        ))}
      </div>

      {/* Stop button — full width filled */}
      <div style={{ position: 'absolute', left: 16, right: 16, bottom: 24 }}>
        <div style={{
          height: 56, borderRadius: 28, background: M3.error,
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10,
        }}>
          <MIcon name="stop" size={24} color="#fff" fill={1} />
          <span style={{ fontFamily: M3.font, fontSize: 16, color: '#fff', fontWeight: 600 }}>Stop recording</span>
        </div>
      </div>
    </ScreenFrame>
  );
}

// --- screen 3 · SUMMARY --------------------------------------------------

function M3Summary() {
  const M3 = useM3();
  return (
    <ScreenFrame bg={M3.surface}>
      <M3Status />
      <M3TopBar title="Trip summary" leading="arrow_back" trailing="share" />

      <div style={{ position: 'absolute', top: 84, left: 0, right: 0, bottom: 0, overflow: 'hidden', padding: '0 16px' }}>
        <div style={{ fontFamily: M3.font, fontSize: 24, color: M3.onSurface, fontWeight: 500 }}>Col de Turini</div>
        <div style={{ fontFamily: M3.font, fontSize: 13, color: M3.onSurfaceVar, marginTop: 2 }}>26 May · 14:32 → 15:19 · 47:12</div>

        {/* Top speed hero */}
        <div style={{ marginTop: 14, background: M3.primary, borderRadius: 28, padding: '20px 22px 18px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontFamily: M3.font, fontSize: 12, color: M3.onPrimary, opacity: 0.85, fontWeight: 600, letterSpacing: '0.08em' }}>TOP SPEED</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: 4, background: 'rgba(255,255,255,0.2)', borderRadius: 8, padding: '3px 8px' }}>
              <MIcon name="trophy" size={14} color={M3.onPrimary} fill={1} />
              <span style={{ fontFamily: M3.font, fontSize: 11, color: M3.onPrimary, fontWeight: 600 }}>NEW PB</span>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginTop: 4 }}>
            <span style={{ fontFamily: M3.font, fontSize: 80, color: M3.onPrimary, fontWeight: 700, lineHeight: 0.9, letterSpacing: '-0.04em' }}>187</span>
            <span style={{ fontFamily: M3.font, fontSize: 16, color: M3.onPrimary, opacity: 0.85 }}>km/h</span>
          </div>
        </div>

        {/* Stat grid */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: 12 }}>
          {[
            ['Max G lateral', '1.24 g', M3.tertiaryContainer, M3.onTertiaryContainer],
            ['Max lean', '52°', M3.secondaryContainer, M3.onSecondaryContainer],
            ['Distance', '54.8 km', M3.surfaceContainerHigh, M3.onSurface],
            ['Avg speed', '69.7 km/h', M3.surfaceContainerHigh, M3.onSurface],
            ['Hard brake', '−0.92 g', M3.surfaceContainerHigh, M3.onSurface],
            ['Moving', '93%', M3.surfaceContainerHigh, M3.onSurface],
          ].map(([k, v, bg, on]) => (
            <div key={k} style={{ background: bg, borderRadius: 18, padding: '12px 14px' }}>
              <div style={{ fontFamily: M3.font, fontSize: 12, color: on, opacity: 0.8 }}>{k}</div>
              <div style={{ fontFamily: M3.font, fontSize: 22, color: on, fontWeight: 600, marginTop: 4, letterSpacing: '-0.01em' }}>{v}</div>
            </div>
          ))}
        </div>

        {/* segment link */}
        <div style={{
          marginTop: 12, height: 56, borderRadius: 16, border: `1px solid ${M3.outlineVar}`,
          display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 18px',
        }}>
          <span style={{ fontFamily: M3.font, fontSize: 15, color: M3.onSurface, fontWeight: 500 }}>3 personal bests by segment</span>
          <MIcon name="chevron_right" size={24} color={M3.onSurfaceVar} />
        </div>
      </div>
    </ScreenFrame>
  );
}

// --- screen 4 · DETAIL ---------------------------------------------------

function M3Detail() {
  const M3 = useM3();
  return (
    <ScreenFrame bg={M3.surface}>
      <M3Status />
      <M3TopBar title="Trace" leading="arrow_back" trailing="download" />

      <div style={{ position: 'absolute', top: 84, left: 0, right: 0, bottom: 0, overflow: 'hidden', padding: '0 16px' }}>
        {/* Map card */}
        <div style={{ borderRadius: 24, overflow: 'hidden', background: M3.surfaceContainerHigh, height: 220, position: 'relative' }}>
          <svg width="100%" height="100%" viewBox="0 0 288 220" preserveAspectRatio="none">
            {[60,110,160].map(y => (
              <path key={y} d={`M-10,${y} Q70,${y-12} 142,${y-2} T300,${y-16}`} stroke={M3.outlineVar} strokeWidth="1" fill="none" />
            ))}
            <path d="M28,196 L50,172 L66,160 Q86,150 102,132 L118,122 Q138,110 148,98 L162,80 Q175,70 188,58 L205,48 Q230,42 252,34 L276,26"
              stroke={M3.primary} strokeWidth="5" fill="none" strokeLinecap="round" strokeLinejoin="round" />
            <circle cx="28" cy="196" r="7" fill={M3.primary} />
            <circle cx="28" cy="196" r="3" fill="#fff" />
            <circle cx="276" cy="26" r="7" fill={M3.error} />
          </svg>
          <div style={{ position: 'absolute', top: 12, left: 12, background: M3.surface, borderRadius: 8, padding: '6px 10px', display: 'flex', gap: 6, alignItems: 'center' }}>
            <MIcon name="speed" size={16} color={M3.primary} fill={1} />
            <span style={{ fontFamily: M3.font, fontSize: 12, color: M3.onSurface, fontWeight: 600 }}>187 km/h</span>
          </div>
        </div>

        {/* Speed chart card */}
        <div style={{ marginTop: 12, background: M3.surfaceContainerLow, borderRadius: 20, padding: '14px 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontFamily: M3.font, fontSize: 14, color: M3.onSurface, fontWeight: 600 }}>Speed</span>
            <span style={{ fontFamily: M3.font, fontSize: 12, color: M3.onSurfaceVar }}>0 – 187 km/h</span>
          </div>
          <svg width="100%" height="64" viewBox="0 0 256 64" preserveAspectRatio="none" style={{ marginTop: 8 }}>
            <path d="M0,56 L18,46 L32,42 L48,32 L62,36 L78,22 L94,30 L110,14 L128,18 L148,10 L168,18 L184,8 L200,20 L220,24 L240,14 L256,20 L256,64 L0,64 Z" fill={M3.primaryContainer} />
            <path d="M0,56 L18,46 L32,42 L48,32 L62,36 L78,22 L94,30 L110,14 L128,18 L148,10 L168,18 L184,8 L200,20 L220,24 L240,14 L256,20" stroke={M3.primary} strokeWidth="2.5" fill="none" strokeLinejoin="round" />
          </svg>
        </div>

        {/* G chart card */}
        <div style={{ marginTop: 12, background: M3.surfaceContainerLow, borderRadius: 20, padding: '14px 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontFamily: M3.font, fontSize: 14, color: M3.onSurface, fontWeight: 600 }}>G lateral</span>
            <span style={{ fontFamily: M3.font, fontSize: 12, color: M3.onSurfaceVar }}>−1.18 / +1.24</span>
          </div>
          <svg width="100%" height="52" viewBox="0 0 256 52" preserveAspectRatio="none" style={{ marginTop: 8 }}>
            <line x1="0" y1="26" x2="256" y2="26" stroke={M3.outlineVar} />
            <path d="M0,26 L18,30 L32,22 L48,16 L62,32 L78,10 L94,40 L110,8 L128,44 L148,14 L168,38 L184,6 L200,42 L220,18 L240,34 L256,22" stroke={M3.tertiary} strokeWidth="2.5" fill="none" strokeLinejoin="round" />
          </svg>
        </div>
      </div>
    </ScreenFrame>
  );
}

// --- screen 5 · HISTORY --------------------------------------------------

function M3History() {
  const M3 = useM3();
  const runs = [
    ['Col de Turini', '26 May · 54.8 km', '187', true],
    ['Commute west', '25 May · 12.4 km', '92', false],
    ['Route Napoléon', '24 May · 312 km', '176', false],
    ['Tourtour loop', '22 May · 88 km', '162', false],
    ['Gorges du Verdon', '19 May · 224 km', '168', true],
  ];
  return (
    <ScreenFrame bg={M3.surface}>
      <M3Status />
      <M3TopBar title="Trips" trailing="search" />

      <div style={{ position: 'absolute', top: 84, left: 0, right: 0, bottom: 72, overflow: 'hidden', padding: '0 16px' }}>
        {/* Search bar */}
        <div style={{ height: 48, borderRadius: 24, background: M3.surfaceContainerHigh, display: 'flex', alignItems: 'center', gap: 10, padding: '0 16px' }}>
          <MIcon name="search" size={22} color={M3.onSurfaceVar} />
          <span style={{ fontFamily: M3.font, fontSize: 15, color: M3.onSurfaceVar }}>Search 142 trips</span>
        </div>

        {/* filter chips */}
        <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
          {[['All', true], ['This week', false]].map(([t, on]) => (
            <div key={t} style={{
              height: 32, borderRadius: 8, padding: '0 12px',
              display: 'flex', alignItems: 'center', gap: 6,
              background: on ? M3.secondaryContainer : 'transparent',
              border: on ? 'none' : `1px solid ${M3.outline}`,
            }}>
              {on && <MIcon name="check" size={16} color={M3.onSecondaryContainer} />}
              <span style={{ fontFamily: M3.font, fontSize: 13, color: on ? M3.onSecondaryContainer : M3.onSurfaceVar, fontWeight: 500 }}>{t}</span>
            </div>
          ))}
        </div>

        {/* list */}
        <div style={{ marginTop: 12 }}>
          {runs.map(([name, sub, vmax, star], i) => (
            <div key={name} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '12px 4px' }}>
              <div style={{ width: 44, height: 44, borderRadius: 22, background: M3.tertiaryContainer, display: 'flex', alignItems: 'center', justifyContent: 'center', flex: '0 0 auto' }}>
                <MIcon name="two_wheeler" size={22} color={M3.onTertiaryContainer} fill={1} />
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span style={{ fontFamily: M3.font, fontSize: 16, color: M3.onSurface, fontWeight: 500 }}>{name}</span>
                </div>
                <div style={{ fontFamily: M3.font, fontSize: 13, color: M3.onSurfaceVar, marginTop: 1 }}>{sub}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontFamily: M3.font, fontSize: 18, color: M3.onSurface, fontWeight: 600, lineHeight: 1 }}>{vmax}</div>
                <div style={{ fontFamily: M3.font, fontSize: 11, color: M3.onSurfaceVar }}>km/h</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <M3Nav active="history" />
    </ScreenFrame>
  );
}

// --- screen 6a · SEGMENT LIST --------------------------------------------

function M3SegmentList() {
  const M3 = useM3();
  const segments = [
    ['Ascent, west',    '2.84 km', 7,  '2:14.8', -3.2,  true ],
    ['Col du Lachat',   '4.20 km', 12, '5:02.1', -1.8,  false],
    ['La Grange climb', '1.60 km', 4,  '1:44.3',  0.0,  false],
    ['Valley sprint',   '3.10 km', 9,  '3:31.7', +2.1,  false],
    ['Ridge descent',   '5.50 km', 6,  '6:18.0', -5.4,  false],
    ['Hairpin loop',    '0.90 km', 3,  '0:58.2',  0.0,  false],
    ['East switchback', '2.20 km', 5,  '2:40.6', +1.3,  false],
  ];
  return (
    <ScreenFrame bg={M3.surface}>
      <M3Status />
      <M3TopBar title="Segments" leading="arrow_back" trailing="add" />

      <div style={{ position: 'absolute', top: 84, left: 0, right: 0, bottom: 72, overflow: 'hidden', padding: '0 16px' }}>

        {/* search bar */}
        <div style={{ height: 48, borderRadius: 24, background: M3.surfaceContainerHigh, display: 'flex', alignItems: 'center', gap: 10, padding: '0 16px' }}>
          <MIcon name="search" size={20} color={M3.onSurfaceVar} />
          <span style={{ fontFamily: M3.font, fontSize: 15, color: M3.onSurfaceVar }}>Search segments…</span>
        </div>

        {/* sort chips */}
        <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
          {[['Best time', true], ['Most runs', false], ['Nearby', false]].map(([t, on]) => (
            <div key={t} style={{
              height: 32, borderRadius: 8, padding: '0 12px',
              display: 'flex', alignItems: 'center', gap: 6,
              background: on ? M3.secondaryContainer : 'transparent',
              border: on ? 'none' : `1px solid ${M3.outline}`,
            }}>
              {on && <MIcon name="check" size={16} color={M3.onSecondaryContainer} />}
              <span style={{ fontFamily: M3.font, fontSize: 13, color: on ? M3.onSecondaryContainer : M3.onSurfaceVar, fontWeight: 500 }}>{t}</span>
            </div>
          ))}
        </div>

        {/* count */}
        <div style={{ fontFamily: M3.font, fontSize: 12, color: M3.onSurfaceVar, marginTop: 14, paddingLeft: 2 }}>14 SEGMENTS</div>

        {/* list */}
        <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 2 }}>
          {segments.map(([name, dist, runs, best, delta, starred], i) => {
            const faster = delta < 0;
            const same   = delta === 0;
            const trendColor = faster ? M3.primary : same ? M3.onSurfaceVar : M3.error;
            const trendIcon  = faster ? 'arrow_downward' : same ? 'remove' : 'arrow_upward';
            return (
              <div key={name} style={{
                display: 'flex', alignItems: 'center', gap: 12, padding: '12px 4px',
                borderBottom: i < segments.length - 1 ? `1px solid ${M3.outlineVar}` : 'none',
              }}>
                {/* icon */}
                <div style={{
                  width: 44, height: 44, flex: '0 0 44px', borderRadius: 22,
                  background: starred ? M3.primaryContainer : M3.surfaceContainerHigh,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>
                  <MIcon name="timer" size={22} color={starred ? M3.onPrimaryContainer : M3.onSurfaceVar} fill={1} />
                </div>

                {/* name + meta */}
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <span style={{ fontFamily: M3.font, fontSize: 15, color: M3.onSurface, fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{name}</span>
                  </div>
                  <div style={{ fontFamily: M3.font, fontSize: 12, color: M3.onSurfaceVar, marginTop: 2 }}>{dist} · {runs} runs</div>
                </div>

                {/* best time + trend */}
                <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
                  <span style={{ fontFamily: M3.font, fontSize: 16, color: starred ? M3.primary : M3.onSurface, fontWeight: 600 }}>{best}</span>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <MIcon name={trendIcon} size={12} color={trendColor} fill={1} />
                    <span style={{ fontFamily: M3.font, fontSize: 11, color: trendColor }}>{same ? '—' : `${Math.abs(delta)}s`}</span>
                  </div>
                </div>

                <MIcon name="chevron_right" size={20} color={M3.onSurfaceVar} />
              </div>
            );
          })}
        </div>
      </div>

      <M3Nav active="stats" />
    </ScreenFrame>
  );
}

// --- screen 6 · SEGMENT --------------------------------------------------

function M3Segment() {
  const M3 = useM3();
  const attempts = [
    ['26 May', '2:14.8', 100, '−3.2', true],
    ['12 May', '2:18.0', 86, '±0.0', false],
    ['28 Apr', '2:21.4', 72, '+3.4', false],
    ['14 Apr', '2:24.8', 58, '+6.8', false],
  ];
  return (
    <ScreenFrame bg={M3.surface}>
      <M3Status />
      <M3TopBar title="Segment" leading="arrow_back" trailing="more_vert" />

      <div style={{ position: 'absolute', top: 84, left: 0, right: 0, bottom: 0, overflow: 'hidden', padding: '0 16px' }}>
        <div style={{ fontFamily: M3.font, fontSize: 24, color: M3.onSurface, fontWeight: 500 }}>Ascent, west</div>
        <div style={{ fontFamily: M3.font, fontSize: 13, color: M3.onSurfaceVar, marginTop: 2 }}>2.84 km · 7 attempts</div>

        {/* Best card */}
        <div style={{ marginTop: 14, background: M3.primaryContainer, borderRadius: 28, padding: '20px 22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontFamily: M3.font, fontSize: 12, color: M3.onPrimaryContainer, fontWeight: 600, letterSpacing: '0.06em', opacity: 0.8 }}>PERSONAL BEST</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <MIcon name="arrow_downward" size={16} color={M3.primary} fill={1} />
              <span style={{ fontFamily: M3.font, fontSize: 13, color: M3.onPrimaryContainer, fontWeight: 600 }}>3.2s faster</span>
            </div>
          </div>
          <div style={{ fontFamily: M3.font, fontSize: 64, color: M3.onPrimaryContainer, fontWeight: 700, lineHeight: 0.9, letterSpacing: '-0.03em', marginTop: 4 }}>2:14.8</div>
          <div style={{ display: 'flex', gap: 22, marginTop: 14 }}>
            {[['142', 'v.max'], ['0.91', 'g lat'], ['38°', 'lean']].map(([v, k]) => (
              <div key={k}>
                <div style={{ fontFamily: M3.font, fontSize: 20, color: M3.onPrimaryContainer, fontWeight: 600, lineHeight: 1 }}>{v}</div>
                <div style={{ fontFamily: M3.font, fontSize: 11, color: M3.onPrimaryContainer, opacity: 0.75, marginTop: 1 }}>{k}</div>
              </div>
            ))}
          </div>
        </div>

        {/* attempts */}
        <div style={{ marginTop: 16 }}>
          <span style={{ fontFamily: M3.font, fontSize: 13, color: M3.onSurfaceVar, fontWeight: 600, letterSpacing: '0.04em' }}>HISTORY</span>
          <div style={{ marginTop: 8 }}>
            {attempts.map(([d, t, pct, delta, best], i) => (
              <div key={i} style={{ display: 'grid', gridTemplateColumns: '60px 64px 1fr 46px', alignItems: 'center', columnGap: 10, padding: '10px 0', borderBottom: `1px solid ${M3.outlineVar}` }}>
                <span style={{ fontFamily: M3.font, fontSize: 13, color: M3.onSurfaceVar }}>{d}</span>
                <span style={{ fontFamily: M3.font, fontSize: 16, fontWeight: 600, color: best ? M3.primary : M3.onSurface }}>{t}</span>
                <div style={{ height: 8, borderRadius: 4, background: M3.surfaceContainerHigh, overflow: 'hidden' }}>
                  <div style={{ width: `${pct}%`, height: '100%', background: best ? M3.primary : M3.outline, borderRadius: 4 }} />
                </div>
                <span style={{ fontFamily: M3.font, fontSize: 13, textAlign: 'right', color: best ? M3.primary : M3.onSurfaceVar }}>{delta}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </ScreenFrame>
  );
}

// --- screen 7 · SETTINGS / EXPORT ----------------------------------------

function M3Settings() {
  const M3 = useM3();
  const Switch = ({ on }) => (
    <div style={{
      width: 52, height: 32, borderRadius: 16, position: 'relative',
      background: on ? M3.primary : M3.surfaceContainerHighest,
      border: on ? 'none' : `2px solid ${M3.outline}`,
    }}>
      <div style={{
        position: 'absolute', top: on ? 4 : 6, left: on ? 24 : 6,
        width: on ? 24 : 16, height: on ? 24 : 16, borderRadius: 12,
        background: on ? M3.onPrimary : M3.outline,
      }} />
    </div>
  );
  const Row = ({ ic, k, v, sw, last }) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '14px 16px', borderBottom: last ? 'none' : `1px solid ${M3.outlineVar}` }}>
      <MIcon name={ic} size={22} color={M3.onSurfaceVar} />
      <span style={{ flex: 1, fontFamily: M3.font, fontSize: 15, color: M3.onSurface, fontWeight: 500 }}>{k}</span>
      {sw !== undefined ? <Switch on={sw} /> : <span style={{ fontFamily: M3.font, fontSize: 14, color: M3.onSurfaceVar }}>{v}</span>}
    </div>
  );
  return (
    <ScreenFrame bg={M3.surface}>
      <M3Status />
      <M3TopBar title="Settings" trailing="help" />

      <div style={{ position: 'absolute', top: 84, left: 0, right: 0, bottom: 72, overflow: 'hidden', padding: '0 16px' }}>
        <span style={{ fontFamily: M3.font, fontSize: 13, color: M3.primary, fontWeight: 600, letterSpacing: '0.04em', paddingLeft: 4 }}>SAMPLING</span>
        <div style={{ background: M3.surfaceContainerLow, borderRadius: 20, marginTop: 8 }}>
          <Row ic="gps_fixed" k="GPS rate" v="10 Hz" />
          <Row ic="sensors" k="IMU rate" v="100 Hz" />
          <Row ic="pause_circle" k="Auto-pause" sw={true} last />
        </div>

        <span style={{ fontFamily: M3.font, fontSize: 13, color: M3.primary, fontWeight: 600, letterSpacing: '0.04em', paddingLeft: 4, display: 'block', marginTop: 18 }}>DISPLAY</span>
        <div style={{ background: M3.surfaceContainerLow, borderRadius: 20, marginTop: 8 }}>
          <Row ic="straighten" k="Units" v="Metric" />
          <Row ic="dark_mode" k="Dark theme" sw={false} last />
        </div>

        {/* Export card */}
        <div style={{ marginTop: 18, background: M3.tertiaryContainer, borderRadius: 24, padding: '18px 18px 16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <MIcon name="database" size={24} color={M3.onTertiaryContainer} fill={1} />
            <span style={{ fontFamily: M3.font, fontSize: 16, color: M3.onTertiaryContainer, fontWeight: 600 }}>Raw data export</span>
          </div>
          <div style={{ fontFamily: M3.font, fontSize: 13, color: M3.onTertiaryContainer, opacity: 0.85, marginTop: 6, lineHeight: 1.4 }}>
            142 trips · 2.8 GB at full 100 ms resolution. CSV, GPX or FIT.
          </div>
          <div style={{ marginTop: 14, height: 48, borderRadius: 24, background: M3.tertiary, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
            <MIcon name="download" size={20} color="#fff" />
            <span style={{ fontFamily: M3.font, fontSize: 15, color: '#fff', fontWeight: 600 }}>Export all</span>
          </div>
        </div>
      </div>

      <M3Nav active="settings" />
    </ScreenFrame>
  );
}

// --- screen 8 · STATISTICS (overview) ------------------------------------

function M3Stats() {
  const M3 = useM3();
  const months = [['Dec', 38], ['Jan', 52], ['Feb', 44], ['Mar', 61], ['Apr', 74], ['May', 96]];
  const maxV = 96;
  const records = [
    ['Top speed', '187', 'km/h', 'speed', M3.secondaryContainer, M3.onSecondaryContainer],
    ['Max lean', '52°', '', 'motorcycle', M3.tertiaryContainer, M3.onTertiaryContainer],
    ['Max g lat', '1.24', 'g', 'whatshot', M3.surfaceContainerHigh, M3.onSurface],
    ['Longest', '312', 'km', 'route', M3.surfaceContainerHigh, M3.onSurface],
  ];
  return (
    <ScreenFrame bg={M3.surface}>
      <M3Status />
      <M3TopBar title="Statistics" trailing="date_range" />

      <div style={{ position: 'absolute', top: 84, left: 0, right: 0, bottom: 72, overflow: 'hidden', padding: '0 16px' }}>
        {/* range chips */}
        <div style={{ display: 'flex', gap: 8 }}>
          {[['This year', true], ['90 days', false], ['All time', false]].map(([t, on]) => (
            <div key={t} style={{
              height: 32, borderRadius: 8, padding: '0 12px',
              display: 'flex', alignItems: 'center', gap: 6,
              background: on ? M3.secondaryContainer : 'transparent',
              border: on ? 'none' : `1px solid ${M3.outline}`,
            }}>
              {on && <MIcon name="check" size={16} color={M3.onSecondaryContainer} />}
              <span style={{ fontFamily: M3.font, fontSize: 13, color: on ? M3.onSecondaryContainer : M3.onSurfaceVar, fontWeight: 500 }}>{t}</span>
            </div>
          ))}
        </div>

        {/* distance hero + month bars */}
        <div style={{ marginTop: 12, background: M3.primaryContainer, borderRadius: 28, padding: '20px 22px 18px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontFamily: M3.font, fontSize: 12, color: M3.onPrimaryContainer, fontWeight: 600, letterSpacing: '0.08em', opacity: 0.8 }}>DISTANCE · 2026</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <MIcon name="trending_up" size={16} color={M3.onPrimaryContainer} fill={1} />
              <span style={{ fontFamily: M3.font, fontSize: 13, color: M3.onPrimaryContainer, fontWeight: 600 }}>+18%</span>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginTop: 4 }}>
            <span style={{ fontFamily: M3.font, fontSize: 56, color: M3.onPrimaryContainer, fontWeight: 700, lineHeight: 0.9, letterSpacing: '-0.03em' }}>3 412</span>
            <span style={{ fontFamily: M3.font, fontSize: 16, color: M3.onPrimaryContainer, opacity: 0.85 }}>km</span>
          </div>
          {/* month bars */}
          <div style={{ display: 'flex', alignItems: 'flex-end', gap: 10, marginTop: 18, height: 64 }}>
            {months.map(([m, v], i) => {
              const last = i === months.length - 1;
              return (
                <div key={m} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
                  <div style={{
                    width: '100%', height: (v / maxV) * 48, borderRadius: 6,
                    background: last ? M3.primary : 'rgba(0,0,0,0.12)',
                  }} />
                  <span style={{ fontFamily: M3.font, fontSize: 10, color: M3.onPrimaryContainer, opacity: last ? 0.95 : 0.6, fontWeight: last ? 700 : 500 }}>{m}</span>
                </div>
              );
            })}
          </div>
        </div>

        {/* records grid */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: 12 }}>
          {records.map(([k, v, u, ic, bg, on]) => (
            <div key={k} style={{ background: bg, borderRadius: 20, padding: '14px 16px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontFamily: M3.font, fontSize: 12, color: on, opacity: 0.8 }}>{k}</span>
                <MIcon name={ic} size={18} color={on} fill={1} />
              </div>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 4, marginTop: 8 }}>
                <span style={{ fontFamily: M3.font, fontSize: 26, color: on, fontWeight: 600, lineHeight: 1, letterSpacing: '-0.02em' }}>{v}</span>
                {u && <span style={{ fontFamily: M3.font, fontSize: 12, color: on, opacity: 0.7 }}>{u}</span>}
              </div>
            </div>
          ))}
        </div>

        {/* segments link */}
        <div style={{
          marginTop: 12, height: 56, borderRadius: 16, border: `1px solid ${M3.outlineVar}`,
          display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 18px',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <MIcon name="timer" size={20} color={M3.primary} fill={1} />
            <span style={{ fontFamily: M3.font, fontSize: 15, color: M3.onSurface, fontWeight: 500 }}>14 tracked segments</span>
          </div>
          <MIcon name="chevron_right" size={24} color={M3.onSurfaceVar} />
        </div>
      </div>

      <M3Nav active="stats" />
    </ScreenFrame>
  );
}

// --- screen 9 · EXPORT (new flow payoff) ---------------------------------

function M3Export() {
  const M3 = useM3();
  const Switch = ({ on }) => (
    <div style={{
      width: 52, height: 32, borderRadius: 16, position: 'relative',
      background: on ? M3.primary : M3.surfaceContainerHighest,
      border: on ? 'none' : `2px solid ${M3.outline}`,
    }}>
      <div style={{
        position: 'absolute', top: on ? 4 : 6, left: on ? 24 : 6,
        width: on ? 24 : 16, height: on ? 24 : 16, borderRadius: 12,
        background: on ? M3.onPrimary : M3.outline,
      }} />
    </div>
  );
  const formats = [['CSV', 'description', false], ['GPX', 'map', false], ['FIT', 'memory', true]];
  return (
    <ScreenFrame bg={M3.surface}>
      <M3Status />
      <M3TopBar title="Export data" leading="arrow_back" trailing="help" />

      <div style={{ position: 'absolute', top: 84, left: 0, right: 0, bottom: 0, overflow: 'hidden', padding: '0 16px' }}>
        <div style={{ fontFamily: M3.font, fontSize: 13, color: M3.onSurfaceVar, lineHeight: 1.4 }}>
          142 trips · 2.8 GB at full 100&nbsp;ms resolution
        </div>

        {/* format select */}
        <span style={{ fontFamily: M3.font, fontSize: 13, color: M3.primary, fontWeight: 600, letterSpacing: '0.04em', display: 'block', marginTop: 16, paddingLeft: 2 }}>FORMAT</span>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10, marginTop: 8 }}>
          {formats.map(([t, ic, on]) => (
            <div key={t} style={{
              borderRadius: 18, padding: '14px 12px', position: 'relative',
              background: on ? M3.primaryContainer : M3.surfaceContainerLow,
              border: on ? `2px solid ${M3.primary}` : `1px solid ${M3.outlineVar}`,
              display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8,
            }}>
              <MIcon name={ic} size={24} color={on ? M3.onPrimaryContainer : M3.onSurfaceVar} fill={on ? 1 : 0} />
              <span style={{ fontFamily: M3.font, fontSize: 14, color: on ? M3.onPrimaryContainer : M3.onSurface, fontWeight: 600 }}>{t}</span>
              {on && <div style={{ position: 'absolute', top: 8, right: 8 }}><MIcon name="check_circle" size={16} color={M3.primary} fill={1} /></div>}
            </div>
          ))}
        </div>

        {/* scope */}
        <span style={{ fontFamily: M3.font, fontSize: 13, color: M3.primary, fontWeight: 600, letterSpacing: '0.04em', display: 'block', marginTop: 18, paddingLeft: 2 }}>SCOPE</span>
        <div style={{ background: M3.surfaceContainerLow, borderRadius: 20, marginTop: 8 }}>
          {[['route', 'Trips', 'All 142'], ['calendar_month', 'Date range', 'All time']].map(([ic, k, v], i) => (
            <div key={k} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '14px 16px', borderBottom: i === 0 ? `1px solid ${M3.outlineVar}` : 'none' }}>
              <MIcon name={ic} size={22} color={M3.onSurfaceVar} />
              <span style={{ flex: 1, fontFamily: M3.font, fontSize: 15, color: M3.onSurface, fontWeight: 500 }}>{k}</span>
              <span style={{ fontFamily: M3.font, fontSize: 14, color: M3.onSurfaceVar }}>{v}</span>
              <MIcon name="chevron_right" size={20} color={M3.onSurfaceVar} />
            </div>
          ))}
        </div>

        {/* include */}
        <span style={{ fontFamily: M3.font, fontSize: 13, color: M3.primary, fontWeight: 600, letterSpacing: '0.04em', display: 'block', marginTop: 18, paddingLeft: 2 }}>INCLUDE</span>
        <div style={{ background: M3.surfaceContainerLow, borderRadius: 20, marginTop: 8 }}>
          {[['satellite_alt', 'GPS track', true], ['sensors', 'IMU · accel & gyro', true], ['motorcycle', 'Lean angle', false]].map(([ic, k, on], i, a) => (
            <div key={k} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '12px 16px', borderBottom: i === a.length - 1 ? 'none' : `1px solid ${M3.outlineVar}` }}>
              <MIcon name={ic} size={22} color={M3.onSurfaceVar} />
              <span style={{ flex: 1, fontFamily: M3.font, fontSize: 15, color: M3.onSurface, fontWeight: 500 }}>{k}</span>
              <Switch on={on} />
            </div>
          ))}
        </div>

        {/* export button */}
        <div style={{ marginTop: 18, height: 56, borderRadius: 28, background: M3.primary, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10, boxShadow: '0 2px 6px rgba(0,0,0,0.18)' }}>
          <MIcon name="download" size={22} color={M3.onPrimary} />
          <span style={{ fontFamily: M3.font, fontSize: 16, color: M3.onPrimary, fontWeight: 600 }}>Export 142 trips · 2.6 GB</span>
        </div>
      </div>
    </ScreenFrame>
  );
}

Object.assign(window, {
  M3Theme, M3_GREEN, M3_GREEN_DARK, M3_BLUE, M3_ORANGE, M3_VIOLET,
  M3Idle, M3Live, M3Summary, M3Detail, M3History, M3Segment, M3SegmentList, M3Settings,
  M3Stats, M3Export,
});
