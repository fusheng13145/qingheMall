<template>
  <div class="app" :data-theme="effectiveTheme" data-skin="user">
    <!-- 用户端壳：仅前台路由渲染头部/页脚；/admin、/merchant 由各自布局接管 -->
    <AppHeader v-if="!isConsoleRoute" />
    <main class="main-content" :class="{ 'console-shell': isConsoleRoute }">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <AppFooter v-if="!isConsoleRoute" />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './components/AppHeader.vue'
import AppFooter from './components/AppFooter.vue'
import { useThemeStore } from './stores/theme'
import { useUserStore } from './stores/user'

const route = useRoute()
const themeStore = useThemeStore()
const userStore = useUserStore()

const effectiveTheme = computed(() => themeStore.getEffectiveTheme())

// 控制台类路由（管理后台 / 商家工作台）：隐藏用户端头部与页脚，由各布局独立 chrome 接管
const isConsoleRoute = computed(
  () => route.path.startsWith('/admin') || route.path.startsWith('/merchant')
)

onMounted(() => {
  userStore.checkLogin()
  themeStore.applyTheme()
})
</script>

<style>
/* ========== 亮色主题变量 ========== */
:root,
[data-theme="light"] {
  --color-primary: #10b981;
  --color-primary-light: #34d399;
  --color-primary-dark: #059669;
  --color-primary-50: #ecfdf5;
  --color-primary-100: #d1fae5;
  --color-primary-200: #a7f3d0;

  --color-bg: #f8fafc;
  --color-bg-elevated: #ffffff;
  --color-bg-sunken: #f1f5f9;
  --color-bg-overlay: rgba(0, 0, 0, 0.04);

  --color-surface: #ffffff;
  --color-surface-hover: #f8fafc;
  --color-surface-active: #f1f5f9;

  --color-text: #0f172a;
  --color-text-secondary: #475569;
  --color-text-tertiary: #94a3b8;
  --color-text-inverse: #ffffff;

  --color-border: #e2e8f0;
  --color-border-light: #f1f5f9;
  --color-divider: #f1f5f9;

  --color-danger: #ef4444;
  --color-danger-light: #fef2f2;
  --color-warning: #f59e0b;
  --color-warning-light: #fffbeb;
  --color-success: #10b981;
  --color-success-light: #ecfdf5;
  --color-info: #3b82f6;
  --color-info-light: #eff6ff;

  --color-price: #ef4444;

  --shadow-xs: 0 1px 2px rgba(0, 0, 0, 0.04);
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.07), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.08), 0 4px 6px -4px rgba(0, 0, 0, 0.04);
  --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.04);

  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 24px;
  --radius-full: 9999px;

  --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  --font-display: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', sans-serif;

  --transition-fast: 150ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-base: 250ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow: 350ms cubic-bezier(0.4, 0, 0.2, 1);

  --header-height: 64px;
  --max-width: 1240px;
}

/* ========== 暗色主题变量 ========== */
[data-theme="dark"] {
  --color-primary: #34d399;
  --color-primary-light: #6ee7b7;
  --color-primary-dark: #10b981;
  --color-primary-50: rgba(16, 185, 129, 0.08);
  --color-primary-100: rgba(16, 185, 129, 0.15);
  --color-primary-200: rgba(16, 185, 129, 0.25);

  --color-bg: #0c0f1a;
  --color-bg-elevated: #151929;
  --color-bg-sunken: #0a0d17;
  --color-bg-overlay: rgba(255, 255, 255, 0.03);

  --color-surface: #1a1f35;
  --color-surface-hover: #222845;
  --color-surface-active: #282e4a;

  --color-text: #f1f5f9;
  --color-text-secondary: #94a3b8;
  --color-text-tertiary: #64748b;
  --color-text-inverse: #0f172a;

  --color-border: #2a3050;
  --color-border-light: #1e2440;
  --color-divider: #1e2440;

  --color-danger: #f87171;
  --color-danger-light: rgba(239, 68, 68, 0.12);
  --color-warning: #fbbf24;
  --color-warning-light: rgba(245, 158, 11, 0.12);
  --color-success: #34d399;
  --color-success-light: rgba(16, 185, 129, 0.12);
  --color-info: #60a5fa;
  --color-info-light: rgba(59, 130, 246, 0.12);

  --color-price: #f87171;

  --shadow-xs: 0 1px 2px rgba(0, 0, 0, 0.2);
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.3), 0 1px 2px rgba(0, 0, 0, 0.2);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.35), 0 2px 4px -2px rgba(0, 0, 0, 0.25);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.4), 0 4px 6px -4px rgba(0, 0, 0, 0.3);
  --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.45), 0 8px 10px -6px rgba(0, 0, 0, 0.35);
}

/* ========== 全局重置 ========== */
*,
*::before,
*::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  scroll-behavior: smooth;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

body {
  font-family: var(--font-sans);
  background-color: var(--color-bg);
  color: var(--color-text);
  min-height: 100vh;
  transition: background-color var(--transition-slow), color var(--transition-slow);
  line-height: 1.6;
}

a {
  text-decoration: none;
  color: inherit;
}

button {
  cursor: pointer;
  border: none;
  outline: none;
  font-family: inherit;
}

input {
  outline: none;
  font-family: inherit;
}

img {
  max-width: 100%;
  display: block;
}

/* ========== 布局 ========== */
.app {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  width: 100%;
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 24px 20px;
}

/* 控制台类路由（/admin、/merchant）：不套用商城内容区宽度约束 */
.main-content.console-shell {
  max-width: none;
  padding: 0;
}

/* ========== 页面切换动画 ========== */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ========== 滚动条美化 ========== */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: var(--color-text-tertiary);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--color-text-secondary);
}

/* ========== 通用工具类 ========== */
.glass {
  background: var(--color-surface);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}
</style>
