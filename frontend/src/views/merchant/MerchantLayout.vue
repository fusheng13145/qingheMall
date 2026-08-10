<template>
  <div class="merchant-layout" data-skin="merchant">
    <ConsoleSidebar title="商家工作台" :sidebar-open="sidebarOpen" @close="sidebarOpen = false">
      <template #logo>
        <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 9l1.5-5h15L21 9"/><path d="M3 9v11a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1V9"/><path d="M3 9h18a2 2 0 0 1 0 4H3a2 2 0 0 1 0-4z"/>
        </svg>
      </template>
      <router-link to="/merchant" class="nav-item" :class="{ active: $route.name === 'MerchantOverview' }" @click="onNavClick">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
        </svg>
        <span>店铺概览</span>
      </router-link>
      <router-link to="/merchant/products" class="nav-item" :class="{ active: $route.name === 'MerchantProducts' }" @click="onNavClick">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
        </svg>
        <span>商品管理</span>
      </router-link>
      <router-link to="/merchant/orders" class="nav-item" :class="{ active: $route.name === 'MerchantOrders' }" @click="onNavClick">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
        </svg>
        <span>订单管理</span>
      </router-link>
    </ConsoleSidebar>

    <!-- 主内容区 -->
    <div class="merchant-main">
      <header class="merchant-topbar">
        <button class="menu-toggle" aria-label="打开菜单" @click="sidebarOpen = !sidebarOpen">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/>
          </svg>
        </button>
        <div class="topbar-right">
          <span class="merchant-name">{{ userStore.nickName }}</span>
          <router-link to="/" class="back-link">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>
            </svg>
            返回商城
          </router-link>
        </div>
      </header>
      <div class="merchant-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '../../stores/user'
import ConsoleSidebar from '../../components/common/ConsoleSidebar.vue'

const userStore = useUserStore()
const sidebarOpen = ref(false)

function onNavClick() {
  if (window.innerWidth <= 768) {
    sidebarOpen.value = false
  }
}
</script>

<style scoped>
.merchant-layout {
  display: flex;
  min-height: 100vh;
  background: var(--color-bg);
}

.merchant-main {
  flex: 1;
  margin-left: 240px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.merchant-topbar {
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

.merchant-name {
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

.merchant-content {
  flex: 1;
  padding: 24px;
}

@media (max-width: 768px) {
  .merchant-main {
    margin-left: 0;
  }

  .menu-toggle {
    display: flex;
  }

  .merchant-content {
    padding: 16px;
  }
}
</style>
