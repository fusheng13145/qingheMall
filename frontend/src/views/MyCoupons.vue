<template>
  <div class="my-coupons-page">
    <div class="page-header">
      <h1 class="page-title">我的优惠券</h1>
      <p class="page-desc">查看您领取的优惠券</p>
    </div>

    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-btn"
        :class="{ active: activeTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="loading" class="state">加载中...</div>
    <div v-else-if="coupons.length === 0" class="state">
      {{ activeTab === 'UNUSED' ? '暂无未使用的优惠券，去' : '暂无相关优惠券' }}
      <router-link v-if="activeTab === 'UNUSED'" to="/coupons" class="link">领券中心</router-link>
    </div>
    <div v-else class="coupon-list">
      <div
        v-for="uc in coupons"
        :key="uc.id"
        class="coupon-card"
        :class="statusClass(uc.status)"
      >
        <div class="coupon-left">
          <template v-if="uc.couponType === 'FULL_REDUCTION'">
            <span class="symbol">¥</span><span class="big">{{ formatPrice(uc.couponAmount) }}</span>
          </template>
          <template v-else>
            <span class="big">{{ (Number(uc.couponDiscount) * 10).toFixed(1) }}</span><span class="symbol">折</span>
          </template>
        </div>
        <div class="coupon-right">
          <div class="coupon-name">{{ uc.couponName }}</div>
          <div class="coupon-rule">{{ ruleText(uc) }}</div>
          <div class="coupon-time">有效期至 {{ formatDateTime(uc.couponEndTime) }}</div>
          <div class="coupon-status">{{ statusText(uc.status) }}</div>
          <div v-if="uc.status === 'USED' && uc.orderNumber" class="coupon-order">
            已用于订单 {{ uc.orderNumber }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { myCoupons } from '../api/coupon'
import { couponRuleText } from '../utils/coupon'

const tabs = [
  { label: '未使用', value: 'UNUSED' },
  { label: '已使用', value: 'USED' },
  { label: '已过期', value: 'EXPIRED' },
  { label: '全部', value: '' }
]

const activeTab = ref('UNUSED')
const coupons = ref([])
const loading = ref(true)

function formatPrice(p) {
  return Number(p || 0).toFixed(0)
}

function ruleText(uc) {
  return couponRuleText(uc)
}

function formatDateTime(t) {
  if (!t) return '—'
  const d = new Date(t)
  if (isNaN(d.getTime())) return String(t)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function statusText(status) {
  const map = { UNUSED: '未使用', USED: '已使用', EXPIRED: '已过期', RELEASED: '已释放' }
  return map[status] || status
}

function statusClass(status) {
  const map = { UNUSED: 'st-unused', USED: 'st-used', EXPIRED: 'st-expired', RELEASED: 'st-expired' }
  return map[status] || ''
}

async function load() {
  loading.value = true
  try {
    const res = await myCoupons(activeTab.value)
    coupons.value = res.data || []
  } catch (e) {
    alert('加载失败：' + (e.message || '请稍后重试'))
  } finally {
    loading.value = false
  }
}

function switchTab(value) {
  if (activeTab.value === value) return
  activeTab.value = value
  load()
}

onMounted(load)
</script>

<style scoped>
.my-coupons-page {
  padding: 20px 0;
  max-width: 860px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}

.page-desc {
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  background: var(--color-surface);
  border-radius: var(--radius-sm);
  padding: 4px;
  box-shadow: var(--shadow-xs);
  border: 1px solid var(--color-border-light);
}

.tab-btn {
  flex: 1;
  padding: 10px 0;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
  border-radius: 6px;
  transition: all var(--transition-fast);
}

.tab-btn.active {
  color: var(--color-primary);
  font-weight: 600;
  background: var(--color-primary-50);
}

.state {
  text-align: center;
  color: var(--color-text-tertiary);
  padding: 60px 0;
  font-size: 15px;
}

.link {
  color: var(--color-primary);
  font-weight: 600;
}

.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.coupon-card {
  display: flex;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs);
  background: var(--color-surface);
}

.coupon-card.st-used,
.coupon-card.st-expired {
  opacity: 0.55;
}

.coupon-left {
  width: 110px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  flex-shrink: 0;
}

.coupon-left .big {
  font-size: 34px;
  font-weight: 800;
  line-height: 1;
}

.coupon-left .symbol {
  font-size: 16px;
  font-weight: 600;
}

.coupon-right {
  flex: 1;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.coupon-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

.coupon-rule {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.coupon-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.coupon-status {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
}

.st-used .coupon-status {
  color: var(--color-text-tertiary);
}

.st-expired .coupon-status {
  color: var(--color-danger);
}

.coupon-order {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
</style>
