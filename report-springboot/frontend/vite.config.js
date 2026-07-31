import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => ({
  base: mode === 'production' ? '/report/' : '/',
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        // 拆分 vendor chunk：浏览器并行加载 + 长缓存（index.js 从 1.8MB 拆小）
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router'],
          'vendor-element': ['element-plus', '@element-plus/icons-vue'],
          'vendor-echarts': ['echarts', 'vue-echarts']
        }
      }
    }
  },
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
}))
