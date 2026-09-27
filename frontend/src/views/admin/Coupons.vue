<template>
  <div class="admin-coupons">
    <div class="section">
      <h2 class="section-title">创建优惠券</h2>
      <div class="form-grid">
        <div class="form-group">
          <label>券名称 <span class="req">*</span></label>
          <input v-model.trim="form.name" type="text" maxlength="64" placeholder="如：新人满减券" />
        </div>
        <div class="form-group">
          <label>类型 <span class="req">*</span></label>
          <select v-model="form.type">
            <option value="FULL_REDUCTION">满减券</option>
            <option value="DISCOUNT">折扣券</option>
          </select>
        </div>
        <div class="form-group">
          <label>使用门槛（元，0 表示无门槛）</label>
          <input v-model="form.threshold" type="number" min="0" step="0.01" />
        </div>
        <div v-if="form.type === 'FULL_REDUCTION'" class="form-group">
          <label>满减面额（元） <span class="req">*</span></label>
          <input v-model="form.amount" type="number" min="0" step="0.01" />
        </div>
        <div v-if="form.type === 'DISCOUNT'" class="form-group">
          <label>折扣率（如 0.85 表示 85 折） <span class="req">*</span></label>
          <input v-model="form.discount" type="number" min="0.01" max="1" step="0.01" />
        </div>
        <div v-if="form.type === 'DISCOUNT'" class="form-group">
          <label>折扣封顶（元，可空）</label>
          <input v-model="form.maxDiscount" type="number" min="0" step="0.01" placeholder="不封顶留空" />
        </div>
        <div class="form-group">
          <label>发放总量 <span class="req">*</span></label>
          <input v-model="form.total" type="number" min="0" step="1" />
        </div>
        <div class="form-group">
          <label>每人限领（张）</label>
          <input v-model="form.perLimit" type="number" min="1" step="1" />
        </div>
        <div class="form-group">
          <label>生效开始 <span class="req">*</span></label>
          <input v-model="form.startTime" type="datetime-local" />
        </div>
        <div class="form-group">
          <label>生效结束 <span class="req">*</span></label>
          <input v-model="form.endTime" type="datetime-local" />
        </div>
      </div>
      <button class="btn-create" :disabled="creating" @click="handleCreate">
        {{ creating ? '创建中...' : '创建优惠券' }}
      </button>
    </div>

    <div class="section">
      <h2 class="section-title">优惠券列表</h2>
      <table class="coupon-table">
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
          <tr v-for="c in list" :key="c.id">
            <td>{{ c.name }}</td>
            <td>{{ c.type === 'FULL_REDUCTION' ? '满减' : '折扣' }}</td>
            <td>{{ ruleText(c) }}</td>
            <td>{{ Number(c.threshold || 0) }}</td>
            <td>{{ c.issued }}/{{ c.total }}</td>
            <td class="time">{{ fmt(c.startTime) }} ~ {{ fmt(c.endTime) }}</td>
            <td>
              <span class="badge" :class="c.status === 'ACTIVE' ? 'on' : 'off'">
                {{ c.status === 'ACTIVE' ? '上架' : '下架' }}
              </span>
            </td>
            <td>
              <button class="btn-toggle" @click="handleToggle(c)">
                {{ c.status === 'ACTIVE' ? '下架' : '上架' }}
              </button>
            </td>
          </tr>
          <tr v-if="list.length === 0">
            <td colspan="8" class="empty">暂无优惠券</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { toast, apiError } from '../../utils/toast'
import { createCoupon, listCoupons, toggleCoupon } from '../../api/admin'
import { couponRuleText } from '../../utils/coupon'

const form = ref({
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
const creating = ref(false)
const list = ref([])

function toLocalInput(d) {
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function ruleText(c) {
  return couponRuleText(c)
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

async function loadList() {
  try {
    const res = await listCoupons(1, 50)
    list.value = (res.data && res.data.data) || []
  } catch (e) {
    apiError(e, '加载列表失败')
  }
}

function handleCreate() {
  if (!form.value.name) {
    toast.warning('请填写券名称')
    return
  }
  if (!form.value.startTime || !form.value.endTime) {
    toast.warning('请选择生效起止时间')
    return
  }
  if (new Date(form.value.endTime) <= new Date(form.value.startTime)) {
    toast.warning('结束时间须晚于开始时间')
    return
  }
  if (Number(form.value.total) < 0) {
    toast.warning('发放总量不能为负')
    return
  }
  creating.value = true
  const payload = {
    name: form.value.name,
    type: form.value.type,
    threshold: Number(form.value.threshold || 0),
    amount: form.value.type === 'FULL_REDUCTION' ? Number(form.value.amount || 0) : 0,
    discount: form.value.type === 'DISCOUNT' ? Number(form.value.discount || 1) : 1,
    maxDiscount: form.value.maxDiscount === '' ? null : Number(form.value.maxDiscount),
    total: Number(form.value.total || 0),
    perLimit: Number(form.value.perLimit || 1),
    startTime: toServerTime(form.value.startTime),
    endTime: toServerTime(form.value.endTime),
    status: 'ACTIVE'
  }
  createCoupon(payload)
    .then(() => {
      toast.success('创建成功')
      loadList()
      resetForm()
    })
    .catch(e => {
      apiError(e, '创建失败')
    })
    .finally(() => {
      creating.value = false
    })
}

function handleToggle(c) {
  const next = c.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  toggleCoupon(c.id, next)
    .then(() => loadList())
    .catch(e => apiError(e, '操作失败'))
}

function resetForm() {
  const now = new Date()
  const later = new Date(now.getTime() + 7 * 86400000)
  form.value = {
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

onMounted(() => {
  resetForm()
  loadList()
})
</script>

<style scoped>
.admin-coupons {
  display: flex;
  flex-direction: column;
  gap: 24px;
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
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  border-radius: var(--radius-sm);
}

.btn-create:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.coupon-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.coupon-table th,
.coupon-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid var(--color-divider);
  color: var(--color-text);
}

.coupon-table th {
  color: var(--color-text-tertiary);
  font-weight: 600;
}

.coupon-table .time {
  color: var(--color-text-tertiary);
  font-size: 12px;
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
}

.empty {
  text-align: center;
  color: var(--color-text-tertiary);
  padding: 20px 0;
}

/* ========== D1（v1.9）移动端适配：控制台表格横向滚动（口径见 qinghe-skins.css --sheet-* 令牌） ========== */
@media (max-width: 640px) {
  .coupon-table {
    display: block;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
}
</style>
