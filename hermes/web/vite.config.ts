import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        manualChunks(id) {
          const moduleId = id.replaceAll('\\', '/');
          if (!moduleId.includes('node_modules')) {
            return undefined;
          }
          if (moduleId.includes('/@tabler/icons-react/')) {
            return 'vendor-icons';
          }
          if (moduleId.includes('/yaml/')) {
            return 'vendor-yaml';
          }
          if (
            moduleId.includes('/react/')
            || moduleId.includes('/react-dom/')
            || moduleId.includes('/scheduler/')
            || moduleId.includes('/@mantine/')
            || moduleId.includes('/@tiptap/')
            || moduleId.includes('/prosemirror-')
          ) {
            return 'vendor-ui';
          }
          return undefined;
        }
      }
    }
  },
  server: {
    proxy: {
      '/api/control': 'http://localhost:3000',
      '/api': 'http://localhost:8081'
    }
  }
});
