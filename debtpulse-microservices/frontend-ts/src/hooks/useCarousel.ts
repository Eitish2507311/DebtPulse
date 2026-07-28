import { useState, useEffect, useCallback } from 'react';

interface CarouselOptions {
  intervalMs?: number;
  auto?: boolean;
}

interface CarouselState {
  index: number;
  next: () => void;
  prev: () => void;
  goTo: (i: number) => void;
  paused: boolean;
  pause: () => void;
  resume: () => void;
}

/**
 * Headless carousel state: current index, prev/next/goTo, and auto-advance that
 * pauses on demand (hover/focus). Timer is cleaned up on every dependency change
 * so it never leaks or double-fires.
 */
export function useCarousel(count: number, { intervalMs = 5000, auto = true }: CarouselOptions = {}): CarouselState {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);

  const goTo = useCallback((i: number) => setIndex(((i % count) + count) % count), [count]);
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
