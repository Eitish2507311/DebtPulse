import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SpotlightCarousel from './SpotlightCarousel';

describe('SpotlightCarousel', () => {
  it('renders all five module slides and matching dots', () => {
    const { container } = render(<SpotlightCarousel />);
    expect(container.querySelectorAll('.lp-car-slide')).toHaveLength(5);
    expect(container.querySelectorAll('.lp-car-dot')).toHaveLength(5);
  });

  it('starts on the first slide', () => {
    render(<SpotlightCarousel />);
    const dots = screen.getAllByRole('tab');
    expect(dots[0]).toHaveAttribute('aria-selected', 'true');
    expect(dots[1]).toHaveAttribute('aria-selected', 'false');
  });

  it('advances with the next control and jumps via a dot', async () => {
    const user = userEvent.setup();
    render(<SpotlightCarousel />);
    const dots = screen.getAllByRole('tab');

    await user.click(screen.getByRole('button', { name: /next slide/i }));
    expect(dots[1]).toHaveAttribute('aria-selected', 'true');

    await user.click(dots[3]);
    expect(dots[3]).toHaveAttribute('aria-selected', 'true');
    expect(dots[1]).toHaveAttribute('aria-selected', 'false');
  });

  it('wraps to the last slide when going previous from the first', async () => {
    const user = userEvent.setup();
    render(<SpotlightCarousel />);
    const dots = screen.getAllByRole('tab');
    await user.click(screen.getByRole('button', { name: /previous slide/i }));
    expect(dots[4]).toHaveAttribute('aria-selected', 'true');
  });
});
