import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// ITE4120 course template frontend. The @helex/* packages resolve from
// node_modules (published @helex-solutions/* — see .npmrc); the dedupe list
// keeps exactly ONE React in the bundle, which the hooks rules require.
export default defineConfig({
  plugins: [react()],
  resolve: {
    dedupe: [
      'react',
      'react-dom',
      'react-redux',
      '@reduxjs/toolkit',
      'react-router-dom',
      '@tanstack/react-query',
    ],
  },
  server: {
    port: 18640,
    proxy: {
      // Everything the app calls goes through the dev proxy to the backend —
      // same-origin in the browser, so no CORS configuration anywhere.
      '/api': { target: 'http://localhost:18440', changeOrigin: true },
      '/mock-registry': { target: 'http://localhost:18440', changeOrigin: true },
    },
  },
});
