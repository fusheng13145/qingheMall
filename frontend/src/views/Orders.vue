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
      </button>
    </div>
    <div v-if="orders.length > 0" class="order-list">
      <div v-for="order in orders" :key="order.id" class="order-card" @click="openDetail(order)">
        <div class="order-header">
          <div class="order-no-wrapper">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            <span class="order-no">{{ order.orderNumber }}</span>
          </div>
          <span class="order-status" :class="statusClass(order.status)">{{ statusText(order.status) }}</span>
        </div>
        <div class="order-body">
          <div class="order-product-info">
            <div class="order-product-line">
              <img v-if="order.productImg" class="order-product-img" :src="order.productImg" :alt="order.productName" loading="lazy" />
              <div class="order-product-text">
                <span class="product-name">{{ order.productName || '商品' }}</span>
                <span v-if="order.productDetail && order.productDetail.size != null" class="product-detail">规格: {{ formatSize(order.productDetail.size) }} × {{ order.quantity || 1 }}</span>
              </div>
            </div>
          </div>
          <div class="order-price-col">
            <div class="order-price">¥{{ formatPrice(order.totalPrice) }}</div>
            <div v-if="order.discountAmount > 0" class="order-discount">已优惠 ¥{{ formatPrice(order.discountAmount) }}</div>
          </div>
        </div>
        <div class="order-footer">
          <span class="order-time">{{ formatTime(order.gmtCreated) }}</span>
          <div class="order-actions" @click.stop>
            <button
              v-if="order.status === 'WAIT_BUYER_PAY'"
              class="btn-pay"
              @click="handlePay(order)"
            >
              立即付款
            </button>
            <button
              v-if="order.status === 'WAIT_BUYER_PAY'"
              class="btn-cancel"
              @click="handleCancel(order)"
            >
              取消订单
            </button>
            <button
              v-if="['TRADE_PAID_SUCCESS', 'TRADE_SHIPPED', 'TRADE_COMPLETED'].includes(order.status)"
              class="btn-refund"
              @click="openRefundModal(order)"
            >
              {{ order.status === 'TRADE_PAID_SUCCESS' ? '申请退款' : '申请退货退款' }}
            </button>
            <button
              v-if="['TRADE_SHIPPED', 'TRADE_COMPLETED', 'TRADE_REFUNDED'].includes(order.status)"
              class="btn-comment"
              @click="openLogistics(order)"
            >
              查看物流
            </button>
            <button
              v-if="['TRADE_REFUNDING', 'TRADE_REFUNDED'].includes(order.status)"
              class="btn-refund"
              @click="openRefundDetail(order)"
            >
              退款详情
            </button>
            <button
              v-if="order.status === 'TRADE_SHIPPED'"
              class="btn-confirm"
              @click="handleConfirmReceipt(order)"
            >
              确认收货
            </button>
            <button
              v-if="(order.status === 'TRADE_PAID_SUCCESS' || order.status === 'TRADE_SHIPPED' || order.status === 'TRADE_COMPLETED') && !order.commented"
              class="btn-comment"
              @click="openComment(order)"
            >
              评价
            </button>
          </div>
        </div>
      </div>
    </div>
    <!-- 分页控件 -->
    <div v-if="totalPages > 1" class="pagination">
      <button class="page-btn" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">上一页</button>
      <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页 · 共 {{ totalCount }} 笔</span>
      <button class="page-btn" :disabled="currentPage >= totalPages" @click="changePage(currentPage + 1)">下一页</button>
    </div>
    <div v-else-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>
    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
      <p>暂无订单</p>
    </div>

    <!-- 订单详情弹窗 -->
    <div v-if="detailOrder" class="modal-overlay" @click.self="detailOrder = null">
      <div class="modal">
        <div class="modal-header">
          <h3>订单详情</h3>
          <button class="modal-close" @click="detailOrder = null">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div v-if="detailOrder" class="modal-body">
          <div class="detail-row">
            <span class="detail-label">订单号</span>
            <span class="detail-value mono">{{ detailOrder.orderNumber }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">状态</span>
            <span class="detail-value">{{ statusText(detailOrder.status) }}</span>
          </div>
          <div class="detail-product">
            <img v-if="detailOrder.productImg" class="detail-img" :src="detailOrder.productImg" :alt="detailOrder.productName" loading="lazy" />
            <div class="detail-product-info">
              <span class="detail-product-name">{{ detailOrder.productName || '商品' }}</span>
              <span v-if="detailOrder.productDetail" class="detail-product-spec">规格: {{ formatSize(detailOrder.productDetail.size) }} × {{ detailOrder.quantity || 1 }}</span>
              <span class="detail-product-price">单价 ¥{{ formatPrice(detailOrder.productDetail && detailOrder.productDetail.price) }}</span>
            </div>
            <span class="detail-product-total">¥{{ formatPrice(detailOrder.totalPrice) }}</span>
          </div>
          <div v-if="detailOrder.discountAmount > 0" class="detail-row">
            <span class="detail-label">优惠抵扣</span>
            <span class="detail-value discount-value">-¥{{ formatPrice(detailOrder.discountAmount) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">收货人</span>
            <span class="detail-value">{{ detailOrder.receiverName || '-' }} <template v-if="detailOrder.receiverPhone">（{{ detailOrder.receiverPhone }}）</template></span>
          </div>
          <div class="detail-row">
            <span class="detail-label">收货地址</span>
            <span class="detail-value">{{ detailOrder.receiverAddress || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">下单时间</span>
            <span class="detail-value">{{ formatTime(detailOrder.gmtCreated) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 评价弹窗 -->
    <div v-if="commentOrder" class="modal-overlay" @click.self="commentOrder = null">
      <div class="modal">
        <div class="modal-header">
          <h3>评价商品</h3>
          <button class="modal-close" @click="commentOrder = null">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div v-if="commentOrder" class="modal-body">
          <div class="comment-product">
            <img v-if="commentOrder.productImg" class="comment-product-img" :src="commentOrder.productImg" :alt="commentOrder.productName" loading="lazy" />
            <span class="comment-product-name">{{ commentOrder.productName || '商品' }}</span>
          </div>
          <div class="rating-input">
            <span
              v-for="n in 5"
              :key="n"
              class="star big"
              :class="{ filled: n <= commentRating }"
              @click="commentRating = n"
            >★</span>
            <span class="rating-text">{{ ratingText }}</span>
          </div>
          <textarea v-model="commentContent" class="comment-textarea" rows="4" maxlength="500" placeholder="分享您的使用体验（必填，最多 500 字）"></textarea>
          <div class="modal-actions">
            <button class="btn-cancel" @click="commentOrder = null">取消</button>
            <button class="btn-submit" :disabled="submitting" @click="submitComment">
              {{ submitting ? '提交中...' : '提交评价' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 退款申请弹窗 -->
    <div v-if="refundOrder" class="modal-overlay" @click.self="refundOrder = null">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ refundOrder.status === 'TRADE_PAID_SUCCESS' ? '申请退款' : '申请退货退款' }}</h3>
          <button class="modal-close" @click="refundOrder = null">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="detail-row">
            <span class="detail-label">订单号</span>
            <span class="detail-value mono">{{ refundOrder.orderNumber }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">退款类型</span>
            <span class="detail-value">{{ refundOrder.status === 'TRADE_PAID_SUCCESS' ? '仅退款' : '退货退款' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">退款金额</span>
            <span class="detail-value discount-value">¥{{ formatPrice(refundOrder.totalPrice) }}</span>
          </div>
          <textarea v-model="refundReason" class="comment-textarea" rows="3" maxlength="200" placeholder="请填写退款原因（必填，最多 200 字）"></textarea>
          <div class="modal-actions">
            <button class="btn-cancel" @click="refundOrder = null">取消</button>
            <button class="btn-submit" :disabled="refundSubmitting" @click="submitRefund">
              {{ refundSubmitting ? '提交中...' : '提交申请' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 物流信息弹窗 -->
    <div v-if="logisticsOrder" class="modal-overlay" @click.self="logisticsOrder = null">
      <div class="modal">
        <div class="modal-header">
          <h3>物流信息</h3>
          <button class="modal-close" @click="logisticsOrder = null">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div v-if="logisticsLoading" class="modal-loading">加载中...</div>
          <LogisticsTimeline v-else-if="logisticsData" :logistics="logisticsData" />
          <div v-else class="modal-empty">暂无物流信息</div>
        </div>
      </div>
    </div>

    <!-- 退款详情弹窗 -->
    <div v-if="refundDetailOrder" class="modal-overlay" @click.self="refundDetailOrder = null">
      <div class="modal">
        <div class="modal-header">
          <h3>退款详情</h3>
          <button class="modal-close" @click="refundDetailOrder = null">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="detail-row">
            <span class="detail-label">订单号</span>
            <span class="detail-value mono">{{ refundDetailOrder.orderNumber }}</span>
          </div>
          <div v-if="refundDetailLoading" class="modal-loading">加载中...</div>
          <div v-else-if="refundDetailList.length === 0" class="modal-empty">暂无退款记录</div>
          <template v-else>
            <div v-for="item in refundDetailList" :key="item.id" class="refund-item">
              <div class="detail-row">
                <span class="detail-label">类型</span>
                <span class="detail-value">{{ refundTypeText(item.type) }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">状态</span>
                <span class="detail-value"><span class="order-status" :class="refundStatusClass(item.status)">{{ refundStatusText(item.status) }}</span></span>
              </div>
              <div class="detail-row">
                <span class="detail-label">原因</span>
                <span class="detail-value">{{ item.reason || '-' }}</span>
              </div>
              <div v-if="item.reviewComment" class="detail-row">
                <span class="detail-label">商家备注</span>
                <span class="detail-value">{{ item.reviewComment }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">申请时间</span>
                <span class="detail-value">{{ formatTime(item.gmtCreated) }}</span>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { toast, apiError } from '../utils/toast'
import { useRouter } from 'vue-router'
import { listOrders, cancelOrder, confirmReceipt, applyRefund } from '../api/order'
import { addComment } from '../api/comment'
import { trackLogistics } from '../api/logistics'
import { listOrderRefunds } from '../api/refund'
import LogisticsTimeline from '../components/common/LogisticsTimeline.vue'

const router = useRouter()

const orders = ref([])
const loading = ref(true)
const activeTab = ref('all')
const detailOrder = ref(null)
const currentPage = ref(1)
const pageSize = 10
const totalPages = ref(1)
const totalCount = ref(0)

// 评价弹窗
const commentOrder = ref(null)
const commentRating = ref(5)
const commentContent = ref('')
const submitting = ref(false)

// 退款申请弹窗
const refundOrder = ref(null)
const refundReason = ref('')
const refundSubmitting = ref(false)

// 物流弹窗
const logisticsOrder = ref(null)
const logisticsData = ref(null)
const logisticsLoading = ref(false)

// 退款详情弹窗
const refundDetailOrder = ref(null)
const refundDetailList = ref([])
const refundDetailLoading = ref(false)

const ratingText = computed(() => {
  return ['', '很差', '较差', '一般', '满意', '非常满意'][commentRating.value] || ''
})

// 与后端 OrderStatus 枚举一致
const tabs = [
  { label: '全部', value: 'all' },
  { label: '待付款', value: 'WAIT_BUYER_PAY' },
  { label: '待发货', value: 'TRADE_PAID_SUCCESS' },
  { label: '待收货', value: 'TRADE_SHIPPED' },
  { label: '已完成', value: 'TRADE_COMPLETED' },
  { label: '退款中', value: 'TRADE_REFUNDING' },
  { label: '已退款', value: 'TRADE_REFUNDED' },
  { label: '已关闭', value: 'TRADE_CLOSED' }
]

function switchTab(value) {
  if (activeTab.value === value) return
  activeTab.value = value
  currentPage.value = 1
  loadOrders(1)
}

function statusText(status) {
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
  return map[status] || '未知'
}

function statusClass(status) {
  const map = {
    'WAIT_BUYER_PAY': 'status-pending',
    'TRADE_PAID_SUCCESS': 'status-paid',
    'TRADE_SHIPPED': 'status-shipped',
    'TRADE_COMPLETED': 'status-completed',
    'TRADE_REFUNDING': 'status-refunding',
    'TRADE_REFUNDED': 'status-refunded',
    'TRADE_CLOSED': 'status-closed',
    'TRADE_PAID_FAILED': 'status-closed'
  }
  return map[status] || ''
}

function formatPrice(price) {
  const n = Number(price || 0)
  return n.toFixed(2)
}

function formatSize(size) {
  if (size === null || size === undefined || size === '') return ''
  return String(Number(size))
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

async function loadOrders(page) {
  loading.value = true
  try {
    const res = await listOrders(activeTab.value === 'all' ? null : activeTab.value, page, pageSize)
    // res.data 为 Paging<Order>：{ pageNum, pageSize, totalPage, totalCount, data }
    orders.value = res.data.data || []
    totalPages.value = res.data.totalPage || 1
    totalCount.value = res.data.totalCount || 0
  } catch (error) {
    console.error('加载订单失败:', error)
  } finally {
    loading.value = false
  }
}

function changePage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loadOrders(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function handlePay(order) {
  router.push({ name: 'Pay', query: { orderNumber: order.orderNumber } })
}

function handleCancel(order) {
  if (!confirm(`确定要取消订单 ${order.orderNumber} 吗？取消后库存将自动回滚。`)) return
  cancelOrder(order.orderNumber)
    .then(() => {
      toast.success('订单已取消')
      loadOrders(currentPage.value)
    })
    .catch(e => {
      apiError(e, '取消失败')
    })
}

// 退款申请：待发货订单仅退款，已发货/已完成订单退货退款（与后端 RefundService 推导逻辑一致）
function openRefundModal(order) {
  refundOrder.value = order
  refundReason.value = ''
}

const refundTypeTextMap = {
  REFUND_ONLY: '仅退款',
  RETURN_REFUND: '退货退款'
}

function refundTypeText(type) {
  return refundTypeTextMap[type] || type || '-'
}

const refundStatusTextMap = {
  PENDING: '待商家审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝'
}

function refundStatusText(status) {
  return refundStatusTextMap[status] || status || '-'
}

function refundStatusClass(status) {
  if (status === 'APPROVED') return 'status-completed'
  if (status === 'REJECTED') return 'status-closed'
  return 'status-refunding'
}

async function submitRefund() {
  if (!refundReason.value.trim()) {
    toast.warning('请填写退款原因')
    return
  }
  const order = refundOrder.value
  const type = order.status === 'TRADE_PAID_SUCCESS' ? 'REFUND_ONLY' : 'RETURN_REFUND'
  refundSubmitting.value = true
  try {
    await applyRefund(order.orderNumber, refundReason.value.trim(), type)
    toast.success('退款申请已提交，等待商家处理')
    refundOrder.value = null
    loadOrders(currentPage.value)
  } catch (e) {
    apiError(e, '申请失败')
  } finally {
    refundSubmitting.value = false
  }
}

// 查看物流
async function openLogistics(order) {
  logisticsOrder.value = order
  logisticsData.value = null
  logisticsLoading.value = true
  try {
    const res = await trackLogistics(order.orderNumber)
    logisticsData.value = res.data || null
  } catch (e) {
    apiError(e, '物流信息加载失败')
  } finally {
    logisticsLoading.value = false
  }
}

// 退款详情（订单的退款申请历史）
async function openRefundDetail(order) {
  refundDetailOrder.value = order
  refundDetailList.value = []
  refundDetailLoading.value = true
  try {
    const res = await listOrderRefunds(order.orderNumber)
    refundDetailList.value = res.data || []
  } catch (e) {
    apiError(e, '退款详情加载失败')
  } finally {
    refundDetailLoading.value = false
  }
}

async function handleConfirmReceipt(order) {
  if (!confirm(`确认已收到订单 ${order.orderNumber} 的商品吗？`)) return
  confirmReceipt(order.orderNumber)
    .then(() => {
      toast.success('已确认收货')
      loadOrders(currentPage.value)
    })
    .catch(e => {
      apiError(e, '操作失败')
    })
}

function openDetail(order) {
  detailOrder.value = order
}

function openComment(order) {
  commentOrder.value = order
  commentRating.value = 5
  commentContent.value = ''
}

async function submitComment() {
  if (!commentContent.value.trim()) {
    toast.warning('请填写评价内容')
    return
  }
  submitting.value = true
  try {
    const productId = commentOrder.value.productDetail && commentOrder.value.productDetail.productId
    await addComment({
      productId,
      orderNumber: commentOrder.value.orderNumber,
      rating: commentRating.value,
      content: commentContent.value.trim()
    })
    toast.success('评价成功，感谢您的反馈！')
    commentOrder.value = null
    loadOrders(currentPage.value)
  } catch (error) {
    apiError(error, '评价失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadOrders(1)
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

.status-shipped {
  color: #0e7490;
  background: rgba(14, 116, 148, 0.10);
}

.status-completed {
  color: var(--color-success);
  background: var(--color-success-light);
}

.status-refunding {
  color: #b45309;
  background: rgba(180, 83, 9, 0.10);
}

.status-refunded {
  color: #be123c;
  background: rgba(190, 18, 60, 0.10);
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

.order-price-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.order-discount {
  font-size: 12px;
  color: var(--color-success);
  font-weight: 600;
}

.discount-value {
  color: var(--color-success);
  font-weight: 600;
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

.btn-refund {
  padding: 8px 24px;
  background: transparent;
  color: #b45309;
  border: 1px solid #b45309;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.btn-refund:hover {
  background: rgba(180, 83, 9, 0.08);
}

.btn-confirm {
  padding: 8px 24px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  transition: all var(--transition-fast);
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.25);
}

.btn-confirm:hover {
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.35);
  transform: translateY(-1px);
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

.order-product-line {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-product-img {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  background: var(--color-bg-sunken);
  flex-shrink: 0;
}

.order-product-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* ========== 订单详情弹窗 ========== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 300;
  padding: 20px;
}

.modal {
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  width: 100%;
  max-width: 520px;
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--color-divider);
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}

.modal-close:hover {
  background: var(--color-bg-overlay);
  color: var(--color-text);
}

.modal-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  font-size: 14px;
}

.detail-label {
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.detail-value {
  color: var(--color-text);
  text-align: right;
  word-break: break-all;
}

.mono {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 13px;
}

.detail-product {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--color-bg-sunken);
  border-radius: var(--radius-sm);
  padding: 12px;
}

.detail-img {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  flex-shrink: 0;
}

.detail-product-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-product-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-product-spec {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.detail-product-price {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.detail-product-total {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-price);
  flex-shrink: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 80px 0;
  color: var(--color-text-tertiary);
}

/* ========== 分页控件 ========== */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 28px 0 8px;
}

.page-btn {
  padding: 8px 20px;
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.page-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* ========== 评价按钮 ========== */
.btn-comment {
  padding: 8px 24px;
  background: transparent;
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.btn-comment:hover {
  background: var(--color-primary-50);
}

/* ========== 评价弹窗 ========== */
.comment-product {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--color-bg-sunken);
  border-radius: var(--radius-sm);
}

.comment-product-img {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  flex-shrink: 0;
}

.comment-product-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rating-input {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 0;
}

.star.big {
  font-size: 28px;
  cursor: pointer;
  transition: transform var(--transition-fast);
}

.star.big:hover {
  transform: scale(1.15);
}

.star {
  color: var(--color-border);
}

.star.filled {
  color: #f5a623;
}

.rating-text {
  font-size: 13px;
  color: var(--color-text-tertiary);
  margin-left: 6px;
}

.comment-textarea {
  width: 100%;
  padding: 12px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text);
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  outline: none;
  transition: border-color var(--transition-fast);
}

.comment-textarea:focus {
  border-color: var(--color-primary);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 8px;
}

.btn-submit {
  padding: 9px 28px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.25);
}

.btn-submit:hover:not(:disabled) {
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.35);
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.empty-state svg {
  opacity: 0.4;
}

.empty-state p {
  font-size: 15px;
}

.modal-loading {
  text-align: center;
  color: var(--color-text-tertiary);
  font-size: 13px;
  padding: 24px 0;
}

.modal-empty {
  text-align: center;
  color: var(--color-text-tertiary);
  font-size: 13px;
  padding: 24px 0;
}

.refund-item {
  padding: 12px 0;
  border-top: 1px dashed var(--color-border-light);
}

.refund-item .detail-row {
  margin-bottom: 6px;
}
</style>
