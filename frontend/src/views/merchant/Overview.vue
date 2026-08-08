<template>
  <div class="overview">
    <div class="page-header">
      <h2 class="page-title">店铺概览</h2>
    </div>

    <!-- 入驻状态提示 -->
    <div v-if="merchant && merchant.status === 'PENDING'" class="status-banner pending">
      入驻申请审核中，平台通过后即可经营店铺
    </div>
    <div v-else-if="merchant && merchant.status === 'REJECTED'" class="status-banner rejected">
      入驻申请未通过：{{ merchant.rejectReason || '未填写原因' }}
    </div>
    <div v-else-if="merchant && merchant.status === 'DISABLED'" class="status-banner rejected">
      店铺已被平台禁用，请联系客服
    </div>

    <!-- 未申请开店 -->
    <div v-if="!loading && !merchant" class="empty-card">
      <p>您还不是入驻商家</p>
      <router-link to="/register" class="btn-primary">去注册商家账号（需平台审核）</router-link>
    </div>

    <!-- 店铺信息 + 统计 -->
    <template v-if="merchant">
      <div class="shop-card">
        <div class="shop-logo">{{ (merchant.shopName || '店').slice(0, 1) }}</div>
        <div class="shop-info">
          <h3 class="shop-name">{{ merchant.shopName }}</h3>
          <p class="shop-desc">{{ merchant.shopDesc || '这家店铺还没有简介' }}</p>
        </div>
      </div>

      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-label">在售商品</div>
          <div class="stat-value">{{ stats.productCount ?? '-' }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">累计订单</div>
          <div class="stat-value">{{ stats.orderCount ?? '-' }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">今日订单</div>
          <div class="stat-value">{{ stats.todayOrderCount ?? '-' }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">累计销售额</div>
          <div class="stat-value">¥{{ formatMoney(stats.paidRevenue) }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">今日销售额</div>
          <div class="stat-value">¥{{ formatMoney(stats.todayRevenue) }}</div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMerchantInfo, getMerchantStats } from '../../api/merchant'

const loading = ref(true)
const merchant = ref(null)
const stats = ref({})

function formatMoney(value) {
  const num = Number(value || 0)
  return num.toFixed(2)
}

onMounted(async () => {
  try {
    merchant.value = (await getMerchantInfo()).data || null
    if (merchant.value && merchant.value.status === 'ACTIVE') {
      stats.value = (await getMerchantStats()).data || {}
    }
  } catch (e) {
    // 静默：入驻状态下接口不可用时保留空态
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
}

.status-banner {
  padding: 14px 18px;
  border-radius: var(--radius-md);
  font-size: 14px;
  margin-bottom: 20px;
}

.status-banner.pending {
  background: var(--color-warning-light, #FAEEDA);
  color: var(--color-warning, #854F0B);
}

.status-banner.rejected {
  background: var(--color-danger-light);
  color: var(--color-danger);
}

.empty-card {
  padding: 60px 24px;
  text-align: center;
  background: var(--color-bg-elevated);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
  color: var(--color-text-secondary);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.btn-primary {
  padding: 10px 20px;
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: 14px;
  transition: opacity var(--transition-fast);
}

.btn-primary:hover {
  opacity: 0.85;
}

.shop-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  margin-bottom: 20px;
}

.shop-logo {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  background: var(--color-primary-50);
  color: var(--color-primary);
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.shop-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
}

.shop-desc {
  font-size: 13px;
  color: var(--color-text-tertiary);
  margin-top: 4px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.stat-card {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 20px;
}

.stat-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary);
}
</style>
