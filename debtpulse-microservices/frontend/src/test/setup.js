import '@testing-library/jest-dom/vitest';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

// jsdom's built-in Storage can be unavailable (opaque origin) — use a simple,
// deterministic in-memory implementation so preference persistence is testable.
class MemoryStorage {
  #store = new Map();
  get length() { return this.#store.size; }
  getItem(k) { return this.#store.has(k) ? this.#store.get(k) : null; }
  setItem(k, v) { this.#store.set(String(k), String(v)); }
  removeItem(k) { this.#store.delete(k); }
  clear() { this.#store.clear(); }
  key(i) { return [...this.#store.keys()][i] ?? null; }
}
const storage = new MemoryStorage();
Object.defineProperty(window, 'localStorage', { value: storage, configurable: true });
Object.defineProperty(globalThis, 'localStorage', { value: storage, configurable: true });

// jsdom lacks matchMedia (used by PreferencesProvider) — provide a minimal stub.
if (!window.matchMedia) {
  window.matchMedia = (query) => ({
    matches: false, media: query, onchange: null,
    addEventListener: () => {}, removeEventListener: () => {},
    addListener: () => {}, removeListener: () => {}, dispatchEvent: () => false,
  });
}

// jsdom implements neither IntersectionObserver (useScrollSpy) nor scrollIntoView.
class IntersectionObserverStub {
  constructor(cb) { this.cb = cb; }
  observe() {}
  unobserve() {}
  disconnect() {}
  takeRecords() { return []; }
}
window.IntersectionObserver = IntersectionObserverStub;
globalThis.IntersectionObserver = IntersectionObserverStub;
if (!Element.prototype.scrollIntoView) Element.prototype.scrollIntoView = () => {};

// Unmount React trees between tests so they don't leak into one another.
afterEach(() => cleanup());
