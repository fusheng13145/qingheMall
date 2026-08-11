<template>
  <div class="seckill-page">
    <div class="page-banner">
      <div class="banner-inner">
        <div class="banner-left">
          <h1 class="banner-title">限时秒杀</h1>
          <p class="banner-sub">超低价抢购，售罄不补 · 同一商品每人限抢一次</p>
        </div>
        <div class="banner-clock">
          <span class="clock-label">距本场结束</span>
          <span class="clock-value">{{ globalCountdown }}</span>
        </div>
      </div>
    </div>

    <div v-if="loading" class="state">加载中...</div>
    <div v-else-if="activities.length === 0" class="state">暂无进行中的秒杀活动</div>

    <div v-else class="seckill-grid">
      <div v-for="a in activities" :key="a.id" class="seckill-card">
        <div class="card-img-wrap">
          <img v-if="firstImg(a.productImg)" :src="firstImg(a.productImg)" :alt="a.productName" loading="lazy" />
          <div v-else class="img-placeholder">青禾</div>
          <span v-if="a.remainStock <= 0" class="sold-out">已抢光</span>
        </div>
        <div class="card-body">
          <div class="card-name" :title="a.productName">{{ a.productName || '秒杀商品' }}</div>
          <div class="card-price">
            <span class="price-current">¥{{ formatPrice(a.seckillPrice) }}</span>
            <span v-if="a.originalPrice" class="price-origin">¥{{ formatPrice(a.originalPrice) }}</span>
          </div>
          <div class="card-stock">
            <div class="progress">
              <div class="progress-bar" :style="{ width: stockPercent(a) + '%' }"></div>
            </div>
            <div class="stock-text">已抢 {{ soldCount(a) }}/{{ a.totalStock }}</div>
          </div>
          <div class="card-foot">
            <span class="card-countdown">{{ remaining(a) }}</span>
            <button
              class="btn-grab"
              :disabled="a.remainStock <= 0"
              @click="openDetail(a)"
            >
              {{ a.remainStock <= 0 ? '已抢光' : '立即抢购' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情 / 抢购弹窗 -->
    <div v-if="detail" class="modal-mask" @click.self="closeDetail">
      <div class="modal">
        <button class="modal-close" @click="closeDetail">×</button>
        <div class="modal-img">
          <img v-if="firstImg(detail.productImg)" :src="firstImg(detail.productImg)" :alt="detail.productName" />
          <div v-else class="img-placeholder">青禾</div>
        </div>
        <div class="modal-info">
          <div class="modal-name">{{ detail.productName || '秒杀商品' }}</div>
          <div class="modal-price">
            <span class="price-current">¥{{ formatPrice(detail.seckillPrice) }}</span>
            <span v-if="detail.originalPrice" class="price-origin">¥{{ formatPrice(detail.originalPrice) }}</span>
            <span class="price-tag">秒杀价</span>
          </div>
          <div class="modal-stock">
            <div class="progress">
              <div class="progress-bar" :style="{ width: stockPercent(detail) + '%' }"></div>
            </div>
            <div class="stock-text">剩余 {{ detail.remainStock }}/{{ detail.totalStock }}</div>
          </div>
          <div class="modal-countdown">距结束：{{ remaining(detail) }}</div>
          <div class="qty-row">
            <span class="qty-label">数量</span>
            <div class="qty-stepper">
              <button :disabled="qty <= 1" @click="qty = qty - 1">−</button>
              <span>{{ qty }}</span>
              <button :disabled="qty >= Math.max(1, detail.remainStock)" @click="qty = qty + 1">＋</button>
            </div>
            <span class="qty-hint">每人限购 1 次，单次最多 {{ detail.remainStock }} 件</span>
          </div>
          <button class="btn-confirm" :disabled="grabbing || detail.remainStock <= 0" @click="handleGrab">
            {{ grabbing ? '抢购中...' : (detail.remainStock <= 0 ? '已抢光' : '确认抢购并支付') }}
          </button>
          <p class="modal-tip">抢购成功将立即为您占位库存，请在 30 分钟内完成支付，超时自动释放。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { toast, apiError } from '../utils/toast'
import { useRouter } from 'vue-router'
import { listSeckillActivities, createSeckillOrder } from '../api/seckill'
import { getDefaultAddress } from '../api/address'
import { formatPrice, firstImg } from '../utils/format'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const activities = ref([])
const loading = ref(true)
const detail = ref(null)
const qty = ref(1)
const grabbing = ref(false)

// 全局时钟：每秒刷新，驱动所有倒计时
const now = ref(Date.now())
let timer = null

const globalCountdown = computed(() => {
  // 取最近结束的活动作为「本场」倒计时
  if (activities.value.length === 0) return '--:--:--'
  const sorted = [...activities.value].sort((a, b) => new Date(a.endTime) - new Date(b.endTime))
  return formatRemaining(new Date(sorted[0].endTime).getTime() - now.value)
})

function pad(n) {
  return String(n).padStart(2, '0')
}

function formatRemaining(ms) {
  if (ms <= 0) return '已结束'
  const totalSec = Math.floor(ms / 1000)
  const days = Math.floor(totalSec / 86400)
  const hours = Math.floor((totalSec % 86400) / 3600)
  const minutes = Math.floor((totalSec % 3600) / 60)
  const seconds = totalSec % 60
  if (days > 0) {
    return `${days}天 ${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
  }
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
}

function remaining(a) {
  if (!a || !a.endTime) return ''
  return '剩 ' + formatRemaining(new Date(a.endTime).getTime() - now.value)
}

function soldCount(a) {
  const total = a.totalStock || 0
  const remain = a.remainStock || 0
  return Math.max(0, total - remain)
}

function stockPercent(a) {
  const total = a.totalStock || 0
  if (total <= 0) return 0
  return Math.min(100, Math.round((soldCount(a) / total) * 100))
}

async function load() {
  loading.value = true
  try {
    const res = await listSeckillActivities()
    activities.value = res.data || []
  } catch (e) {
    apiError(e, '加载秒杀活动失败')
  } finally {
    loading.value = false
  }
}

function openDetail(a) {
  if (a.remainStock <= 0) return
  qty.value = 1
  detail.value = a
}

function closeDetail() {
  detail.value = null
}

async function handleGrab() {
  if (!detail.value) return
  // 未登录：引导登录后再来抢（保留返回）
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: '/seckill' } })
    return
  }
  grabbing.value = true
  try {
    // 取默认收货地址填入订单（无默认地址则提示去设置）
    let receiverName = null
    let receiverPhone = null
    let receiverAddress = null
    try {
      const addrRes = await getDefaultAddress()
      const addr = addrRes && addrRes.data
      if (addr && addr.receiverName) {
        receiverName = addr.receiverName
        receiverPhone = addr.receiverPhone
        receiverAddress = addr.receiverAddress
      }
    } catch (e) {
      // 忽略：无默认地址仍可继续，但下单后需补地址
    }
    if (!receiverName) {
      toast.warning('请先在「个人中心」设置默认收货地址后再抢购')
      router.push('/profile')
      return
    }
    const orderNumber = await createSeckillOrder(detail.value.id, {
      quantity: qty.value,
      receiverName,
      receiverPhone,
      receiverAddress
    }).then(r => r.data)
    closeDetail()
    router.push({ path: '/pay', query: { orderNumber } })
  } catch (e) {
    apiError(e, '抢购失败')
  } finally {
    grabbing.value = false
  }
}

onMounted(() => {
  load()
  timer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.seckill-page {
  padding: 20px 0;
  max-width: 1080px;
  margin: 0 auto;
}

.page-banner {
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.25);
}

.banner-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28px 32px;
  color: #fff;
}

.banner-title {
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 2px;
  margin-bottom: 6px;
}

.banner-sub {
  font-size: 14px;
  opacity: 0.9;
}

.banner-clock {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.clock-label {
  font-size: 12px;
  opacity: 0.9;
}

.clock-value {
  font-size: 26px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  background: rgba(0, 0, 0, 0.18);
  padding: 4px 14px;
  border-radius: var(--radius-sm);
}

.state {
  text-align: center;
  color: var(--color-text-tertiary);
  padding: 60px 0;
  font-size: 15px;
}

.seckill-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 18px;
}

.seckill-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-xs);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.seckill-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}

.card-img-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: var(--color-bg-overlay);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.card-img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.img-placeholder {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 800;
  color: var(--color-primary);
  opacity: 0.5;
}

.sold-out {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: var(--radius-full);
}

.card-body {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.price-current {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-danger);
}

.price-origin {
  font-size: 13px;
  color: var(--color-text-tertiary);
  text-decoration: line-through;
}

.card-stock {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.progress {
  height: 8px;
  border-radius: var(--radius-full);
  background: var(--color-bg-overlay);
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-light));
  border-radius: var(--radius-full);
  transition: width 0.4s ease;
}

.stock-text {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 2px;
}

.card-countdown {
  font-size: 12px;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

.btn-grab {
  padding: 7px 18px;
  background: linear-gradient(135deg, var(--color-danger), #f87171);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.btn-grab:hover:not(:disabled) {
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.35);
}

.btn-grab:disabled {
  background: var(--color-text-tertiary);
  cursor: not-allowed;
}

/* 弹窗 */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal {
  position: relative;
  width: 100%;
  max-width: 720px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  display: flex;
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}

.modal-close {
  position: absolute;
  top: 10px;
  right: 14px;
  background: transparent;
  border: none;
  font-size: 26px;
  line-height: 1;
  color: var(--color-text-tertiary);
  cursor: pointer;
  z-index: 2;
}

.modal-img {
  width: 300px;
  flex-shrink: 0;
  background: var(--color-bg-overlay);
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.modal-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.modal-info {
  flex: 1;
  padding: 28px 26px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}

.modal-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
}

.modal-price {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.modal-price .price-current {
  font-size: 30px;
}

.price-tag {
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: var(--color-danger);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}

.modal-stock {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.modal-countdown {
  font-size: 13px;
  color: var(--color-primary);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.qty-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.qty-label {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.qty-stepper {
  display: flex;
  align-items: center;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.qty-stepper button {
  width: 34px;
  height: 34px;
  background: var(--color-bg);
  border: none;
  font-size: 18px;
  color: var(--color-text);
  cursor: pointer;
}

.qty-stepper button:disabled {
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}

.qty-stepper span {
  min-width: 40px;
  text-align: center;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

.qty-hint {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.btn-confirm {
  margin-top: 4px;
  padding: 12px 0;
  background: linear-gradient(135deg, var(--color-danger), #f87171);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.btn-confirm:hover:not(:disabled) {
  box-shadow: 0 6px 18px rgba(239, 68, 68, 0.4);
}

.btn-confirm:disabled {
  background: var(--color-text-tertiary);
  cursor: not-allowed;
}

.modal-tip {
  font-size: 12px;
  color: var(--color-text-tertiary);
  line-height: 1.5;
}

@media (max-width: 680px) {
  .modal {
    flex-direction: column;
    max-width: 360px;
  }

  .modal-img {
    width: 100%;
    min-height: 200px;
  }

  .banner-inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
    padding: 22px;
  }

  .banner-clock {
    align-items: flex-start;
  }
}
</style>
