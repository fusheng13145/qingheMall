<template>
  <div class="settlement">
    <div class="page-header">
      <h2 class="page-title">结算中心</h2>
      <p class="page-desc">确认收货的订单货款自动分账入账，平台按费率抽取佣金；退款订单自动冲销</p>
    </div>

    <!-- 概览 -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">可结算余额</div>
        <div class="stat-value">¥{{ formatPrice(summary.balance) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">累计净入</div>
        <div class="stat-value">¥{{ formatPrice(summary.totalNet) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">平台佣金</div>
        <div class="stat-value">¥{{ formatPrice(summary.totalCommission) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已放款</div>
        <div class="stat-value">¥{{ formatPrice(summary.paidBills) }}</div>
      </div>
    </div>

    <!-- 结算单 -->
    <section class="panel">
      <h3 class="panel-title">结算单（平台生成并放款）</h3>
      <table class="data-table">
        <thead>
          <tr>
            <th>结算单号</th><th>货款</th><th>佣金</th><th>应放款</th><th>笔数</th><th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="b in bills" :key="b.id">
            <td>{{ b.id }}</td>
            <td>¥{{ formatPrice(b.totalGross) }}</td>
            <td>¥{{ formatPrice(b.totalCommission) }}</td>
            <td>¥{{ formatPrice(b.totalNet) }}</td>
            <td>{{ b.entryCount }}</td>
            <td>{{ billStatusText(b.status) }}</td>
          </tr>
          <tr v-if="!bills.length">
            <td colspan="6" class="empty-cell">暂无结算单，平台对账后会按周期生成</td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 分账流水 -->
    <section class="panel">
      <h3 class="panel-title">分账流水</h3>
      <table class="data-table">
        <thead>
          <tr>
            <th>订单号</th><th>类型</th><th>实付</th><th>佣金</th><th>净入</th><th>结算单</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="l in ledger" :key="l.id">
            <td>{{ l.orderNumber }}</td>
            <td>{{ l.type === 'EARN' ? '分账' : '冲销' }}</td>
            <td>¥{{ formatPrice(l.gross) }}</td>
            <td>¥{{ formatPrice(l.commission) }}</td>
            <td :class="l.net < 0 ? 'net-negative' : ''">¥{{ formatPrice(l.net) }}</td>
            <td>{{ l.billId ? '已入单' : '未结算' }}</td>
          </tr>
          <tr v-if="!ledger.length">
            <td colspan="6" class="empty-cell">暂无分账流水，买家确认收货后自动入账</td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getSettlementSummary, listSettlementLedger, listSettlementBills } from '../../api/settlement'
import { formatPrice } from '../../utils/format'
import { toast } from '../../utils/toast'

const summary = ref({})
const ledger = ref([])
const bills = ref([])

function billStatusText(status) {
  if (status === 'PAID') return '已放款'
  if (status === 'REJECTED') return '已驳回'
  if (status === 'PENDING') return '审核中'
  return status || '-'
}

onMounted(async () => {
  try {
    const [s, l, b] = await Promise.all([
      getSettlementSummary(),
      listSettlementLedger(1, 20),
      listSettlementBills(1, 10)
    ])
    summary.value = s.data || {}
    ledger.value = (l.data && l.data.data) || []
    bills.value = (b.data && b.data.data) || []
  } catch (e) {
    toast.error('结算数据加载失败')
  }
})
</script>

<style scoped>
.page-desc { color: #6b7280; font-size: 13px; margin: 4px 0 0; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.stat-card { background: #fff; border-radius: 12px; padding: 16px; border: 1px solid #f3f4f6; }
.stat-label { font-size: 12px; color: #6b7280; }
.stat-value { font-size: 20px; font-weight: 700; margin-top: 4px; }
.panel { background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 16px; border: 1px solid #f3f4f6; }
.panel-title { margin: 0 0 12px; font-size: 15px; }
.empty-cell { text-align: center; color: #9ca3af; }
.net-negative { color: #ef4444; }

/* ========== D1（v1.9）移动端适配：控制台表格横向滚动（口径见 qinghe-skins.css --sheet-* 令牌） ========== */
@media (max-width: 640px) {
  .data-table {
    display: block;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
}
</style>
