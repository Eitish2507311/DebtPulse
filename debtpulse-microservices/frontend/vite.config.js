/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The frontend talks to the API Gateway (9090). During dev we proxy /api and the
// swagger/actuator paths so the browser stays same-origin (no CORS needed locally),
// and in production the app is served behind the same gateway/ingress.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://localhost:9090',
        changeOrigin: true,
      },
    },
  },
  // Component tests run under jsdom with RTL; setup registers jest-dom matchers.
  test: {
    globals: true,
    environment: 'jsdom',
    // A concrete origin so jsdom's localStorage works (opaque origins throw).
    environmentOptions: { jsdom: { url: 'http://localhost/' } },
    setupFiles: './src/test/setup.js',
    css: false,
  },
});
