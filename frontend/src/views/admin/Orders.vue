<template>
  <div class="orders-page">
    <h1 class="page-title">订单管理</h1>

    <div class="filter-bar">
      <button
        v-for="f in filters"
        :key="f.value"
        class="filter-btn"
        :class="{ active: currentFilter === f.value }"
        @click="currentFilter = f.value"
      >
        {{ f.label }}
      </button>
    </div>

    <div class="table-section">
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
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in filteredOrders" :key="order.id">
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
              <td class="actions">
                <select
                  class="status-select"
                  :value="order.status"
                  @change="handleStatusChange(order, $event)"
                >
                  <option value="WAIT_BUYER_PAY">待付款</option>
                  <option value="TRADE_PAID_SUCCESS">已付款</option>
                  <option value="TRADE_FINISHED">已完成</option>
                  <option value="TRADE_CLOSED">已关闭</option>
                </select>
              </td>
            </tr>
            <tr v-if="filteredOrders.length === 0">
              <td colspan="7" class="empty">暂无订单数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOrderList, updateOrderStatus } from '../../api/admin'

const orders = ref([])
const currentFilter = ref('ALL')

const filters = [
  { label: '全部', value: 'ALL' },
  { label: '待付款', value: 'WAIT_BUYER_PAY' },
  { label: '已付款', value: 'TRADE_PAID_SUCCESS' },
  { label: '已完成', value: 'TRADE_FINISHED' },
  { label: '已关闭', value: 'TRADE_CLOSED' }
]

const filteredOrders = computed(() => {
  if (currentFilter.value === 'ALL') return orders.value
  return orders.value.filter(o => o.status === currentFilter.value)
})

onMounted(async () => {
  await loadOrders()
})

async function loadOrders() {
  try {
    const res = await getOrderList()
    if (res.data.code === 200) {
      orders.value = res.data.data || []
    }
  } catch (e) {
    // ignore
  }
}

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

async function handleStatusChange(order, event) {
  const newStatus = event.target.value
  if (!confirm(`确定要将订单 ${order.orderNumber} 状态修改为「${getStatusText(newStatus)}」吗？`)) {
    event.target.value = order.status
    return
  }
  try {
    await updateOrderStatus(order.orderNumber, newStatus)
    order.status = newStatus
  } catch (e) {
    event.target.value = order.status
  }
}
</script>

<style scoped>
.orders-page {
  max-width: 1200px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 24px;
}

.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-btn {
  padding: 8px 18px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  transition: all var(--transition-fast);
}

.filter-btn:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.filter-btn.active {
  color: #fff;
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.table-section {
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  overflow: hidden;
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

.actions {
  min-width: 120px;
}

.status-select {
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 13px;
  outline: none;
  transition: border-color var(--transition-fast);
  cursor: pointer;
}

.status-select:focus {
  border-color: var(--color-primary);
}

.empty {
  text-align: center;
  color: var(--color-text-tertiary);
  padding: 40px 24px !important;
}
</style>
