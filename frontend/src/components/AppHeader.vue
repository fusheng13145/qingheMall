<template>
  <header class="app-header">
    <div class="header-inner">
      <div class="header-left">
        <router-link to="/" class="logo">
          <span class="logo-icon">
            <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
              <polyline points="9 22 9 12 15 12 15 22"/>
            </svg>
          </span>
          <span class="logo-text">青禾商城</span>
        </router-link>
        <nav class="nav-links">
          <router-link to="/" class="nav-link" exact-active-class="active">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
            <span>首页</span>
          </router-link>
          <router-link to="/products" class="nav-link" active-class="active">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>
            <span>商品</span>
          </router-link>
          <router-link to="/orders" class="nav-link" active-class="active">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            <span>订单</span>
          </router-link>
        </nav>
      </div>
      <div class="header-right">
        <button class="theme-toggle" @click="themeStore.toggleTheme()" :title="isDark ? '切换到亮色模式' : '切换到暗色模式'">
          <transition name="theme-icon" mode="out-in">
            <svg v-if="isDark" key="sun" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
            </svg>
            <svg v-else key="moon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
            </svg>
          </transition>
        </button>
        <div class="auth-section">
          <template v-if="userStore.isLoggedIn">
            <div class="user-info">
              <div class="avatar">{{ userStore.nickName?.charAt(0) || 'U' }}</div>
              <span class="user-name">{{ userStore.nickName }}</span>
              <router-link v-if="userStore.isAdmin" to="/admin" class="admin-link">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
                </svg>
                管理后台
              </router-link>
            </div>
            <button class="btn-logout" @click="handleLogout">退出</button>
          </template>
          <template v-else>
            <router-link to="/login" class="btn-login">登录</router-link>
            <router-link to="/register" class="btn-register">注册</router-link>
          </template>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '../stores/user'
import { useThemeStore } from '../stores/theme'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const themeStore = useThemeStore()
const router = useRouter()

const isDark = computed(() => themeStore.getEffectiveTheme() === 'dark')

async function handleLogout() {
  await userStore.logout()
  router.push('/')
}
</script>

<style scoped>
.app-header {
  background: var(--color-bg-elevated);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  transition: background-color var(--transition-base), border-color var(--transition-base);
}

.header-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 20px;
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 36px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--color-primary);
  transition: opacity var(--transition-fast);
}

.logo:hover {
  opacity: 0.85;
}

.logo-icon {
  display: flex;
  align-items: center;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 1px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-links {
  display: flex;
  gap: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.nav-link:hover {
  color: var(--color-text);
  background: var(--color-bg-overlay);
}

.nav-link.active {
  color: var(--color-primary);
  background: var(--color-primary-50);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  background: var(--color-bg-overlay);
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}

.theme-toggle:hover {
  color: var(--color-primary);
  background: var(--color-primary-50);
}

.theme-icon-enter-active,
.theme-icon-leave-active {
  transition: all 0.2s ease;
}

.theme-icon-enter-from {
  opacity: 0;
  transform: rotate(-90deg) scale(0.6);
}

.theme-icon-leave-to {
  opacity: 0;
  transform: rotate(90deg) scale(0.6);
}

.auth-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  color: var(--color-text-inverse);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  color: var(--color-text);
  font-weight: 500;
}

.admin-link {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-primary);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  background: var(--color-primary-50);
  transition: all var(--transition-fast);
}

.admin-link:hover {
  background: var(--color-primary);
  color: #fff;
}

.btn-logout {
  padding: 6px 16px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-tertiary);
  background: transparent;
  border: 1px solid var(--color-border);
  transition: all var(--transition-fast);
}

.btn-logout:hover {
  color: var(--color-danger);
  border-color: var(--color-danger);
  background: var(--color-danger-light);
}

.btn-login {
  padding: 6px 18px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}

.btn-login:hover {
  color: var(--color-primary);
  background: var(--color-primary-50);
}

.btn-register {
  padding: 6px 18px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-inverse);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  transition: all var(--transition-fast);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);
}

.btn-register:hover {
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.35);
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .nav-link span {
    display: none;
  }

  .nav-links {
    gap: 2px;
  }

  .nav-link {
    padding: 8px 10px;
  }

  .user-name {
    display: none;
  }

  .header-left {
    gap: 16px;
  }
}
</style>
