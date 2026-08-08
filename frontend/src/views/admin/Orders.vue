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
              <td>{{ (order.user && order.user.userName) || '-' }}</td>
              <td>{{ order.productName || '-' }}</td>
              <td class="price">&yen;{{ Number(order.totalPrice || 0).toFixed(2) }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(order.status)">
                  {{ getStatusText(order.status) }}
                </span>
              </td>
              <td class="time">{{ formatTime(order.gmtCreated) }}</td>
              <td class="actions">
                <div class="action-group">
                  <button v-if="order.status === 'TRADE_PAID_SUCCESS'" class="btn-ship" @click="handleShip(order)">发货</button>
                  <template v-if="order.status === 'TRADE_REFUNDING'">
                    <button class="btn-approve" @click="handleRefund(order, true)">同意</button>
                    <button class="btn-reject" @click="handleRefund(order, false)">拒绝</button>
                  </template>
                  <select
                    class="status-select"
                    :value="order.status"
                    @change="handleStatusChange(order, $event)"
                  >
                    <option value="WAIT_BUYER_PAY">待付款</option>
                    <option value="TRADE_PAID_SUCCESS">待发货</option>
                    <option value="TRADE_SHIPPED">待收货</option>
                    <option value="TRADE_COMPLETED">已完成</option>
                    <option value="TRADE_REFUNDING">退款中</option>
                    <option value="TRADE_REFUNDED">已退款</option>
                    <option value="TRADE_CLOSED">已关闭</option>
                    <option value="TRADE_PAID_FAILED">支付失败</option>
                  </select>
                </div>
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
import { getOrderList, updateOrderStatus, shipOrder, processRefund } from '../../api/admin'

const orders = ref([])
const currentFilter = ref('ALL')

const filters = [
  { label: '全部', value: 'ALL' },
  { label: '待付款', value: 'WAIT_BUYER_PAY' },
  { label: '待发货', value: 'TRADE_PAID_SUCCESS' },
  { label: '待收货', value: 'TRADE_SHIPPED' },
  { label: '已完成', value: 'TRADE_COMPLETED' },
  { label: '退款中', value: 'TRADE_REFUNDING' },
  { label: '已退款', value: 'TRADE_REFUNDED' },
  { label: '已关闭', value: 'TRADE_CLOSED' },
  { label: '支付失败', value: 'TRADE_PAID_FAILED' }
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
    orders.value = res.data || []
  } catch (e) {
    alert('加载订单列表失败：' + (e.message || '请稍后重试'))
  }
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
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
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
    alert('状态修改失败：' + (e.message || '请稍后重试'))
  }
}

async function handleShip(order) {
  if (!confirm(`确认将订单 ${order.orderNumber} 标记为已发货吗？`)) return
  try {
    await shipOrder(order.orderNumber)
    order.status = 'TRADE_SHIPPED'
  } catch (e) {
    alert('发货失败：' + (e.message || '请稍后重试'))
  }
}

async function handleRefund(order, approve) {
  const tip = approve ? '同意退款' : '拒绝退款（订单将回退为已付款）'
  if (!confirm(`确认${tip}：订单 ${order.orderNumber} 吗？`)) return
  try {
    await processRefund(order.orderNumber, approve)
    order.status = approve ? 'TRADE_REFUNDED' : 'TRADE_PAID_SUCCESS'
  } catch (e) {
    alert('操作失败：' + (e.message || '请稍后重试'))
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
  min-width: 240px;
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

.action-group {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.btn-ship,
.btn-approve,
.btn-reject {
  padding: 5px 12px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all var(--transition-fast);
}

.btn-ship {
  color: #fff;
  background: var(--color-primary);
}

.btn-ship:hover {
  background: var(--color-primary-dark);
}

.btn-approve {
  color: var(--color-success);
  border-color: var(--color-success);
  background: var(--color-success-light);
}

.btn-approve:hover {
  background: var(--color-success);
  color: #fff;
}

.btn-reject {
  color: var(--color-danger);
  border-color: var(--color-danger);
  background: var(--color-danger-light);
}

.btn-reject:hover {
  background: var(--color-danger);
  color: #fff;
}

.empty {
  text-align: center;
  color: var(--color-text-tertiary);
  padding: 40px 24px !important;
}
</style>
