import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PreferencesProvider, PreferenceToggle } from './Preferences.jsx';

beforeEach(() => {
  localStorage.clear();
  document.documentElement.removeAttribute('data-theme');
  document.documentElement.style.removeProperty('--dp-font-scale');
});

function setup() {
  return render(
    <PreferencesProvider>
      <PreferenceToggle />
    </PreferencesProvider>,
  );
}

describe('PreferenceToggle (theme + font-resize)', () => {
  it('toggles the html data-theme attribute and persists it', async () => {
    const user = userEvent.setup();
    setup();
    // starts light (matchMedia stub reports no dark preference)
    expect(document.documentElement.dataset.theme).toBe('light');

    await user.click(screen.getByRole('button', { name: /switch to dark mode/i }));
    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(localStorage.getItem('dp.theme')).toBe('dark');

    await user.click(screen.getByRole('button', { name: /switch to light mode/i }));
    expect(document.documentElement.dataset.theme).toBe('light');
  });

  it('increases and resets the font scale via the CSS custom property', async () => {
    const user = userEvent.setup();
    setup();
    const read = () => document.documentElement.style.getPropertyValue('--dp-font-scale');
    expect(read()).toBe('1');

    await user.click(screen.getByRole('button', { name: /increase font size/i }));
    expect(read()).toBe('1.1');

    await user.click(screen.getByRole('button', { name: /reset font size/i }));
    expect(read()).toBe('1');
  });

  it('disables shrink at the smallest step', async () => {
    const user = userEvent.setup();
    setup();
    const shrink = screen.getByRole('button', { name: /decrease font size/i });
    await user.click(shrink); // 1 -> 0.9 (minimum)
    expect(document.documentElement.style.getPropertyValue('--dp-font-scale')).toBe('0.9');
    expect(shrink).toBeDisabled();
  });
});
