<template>
  <div class="admin-layout">
    <!-- 移动端遮罩 -->
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false"></div>

    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: !sidebarOpen }">
      <div class="sidebar-header">
        <div class="sidebar-logo">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
            <polyline points="9 22 9 12 15 12 15 22"/>
          </svg>
          <span class="sidebar-title">管理后台</span>
        </div>
        <button class="sidebar-close" @click="sidebarOpen = false">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
      <nav class="sidebar-nav">
        <router-link to="/admin" class="nav-item" :class="{ active: $route.name === 'AdminDashboard' }" @click="onNavClick">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
          </svg>
          <span>仪表盘</span>
        </router-link>
        <router-link to="/admin/products" class="nav-item" :class="{ active: $route.name === 'AdminProducts' }" @click="onNavClick">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
          </svg>
          <span>商品管理</span>
        </router-link>
        <router-link to="/admin/orders" class="nav-item" :class="{ active: $route.name === 'AdminOrders' }" @click="onNavClick">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
          </svg>
          <span>订单管理</span>
        </router-link>
        <router-link to="/admin/users" class="nav-item" :class="{ active: $route.name === 'AdminUsers' }" @click="onNavClick">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          <span>用户管理</span>
        </router-link>
      </nav>
    </aside>

    <!-- 主内容区 -->
    <div class="admin-main">
      <header class="admin-topbar">
        <button class="menu-toggle" @click="sidebarOpen = !sidebarOpen">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/>
          </svg>
        </button>
        <div class="topbar-right">
          <span class="admin-name">{{ userStore.nickName }}</span>
          <router-link to="/" class="back-link">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>
            </svg>
            返回商城
          </router-link>
        </div>
      </header>
      <div class="admin-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const sidebarOpen = ref(false)

function onNavClick() {
  if (window.innerWidth <= 768) {
    sidebarOpen.value = false
  }
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--color-bg);
}

.sidebar-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 199;
}

.sidebar {
  width: 240px;
  min-height: 100vh;
  background: var(--color-bg-elevated);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 200;
  transition: transform var(--transition-base);
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid var(--color-border);
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--color-primary);
}

.sidebar-title {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sidebar-close {
  display: none;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  background: transparent;
  transition: all var(--transition-fast);
}

.sidebar-close:hover {
  background: var(--color-bg-overlay);
  color: var(--color-text);
}

.sidebar-nav {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.nav-item:hover {
  background: var(--color-bg-overlay);
  color: var(--color-text);
}

.nav-item.active {
  background: var(--color-primary-50);
  color: var(--color-primary);
}

.admin-main {
  flex: 1;
  margin-left: 240px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.admin-topbar {
  height: 60px;
  background: var(--color-bg-elevated);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.menu-toggle {
  display: none;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  background: transparent;
  transition: all var(--transition-fast);
}

.menu-toggle:hover {
  background: var(--color-bg-overlay);
  color: var(--color-text);
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.admin-name {
  font-size: 14px;
  color: var(--color-text);
  font-weight: 500;
}

.back-link {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  transition: all var(--transition-fast);
}

.back-link:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: var(--color-primary-50);
}

.admin-content {
  flex: 1;
  padding: 24px;
}

@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
  }

  .sidebar.collapsed {
    transform: translateX(-100%);
  }

  .sidebar:not(.collapsed) {
    transform: translateX(0);
  }

  .sidebar-overlay {
    display: block;
  }

  .sidebar-close {
    display: flex;
  }

  .admin-main {
    margin-left: 0;
  }

  .menu-toggle {
    display: flex;
  }

  .admin-content {
    padding: 16px;
  }
}
</style>
