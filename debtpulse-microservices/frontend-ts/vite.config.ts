/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The frontend talks to the API Gateway (9090). During dev we proxy /api so the browser
// stays same-origin (no CORS needed locally); in production nginx proxies /api to the gateway.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
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
    environmentOptions: { jsdom: { url: 'http://localhost/' } },
    setupFiles: './src/test/setup.ts',
    css: false,
  },
});
