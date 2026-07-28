import '@testing-library/jest-dom/vitest';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

// jsdom's built-in Storage can be unavailable (opaque origin) — use a simple,
// deterministic in-memory implementation so preference persistence is testable.
function memoryStorage(): Storage {
  const map = new Map<string, string>();
  return {
    get length() { return map.size; },
    getItem: (k: string) => (map.has(k) ? map.get(k)! : null),
    setItem: (k: string, v: string) => { map.set(String(k), String(v)); },
    removeItem: (k: string) => { map.delete(k); },
    clear: () => map.clear(),
    key: (i: number) => [...map.keys()][i] ?? null,
  } as Storage;
}
const storage = memoryStorage();
Object.defineProperty(window, 'localStorage', { value: storage, configurable: true });
Object.defineProperty(globalThis, 'localStorage', { value: storage, configurable: true });

// jsdom lacks matchMedia (used by PreferencesProvider) — provide a minimal stub.
if (!window.matchMedia) {
  window.matchMedia = (query: string) => ({
    matches: false, media: query, onchange: null,
    addEventListener: () => {}, removeEventListener: () => {},
    addListener: () => {}, removeListener: () => {}, dispatchEvent: () => false,
  }) as MediaQueryList;
}

// jsdom implements neither IntersectionObserver (useScrollSpy) nor scrollIntoView.
class IntersectionObserverStub {
  constructor(_cb: IntersectionObserverCallback) {}
  observe() {}
  unobserve() {}
  disconnect() {}
  takeRecords() { return []; }
}
window.IntersectionObserver = IntersectionObserverStub as unknown as typeof IntersectionObserver;
globalThis.IntersectionObserver = IntersectionObserverStub as unknown as typeof IntersectionObserver;
if (!Element.prototype.scrollIntoView) Element.prototype.scrollIntoView = () => {};

// Unmount React trees between tests so they don't leak into one another.
afterEach(() => cleanup());
