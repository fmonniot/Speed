// ScreenFrame.jsx — minimal phone-screen wrapper used inside DCArtboards.
// Not a full Android bezel — just the screen surface, so the canvas reads
// as a storyboard rather than a device showcase.

function ScreenFrame({ width = 320, height = 680, bg = '#000', children, style }) {
  return (
    <div
      style={{
        width,
        height,
        background: bg,
        position: 'relative',
        overflow: 'hidden',
        fontFamily: 'system-ui, sans-serif',
        ...style,
      }}
    >
      {children}
    </div>
  );
}

// Generic Android-ish status bar (time + signal/battery glyphs).
// Each variation styles it with its own color via `color` prop.
function StatusBar({ time = '14:32', color = '#fff', opacity = 0.7, font = 'inherit' }) {
  return (
    <div
      style={{
        position: 'absolute',
        top: 0, left: 0, right: 0,
        height: 28,
        padding: '0 14px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        color,
        opacity,
        fontSize: 11,
        fontFamily: font,
        letterSpacing: '0.02em',
        zIndex: 5,
      }}
    >
      <span>{time}</span>
      <span style={{ display: 'flex', gap: 6, alignItems: 'center', fontSize: 10 }}>
        <span>5G</span>
        <span style={{ letterSpacing: '0.5px' }}>···</span>
        <span style={{
          display: 'inline-block',
          width: 18, height: 9,
          border: `1px solid ${color}`,
          borderRadius: 2,
          position: 'relative',
        }}>
          <span style={{
            position: 'absolute', inset: 1, right: 6,
            background: color,
          }} />
        </span>
      </span>
    </div>
  );
}

Object.assign(window, { ScreenFrame, StatusBar });
