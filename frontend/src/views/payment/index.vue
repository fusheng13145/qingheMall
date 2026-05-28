<template>
  <div class="payment-page">
    <div class="container">
      <div class="payment-content">
        <!-- 订单信息 -->
        <div class="order-info-card">
          <div class="order-amount">
            <span class="label">应付金额</span>
            <span class="amount">¥{{ orderAmount.toFixed(2) }}</span>
          </div>
          <div class="order-no">订单号: {{ orderNo }}</div>
          <div class="order-time" v-if="countdown > 0">
            <span class="countdown-label">剩余支付时间:</span>
            <span class="countdown-value">{{ formattedCountdown }}</span>
          </div>
        </div>

        <!-- 支付方式 -->
        <div class="pay-method-card">
          <h3 class="card-title">选择支付方式</h3>
          <div class="pay-methods">
            <div
              :class="['pay-method-item', { active: selectedMethod === 'alipay' }]"
              @click="selectedMethod = 'alipay'"
            >
              <span class="method-icon alipay">支付宝</span>
              <span class="method-name">支付宝</span>
              <el-icon v-if="selectedMethod === 'alipay'" class="check-icon"><Check /></el-icon>
            </div>
            <div
              :class="['pay-method-item', { active: selectedMethod === 'wechat' }]"
              @click="selectedMethod = 'wechat'"
            >
              <span class="method-icon wechat">微信</span>
              <span class="method-name">微信支付</span>
              <el-icon v-if="selectedMethod === 'wechat'" class="check-icon"><Check /></el-icon>
            </div>
            <div
              :class="['pay-method-item', { active: selectedMethod === 'unionpay' }]"
              @click="selectedMethod = 'unionpay'"
            >
              <span class="method-icon unionpay">银联</span>
              <span class="method-name">银行卡</span>
              <el-icon v-if="selectedMethod === 'unionpay'" class="check-icon"><Check /></el-icon>
            </div>
          </div>
        </div>

        <!-- 二维码展示区域 -->
        <div class="qrcode-card" v-if="showQrCode">
          <div class="qrcode-header">
            <span class="qrcode-title">请使用{{ methodName }}扫码支付</span>
          </div>
          <div class="qrcode-content">
            <div class="qrcode-wrapper">
              <!-- 模拟二维码 -->
              <div class="qrcode-placeholder">
                <el-icon size="120"><QrCode /></el-icon>
              </div>
            </div>
            <div class="qrcode-hint">
              <p>请在 <em>{{ formattedCountdown }}</em> 内完成支付</p>
              <p>扫码支付完成后，点击"已完成支付"按钮</p>
            </div>
          </div>
          <div class="qrcode-actions">
            <el-button type="primary" :loading="querying" @click="handleQueryPay">
              已完成支付
            </el-button>
            <el-button @click="handleChangeMethod">更换支付方式</el-button>
          </div>
        </div>

        <!-- 支付按钮 -->
        <div class="pay-action" v-if="!showQrCode">
          <el-button type="primary" size="large" :loading="paying" @click="handleShowQrCode">
            确认支付 ¥{{ orderAmount.toFixed(2) }}
          </el-button>
        </div>

        <!-- 取消支付 -->
        <div class="cancel-action" v-if="!showQrCode">
          <el-button text @click="handleCancelPay">取消支付</el-button>
        </div>

        <!-- 支付提示 -->
        <div class="pay-tips">
          <p>1. 请在 <em>30分钟</em> 内完成支付，超时订单将自动关闭</p>
          <p>2. 支付成功后，请注意查收短信通知</p>
          <p>3. 如遇支付问题，请联系客服</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, QrCode } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const orderNo = ref('')
const orderAmount = ref(0)
const selectedMethod = ref<'alipay' | 'wechat' | 'unionpay'>('alipay')
const paying = ref(false)
const querying = ref(false)
const showQrCode = ref(false)

// 倒计时 30分钟
const countdown = ref(30 * 60)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const formattedCountdown = computed(() => {
  const minutes = Math.floor(countdown.value / 60)
  const seconds = countdown.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const methodName = computed(() => {
  switch (selectedMethod.value) {
    case 'alipay': return '支付宝'
    case 'wechat': return '微信'
    case 'unionpay': return '银联'
    default: return ''
  }
})

const startCountdown = () => {
  countdownTimer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    } else {
      handleTimeout()
    }
  }, 1000)
}

const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

const handleTimeout = () => {
  stopCountdown()
  ElMessage.warning('支付超时，订单已关闭')
  router.replace('/order/list')
}

const handleShowQrCode = () => {
  showQrCode.value = true
  startCountdown()
}

const handleChangeMethod = () => {
  showQrCode.value = false
  stopCountdown()
  countdown.value = 30 * 60
}

