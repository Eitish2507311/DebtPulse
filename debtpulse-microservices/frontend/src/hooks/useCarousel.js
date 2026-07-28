import { useState, useEffect, useCallback } from 'react';

/**
 * Headless carousel state: current index, prev/next/goTo, and auto-advance that
 * pauses on demand (hover/focus). Timer is cleaned up on every dependency change
 * so it never leaks or double-fires.
 *
 * @param {number} count       number of slides
 * @param {object} [opts]
 * @param {number} [opts.intervalMs=5000]  auto-advance cadence
 * @param {boolean} [opts.auto=true]        enable auto-advance
 */
export function useCarousel(count, { intervalMs = 5000, auto = true } = {}) {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);

  const goTo = useCallback((i) => setIndex(((i % count) + count) % count), [count]);
  const next = useCallback(() => setIndex((i) => (i + 1) % count), [count]);
  const prev = useCallback(() => setIndex((i) => (i - 1 + count) % count), [count]);

  useEffect(() => {
    if (!auto || paused || count <= 1) return undefined;
    const timer = setInterval(() => setIndex((i) => (i + 1) % count), intervalMs);
    return () => clearInterval(timer);
  }, [auto, paused, count, intervalMs]);

  return {
    index, next, prev, goTo, paused,
    pause: () => setPaused(true),
    resume: () => setPaused(false),
  };
}
