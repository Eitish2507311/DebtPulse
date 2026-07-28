import { createContext, useContext, useEffect, useMemo, useState, useCallback } from 'react';

/**
 * App-wide UI preferences: colour theme (light/dark) and font scale (font-resize).
 * Both are persisted to localStorage and applied to <html> via a data attribute and
 * a CSS custom property, so the token-based theme reacts automatically.
 */
const PreferencesContext = createContext(null);

const THEME_KEY = 'dp.theme';
const FONT_KEY = 'dp.fontScale';
const FONT_STEPS = [0.9, 1, 1.1, 1.2];

function readTheme() {
  const saved = localStorage.getItem(THEME_KEY);
  if (saved === 'light' || saved === 'dark') return saved;
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}
function readFont() {
  const n = Number(localStorage.getItem(FONT_KEY));
  return FONT_STEPS.includes(n) ? n : 1;
}

export function PreferencesProvider({ children }) {
  const [theme, setTheme] = useState(readTheme);
  const [fontScale, setFontScale] = useState(readFont);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem(THEME_KEY, theme);
  }, [theme]);

  useEffect(() => {
    document.documentElement.style.setProperty('--dp-font-scale', String(fontScale));
    localStorage.setItem(FONT_KEY, String(fontScale));
  }, [fontScale]);

  const toggleTheme = useCallback(() => setTheme((t) => (t === 'dark' ? 'light' : 'dark')), []);
  const stepFont = useCallback((dir) => setFontScale((f) => {
    const i = FONT_STEPS.indexOf(f);
    const next = Math.min(FONT_STEPS.length - 1, Math.max(0, (i === -1 ? 1 : i) + dir));
    return FONT_STEPS[next];
  }), []);
  const resetFont = useCallback(() => setFontScale(1), []);

  const value = useMemo(
    () => ({ theme, toggleTheme, fontScale, stepFont, resetFont, canGrow: fontScale < FONT_STEPS.at(-1), canShrink: fontScale > FONT_STEPS[0] }),
    [theme, toggleTheme, fontScale, stepFont, resetFont],
  );

  return <PreferencesContext.Provider value={value}>{children}</PreferencesContext.Provider>;
}

export function usePreferences() {
  const ctx = useContext(PreferencesContext);
  if (!ctx) throw new Error('usePreferences must be used within a PreferencesProvider');
  return ctx;
}

/** Compact control: font A− / A / A＋ and a light/dark toggle. Inherits text colour. */
export function PreferenceToggle() {
  const { theme, toggleTheme, stepFont, resetFont, canGrow, canShrink } = usePreferences();
  return (
    <div className="pref" role="group" aria-label="Display preferences">
      <button className="pref-btn sm" onClick={() => stepFont(-1)} disabled={!canShrink} aria-label="Decrease font size" title="Decrease font size">A−</button>
      <button className="pref-btn" onClick={resetFont} aria-label="Reset font size" title="Reset font size">A</button>
      <button className="pref-btn lg" onClick={() => stepFont(1)} disabled={!canGrow} aria-label="Increase font size" title="Increase font size">A＋</button>
      <span className="pref-divider" aria-hidden="true" />
      <button className="pref-btn" onClick={toggleTheme}
        aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
        title={theme === 'dark' ? 'Light mode' : 'Dark mode'}>
        <i className={`bi bi-${theme === 'dark' ? 'sun' : 'moon-stars'}`} />
      </button>
    </div>
  );
}
