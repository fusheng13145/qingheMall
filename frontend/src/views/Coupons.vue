<template>
  <div class="coupons-page">
    <div class="page-header">
      <h1 class="page-title">领券中心</h1>
      <p class="page-desc">领取优惠券，结算时自动抵扣</p>
    </div>

    <div v-if="loading" class="state">加载中...</div>
    <div v-else-if="coupons.length === 0" class="state">暂无可用优惠券</div>
    <div v-else class="coupon-grid">
      <div
        v-for="c in coupons"
        :key="c.id"
        class="coupon-card"
        :class="{ disabled: claimedSet.has(c.id) || c.issued >= c.total }"
      >
        <div class="coupon-left">
          <template v-if="c.type === 'FULL_REDUCTION'">
            <span class="symbol">¥</span><span class="big">{{ formatPrice(c.amount) }}</span>
          </template>
          <template v-else>
            <span class="big">{{ (Number(c.discount) * 10).toFixed(1) }}</span><span class="symbol">折</span>
          </template>
        </div>
        <div class="coupon-right">
          <div class="coupon-name">{{ c.name }}</div>
          <div class="coupon-rule">{{ couponRuleText(c) }}</div>
          <div class="coupon-time">{{ formatTime(c.startTime) }} ~ {{ formatTime(c.endTime) }}</div>
          <div class="coupon-foot">
            <span class="coupon-stock">已领 {{ c.issued }}/{{ c.total }}</span>
            <button
              class="btn-claim"
              :disabled="claimedSet.has(c.id) || c.issued >= c.total"
              @click="handleClaim(c)"
            >
              {{ claimedSet.has(c.id) ? '已领取' : (c.issued >= c.total ? '已领完' : '立即领取') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listCoupons, claimCoupon, myCoupons } from '../api/coupon'
import { couponRuleText } from '../utils/coupon'

const coupons = ref([])
const claimedSet = ref(new Set())
const loading = ref(true)

function formatPrice(p) {
  return Number(p || 0).toFixed(0)
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return String(t)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${m}-${day}`
}

async function load() {
  loading.value = true
  try {
    const res = await listCoupons()
    coupons.value = res.data || []
    // 标记已领取（避免重复领）
    try {
      const mineRes = await myCoupons('UNUSED')
      const set = new Set()
      ;(mineRes.data || []).forEach(u => set.add(u.couponId))
      claimedSet.value = set
    } catch (e) { /* 忽略 */ }
  } catch (e) {
    alert('加载优惠券失败：' + (e.message || '请稍后重试'))
  } finally {
    loading.value = false
  }
}

function handleClaim(c) {
  claimCoupon(c.id)
    .then(() => {
      claimedSet.value = new Set(claimedSet.value).add(c.id)
      c.issued = (c.issued || 0) + 1
      alert('领取成功，可在「我的优惠券」中查看')
    })
    .catch(e => {
      alert('领取失败：' + (e.message || '请稍后重试'))
    })
}

onMounted(load)
</script>

<style scoped>
.coupons-page {
  padding: 20px 0;
  max-width: 860px;
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

.state {
  text-align: center;
  color: var(--color-text-tertiary);
  padding: 60px 0;
  font-size: 15px;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.coupon-card {
  display: flex;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs);
  background: var(--color-surface);
  transition: all var(--transition-fast);
}

.coupon-card.disabled {
  opacity: 0.6;
}

.coupon-left {
  width: 110px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  flex-shrink: 0;
}

.coupon-left .big {
  font-size: 34px;
  font-weight: 800;
  line-height: 1;
}

.coupon-left .symbol {
  font-size: 16px;
  font-weight: 600;
}

.coupon-right {
  flex: 1;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.coupon-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

.coupon-rule {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.coupon-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.coupon-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

.coupon-stock {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.btn-claim {
  padding: 6px 18px;
  background: var(--color-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.btn-claim:hover:not(:disabled) {
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.35);
}

.btn-claim:disabled {
  background: var(--color-text-tertiary);
  cursor: not-allowed;
}
</style>
