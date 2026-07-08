import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:9001',
        changeOrigin: true
      },
      '/actuator': {
        target: 'http://localhost:9001',
        changeOrigin: true
      },
      '/admin': {
        target: 'http://localhost:9001',
        changeOrigin: true
      }
    }
  }
})
