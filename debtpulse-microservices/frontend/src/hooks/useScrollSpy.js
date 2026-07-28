import { useEffect, useState } from 'react';

/**
 * Tracks which in-page section is currently in view (for highlighting nav links).
 * Uses IntersectionObserver so it stays cheap and doesn't run layout on every scroll.
 *
 * @param {string[]} ids  DOM ids of the sections to watch, in document order.
 * @returns {string} the id of the section considered "active".
 */
export function useScrollSpy(ids) {
  const [active, setActive] = useState(ids[0]);
  const key = ids.join(',');

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        // Pick the entry nearest the top of the viewport that is intersecting.
        const visible = entries
          .filter((e) => e.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
        if (visible[0]) setActive(visible[0].target.id);
      },
      // A section counts as active once it crosses the vertical middle of the screen.
      { rootMargin: '-45% 0px -50% 0px', threshold: 0 },
    );
    ids.forEach((id) => {
      const el = document.getElementById(id);
      if (el) observer.observe(el);
    });
    return () => observer.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  return active;
}

/**
 * True once the window has scrolled past `threshold` px — used to switch the
 * landing nav from transparent (over the hero) to a solid, elevated bar.
 */
export function useScrolled(threshold = 24) {
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > threshold);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, [threshold]);
  return scrolled;
}