const handleQueryPay = async () => {
  querying.value = true
  try {
    // 模拟查询支付结果
    await new Promise(resolve => setTimeout(resolve, 1500))
    
    // 模拟支付成功
    const success = Math.random() > 0.2 // 80% 概率成功
    
    if (success) {
      stopCountdown()
      ElMessage.success('支付成功')
      router.replace('/order/list')
    } else {
      ElMessage.info('暂未收到支付结果，请稍后再试')
    }
  } catch {
    ElMessage.error('查询失败')
  } finally {
    querying.value = false
  }
}

const handlePay = async () => {
  paying.value = true
  try {
    // 模拟支付
    await new Promise(resolve => setTimeout(resolve, 2000))
    ElMessage.success('支付成功')
    stopCountdown()
    router.replace('/order/list')
  } catch (error) {
    ElMessage.error('支付失败')
  } finally {
    paying.value = false
  }
}

const handleCancelPay = async () => {
  try {
    await ElMessageBox.confirm('确定要取消支付吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '继续支付',
      type: 'warning'
    })
    stopCountdown()
    router.replace('/order/confirm')
  } catch {
    // 继续支付
  }
}

onMounted(() => {
  orderNo.value = route.query.orderNo as string || 'ORDER20240001'
  orderAmount.value = 299

  if (!orderNo.value) {
    router.replace('/order/list')
  }
})

onUnmounted(() => {
  stopCountdown()
})
</script>

<style lang="scss" scoped>
.payment-page {
  background-color: #f5f7fa;
  min-height: 100vh;
  padding: 40px 0;
}

.container {
  width: 800px;
  margin: 0 auto;
  padding: 0 16px;
}

.payment-content {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
}

.order-info-card {
  text-align: center;
  padding: 30px 0;
  border-bottom: 1px solid #f0f2f5;
  margin-bottom: 30px;

  .order-amount {
    margin-bottom: 16px;

    .label {
      font-size: 14px;
      color: #909399;
      margin-right: 8px;
    }

    .amount {
      font-size: 32px;
      font-weight: bold;
      color: #f56c6c;
    }
  }

  .order-no {
    font-size: 13px;
    color: #909399;
  }

  .order-time {
    margin-top: 12px;

    .countdown-label {
      font-size: 13px;
      color: #909399;
      margin-right: 8px;
    }

    .countdown-value {
      font-size: 18px;
      font-weight: bold;
      color: #f56c6c;
    }
  }
}

.pay-method-card {
  .card-title {
    font-size: 16px;
    font-weight: bold;
    color: #303133;
    margin-bottom: 20px;
  }
}

.pay-methods {
  display: flex;
  gap: 16px;
}

.pay-method-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;

  &:hover {
    border-color: #409eff;
  }

  &.active {
    border-color: #409eff;
    background: #f0f7ff;
  }

  .method-icon {
    width: 48px;
    height: 48px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: bold;

    &.alipay {
      background: #1677ff;
      color: #fff;
    }

    &.wechat {
      background: #07c160;
      color: #fff;
    }

    &.unionpay {
      background: #e8683a;
      color: #fff;
    }
  }

  .method-name {
    font-size: 14px;
    color: #303133;
  }

  .check-icon {
    position: absolute;
    top: 8px;
    right: 8px;
    color: #409eff;
    font-size: 16px;
  }
}

.qrcode-card {
  margin-top: 30px;
  padding: 30px;
  background: #fafafa;
  border-radius: 8px;

  .qrcode-header {
    text-align: center;
    margin-bottom: 24px;

    .qrcode-title {
      font-size: 16px;
      font-weight: bold;
      color: #303133;
    }
  }

  .qrcode-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 24px;
  }

  .qrcode-wrapper {
    padding: 16px;
    background: #fff;
    border-radius: 8px;
    border: 1px solid #e4e7ed;

    .qrcode-placeholder {
      width: 180px;
      height: 180px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f5f7fa;
      color: #909399;
    }
  }

  .qrcode-hint {
    text-align: center;

    p {
      font-size: 13px;
      color: #606266;
      line-height: 1.8;

      em {
        color: #f56c6c;
        font-style: normal;
        font-weight: bold;
      }
    }
  }

  .qrcode-actions {
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-top: 24px;
  }
}

.pay-action {
  margin-top: 40px;
  text-align: center;

  :deep(.el-button) {
    width: 200px;
    height: 48px;
    font-size: 16px;
  }
}

.cancel-action {
  margin-top: 16px;
  text-align: center;
}

.pay-tips {
  margin-top: 30px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;

  p {
    font-size: 13px;
    color: #909399;
    line-height: 2;

    em {
      color: #f56c6c;
      font-style: normal;
    }
  }
}
</style>
