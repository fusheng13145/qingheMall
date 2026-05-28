<template>
  <div class="order-detail-page">
    <div class="container">
      <Breadcrumb :items="breadcrumbItems" />

      <div class="order-detail" v-loading="loading">
        <!-- 订单状态进度条 -->
        <div class="order-status-card">
          <div class="status-progress">
            <el-steps :active="statusStep" finish-status="success" align-center>
              <el-step title="提交订单" :description="order.createdAt ? formatDateTime(order.createdAt) : ''" />
              <el-step title="支付成功" :description="order.payAt ? formatDateTime(order.payAt) : '待支付'" />
              <el-step title="商家发货" :description="order.shipAt ? formatDateTime(order.shipAt) : '待发货'" />
              <el-step title="确认收货" :description="order.receiveAt ? formatDateTime(order.receiveAt) : '待收货'" />
            </el-steps>
          </div>
          <div class="status-info">
            <div :class="['status-icon', `status-${order.status}`]">
              <el-icon v-if="order.status === 1"><Clock /></el-icon>
              <el-icon v-else-if="order.status === 2"><Sell /></el-icon>
              <el-icon v-else-if="order.status === 3"><Box /></el-icon>
              <el-icon v-else-if="order.status === 4"><CircleCheck /></el-icon>
              <el-icon v-else><Close /></el-icon>
            </div>
            <div class="status-text">
              <h3>{{ order.statusText }}</h3>
              <p v-if="order.status === 1">请尽快完成支付</p>
              <p v-else-if="order.status === 2">商家正在准备发货</p>
              <p v-else-if="order.status === 3">货物运输中，请注意查收</p>
              <p v-else-if="order.status === 4">交易已完成，感谢您的购买</p>
              <p v-else-if="order.status === -1">订单已取消</p>
            </div>
          </div>
          <div class="order-actions" v-if="order.status === 1">
            <el-button type="primary" @click="handlePay">去支付</el-button>
            <el-button @click="handleCancel">取消订单</el-button>
          </div>
          <div class="order-actions" v-else-if="order.status === 3">
            <el-button type="success" @click="handleConfirmReceive">确认收货</el-button>
          </div>
        </div>

        <!-- 收货信息 -->
        <div class="info-card">
          <h3 class="card-title">
            <el-icon><Location /></el-icon>
            收货信息
          </h3>
          <div class="address-info">
            <div class="address-main">
              <span class="name">{{ order.address?.name }}</span>
              <span class="phone">{{ order.address?.phone }}</span>
            </div>
            <p class="address-detail">{{ formatAddress(order.address || {}) }}</p>
          </div>
        </div>

        <!-- 商品列表 -->
        <div class="info-card">
          <h3 class="card-title">
            <el-icon><Goods /></el-icon>
            商品清单
          </h3>
          <div class="goods-list">
            <div
              class="goods-item"
              v-for="item in order.items"
              :key="item.id"
            >
              <img :src="item.image || defaultImage" class="goods-image" @click="goToGoods(item.goodsId)" />
              <div class="goods-info">
                <p class="goods-name" @click="goToGoods(item.goodsId)">{{ item.goodsName }}</p>
                <p class="goods-price">¥{{ item.price.toFixed(2) }} × {{ item.quantity }}</p>
              </div>
              <div class="goods-subtotal">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
            </div>
          </div>
        </div>

        <!-- 订单金额 -->
        <div class="info-card">
          <h3 class="card-title">
            <el-icon><Wallet /></el-icon>
            订单金额
          </h3>
          <div class="amount-detail">
            <div class="amount-row">
              <span class="label">商品总额</span>
              <span class="value">¥{{ order.totalAmount?.toFixed(2) }}</span>
            </div>
            <div class="amount-row">
              <span class="label">运费</span>
              <span class="value">{{ order.freight > 0 ? `¥${order.freight.toFixed(2)}` : '免费' }}</span>
            </div>
            <div class="amount-row" v-if="order.discountAmount > 0">
              <span class="label">优惠</span>
              <span class="value discount">-¥{{ order.discountAmount.toFixed(2) }}</span>
            </div>
            <div class="amount-row total">
              <span class="label">应付总额</span>
              <span class="value">¥{{ order.payAmount?.toFixed(2) }}</span>
            </div>
          </div>
        </div>

        <!-- 订单信息 -->
        <div class="info-card">
          <h3 class="card-title">
            <el-icon><Document /></el-icon>
            订单信息
          </h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">订单编号</span>
              <span class="value">
                {{ order.orderNo }}
                <el-button type="primary" text size="small" @click="copyOrderNo">复制</el-button>
              </span>
            </div>
            <div class="info-item">
              <span class="label">下单时间</span>
              <span class="value">{{ formatDateTime(order.createdAt) }}</span>
            </div>
            <div class="info-item" v-if="order.payAt">
              <span class="label">支付时间</span>
              <span class="value">{{ formatDateTime(order.payAt) }}</span>
            </div>
            <div class="info-item" v-if="order.shipAt">
              <span class="label">发货时间</span>
              <span class="value">{{ formatDateTime(order.shipAt) }}</span>
            </div>
            <div class="info-item" v-if="order.receiveAt">
              <span class="label">收货时间</span>
              <span class="value">{{ formatDateTime(order.receiveAt) }}</span>
            </div>
            <div class="info-item" v-if="order.remark">
              <span class="label">订单备注</span>
              <span class="value">{{ order.remark }}</span>
            </div>
          </div>
        </div>

        <!-- 操作日志时间线 -->
        <div class="info-card">
          <h3 class="card-title">
            <el-icon><Timer /></el-icon>
            操作日志
          </h3>
          <el-timeline>
            <el-timeline-item
              v-for="(log, index) in orderLogs"
              :key="index"
              :timestamp="log.time"
              :type="log.type"
              :hollow="log.hollow"
            >
              <div class="log-content">
                <span class="log-title">{{ log.title }}</span>
                <span class="log-desc" v-if="log.description">{{ log.description }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Sell, Box, CircleCheck, Close, Location, Goods, Wallet, Document, Timer } from '@element-plus/icons-vue'
