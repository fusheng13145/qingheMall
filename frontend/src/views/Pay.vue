<template>
  <div class="pay-page">
    <div class="page-header">
      <h1 class="page-title">收银台</h1>
      <p class="page-desc">请完成订单支付，支付成功后商品将尽快发出</p>
    </div>

    <div v-if="order" class="pay-card">
      <!-- 订单信息 -->
      <div class="order-info">
        <div class="order-product">
          <img v-if="order.productImg" class="order-img" :src="order.productImg" :alt="order.productName" loading="lazy" />
          <div class="order-text">
            <span class="order-name">{{ order.productName || '商品' }}</span>
            <span v-if="order.productDetail" class="order-spec">规格: {{ formatSize(order.productDetail.size) }} × {{ order.quantity || 1 }}</span>
            <span class="order-no">订单号：{{ order.orderNumber }}</span>
          </div>
        </div>
        <div class="order-amount">
          <span class="amount-label">应付金额</span>
          <span class="amount-value">¥{{ formatPrice(order.totalPrice) }}</span>
        </div>
      </div>

      <!-- 支付方式 -->
      <div class="pay-methods">
        <div class="method-title">选择支付方式</div>
        <div class="method-list">
          <div class="method-item" :class="{ active: payType === 'WECHAT' }" @click="payType = 'WECHAT'">
            <span class="method-icon wechat">微</span>
            <div class="method-text">
              <span class="method-name">微信支付</span>
              <span class="method-desc">扫码支付（需商户配置，未配置时自动切换模拟）</span>
            </div>
          </div>
          <div class="method-item" :class="{ active: payType === 'ALIPAY' }" @click="payType = 'ALIPAY'">
            <span class="method-icon alipay">支</span>
            <div class="method-text">
              <span class="method-name">支付宝</span>
              <span class="method-desc">当前为模拟通道</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 支付区 -->
      <div class="pay-action">
        <!-- 模拟支付 -->
        <template v-if="payState.mock">
          <div class="mock-tip">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
            <span>{{ mockTip }}</span>
          </div>
          <button class="btn-mock-pay" :disabled="paying" @click="handleMockPay">
            {{ paying ? '处理中...' : '模拟支付' }}
          </button>
        </template>

        <!-- 真实渠道扫码 -->
        <template v-else>
          <div class="qr-box">
            <img v-if="payState.codeUrl" :src="payState.codeUrl" alt="支付二维码" class="qr-img" />
            <div class="qr-tip">{{ qrTip }}</div>
          </div>
          <div class="qr-status">{{ statusText }}</div>
          <button class="btn-query" :disabled="querying" @click="queryNow">
            {{ querying ? '查询中...' : '我已支付' }}
          </button>
        </template>
      </div>
    </div>

    <div v-else class="loading-state">
      <div class="loading-spinner"></div>
      <span>加载订单中...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { apiError } from '../utils/toast'
import { useRoute, useRouter } from 'vue-router'
import { getOrder } from '../api/order'
import { createPay, mockPay, queryPay } from '../api/payment'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const payType = ref('WECHAT')
const payState = ref({ mock: true })
const paying = ref(false)
const querying = ref(false)
const statusText = ref('等待扫码支付...')
let timer = null

const mockTip = ref('')

// 二维码提示文案：按渠道动态显示
const qrTip = computed(() => {
  if (payState.value.channel === 'ALIPAY') return '请使用支付宝 App「扫一扫」完成支付'
  if (payState.value.channel === 'WECHAT') return '请使用微信「扫一扫」完成支付'
  return '请使用对应 App「扫一扫」完成支付'
})

function formatPrice(p) {
  return Number(p || 0).toFixed(2)
}

function formatSize(size) {
  if (size === null || size === undefined || size === '') return ''
  return String(Number(size))
}

function orderNumber() {
  return route.query.orderNumber || ''
}

