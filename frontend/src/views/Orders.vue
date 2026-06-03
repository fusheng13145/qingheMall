<template>
  <div class="orders-page">
    <div class="page-header">
      <h1 class="page-title">我的订单</h1>
      <p class="page-desc">查看和管理您的订单</p>
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
        <span class="tab-count" v-if="tab.value !== 'all' && getOrderCount(tab.value) > 0">
          {{ getOrderCount(tab.value) }}
        </span>
      </button>
    </div>
    <div class="order-list" v-if="filteredOrders.length > 0">
      <div class="order-card" v-for="order in filteredOrders" :key="order.id">
        <div class="order-header">
          <div class="order-no-wrapper">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            <span class="order-no">{{ order.orderNo || order.id }}</span>
          </div>
          <span class="order-status" :class="statusClass(order.status)">{{ statusText(order.status) }}</span>
        </div>
        <div class="order-body">
          <div class="order-product-info">
            <span class="product-name">{{ order.productName || order.name || '商品' }}</span>
            <span class="product-detail" v-if="order.size">规格: {{ order.size }}</span>
          </div>
          <div class="order-price">¥{{ order.totalPrice || order.price || 0 }}</div>
        </div>
        <div class="order-footer">
          <span class="order-time">{{ formatTime(order.createTime) }}</span>
          <div class="order-actions">
            <button
              v-if="order.status === 0"
              class="btn-pay"
              @click="handlePay(order)"
            >
              立即付款
            </button>
            <button
              v-if="order.status === 0"
              class="btn-cancel"
              @click="handleCancel(order)"
            >
              取消订单
            </button>
          </div>
        </div>
      </div>
    </div>
    <div class="loading-state" v-else-if="loading">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>
    <div class="empty-state" v-else>
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
      <p>暂无订单</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { queryRecentPaySuccess } from '../api/order'
import { payOrder } from '../api/payment'

const router = useRouter()

const orders = ref([])
const loading = ref(true)
const activeTab = ref('all')

const tabs = [
  { label: '全部', value: 'all' },
  { label: '待付款', value: 0 },
  { label: '已付款', value: 1 },
  { label: '已关闭', value: 2 }
]

const filteredOrders = computed(() => {
  if (activeTab.value === 'all') {
    return orders.value
  }
  return orders.value.filter(order => order.status === activeTab.value)
})

function getOrderCount(status) {
  return orders.value.filter(order => order.status === status).length
}

function switchTab(value) {
  activeTab.value = value
}

function statusText(status) {
  const map = {
    0: '待付款',
    1: '已付款',
    2: '已关闭'
  }
  return map[status] || '未知'
}

function statusClass(status) {
  const map = {
    0: 'status-pending',
    1: 'status-paid',
    2: 'status-closed'
  }
  return map[status] || ''
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

async function loadOrders() {
  loading.value = true
  try {
    const res = await queryRecentPaySuccess({})
    if (res.data.code === 200) {
      orders.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载订单失败:', error)
  } finally {
    loading.value = false
  }
}

async function handlePay(order) {
  try {
    const payRes = await payOrder({
      orderId: order.id || order.orderId,
      payType: 1
    })
    if (payRes.data.code === 200) {
      router.push({ name: 'PaySuccess', query: { orderId: order.id || order.orderId } })
    } else {
      alert('支付失败：' + (payRes.data.msg || '请稍后重试'))
    }
  } catch (error) {
    alert('支付请求失败，请稍后重试')
  }
}

function handleCancel(order) {
  if (confirm('确定要取消该订单吗？')) {
    order.status = 2
  }
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.orders-page {
  padding: 20px 0;
}

.page-header {
  margin-bottom: 28px;
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
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.tab-btn:hover {
  color: var(--color-text);
  background: var(--color-bg-overlay);
}

.tab-btn.active {
  color: var(--color-primary);
  font-weight: 600;
  background: var(--color-primary-50);
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--color-primary);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: var(--radius-full);
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  background: var(--color-surface);
  border-radius: var(--radius-md);
  padding: 20px 24px;
  box-shadow: var(--shadow-xs);
  border: 1px solid var(--color-border-light);
  transition: all var(--transition-fast);
}

.order-card:hover {
  box-shadow: var(--shadow-md);
  border-color: transparent;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--color-divider);
  margin-bottom: 14px;
}

.order-no-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text-tertiary);
}

.order-no {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-family: 'SF Mono', 'Fira Code', monospace;
}

.order-status {
  font-size: 13px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: var(--radius-full);
}

.status-pending {
  color: var(--color-warning);
  background: var(--color-warning-light);
}

.status-paid {
  color: var(--color-success);
  background: var(--color-success-light);
}

.status-closed {
  color: var(--color-text-tertiary);
  background: var(--color-bg-overlay);
}

.order-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}

.order-product-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name {
  font-size: 16px;
  color: var(--color-text);
  font-weight: 500;
}

.product-detail {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.order-price {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-price);
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  border-top: 1px solid var(--color-divider);
  margin-top: 10px;
}

.order-time {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.order-actions {
  display: flex;
  gap: 10px;
}

.btn-pay {
  padding: 8px 24px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  transition: all var(--transition-fast);
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.25);
}

.btn-pay:hover {
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.35);
  transform: translateY(-1px);
}

.btn-cancel {
  padding: 8px 24px;
  background: transparent;
  color: var(--color-text-tertiary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.btn-cancel:hover {
  border-color: var(--color-danger);
  color: var(--color-danger);
  background: var(--color-danger-light);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 80px 0;
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 80px 0;
  color: var(--color-text-tertiary);
}

.empty-state svg {
  opacity: 0.4;
}

.empty-state p {
  font-size: 15px;
}
</style>
