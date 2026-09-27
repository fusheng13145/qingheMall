<template>
  <div class="admin-seckill">
    <div class="section">
      <h2 class="section-title">创建秒杀活动</h2>
      <div class="form-grid">
        <div class="form-group">
          <label>商品规格ID <span class="req">*</span></label>
          <input v-model.trim="form.productDetailId" type="text" maxlength="64" placeholder="如 pd001（可在商品管理中查看）" />
        </div>
        <div class="form-group">
          <label>秒杀价（元） <span class="req">*</span></label>
          <input v-model="form.seckillPrice" type="number" min="0.01" step="0.01" />
        </div>
        <div class="form-group">
          <label>活动库存（件） <span class="req">*</span></label>
          <input v-model="form.totalStock" type="number" min="1" step="1" />
        </div>
        <div class="form-group">
          <label>开始时间 <span class="req">*</span></label>
          <input v-model="form.startTime" type="datetime-local" />
        </div>
        <div class="form-group">
          <label>结束时间 <span class="req">*</span></label>
          <input v-model="form.endTime" type="datetime-local" />
        </div>
      </div>
      <p class="form-hint">创建后状态为「未开始」，请在列表中点击「开始」使其生效（进行中且处于时间窗口内用户方可抢购）。</p>
      <button class="btn-create" :disabled="creating" @click="handleCreate">
        {{ creating ? '创建中...' : '创建活动' }}
      </button>
    </div>

    <div class="section">
      <h2 class="section-title">秒杀活动列表</h2>
      <table class="seckill-table">
        <thead>
          <tr>
            <th>商品</th>
            <th>秒杀价</th>
            <th>原价</th>
            <th>剩余/总量</th>
            <th>起止时间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in list" :key="a.id">
            <td class="name">{{ a.productName || a.productDetailId }}</td>
            <td class="price">¥{{ formatPrice(a.seckillPrice) }}</td>
            <td class="muted">¥{{ formatPrice(a.originalPrice) }}</td>
            <td>{{ a.remainStock }}/{{ a.totalStock }}</td>
            <td class="time">{{ fmt(a.startTime) }} ~ {{ fmt(a.endTime) }}</td>
            <td>
              <span class="badge" :class="statusClass(a.status)">{{ statusText(a.status) }}</span>
            </td>
            <td>
              <button class="btn-toggle" @click="handleToggle(a)">
                {{ a.status === 'ONGOING' ? '结束' : '开始' }}
              </button>
            </td>
          </tr>
          <tr v-if="list.length === 0">
            <td colspan="7" class="empty">暂无秒杀活动</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { toast, apiError } from '../../utils/toast'
import { createSeckill, listSeckills, toggleSeckill } from '../../api/admin'
import { formatPrice } from '../../utils/format'

const form = ref({
  productDetailId: '',
  seckillPrice: '',
  totalStock: '',
  startTime: '',
  endTime: ''
})
const creating = ref(false)
const list = ref([])

function toLocalInput(d) {
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function toServerTime(local) {
  if (!local) return null
  const d = new Date(local)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function fmt(t) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return String(t)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function statusText(s) {
  const map = { NOT_START: '未开始', ONGOING: '进行中', CLOSED: '已关闭', ENDED: '已结束' }
  return map[s] || s || '未知'
}

function statusClass(s) {
  if (s === 'ONGOING') return 'on'
  if (s === 'NOT_START') return 'wait'
  return 'off'
}

async function loadList() {
  try {
    const res = await listSeckills(1, 50)
    list.value = (res.data && res.data.data) || []
  } catch (e) {
    apiError(e, '加载列表失败')
  }
}

function handleCreate() {
  if (!form.value.productDetailId) {
    toast.warning('请填写商品规格ID')
    return
  }
  if (!(Number(form.value.seckillPrice) > 0)) {
    toast.warning('秒杀价必须大于 0')
    return
  }
  if (!(Number(form.value.totalStock) > 0)) {
    toast.warning('活动库存必须大于 0')
    return
  }
  if (!form.value.startTime || !form.value.endTime) {
    toast.warning('请选择起止时间')
    return
  }
  if (new Date(form.value.endTime) <= new Date(form.value.startTime)) {
    toast.warning('结束时间须晚于开始时间')
    return
  }
  creating.value = true
  const payload = {
    productDetailId: form.value.productDetailId,
    seckillPrice: Number(form.value.seckillPrice),
    totalStock: Number(form.value.totalStock),
    startTime: toServerTime(form.value.startTime),
    endTime: toServerTime(form.value.endTime)
  }
  createSeckill(payload)
    .then(() => {
      toast.success('创建成功（状态：未开始，可在列表中点击「开始」生效）')
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

function handleToggle(a) {
  // 非进行中 → 开始（ONGOING）；进行中 → 结束（CLOSED）
  const next = a.status === 'ONGOING' ? 'CLOSED' : 'ONGOING'
  toggleSeckill(a.id, next)
    .then(() => loadList())
    .catch(e => apiError(e, '操作失败'))
}

function resetForm() {
  const now = new Date()
  const later = new Date(now.getTime() + 2 * 3600000)
  form.value = {
    productDetailId: '',
    seckillPrice: '',
    totalStock: '',
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
.admin-seckill {
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

.form-group input {
  padding: 9px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 14px;
  outline: none;
  font-family: var(--font-sans);
}

.form-group input:focus {
  border-color: var(--color-primary);
}

.form-hint {
  margin: 14px 0 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
  line-height: 1.5;
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

.seckill-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.seckill-table th,
.seckill-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid var(--color-divider);
  color: var(--color-text);
}

.seckill-table th {
  color: var(--color-text-tertiary);
  font-weight: 600;
}

.seckill-table .name {
  font-weight: 600;
}

.seckill-table .price {
  color: var(--color-danger);
  font-weight: 700;
}

.seckill-table .muted {
  color: var(--color-text-tertiary);
}

.seckill-table .time {
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
  .seckill-table {
    display: block;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
}
</style>
