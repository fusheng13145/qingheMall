<template>
  <div class="console-shell-root">
    <!-- 移动端遮罩 -->
    <div v-if="sidebarOpen" class="console-overlay" @click="$emit('close')"></div>

    <!-- 侧边栏：chrome 明暗与导航配色由当前皮肤令牌（data-skin）驱动 -->
    <aside class="console-sidebar" :class="{ open: sidebarOpen }">
      <div class="console-sidebar-header">
        <div class="console-sidebar-logo">
          <slot name="logo"></slot>
          <span class="console-sidebar-title">{{ title }}</span>
        </div>
        <button class="console-close" aria-label="关闭菜单" @click="$emit('close')">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
      <nav class="console-nav">
        <slot></slot>
      </nav>
    </aside>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  sidebarOpen: { type: Boolean, default: false }
})

defineEmits(['close'])
</script>

<!-- 全局样式（前缀 console- 防冲突）：供插槽内导航链接统一消费，无需 scoped -->
<style>
.console-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 199;
}

.console-sidebar {
  width: 240px;
  min-height: 100vh;
  background: var(--skin-chrome-bg);
  color: var(--skin-chrome-fg);
  border-right: 1px solid var(--skin-chrome-border);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 200;
  transition: transform var(--transition-base, 250ms cubic-bezier(0.4, 0, 0.2, 1));
}

.console-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid var(--skin-chrome-border);
}

.console-sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--brand);
}

.console-sidebar-title {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--brand), var(--brand-strong));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.console-close {
  display: none;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm, 8px);
  color: var(--skin-chrome-fg);
  background: transparent;
  transition: all var(--transition-fast, 150ms);
}

.console-close:hover {
  background: var(--skin-nav-hover-bg);
  color: var(--skin-nav-hover-fg);
}

.console-nav {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.console-nav .nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-sm, 8px);
  color: var(--skin-nav-fg);
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast, 150ms);
}

.console-nav .nav-item:hover {
  background: var(--skin-nav-hover-bg);
  color: var(--skin-nav-hover-fg);
}

.console-nav .nav-item.active {
  background: var(--skin-nav-active-bg);
  color: var(--skin-nav-active-fg);
  font-weight: 700;
}

@media (max-width: 768px) {
  .console-sidebar {
    transform: translateX(-100%);
  }

  .console-sidebar.open {
    transform: translateX(0);
  }

  .console-overlay {
    display: block;
  }

  .console-close {
    display: flex;
  }
}
</style>