import Breadcrumb from '@/components/common/Breadcrumb.vue'
import { formatDateTime, formatAddress } from '@/utils/format'
import type { OrderItem } from '@/api/modules/order'

interface OrderLog {
  time: string
  title: string
  description?: string
  type?: '' | 'success' | 'warning' | 'info' | 'danger' | 'primary'
  hollow?: boolean
}

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const defaultImage = 'https://cube.elemecdn.com/6/74/3a6f1b1d4b7c3a2d2e1e2e3e4e5f6g7.png'

const order = ref<any>({
  id: 0,
  orderNo: '',
  status: 1,
  statusText: '待支付',
  totalAmount: 0,
  payAmount: 0,
  freight: 0,
  discountAmount: 0,
  createdAt: '',
  payAt: '',
  shipAt: '',
  receiveAt: '',
  remark: '',
  items: [],
  address: {
    id: 0,
    name: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    isDefault: false
  }
})

// 订单状态步骤
const statusStep = computed(() => {
  switch (order.value.status) {
    case 1: return 0
    case 2: return 1
    case 3: return 2
    case 4: return 3
    default: return 0
  }
})

// 操作日志
const orderLogs = computed<OrderLog[]>(() => {
  const logs: OrderLog[] = []
  
  // 提交订单
  logs.push({
    time: order.value.createdAt || new Date().toISOString(),
    title: '提交订单',
    description: '订单已提交成功',
    type: 'primary'
  })
  
  // 支付
  if (order.value.payAt) {
    logs.push({
      time: order.value.payAt,
      title: '支付成功',
      description: `支付方式: 在线支付`,
      type: 'success'
    })
  }
  
  // 发货
  if (order.value.shipAt) {
    logs.push({
      time: order.value.shipAt,
      title: '商家发货',
      description: '货物已发出，请注意查收',
      type: 'info'
    })
  }
  
  // 收货
  if (order.value.receiveAt) {
    logs.push({
      time: order.value.receiveAt,
      title: '确认收货',
      description: '交易完成',
      type: 'success'
    })
  }
  
  // 取消
  if (order.value.status === -1) {
    logs.push({
      time: new Date().toISOString(),
      title: '订单取消',
      description: '订单已取消',
      type: 'danger'
    })
  }
  
  return logs.reverse()
})

const breadcrumbItems = computed(() => [
  { title: '我的订单', path: '/order/list' },
  { title: '订单详情' }
])

const fetchOrderDetail = async () => {
  loading.value = true
  const id = Number(route.params.id)

  // 模拟数据
  order.value = {
    id,
    orderNo: 'ORDER20240001',
    status: 1,
    statusText: '待支付',
    totalAmount: 299,
    payAmount: 299,
    freight: 0,
    discountAmount: 0,
    createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    remark: '请尽快发货',
    items: [{
      id: 1,
      goodsId: 1,
      goodsName: '示例商品',
      image: defaultImage,
      price: 299,
      quantity: 1
    }],
    address: {
      id: 1,
      name: '张三',
      phone: '13800138000',
      province: '广东省',
      city: '深圳市',
      district: '南山区',
      detail: '科技园路1号',
      isDefault: true
    }
  }

  loading.value = false
}

