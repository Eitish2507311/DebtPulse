import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { PreferencesProvider } from '../components/Preferences';
import LandingPage from './LandingPage';

// Isolate the page from auth state — we only care about its rendered structure here.
vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ isAuthenticated: false }),
}));

function renderLanding() {
  return render(
    <MemoryRouter>
      <PreferencesProvider>
        <LandingPage />
      </PreferencesProvider>
    </MemoryRouter>,
  );
}

describe('LandingPage', () => {
  it('renders the hero headline and a sign-in call to action for guests', () => {
    renderLanding();
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/recover more/i);
    expect(screen.getAllByRole('button', { name: /sign in/i }).length).toBeGreaterThan(0);
    expect(screen.queryByRole('button', { name: /open app/i })).toBeNull();
  });

  it('renders every data-driven section card (8 features, 6 steps, 6 roles)', () => {
    const { container } = renderLanding();
    expect(container.querySelectorAll('.lp-feature')).toHaveLength(8);
    expect(container.querySelectorAll('.lp-step')).toHaveLength(6);
    expect(container.querySelectorAll('.lp-role')).toHaveLength(6);
  });

  it('exposes the scroll-spy nav links and all page sections', () => {
    const { container } = renderLanding();
    ['Platform', 'Spotlight', 'Workflows', 'Roles', 'Impact'].forEach((label) => {
      expect(screen.getAllByRole('button', { name: label }).length).toBeGreaterThan(0);
    });
    ['home', 'platform', 'spotlight', 'workflows', 'roles', 'impact'].forEach((id) => {
      expect(container.querySelector(`#${id}`)).not.toBeNull();
    });
  });
});
