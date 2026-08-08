<template>
  <div class="orders">
    <div class="page-header">
      <h2 class="page-title">订单管理</h2>
    </div>

    <div class="tabs">
      <button v-for="tab in tabs" :key="tab.value" class="tab" :class="{ active: status === tab.value }" @click="switchTab(tab.value)">
        {{ tab.label }}
      </button>
    </div>

    <div class="table-card">
      <table class="data-table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>商品</th>
            <th>买家</th>
            <th>金额</th>
            <th>状态</th>
            <th>下单时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="o in orders" :key="o.orderNumber">
            <td class="mono">{{ o.orderNumber }}</td>
            <td>
              <div class="cell-product">
                <img v-if="o.productImg" :src="o.productImg" class="thumb" alt="" />
                <div v-else class="thumb thumb-empty"></div>
                <div>
                  <div class="p-name">{{ o.productName }}</div>
                  <div class="p-qty">x{{ o.quantity }} · {{ o.productDetail ? '规格' + o.productDetail.size : '' }}</div>
                </div>
              </div>
            </td>
            <td>{{ o.user ? o.user.nickName || o.user.userName : '-' }}</td>
            <td>¥{{ Number(o.totalPrice).toFixed(2) }}</td>
            <td><span class="tag" :class="'tag-' + statusClass(o.status)">{{ statusText(o.status) }}</span></td>
            <td class="time">{{ formatTime(o.gmtCreated) }}</td>
            <td>
              <div class="row-actions">
                <button v-if="o.status === 'TRADE_PAID_SUCCESS'" class="link-btn" @click="ship(o)">发货</button>
                <template v-if="o.status === 'TRADE_REFUNDING'">
                  <button class="link-btn" @click="refund(o, true)">同意退款</button>
                  <button class="link-btn danger" @click="refund(o, false)">拒绝</button>
                </template>
                <span v-if="!canOperate(o.status)" class="muted">-</span>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && orders.length === 0">
            <td colspan="7" class="empty-row">暂无订单</td>
          </tr>
        </tbody>
      </table>
      <div class="pager">
        <button class="btn-ghost" :disabled="pageNum <= 1" @click="load(pageNum - 1)">上一页</button>
        <span class="page-info">第 {{ pageNum }} / {{ totalPage || 1 }} 页（共 {{ totalCount }} 笔）</span>
        <button class="btn-ghost" :disabled="pageNum >= totalPage" @click="load(pageNum + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listMerchantOrders, merchantShip, merchantProcessRefund } from '../../api/merchant'

const tabs = [
  { label: '全部', value: '' },
  { label: '待付款', value: 'WAIT_BUYER_PAY' },
  { label: '待发货', value: 'TRADE_PAID_SUCCESS' },
  { label: '待收货', value: 'TRADE_SHIPPED' },
  { label: '已完成', value: 'TRADE_COMPLETED' },
  { label: '退款中', value: 'TRADE_REFUNDING' },
  { label: '已退款', value: 'TRADE_REFUNDED' }
]

const orders = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const totalPage = ref(1)
const totalCount = ref(0)
const status = ref('')

function statusText(s) {
  const map = {
    WAIT_BUYER_PAY: '待付款',
    TRADE_PAID_SUCCESS: '待发货',
    TRADE_CLOSED: '已关闭',
    TRADE_PAID_FAILED: '支付失败',
    TRADE_SHIPPED: '待收货',
    TRADE_COMPLETED: '已完成',
    TRADE_REFUNDING: '退款中',
    TRADE_REFUNDED: '已退款'
  }
  return map[s] || s
}

function statusClass(s) {
  const on = ['TRADE_PAID_SUCCESS', 'TRADE_SHIPPED']
  const warn = ['WAIT_BUYER_PAY', 'TRADE_REFUNDING']
  if (on.includes(s)) return 'on'
  if (warn.includes(s)) return 'warn'
  if (s === 'TRADE_COMPLETED') return 'on'
  if (s === 'TRADE_REFUNDED') return 'off'
  return 'off'
}

function canOperate(s) {
  return s === 'TRADE_PAID_SUCCESS' || s === 'TRADE_REFUNDING'
}

function switchTab(v) {
  status.value = v
  load(1)
}

function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

async function load(page) {
  loading.value = true
  try {
    const res = await listMerchantOrders(page, pageSize.value, status.value)
    pageNum.value = page
    totalPage.value = res.data.totalPage
    totalCount.value = res.data.totalCount
    orders.value = res.data.data
  } catch (e) {
    alert(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function ship(o) {
  if (!confirm(`确认发货订单 ${o.orderNumber}？`)) return
  try {
    await merchantShip(o.orderNumber)
    load(pageNum.value)
  } catch (e) {
    alert(e.message || '发货失败')
  }
}

async function refund(o, approve) {
  const tip = approve ? '同意退款' : '拒绝退款'
  if (!confirm(`确认${tip}订单 ${o.orderNumber}？`)) return
  try {
    await merchantProcessRefund(o.orderNumber, approve)
    load(pageNum.value)
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
}

.tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.tab {
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  font-size: 13px;
  background: transparent;
  transition: all var(--transition-fast);
}

.tab:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.tab.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

.table-card {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px 14px;
  text-align: left;
  font-size: 14px;
  border-bottom: 1px solid var(--color-border);
}

.data-table th {
  color: var(--color-text-tertiary);
  font-weight: 500;
  font-size: 13px;
  background: var(--color-bg);
}

.mono {
  font-family: var(--font-mono, ui-monospace, 'SF Mono', Consolas, monospace);
  font-size: 13px;
}

.cell-product {
  display: flex;
  align-items: center;
  gap: 10px;
}

.thumb {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  background: var(--color-bg);
}

.thumb-empty {
  border: 1px dashed var(--color-border);
}

.p-name {
  font-weight: 500;
  color: var(--color-text);
}

.p-qty {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

.tag {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.tag-on {
  background: var(--color-success-light, #E1F5EE);
  color: var(--color-primary);
}

.tag-warn {
  background: var(--color-warning-light, #FAEEDA);
  color: var(--color-warning, #854F0B);
}

.tag-off {
  background: var(--color-bg-overlay);
  color: var(--color-text-tertiary);
}

.time {
  color: var(--color-text-tertiary);
  font-size: 13px;
}

.row-actions {
  display: flex;
  gap: 10px;
}

.link-btn {
  color: var(--color-primary);
  font-size: 13px;
  padding: 2px 0;
}

.link-btn.danger {
  color: var(--color-danger);
}

.link-btn:hover {
  opacity: 0.75;
}

.muted {
  color: var(--color-text-tertiary);
}

.empty-row {
  text-align: center;
  color: var(--color-text-tertiary);
  padding: 40px !important;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 14px;
}

.page-info {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.btn-ghost {
  padding: 8px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  font-size: 13px;
  background: transparent;
}

.btn-ghost:hover:not(:disabled) {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.btn-ghost:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
