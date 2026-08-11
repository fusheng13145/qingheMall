<script setup>
/**
 * 全局 Toast 渲染容器（P2-13）。
 * 在 App.vue 挂载一次，渲染 utils/toast.js 的响应式队列；
 * 点击单条可提前关闭。配色沿用主题 CSS 变量，自动适配亮/暗色。
 */
import { toasts, dismissToast } from '../../utils/toast'
</script>

<template>
  <div class="toast-stack" role="status" aria-live="polite">
    <TransitionGroup name="toast">
      <div
        v-for="t in toasts"
        :key="t.id"
        :class="['toast-item', `toast-${t.type}`]"
        @click="dismissToast(t.id)"
      >
        {{ t.message }}
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-stack {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  pointer-events: none;
}

.toast-item {
  pointer-events: auto;
  cursor: pointer;
  max-width: min(80vw, 480px);
  padding: 10px 18px;
  border-radius: var(--radius-md, 12px);
  color: #fff;
  font-size: 14px;
  line-height: 1.5;
  text-align: center;
  word-break: break-all;
  box-shadow: var(--shadow-lg, 0 10px 15px -3px rgba(0, 0, 0, 0.1));
}

.toast-success {
  background: var(--color-success, #10b981);
}

.toast-error {
  background: var(--color-danger, #ef4444);
}

.toast-warning {
  background: var(--color-warning, #f59e0b);
}

.toast-info {
  background: var(--color-info, #3b82f6);
}

.toast-enter-active,
.toast-leave-active {
  transition: all var(--transition-base, 0.25s ease);
}

.toast-enter-from {
  opacity: 0;
  transform: translateY(-12px);
}

.toast-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}
</style>
