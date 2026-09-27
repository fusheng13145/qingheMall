<template>
  <div class="merchant-marketing">
    <h1 class="page-title">营销管理</h1>
    <p class="page-desc">自建本店优惠券与限时秒杀，仅对本店商品生效，平台券与平台秒杀不受影响。</p>

    <!-- 页签 -->
    <div class="tabs">
      <button class="tab" :class="{ active: tab === 'coupon' }" @click="tab = 'coupon'">优惠券</button>
      <button class="tab" :class="{ active: tab === 'seckill' }" @click="tab = 'seckill'">限时秒杀</button>
    </div>

    <!-- ===== 优惠券 ===== -->
    <section v-show="tab === 'coupon'" class="panel">
      <div class="section">
        <h2 class="section-title">创建本店优惠券</h2>
        <div class="form-grid">
          <div class="form-group">
            <label>券名称 <span class="req">*</span></label>
            <input v-model.trim="couponForm.name" type="text" maxlength="64" placeholder="如：新人满减券" />
          </div>
          <div class="form-group">
            <label>类型 <span class="req">*</span></label>
            <select v-model="couponForm.type">
              <option value="FULL_REDUCTION">满减券</option>
              <option value="DISCOUNT">折扣券</option>
            </select>
          </div>
          <div class="form-group">
            <label>使用门槛（元，0 表示无门槛）</label>
            <input v-model="couponForm.threshold" type="number" min="0" step="0.01" />
          </div>
          <div v-if="couponForm.type === 'FULL_REDUCTION'" class="form-group">
            <label>满减面额（元） <span class="req">*</span></label>
            <input v-model="couponForm.amount" type="number" min="0" step="0.01" />
          </div>
          <div v-if="couponForm.type === 'DISCOUNT'" class="form-group">
            <label>折扣率（如 0.85 表示 85 折） <span class="req">*</span></label>
            <input v-model="couponForm.discount" type="number" min="0.01" max="1" step="0.01" />
          </div>
          <div v-if="couponForm.type === 'DISCOUNT'" class="form-group">
            <label>折扣封顶（元，可空）</label>
            <input v-model="couponForm.maxDiscount" type="number" min="0" step="0.01" placeholder="不封顶留空" />
          </div>
          <div class="form-group">
            <label>发放总量 <span class="req">*</span></label>
            <input v-model="couponForm.total" type="number" min="0" step="1" />
          </div>
          <div class="form-group">
            <label>每人限领（张）</label>
            <input v-model="couponForm.perLimit" type="number" min="1" step="1" />
          </div>
          <div class="form-group">
            <label>生效开始 <span class="req">*</span></label>
            <input v-model="couponForm.startTime" type="datetime-local" />
          </div>
          <div class="form-group">
            <label>生效结束 <span class="req">*</span></label>
            <input v-model="couponForm.endTime" type="datetime-local" />
          </div>
        </div>
        <button class="btn-create" :disabled="couponCreating" @click="handleCreateCoupon">
          {{ couponCreating ? '创建中...' : '创建优惠券' }}
        </button>
      </div>

      <div class="section">
        <h2 class="section-title">本店优惠券列表</h2>
        <table class="data-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>类型</th>
              <th>规则</th>
              <th>门槛</th>
              <th>已领/总量</th>
              <th>有效期</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in couponList" :key="c.id">
              <td>{{ c.name }}</td>
              <td>{{ c.type === 'FULL_REDUCTION' ? '满减' : '折扣' }}</td>
              <td>{{ couponRuleText(c) }}</td>
              <td>{{ Number(c.threshold || 0) }}</td>
              <td>{{ c.issued }}/{{ c.total }}</td>
              <td class="time">{{ fmt(c.startTime) }} ~ {{ fmt(c.endTime) }}</td>
              <td>
                <span class="badge" :class="c.status === 'ACTIVE' ? 'on' : 'off'">
                  {{ c.status === 'ACTIVE' ? '上架' : '下架' }}
                </span>
              </td>
              <td>
                <button class="btn-toggle" @click="handleToggleCoupon(c)">
                  {{ c.status === 'ACTIVE' ? '下架' : '上架' }}
                </button>
              </td>
            </tr>
            <tr v-if="couponList.length === 0">
              <td colspan="8" class="empty">本店暂无优惠券</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- ===== 限时秒杀 ===== -->
    <section v-show="tab === 'seckill'" class="panel">
      <div class="section">
        <h2 class="section-title">创建本店秒杀</h2>
        <div class="form-grid">
          <div class="form-group">
            <label>选择本店商品 <span class="req">*</span></label>
            <select v-model="seckillForm.productId" :disabled="skuLoading" @change="onProductChange">
              <option value="">请选择商品</option>
              <option v-for="p in products" :key="p.id" :value="p.id">{{ p.name }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>选择规格（SKU） <span class="req">*</span></label>
            <select v-model="seckillForm.productDetailId" :disabled="!seckillForm.productId || skuLoading">
              <option value="">请选择规格</option>
              <option v-for="s in skus" :key="s.id" :value="s.id">
                {{ skuLabel(s) }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>秒杀价（元） <span class="req">*</span></label>
            <input v-model="seckillForm.seckillPrice" type="number" min="0.01" step="0.01" />
          </div>
          <div class="form-group">
            <label>秒杀库存 <span class="req">*</span></label>
            <input v-model="seckillForm.totalStock" type="number" min="1" step="1" />
          </div>
          <div class="form-group">
            <label>生效开始 <span class="req">*</span></label>
            <input v-model="seckillForm.startTime" type="datetime-local" />
          </div>
          <div class="form-group">
            <label>生效结束 <span class="req">*</span></label>
            <input v-model="seckillForm.endTime" type="datetime-local" />
          </div>
        </div>
        <button class="btn-create" :disabled="seckillCreating" @click="handleCreateSeckill">
          {{ seckillCreating ? '创建中...' : '创建秒杀' }}
        </button>
      </div>

      <div class="section">
        <h2 class="section-title">本店秒杀列表</h2>
        <table class="data-table">
          <thead>
            <tr>
              <th>商品</th>
              <th>秒杀价</th>
              <th>原价</th>
              <th>剩余/总量</th>
              <th>有效期</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in seckillList" :key="a.id">
              <td class="name">{{ a.productName || a.productDetailId }}</td>
              <td class="price">¥{{ formatPrice(a.seckillPrice) }}</td>
              <td class="price">¥{{ formatPrice(a.originalPrice) }}</td>
              <td>{{ a.remainStock }}/{{ a.totalStock }}</td>
              <td class="time">{{ fmt(a.startTime) }} ~ {{ fmt(a.endTime) }}</td>
              <td>
                <span class="badge" :class="seckillStatusClass(a.status)">{{ seckillStatusText(a.status) }}</span>
              </td>
              <td>
                <button class="btn-toggle" @click="handleToggleSeckill(a)">
                  {{ a.status === 'ONGOING' ? '结束' : '开始' }}
                </button>
              </td>
            </tr>
            <tr v-if="seckillList.length === 0">
              <td colspan="7" class="empty">本店暂无秒杀活动</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { toast, apiError } from '../../utils/toast'
import { couponRuleText } from '../../utils/coupon'
import {
  createMerchantCoupon,
  listMerchantCoupons,
  toggleMerchantCoupon,
  createMerchantSeckill,
  listMerchantSeckills,
  toggleMerchantSeckill,
  listMerchantProducts
} from '../../api/merchant'
import { getProductDetails } from '../../api/product'

const tab = ref('coupon')

// ---------- 优惠券 ----------
const couponForm = ref({
  name: '',
  type: 'FULL_REDUCTION',
  threshold: 0,
  amount: 0,
  discount: 0.95,
  maxDiscount: '',
  total: 100,
  perLimit: 1,
  startTime: '',
  endTime: ''
})
const couponCreating = ref(false)
const couponList = ref([])

function toLocalInput(d) {
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function fmt(t) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return String(t)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// datetime-local → yyyy-MM-dd HH:mm:ss（后端接受）
function toServerTime(local) {
  if (!local) return null
  const d = new Date(local)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

async function loadCoupons() {
  try {
    const res = await listMerchantCoupons(1, 50)
    const p = res.data || {}
    couponList.value = p.data || []
  } catch (e) {
    apiError(e, '加载优惠券失败')
  }
}

function handleCreateCoupon() {
  if (!couponForm.value.name) {
    toast.warning('请填写券名称')
    return
  }
  if (!couponForm.value.startTime || !couponForm.value.endTime) {
    toast.warning('请选择生效起止时间')
    return
  }
  if (new Date(couponForm.value.endTime) <= new Date(couponForm.value.startTime)) {
    toast.warning('结束时间须晚于开始时间')
    return
  }
  if (Number(couponForm.value.total) < 0) {
    toast.warning('发放总量不能为负')
    return
  }
  couponCreating.value = true
  const payload = {
    name: couponForm.value.name,
    type: couponForm.value.type,
    threshold: Number(couponForm.value.threshold || 0),
    amount: couponForm.value.type === 'FULL_REDUCTION' ? Number(couponForm.value.amount || 0) : 0,
    discount: couponForm.value.type === 'DISCOUNT' ? Number(couponForm.value.discount || 1) : 1,
    maxDiscount: couponForm.value.maxDiscount === '' ? null : Number(couponForm.value.maxDiscount),
    total: Number(couponForm.value.total || 0),
    perLimit: Number(couponForm.value.perLimit || 1),
    startTime: toServerTime(couponForm.value.startTime),
    endTime: toServerTime(couponForm.value.endTime),
    status: 'ACTIVE'
  }
  createMerchantCoupon(payload)
    .then(() => {
      toast.success('创建成功')
      loadCoupons()
      resetCouponForm()
    })
    .catch(e => apiError(e, '创建失败'))
    .finally(() => { couponCreating.value = false })
}

function handleToggleCoupon(c) {
  const next = c.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  toggleMerchantCoupon(c.id, next)
    .then(() => loadCoupons())
    .catch(e => apiError(e, '操作失败'))
}

function resetCouponForm() {
  const now = new Date()
  const later = new Date(now.getTime() + 7 * 86400000)
  couponForm.value = {
    name: '',
    type: 'FULL_REDUCTION',
    threshold: 0,
    amount: 0,
    discount: 0.95,
    maxDiscount: '',
    total: 100,
    perLimit: 1,
    startTime: toLocalInput(now),
    endTime: toLocalInput(later)
  }
}

// ---------- 限时秒杀 ----------
const seckillForm = ref({
  productId: '',
  productDetailId: '',
  seckillPrice: '',
  totalStock: '',
  startTime: '',
  endTime: ''
})
const seckillCreating = ref(false)
const seckillList = ref([])
const products = ref([])
const skus = ref([])
const skuLoading = ref(false)

function formatPrice(v) {
  const n = Number(v || 0)
  return n.toFixed(2)
}

function seckillStatusText(s) {
  const map = { NOT_START: '未开始', ONGOING: '进行中', CLOSED: '已关闭', ENDED: '已结束' }
  return map[s] || s
}

function seckillStatusClass(s) {
  if (s === 'ONGOING') return 'on'
  if (s === 'NOT_START') return 'wait'
  return 'off'
}

function skuLabel(s) {
  const size = s.size || ''
  const price = s.price != null ? ` ¥${formatPrice(s.price)}` : ''
  return size ? `规格 ${size}${price}` : (s.id || '未知规格')
}

async function loadProducts() {
  try {
    const res = await listMerchantProducts(1, 200)
    const p = res.data || {}
    products.value = p.data || []
  } catch (e) {
    apiError(e, '加载本店商品失败')
  }
}

async function onProductChange() {
  seckillForm.value.productDetailId = ''
  skus.value = []
  const pid = seckillForm.value.productId
  if (!pid) return
  skuLoading.value = true
  try {
    const res = await getProductDetails(pid)
    skus.value = res.data || []
    if (skus.value.length === 0) {
      toast.warning('该商品暂无可选规格（SKU）')
    }
  } catch (e) {
    apiError(e, '加载商品规格失败')
  } finally {
    skuLoading.value = false
  }
}

async function loadSeckills() {
  try {
    const res = await listMerchantSeckills('', 1, 50)
    const p = res.data || {}
    seckillList.value = p.data || []
  } catch (e) {
    apiError(e, '加载秒杀活动失败')
  }
}

function handleCreateSeckill() {
  if (!seckillForm.value.productDetailId) {
    toast.warning('请选择本店商品规格（SKU）')
    return
  }
  if (!(Number(seckillForm.value.seckillPrice) > 0)) {
    toast.warning('秒杀价须大于 0')
    return
  }
  if (!(Number(seckillForm.value.totalStock) > 0)) {
    toast.warning('秒杀库存须大于 0')
    return
  }
  if (!seckillForm.value.startTime || !seckillForm.value.endTime) {
    toast.warning('请选择生效起止时间')
    return
  }
  if (new Date(seckillForm.value.endTime) <= new Date(seckillForm.value.startTime)) {
    toast.warning('结束时间须晚于开始时间')
    return
  }
  seckillCreating.value = true
  const payload = {
    productDetailId: seckillForm.value.productDetailId,
    seckillPrice: Number(seckillForm.value.seckillPrice),
    totalStock: Number(seckillForm.value.totalStock),
    startTime: toServerTime(seckillForm.value.startTime),
    endTime: toServerTime(seckillForm.value.endTime)
  }
  createMerchantSeckill(payload)
    .then(() => {
      toast.success('创建成功')
      loadSeckills()
      resetSeckillForm()
    })
    .catch(e => apiError(e, '创建失败'))
    .finally(() => { seckillCreating.value = false })
}

function handleToggleSeckill(a) {
  // 非进行中 → 开始（ONGOING）；进行中 → 结束（CLOSED）
  const next = a.status === 'ONGOING' ? 'CLOSED' : 'ONGOING'
  toggleMerchantSeckill(a.id, next)
    .then(() => loadSeckills())
    .catch(e => apiError(e, '操作失败'))
}

function resetSeckillForm() {
  const now = new Date()
  const later = new Date(now.getTime() + 7 * 86400000)
  seckillForm.value = {
    productId: '',
    productDetailId: '',
    seckillPrice: '',
    totalStock: '',
    startTime: toLocalInput(now),
    endTime: toLocalInput(later)
  }
  skus.value = []
}

onMounted(() => {
  resetCouponForm()
  resetSeckillForm()
  loadCoupons()
  loadSeckills()
  loadProducts()
})
</script>

<style scoped>
.merchant-marketing {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
}

.page-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: -8px;
}

.tabs {
  display: flex;
  gap: 8px;
}

.tab {
  padding: 9px 22px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.tab.active {
  color: #fff;
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section {
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  padding: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.req {
  color: var(--color-danger);
}

.form-group input,
.form-group select {
  padding: 9px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 14px;
  outline: none;
  font-family: var(--font-sans);
}

.form-group input:focus,
.form-group select:focus {
  border-color: var(--color-primary);
}

.btn-create {
  margin-top: 16px;
  padding: 10px 28px;
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  border: none;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.btn-create:hover {
  background: var(--color-primary-dark);
}

.btn-create:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table th,
.data-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid var(--color-divider);
  color: var(--color-text);
}

.data-table th {
  color: var(--color-text-tertiary);
  font-weight: 600;
}

.data-table .time {
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.data-table .price {
  color: var(--color-primary);
  font-weight: 600;
}

.badge {
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
}

.badge.on {
  color: var(--color-success);
  background: var(--color-success-light);
}

.badge.wait {
  color: var(--color-warning);
  background: var(--color-warning-light);
}

.badge.off {
  color: var(--color-text-tertiary);
  background: var(--color-bg-overlay);
}

.btn-toggle {
  padding: 5px 14px;
  background: transparent;
  border: 1px solid var(--color-primary);
  color: var(--color-primary);
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
}

.empty {
  text-align: center;
  color: var(--color-text-tertiary);
  padding: 20px 0;
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}

/* ========== D1（v1.9）移动端适配：控制台表格横向滚动（口径见 qinghe-skins.css --sheet-* 令牌） ========== */
@media (max-width: 640px) {
  .data-table {
    display: block;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
}
</style>
