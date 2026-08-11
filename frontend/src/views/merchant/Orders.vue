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
                <button v-if="o.status === 'TRADE_PAID_SUCCESS'" class="link-btn" @click="openShip(o)">发货</button>
                <button
                  v-if="['TRADE_SHIPPED', 'TRADE_COMPLETED', 'TRADE_REFUNDED'].includes(o.status)"
                  class="link-btn"
                  @click="openLogistics(o)"
                >物流</button>
                <button v-if="o.status === 'TRADE_REFUNDING'" class="link-btn danger" @click="openRefund(o)">退款审核</button>
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

    <!-- 发货弹窗（P2-18：录入承运商与运单号，建立物流档案） -->
    <div v-if="shipModalOpen" class="modal-overlay" @click.self="closeShip">
      <div class="modal">
        <h3 class="modal-title">订单发货</h3>
        <div class="modal-sub">订单号：{{ shipTarget ? shipTarget.orderNumber : '' }}</div>
        <div class="form-grid">
          <label class="form-label">承运商</label>
          <select v-model="shipCompany" class="form-input">
            <option v-for="c in companies" :key="c" :value="c">{{ c }}</option>
          </select>
          <label class="form-label">运单号</label>
          <input v-model="shipTracking" class="form-input" maxlength="64" placeholder="请填写快递运单号" />
        </div>
        <div class="modal-actions">
          <button class="btn-ghost" @click="closeShip">取消</button>
          <button class="btn-primary" :disabled="shipping" @click="submitShip">
            {{ shipping ? '发货中...' : '确认发货' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 物流弹窗（P2-18：轨迹时间线 + 手动推进） -->
    <div v-if="logisticsModalOpen" class="modal-overlay" @click.self="closeLogistics">
      <div class="modal">
        <h3 class="modal-title">物流跟踪</h3>
        <div class="modal-sub">订单号：{{ logisticsTarget ? logisticsTarget.orderNumber : '' }}</div>
        <div v-if="logisticsLoading" class="modal-loading">加载中...</div>
        <LogisticsTimeline v-else-if="logisticsData" :logistics="logisticsData" />
        <div v-else class="modal-loading">暂无物流信息</div>
        <div class="modal-actions">
          <button class="btn-ghost" @click="closeLogistics">关闭</button>
          <button
            v-if="logisticsData && logisticsData.status !== 'SIGNED'"
            class="btn-primary"
            :disabled="advancing"
            @click="advance"
          >
            {{ advancing ? '更新中...' : '更新物流状态' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 退款审核弹窗（P2-18：查看申请原因，填写审核意见） -->
    <div v-if="refundModalOpen" class="modal-overlay" @click.self="closeRefund">
      <div class="modal">
        <h3 class="modal-title">退款审核</h3>
        <div class="modal-sub">订单号：{{ refundTarget ? refundTarget.orderNumber : '' }}</div>
        <div v-if="refundLoading" class="modal-loading">加载中...</div>
        <template v-else>
          <div v-if="refundRequest" class="refund-info">
            <div class="refund-row">
              <span class="refund-label">申请类型</span>
              <span class="tag tag-warn">{{ refundTypeText(refundRequest.type) }}</span>
            </div>
            <div class="refund-row">
              <span class="refund-label">申请原因</span>
              <span class="refund-reason">{{ refundRequest.reason }}</span>
            </div>
            <div class="refund-row">
              <span class="refund-label">申请时间</span>
              <span class="time">{{ formatTime(refundRequest.gmtCreated) }}</span>
            </div>
          </div>
          <div v-else class="modal-loading">未找到待审核的退款申请</div>
          <textarea
            v-model="refundComment"
            class="comment-textarea"
            rows="3"
            maxlength="200"
            placeholder="审核意见（驳回时必填，将展示给用户）"
          ></textarea>
        </template>
        <div class="modal-actions">
          <button class="btn-ghost" @click="closeRefund">关闭</button>
          <button class="btn-danger" :disabled="reviewing || !refundRequest" @click="reviewRefund(false)">驳回</button>
          <button class="btn-primary" :disabled="reviewing || !refundRequest" @click="reviewRefund(true)">同意退款</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { toast, apiError } from '../../utils/toast'
import { listMerchantOrders, merchantShip, merchantProcessRefund, merchantAdvanceLogistics } from '../../api/merchant'
import { trackLogistics } from '../../api/logistics'
import { listOrderRefunds } from '../../api/refund'
import LogisticsTimeline from '../../components/common/LogisticsTimeline.vue'

const tabs = [
  { label: '全部', value: '' },
  { label: '待付款', value: 'WAIT_BUYER_PAY' },
  { label: '待发货', value: 'TRADE_PAID_SUCCESS' },
  { label: '待收货', value: 'TRADE_SHIPPED' },
  { label: '已完成', value: 'TRADE_COMPLETED' },
  { label: '退款中', value: 'TRADE_REFUNDING' },
  { label: '已退款', value: 'TRADE_REFUNDED' }
]

const companies = ['顺丰速运', '圆通速递', '中通快递', '韵达快递', '申通快递', '极兔速递', '其他']

const orders = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const totalPage = ref(1)
const totalCount = ref(0)
const status = ref('')

// 发货弹窗状态
const shipModalOpen = ref(false)
const shipTarget = ref(null)
const shipCompany = ref('顺丰速运')
const shipTracking = ref('')
const shipping = ref(false)

// 物流弹窗状态
const logisticsModalOpen = ref(false)
const logisticsTarget = ref(null)
const logisticsData = ref(null)
const logisticsLoading = ref(false)
const advancing = ref(false)

// 退款审核弹窗状态
const refundModalOpen = ref(false)
const refundTarget = ref(null)
const refundRequest = ref(null)
const refundLoading = ref(false)
const refundComment = ref('')
const reviewing = ref(false)

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

function refundTypeText(type) {
  if (type === 'REFUND_ONLY') return '仅退款'
  if (type === 'RETURN_REFUND') return '退货退款'
  return type || '-'
}

function canOperate(s) {
  return ['TRADE_PAID_SUCCESS', 'TRADE_SHIPPED', 'TRADE_COMPLETED', 'TRADE_REFUNDING', 'TRADE_REFUNDED'].includes(s)
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
    apiError(e, '加载失败')
  } finally {
    loading.value = false
  }
}

// ========== 发货（P2-18：录入物流信息） ==========

function openShip(o) {
  shipTarget.value = o
  shipCompany.value = '顺丰速运'
  shipTracking.value = ''
  shipModalOpen.value = true
}

function closeShip() {
  shipModalOpen.value = false
}

async function submitShip() {
  if (!shipTracking.value.trim()) {
    toast.warning('请填写运单号')
    return
  }
  shipping.value = true
  try {
    await merchantShip(shipTarget.value.orderNumber, shipCompany.value, shipTracking.value.trim())
    toast.success('发货成功，物流信息已录入')
    shipModalOpen.value = false
    load(pageNum.value)
  } catch (e) {
    apiError(e, '发货失败')
  } finally {
    shipping.value = false
  }
}

// ========== 物流跟踪（P2-18） ==========

async function openLogistics(o) {
  logisticsTarget.value = o
  logisticsData.value = null
  logisticsModalOpen.value = true
  logisticsLoading.value = true
  try {
    const res = await trackLogistics(o.orderNumber)
    logisticsData.value = res.data
  } catch (e) {
    apiError(e, '获取物流信息失败')
  } finally {
    logisticsLoading.value = false
  }
}

function closeLogistics() {
  logisticsModalOpen.value = false
}

async function advance() {
  advancing.value = true
  try {
    const res = await merchantAdvanceLogistics(logisticsTarget.value.orderNumber)
    logisticsData.value = res.data
    toast.success('物流状态已更新')
    load(pageNum.value)
  } catch (e) {
    apiError(e, '物流状态更新失败')
  } finally {
    advancing.value = false
  }
}

// ========== 退款审核（P2-18） ==========

async function openRefund(o) {
  refundTarget.value = o
  refundRequest.value = null
  refundComment.value = ''
  refundModalOpen.value = true
  refundLoading.value = true
  try {
    const res = await listOrderRefunds(o.orderNumber)
    refundRequest.value = (res.data || []).find((r) => r.status === 'PENDING') || null
  } catch (e) {
    apiError(e, '获取退款申请失败')
  } finally {
    refundLoading.value = false
  }
}

function closeRefund() {
  refundModalOpen.value = false
}

async function reviewRefund(approve) {
  if (!approve && !refundComment.value.trim()) {
    toast.warning('驳回时请填写审核意见')
    return
  }
  reviewing.value = true
  try {
    await merchantProcessRefund(refundTarget.value.orderNumber, approve, refundComment.value.trim() || null)
    toast.success(approve ? '已通过退款，库存已回补' : '已驳回退款申请')
    refundModalOpen.value = false
    load(pageNum.value)
  } catch (e) {
    apiError(e, '操作失败')
  } finally {
    reviewing.value = false
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

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 300;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 60px 16px;
  overflow-y: auto;
}

.modal {
  width: 100%;
  max-width: 480px;
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-lg);
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 6px;
}

.modal-sub {
  font-size: 13px;
  color: var(--color-text-tertiary);
  margin-bottom: 16px;
}

.modal-loading {
  text-align: center;
  color: var(--color-text-tertiary);
  font-size: 13px;
  padding: 20px 0;
}

.form-grid {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 12px 14px;
  align-items: center;
  margin-bottom: 18px;
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.form-input {
  padding: 9px 12px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 14px;
  width: 100%;
}

.comment-textarea {
  width: 100%;
  margin-top: 12px;
  padding: 10px 12px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 14px;
  resize: vertical;
}

.refund-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.refund-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 13px;
}

.refund-label {
  color: var(--color-text-tertiary);
  width: 56px;
  flex-shrink: 0;
}

.refund-reason {
  color: var(--color-text);
  word-break: break-all;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 18px;
}

.btn-primary {
  padding: 9px 18px;
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: 14px;
  transition: opacity var(--transition-fast);
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.85;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-danger {
  padding: 9px 18px;
  background: var(--color-danger);
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: 14px;
}

.btn-danger:hover:not(:disabled) {
  opacity: 0.85;
}

.btn-danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