const goToGoods = (goodsId: number) => {
  router.push(`/goods/detail/${goodsId}`)
}

const handlePay = () => {
  router.push({
    path: '/payment',
    query: { orderNo: order.value.orderNo }
  })
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    order.value.status = -1
    order.value.statusText = '已取消'
    ElMessage.success('订单已取消')
  } catch {
    // 取消
  }
}

const handleConfirmReceive = async () => {
  try {
    await ElMessageBox.confirm('确认收到货物吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    order.value.status = 4
    order.value.statusText = '已完成'
    order.value.receiveAt = new Date().toISOString()
    ElMessage.success('确认收货成功')
  } catch {
    // 取消
  }
}

const copyOrderNo = () => {
  navigator.clipboard.writeText(order.value.orderNo)
  ElMessage.success('订单号已复制')
}

onMounted(() => {
  fetchOrderDetail()
})
</script>

<style lang="scss" scoped>
.order-detail-page {
  background-color: #f5f7fa;
  min-height: 100vh;
  padding-bottom: 40px;
}

.container {
  width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}

.order-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-status-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  padding: 30px;
  color: #fff;

  .status-progress {
    margin-bottom: 24px;
    padding-bottom: 24px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.2);

    :deep(.el-steps) {
      .el-step__title {
        color: #fff;
      }
      .el-step__description {
        color: rgba(255, 255, 255, 0.8);
      }
    }
  }

  .status-info {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;

    .status-icon {
      width: 60px;
      height: 60px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.2);
      display: flex;
      align-items: center;
      justify-content: center;

      .el-icon {
        font-size: 30px;
      }
    }

    .status-text {
      h3 {
        font-size: 20px;
        margin-bottom: 8px;
      }

      p {
        font-size: 14px;
        opacity: 0.9;
      }
    }
  }

  .order-actions {
    display: flex;
    gap: 12px;

    :deep(.el-button) {
      &.el-button--primary {
        background: #fff;
        border-color: #fff;
        color: #764ba2;
      }
      &.el-button--success {
        background: #67c23a;
        border-color: #67c23a;
      }
    }
  }
}

.info-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;

  .card-title {
    font-size: 16px;
    font-weight: bold;
    color: #303133;
    padding-bottom: 16px;
    border-bottom: 1px solid #f0f2f5;
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    gap: 8px;

    .el-icon {
      color: #409eff;
    }
  }
}

.address-info {
  .address-main {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;

    .name {
      font-size: 15px;
      font-weight: bold;
      color: #303133;
    }

    .phone {
      font-size: 14px;
      color: #606266;
    }
  }

  .address-detail {
    font-size: 13px;
    color: #606266;
  }
}

.goods-list {
  .goods-item {
    display: flex;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #f0f2f5;

    &:last-child {
      border-bottom: none;
    }

    .goods-image {
      width: 60px;
      height: 60px;
      border-radius: 4px;
      margin-right: 12px;
      cursor: pointer;
    }

    .goods-info {
      flex: 1;

      .goods-name {
        font-size: 14px;
        color: #303133;
        margin-bottom: 4px;
        cursor: pointer;

        &:hover {
          color: #409eff;
        }
      }

      .goods-price {
        font-size: 13px;
        color: #909399;
      }
    }

    .goods-subtotal {
      font-size: 14px;
      font-weight: bold;
      color: #f56c6c;
    }
  }
}

.amount-detail {
  .amount-row {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    font-size: 14px;
    color: #606266;

    &.total {
      padding-top: 12px;
      margin-top: 8px;
      border-top: 1px solid #f0f2f5;
      font-size: 16px;
      font-weight: bold;

      .value {
        font-size: 18px;
        color: #f56c6c;
      }
    }

    .discount {
      color: #67c23a;
    }
  }
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;

  .info-item {
    .label {
      display: block;
      font-size: 13px;
      color: #909399;
      margin-bottom: 4px;
    }

    .value {
      font-size: 14px;
      color: #303133;
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }
}

.log-content {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .log-title {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }

  .log-desc {
    font-size: 13px;
    color: #909399;
  }
}

:deep(.el-timeline-item__node) {
  background-color: #409eff;
}
</style>
