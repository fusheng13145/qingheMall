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

    <div class="report-section">
      <div class="report-header">
        <h2 class="section-title report-title">销售报表（已支付订单）</h2>
        <div class="report-range">
          <button
            v-for="d in [7, 30]"
            :key="d"
            class="range-btn"
            :class="{ active: reportDays === d }"
            @click="switchDays(d)"
          >近{{ d }}天</button>
        </div>
      </div>
      <div v-if="report.length > 0" class="report-body">
        <div class="bar-chart">
          <div v-for="item in report" :key="item.day" class="bar-col" :title="`${item.day} 销售额 ¥${Number(item.salesAmount || 0).toFixed(2)}`">
            <div class="bar-track">
              <div class="bar-fill" :style="{ height: barHeight(item.salesAmount) }"></div>
            </div>
            <span class="bar-day">{{ item.day.slice(5) }}</span>
          </div>
        </div>
        <table class="data-table report-table">
          <thead>
            <tr>
              <th>日期</th>
              <th>订单数</th>
              <th>销售额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in report" :key="item.day">
              <td>{{ item.day }}</td>
              <td>{{ item.orderCount }}</td>
              <td class="price">&yen;{{ Number(item.salesAmount || 0).toFixed(2) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="report-empty">近 {{ reportDays }} 天暂无已支付订单数据</div>
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
              <td>{{ (order.user && order.user.userName) || '-' }}</td>
              <td>{{ order.productName || '-' }}</td>
              <td class="price">&yen;{{ Number(order.totalPrice || 0).toFixed(2) }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(order.status)">
                  {{ getStatusText(order.status) }}
                </span>
              </td>
              <td class="time">{{ formatTime(order.gmtCreated) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDashboard, getOrderList, getSalesReport } from '../../api/admin'

const dashboard = ref({
  productCount: 0,
  orderCount: 0,
  userCount: 0,
  totalRevenue: 0,
  recentOrders: []
})

const report = ref([])
const reportDays = ref(7)
const maxSales = ref(0)

onMounted(async () => {
  try {
    const [statsRes, ordersRes] = await Promise.all([getDashboard(), getOrderList()])
    // getOrderList 返回 Paging<Order>，列表在 data 字段（此前误把 Paging 当数组 slice，最近订单恒为空）
    const orderPaging = ordersRes.data || {}
    dashboard.value = {
      ...statsRes.data,
      recentOrders: (orderPaging.data || []).slice(0, 5)
    }
  } catch (e) {
    console.error('加载仪表盘数据失败:', e)
  }
  loadReport(7)
})

async function loadReport(days) {
  try {
    const res = await getSalesReport(days)
    report.value = res.data || []
    maxSales.value = Math.max(0, ...report.value.map(i => Number(i.salesAmount || 0)))
  } catch (e) {
    console.error('加载销售报表失败:', e)
  }
}

function switchDays(days) {
  reportDays.value = days
  loadReport(days)
}

function barHeight(salesAmount) {
  const value = Number(salesAmount || 0)
  if (maxSales.value <= 0) return '0%'
  const percent = Math.max(4, Math.round((value / maxSales.value) * 100))
  return percent + '%'
}

function getStatusText(status) {
  const map = {
    'WAIT_BUYER_PAY': '待付款',
    'TRADE_PAID_SUCCESS': '待发货',
    'TRADE_CLOSED': '已关闭',
    'TRADE_PAID_FAILED': '支付失败',
    'TRADE_SHIPPED': '待收货',
    'TRADE_COMPLETED': '已完成',
    'TRADE_REFUNDING': '退款中',
    'TRADE_REFUNDED': '已退款'
  }
  return map[status] || status
}

function getStatusClass(status) {
  const map = {
    'WAIT_BUYER_PAY': 'warning',
    'TRADE_PAID_SUCCESS': 'success',
    'TRADE_SHIPPED': 'info',
    'TRADE_COMPLETED': 'success',
    'TRADE_REFUNDING': 'warning',
    'TRADE_REFUNDED': 'danger',
    'TRADE_CLOSED': 'danger',
    'TRADE_PAID_FAILED': 'danger'
  }
  return map[status] || ''
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  if (isNaN(d.getTime())) return String(time)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
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

/* 统计卡收敛到管理端「墨石 + 钢蓝」冷色家族，突出严谨与数据感 */
.stat-products {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

.stat-orders {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
}

.stat-users {
  background: linear-gradient(135deg, #64748b, #475569);
}

.stat-revenue {
  background: linear-gradient(135deg, #0f172a, #1e293b);
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

/* ========== 销售报表 ========== */
.report-section {
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  overflow: hidden;
  margin-bottom: 24px;
}

.report-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 24px;
  border-bottom: 1px solid var(--color-divider);
}

.report-title {
  border-bottom: none;
}

.report-range {
  display: flex;
  gap: 8px;
}

.range-btn {
  padding: 6px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.range-btn.active {
  border-color: var(--color-primary);
  background: var(--color-primary-50);
  color: var(--color-primary);
  font-weight: 600;
}

.report-body {
  padding: 24px;
}

.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  height: 160px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--color-divider);
  margin-bottom: 16px;
}

.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.bar-track {
  width: 100%;
  max-width: 48px;
  height: 120px;
  display: flex;
  align-items: flex-end;
  background: var(--color-bg-sunken);
  border-radius: 6px 6px 0 0;
  overflow: hidden;
}

.bar-fill {
  width: 100%;
  background: linear-gradient(180deg, var(--color-primary), var(--color-primary-dark));
  border-radius: 6px 6px 0 0;
  min-height: 2px;
  transition: height 0.4s ease;
}

.bar-day {
  font-size: 11px;
  color: var(--color-text-tertiary);
}

.report-table {
  margin-top: 8px;
}

.report-empty {
  padding: 40px 24px;
  text-align: center;
  color: var(--color-text-tertiary);
  font-size: 14px;
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