async function loadOrder() {
  try {
    const res = await getOrder(orderNumber())
    order.value = res.data
  } catch (e) {
    apiError(e, '加载订单失败')
    router.push('/orders')
  }
}

async function initPay() {
  try {
    const res = await createPay(orderNumber(), payType.value)
    payState.value = res.data
    if (res.data.mock) {
      mockTip.value = payType.value === 'WECHAT'
        ? '微信支付未配置商户凭证，当前为模拟支付（配置后自动启用真实扫码支付）'
        : '支付宝渠道为模拟支付'
      return
    }
    // 真实微信扫码：启动轮询
    statusText.value = '等待扫码支付...'
    startPolling()
  } catch (e) {
    apiError(e, '创建支付失败')
  }
}

function startPolling() {
  stopPolling()
  timer = setInterval(async () => {
    try {
      const res = await queryPay(orderNumber())
      if (res.data === 'TRADE_PAID_SUCCESS') {
        stopPolling()
        router.replace({ name: 'PaySuccess', query: { orderNumber: orderNumber() } })
      }
    } catch (e) {
      // 轮询失败静默，下轮继续
    }
  }, 3000)
}

function stopPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

async function queryNow() {
  querying.value = true
  try {
    const res = await queryPay(orderNumber())
    if (res.data === 'TRADE_PAID_SUCCESS') {
      stopPolling()
      router.replace({ name: 'PaySuccess', query: { orderNumber: orderNumber() } })
    } else {
      statusText.value = '尚未检测到支付，请扫码后点击「我已支付」'
    }
  } catch (e) {
    apiError(e, '查询失败')
  } finally {
    querying.value = false
  }
}

async function handleMockPay() {
  paying.value = true
  try {
    await mockPay(orderNumber())
    router.replace({ name: 'PaySuccess', query: { orderNumber: orderNumber() } })
  } catch (e) {
    apiError(e, '支付失败')
  } finally {
    paying.value = false
  }
}

onMounted(() => {
  loadOrder().then(initPay)
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.pay-page {
  padding: 20px 0;
  max-width: 720px;
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

.pay-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.order-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 24px;
  border-bottom: 1px solid var(--color-divider);
}

.order-product {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.order-img {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  background: var(--color-bg-sunken);
  flex-shrink: 0;
}

.order-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.order-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-spec {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.order-no {
  font-size: 12px;
  color: var(--color-text-tertiary);
  font-family: 'SF Mono', 'Fira Code', monospace;
}

.order-amount {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
}

.amount-label {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.amount-value {
  font-size: 30px;
  font-weight: 800;
  color: var(--color-price);
}

.pay-methods {
  padding: 20px 24px;
  border-bottom: 1px solid var(--color-divider);
}

.method-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}

.method-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.method-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.method-item:hover {
  border-color: var(--color-primary);
}

.method-item.active {
  border-color: var(--color-primary);
  background: var(--color-primary-50);
}

.method-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.method-icon.wechat {
  background: #07c160;
}

.method-icon.alipay {
  background: #1677ff;
}

.method-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.method-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.method-desc {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.pay-action {
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.mock-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-warning);
  background: var(--color-warning-light);
  padding: 8px 16px;
  border-radius: var(--radius-sm);
}

.btn-mock-pay {
  padding: 13px 60px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);
}

.btn-mock-pay:hover:not(:disabled) {
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.35);
}

.btn-mock-pay:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.qr-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.qr-img {
  width: 240px;
  height: 240px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  padding: 8px;
}

.qr-tip {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.qr-status {
  font-size: 13px;
  color: var(--color-text-tertiary);
  min-height: 20px;
}

.btn-query {
  padding: 10px 32px;
  background: var(--color-primary-50);
  color: var(--color-primary);
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 600;
  transition: all var(--transition-fast);
}

.btn-query:hover:not(:disabled) {
  background: var(--color-primary);
  color: #fff;
}

.btn-query:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
</style>
