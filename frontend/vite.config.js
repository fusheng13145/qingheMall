/// <reference types="vitest" />
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 本地上传/种子图片（RR-5 本地化）同样代理到后端，开发环境可直接渲染
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    // P2：vendor 分包——框架/HTTP 库独立 chunk，提升首屏缓存命中与弱网加载
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          axios: ['axios']
        }
      }
    },
    sourcemap: false,
    minify: 'esbuild'
  },
  test: {
    environment: 'jsdom',
    globals: true,
    // P2-16：覆盖率门禁——低于阈值即测试失败，防止覆盖率回退
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{js,vue}'],
      exclude: ['src/**/*.spec.js', 'src/main.js'],
      reporters: ['text', 'lcov'],
      thresholds: {
        statements: 85,
        lines: 85,
        branches: 80,
        functions: 75
      }
    }
  }
})
