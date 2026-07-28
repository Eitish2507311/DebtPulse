import { describe, it, expect } from 'vitest';
import { inr, pct, initials, titleCase } from './format.js';

describe('format utils (pure functions)', () => {
  it('inr formats numbers as Indian-rupee currency and guards null/NaN', () => {
    expect(inr(250000)).toContain('₹');
    expect(inr(250000)).toBe('₹2,50,000');
    expect(inr(null)).toBe('—');
    expect(inr('abc')).toBe('abc');
  });

  it('pct renders one decimal place with a percent sign', () => {
    expect(pct(18)).toBe('18.0%');
    expect(pct('')).toBe('—');
  });

  it('initials takes up to two upper-cased leading letters', () => {
    expect(initials('Ravi Kumar')).toBe('RK');
    expect(initials('admin')).toBe('A');
    expect(initials(undefined)).toBe('?');
  });

  it('titleCase humanises SCREAMING_SNAKE enum names', () => {
    expect(titleCase('COLLECTIONS_AGENT')).toBe('Collections Agent');
    expect(titleCase('NPA')).toBe('Npa');
    expect(titleCase(null)).toBe('');
  });
});
