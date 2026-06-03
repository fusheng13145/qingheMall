<template>
  <div class="pay-success-page">
    <div class="success-card">
      <div class="success-icon-wrapper">
        <div class="success-icon-bg"></div>
        <svg class="success-icon" viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
          <polyline points="22 4 12 14.01 9 11.01"/>
        </svg>
      </div>
      <h2 class="success-title">支付成功</h2>
      <p class="success-desc">感谢您的购买，商品将尽快为您发出</p>
      <p class="order-id" v-if="orderId">订单号: {{ orderId }}</p>
      <div class="success-actions">
        <router-link to="/orders" class="btn-action btn-primary">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          查看订单
        </router-link>
        <router-link to="/" class="btn-action btn-secondary">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
          返回首页
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const orderId = computed(() => {
  return route.query.orderId || ''
})
</script>

<style scoped>
.pay-success-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 520px;
  padding: 40px 20px;
}

.success-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: 56px 48px;
  text-align: center;
  box-shadow: var(--shadow-lg);
  max-width: 460px;
  width: 100%;
  border: 1px solid var(--color-border-light);
}

.success-icon-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.success-icon-bg {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: var(--color-primary-50);
  animation: pulse-ring 1.5s ease-out infinite;
}

@keyframes pulse-ring {
  0% {
    transform: scale(0.8);
    opacity: 1;
  }
  100% {
    transform: scale(1.4);
    opacity: 0;
  }
}

.success-icon {
  position: relative;
  z-index: 1;
  color: var(--color-primary);
  animation: icon-in 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
}

@keyframes icon-in {
  0% {
    transform: scale(0) rotate(-45deg);
    opacity: 0;
  }
  100% {
    transform: scale(1) rotate(0deg);
    opacity: 1;
  }
}

.success-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 8px;
}

.success-desc {
  font-size: 15px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.order-id {
  font-size: 13px;
  color: var(--color-text-tertiary);
  margin-bottom: 32px;
  font-family: 'SF Mono', 'Fira Code', monospace;
}

.success-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.btn-action {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 600;
  transition: all var(--transition-fast);
}

.btn-primary {
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: #fff;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);
}

.btn-primary:hover {
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.35);
  transform: translateY(-1px);
}

.btn-secondary {
  background: var(--color-bg);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

.btn-secondary:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: var(--color-primary-50);
}
</style>
