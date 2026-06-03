<template>
  <div class="dashboard">
    <h1 class="page-title">仪表盘</h1>

    <div class="stats-grid">
      <div class="stat-card stat-products">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ dashboard.productCount }}</span>
          <span class="stat-label">商品总数</span>
        </div>
      </div>
      <div class="stat-card stat-orders">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ dashboard.orderCount }}</span>
          <span class="stat-label">订单总数</span>
        </div>
      </div>
      <div class="stat-card stat-users">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ dashboard.userCount }}</span>
          <span class="stat-label">用户总数</span>
        </div>
      </div>
      <div class="stat-card stat-revenue">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">&yen;{{ dashboard.totalRevenue?.toLocaleString() }}</span>
          <span class="stat-label">总收入</span>
        </div>
      </div>
    </div>

    <div class="recent-section">
      <h2 class="section-title">最近订单</h2>
      <div class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>订单号</th>
              <th>用户</th>
              <th>商品</th>
              <th>金额</th>
              <th>状态</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in dashboard.recentOrders" :key="order.id">
              <td class="mono">{{ order.orderNumber }}</td>
              <td>{{ order.userName }}</td>
              <td>{{ order.productName }}</td>
              <td class="price">&yen;{{ order.totalPrice?.toFixed(2) }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(order.status)">
                  {{ getStatusText(order.status) }}
                </span>
              </td>
              <td class="time">{{ order.createTime }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDashboard } from '../../api/admin'

const dashboard = ref({
  productCount: 0,
  orderCount: 0,
  userCount: 0,
  totalRevenue: 0,
  recentOrders: []
})

onMounted(async () => {
  try {
    const res = await getDashboard()
    if (res.data.code === 200) {
      dashboard.value = res.data.data
    }
  } catch (e) {
    // ignore
  }
})

function getStatusText(status) {
  const map = {
    'WAIT_BUYER_PAY': '待付款',
    'TRADE_PAID_SUCCESS': '已付款',
    'TRADE_CLOSED': '已关闭',
    'TRADE_FINISHED': '已完成'
  }
  return map[status] || status
}

function getStatusClass(status) {
  const map = {
    'WAIT_BUYER_PAY': 'warning',
    'TRADE_PAID_SUCCESS': 'success',
    'TRADE_CLOSED': 'danger',
    'TRADE_FINISHED': 'info'
  }
  return map[status] || ''
}
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  padding: 24px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  gap: 16px;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: -20px;
  right: -20px;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.stat-products {
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
}

.stat-orders {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
}

.stat-users {
  background: linear-gradient(135deg, #8b5cf6, #6d28d9);
}

.stat-revenue {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  opacity: 0.85;
}

.recent-section {
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  overflow: hidden;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  padding: 20px 24px;
  border-bottom: 1px solid var(--color-divider);
}

.table-wrapper {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  text-align: left;
  padding: 14px 24px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  background: var(--color-bg-sunken);
  border-bottom: 1px solid var(--color-border);
}

.data-table td {
  padding: 14px 24px;
  font-size: 14px;
  color: var(--color-text);
  border-bottom: 1px solid var(--color-divider);
}

.data-table tbody tr:nth-child(even) {
  background: var(--color-bg-sunken);
}

.data-table tbody tr:hover {
  background: var(--color-surface-hover);
}

.mono {
  font-family: var(--font-sans);
  font-variant-numeric: tabular-nums;
}

.price {
  color: var(--color-price);
  font-weight: 600;
}

.time {
  color: var(--color-text-secondary);
  font-size: 13px;
}

.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}

.status-badge.success {
  background: var(--color-success-light);
  color: var(--color-success);
}

.status-badge.warning {
  background: var(--color-warning-light);
  color: var(--color-warning);
}

.status-badge.danger {
  background: var(--color-danger-light);
  color: var(--color-danger);
}

.status-badge.info {
  background: var(--color-primary-50);
  color: var(--color-primary);
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .stat-card {
    padding: 16px;
  }

  .stat-value {
    font-size: 22px;
  }

  .stat-icon {
    width: 44px;
    height: 44px;
  }
}
</style>
