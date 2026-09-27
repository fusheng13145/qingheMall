<template>
  <div class="admin-settlement">
    <div class="toolbar">
      <select v-model="statusFilter" class="status-select" @change="load(1)">
        <option value="">全部状态</option>
        <option value="PENDING">审核中</option>
        <option value="PAID">已放款</option>
        <option value="REJECTED">已驳回</option>
      </select>
      <div class="generate-box">
        <input v-model.number="merchantIdInput" class="merchant-input" type="number" min="1" placeholder="商家 ID" />
        <button class="btn-generate" :disabled="generating" @click="generate">生成结算单</button>
      </div>
    </div>

    <table class="data-table">
      <thead>
        <tr>
          <th>结算单号</th><th>商家</th><th>货款</th><th>佣金</th><th>应放款</th><th>笔数</th><th>状态</th><th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="b in bills" :key="b.id">
          <td>{{ b.id }}</td>
          <td>#{{ b.merchantId }}</td>
          <td>¥{{ formatPrice(b.totalGross) }}</td>
          <td>¥{{ formatPrice(b.totalCommission) }}</td>
          <td>¥{{ formatPrice(b.totalNet) }}</td>
          <td>{{ b.entryCount }}</td>
          <td>{{ billStatusText(b.status) }}</td>
          <td>
            <template v-if="b.status === 'PENDING'">
              <button class="btn-approve" :disabled="reviewing" @click="review(b, true)">放款</button>
              <button class="btn-reject" :disabled="reviewing" @click="review(b, false)">驳回</button>
            </template>
            <span v-else class="reviewed-note">{{ b.reviewNote || '-' }}</span>
          </td>
        </tr>
        <tr v-if="!bills.length">
          <td colspan="8" class="empty-cell">暂无结算单</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  adminListSettlementBills,
  adminGenerateSettlementBill,
  adminReviewSettlementBill
} from '../../api/settlement'
import { formatPrice } from '../../utils/format'
import { toast } from '../../utils/toast'

const bills = ref([])
const statusFilter = ref('')
const merchantIdInput = ref('')
const generating = ref(false)
const reviewing = ref(false)

function billStatusText(status) {
  if (status === 'PAID') return '已放款'
  if (status === 'REJECTED') return '已驳回'
  if (status === 'PENDING') return '审核中'
  return status || '-'
}

async function load(page = 1) {
  try {
    const res = await adminListSettlementBills(page, 20, statusFilter.value)
    bills.value = (res.data && res.data.data) || []
  } catch (e) {
    toast.error('结算单加载失败')
  }
}

async function generate() {
  if (!merchantIdInput.value || merchantIdInput.value < 1) {
    toast.error('请输入有效的商家 ID')
    return
  }
  generating.value = true
  try {
    await adminGenerateSettlementBill(merchantIdInput.value)
    toast.success('结算单已生成')
    await load(1)
  } catch (e) {
    toast.error('生成失败（无未结算流水或商家不存在）')
  } finally {
    generating.value = false
  }
}

async function review(bill, approve) {
  reviewing.value = true
  try {
    await adminReviewSettlementBill(bill.id, approve, approve ? '对账无误，同意放款' : '金额存疑，请人工复核')
    toast.success(approve ? '已放款' : '已驳回')
    await load(1)
  } catch (e) {
    toast.error('审核失败')
  } finally {
    reviewing.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.status-select { border: 1px solid #d1d5db; border-radius: 8px; padding: 7px 10px; font-size: 13px; }
.generate-box { display: flex; gap: 8px; }
.merchant-input { width: 120px; border: 1px solid #d1d5db; border-radius: 8px; padding: 7px 10px; font-size: 13px; }
.btn-generate { background: #3b82f6; color: #fff; border: none; border-radius: 8px; padding: 7px 16px; cursor: pointer; }
.btn-approve { background: #10b981; color: #fff; border: none; border-radius: 6px; padding: 5px 12px; cursor: pointer; margin-right: 6px; }
.btn-reject { background: #ef4444; color: #fff; border: none; border-radius: 6px; padding: 5px 12px; cursor: pointer; }
.reviewed-note { color: #9ca3af; font-size: 12px; }
.empty-cell { text-align: center; color: #9ca3af; }

/* ========== D1（v1.9）移动端适配：控制台表格横向滚动（口径见 qinghe-skins.css --sheet-* 令牌） ========== */
@media (max-width: 640px) {
  .data-table {
    display: block;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
}
</style>
