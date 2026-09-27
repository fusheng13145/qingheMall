<template>
  <div class="merchants">
    <div class="page-header">
      <h2 class="page-title">商家入驻审核</h2>
      <div class="tabs">
        <button v-for="t in tabs" :key="t.value" class="tab" :class="{ active: status === t.value }" @click="switchTab(t.value)">
          {{ t.label }}
        </button>
      </div>
    </div>

    <div class="table-card">
      <table class="data-table">
        <thead>
          <tr>
            <th>店铺名称</th>
            <th>店主</th>
            <th>简介</th>
            <th>申请时间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in merchants" :key="m.id">
            <td class="shop-name">{{ m.shopName }}</td>
            <td>{{ m.nickName || m.userName }}</td>
            <td class="desc">{{ m.shopDesc || '-' }}</td>
            <td class="time">{{ formatTime(m.gmtCreated) }}</td>
            <td><span class="tag" :class="'tag-' + statusClass(m.status)">{{ statusText(m.status) }}</span></td>
            <td>
              <div v-if="m.status === 'PENDING'" class="row-actions">
                <button class="link-btn" @click="audit(m, true)">通过</button>
                <button class="link-btn danger" @click="audit(m, false)">驳回</button>
              </div>
              <span v-else class="muted">
                {{ m.status === 'REJECTED' ? (m.rejectReason || '已驳回') : '-' }}
              </span>
            </td>
          </tr>
          <tr v-if="!loading && merchants.length === 0">
            <td colspan="6" class="empty-row">暂无商家申请</td>
          </tr>
        </tbody>
      </table>
      <div class="pager">
        <button class="btn-ghost" :disabled="pageNum <= 1" @click="load(pageNum - 1)">上一页</button>
        <span class="page-info">第 {{ pageNum }} / {{ totalPage || 1 }} 页（共 {{ totalCount }} 家）</span>
        <button class="btn-ghost" :disabled="pageNum >= totalPage" @click="load(pageNum + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { toast, apiError } from '../../utils/toast'
import { listMerchants, auditMerchant } from '../../api/admin'

const tabs = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'ACTIVE' },
  { label: '已驳回', value: 'REJECTED' }
]

const merchants = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const totalPage = ref(1)
const totalCount = ref(0)
const status = ref('PENDING')

function statusText(s) {
  return { PENDING: '待审核', ACTIVE: '已通过', REJECTED: '已驳回', DISABLED: '已禁用' }[s] || s
}

function statusClass(s) {
  if (s === 'ACTIVE') return 'on'
  if (s === 'PENDING') return 'warn'
  return 'off'
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
    const res = await listMerchants(page, pageSize.value, status.value)
    pageNum.value = page
    totalPage.value = res.data.totalPage
    totalCount.value = res.data.totalCount
    merchants.value = res.data.data
  } catch (e) {
    apiError(e, '加载失败')
  } finally {
    loading.value = false
  }
}

async function audit(m, approve) {
  let reason = ''
  if (!approve) {
    reason = window.prompt(`驳回「${m.shopName}」的入驻申请，请填写原因：`, '资料不完整')
    if (reason === null) return
    if (!reason.trim()) {
      toast.warning('驳回必须填写原因')
      return
    }
  } else if (!confirm(`确认通过「${m.shopName}」的入驻申请？`)) {
    return
  }
  try {
    await auditMerchant(m.id, approve, reason.trim())
    toast.success(approve ? '已通过审核，商家可开始经营' : '已驳回')
    load(pageNum.value)
  } catch (e) {
    apiError(e, '操作失败')
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
}

.tabs {
  display: flex;
  gap: 8px;
}

.tab {
  padding: 7px 14px;
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
  padding: 12px 16px;
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

.shop-name {
  font-weight: 600;
  color: var(--color-text);
}

.desc {
  color: var(--color-text-secondary);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time {
  color: var(--color-text-tertiary);
  font-size: 13px;
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
  font-size: 13px;
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

/* ========== D1（v1.9）移动端适配：控制台表格横向滚动（口径见 qinghe-skins.css --sheet-* 令牌） ========== */
@media (max-width: 640px) {
  .data-table {
    display: block;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
}
</style>
