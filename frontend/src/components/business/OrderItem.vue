<template>
  <div class="order-item">
    <div class="order-header">
      <div class="order-info">
        <span class="order-no">订单号: {{ order.orderNo }}</span>
        <span class="order-time">{{ formatDateTime(order.createdAt) }}</span>
      </div>
      <div class="order-status">
        <span :class="['status-text', `status-${order.status}`]">{{ order.statusText }}</span>
      </div>
    </div>

    <div class="order-goods" @click="goToDetail">
      <div
        class="goods-item"
        v-for="item in order.items"
        :key="item.id"
      >
        <img :src="item.image || defaultImage" :alt="item.goodsName" class="goods-image" @click.stop="goToGoodsDetail(item.goodsId)" />
        <div class="goods-info">
          <p class="goods-name" @click.stop="goToGoodsDetail(item.goodsId)">{{ item.goodsName }}</p>
          <p class="goods-price">¥{{ item.price.toFixed(2) }} × {{ item.quantity }}</p>
        </div>
        <div class="goods-subtotal">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
      </div>
    </div>

    <div class="order-footer">
      <div class="order-amount">
        <span class="label">合计:</span>
        <span class="amount">¥{{ order.payAmount.toFixed(2) }}</span>
        <span class="freight" v-if="order.freight > 0">(含运费: ¥{{ order.freight.toFixed(2) }})</span>
      </div>

      <div class="order-actions">
        <slot name="actions">
          <!-- 查看详情 -->
          <el-button size="small" @click="goToDetail">查看详情</el-button>
          
          <!-- 待付款状态 -->
          <template v-if="order.status === 1">
            <el-button size="small" type="primary" @click="handlePay">
              去支付
            </el-button>
            <el-button size="small" @click="handleCancel">
              取消订单
            </el-button>
          </template>
          
          <!-- 已付款状态 -->
          <template v-if="order.status === 2">
            <el-button size="small" type="info" disabled>等待发货</el-button>
          </template>
          
          <!-- 已发货状态 -->
          <template v-if="order.status === 3">
            <el-button size="small" type="success" @click="handleConfirmReceive">
              确认收货
            </el-button>
          </template>
          
          <!-- 已完成状态 -->
          <template v-if="order.status === 4">
            <el-button size="small" @click="handleDelete">
              删除订单
            </el-button>
          </template>
          
          <!-- 已取消状态 -->
          <template v-if="order.status === -1">
            <el-button size="small" @click="handleDelete">
              删除订单
            </el-button>
          </template>
        </slot>
      </div>
    </div>

    <div class="order-address" v-if="showAddress && order.address">
      <el-icon><Location /></el-icon>
      <span>{{ formatAddress(order.address) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Location } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { formatDateTime, formatAddress } from '@/utils/format'
import type { OrderItem } from '@/api/modules/order'

const props = withDefaults(defineProps<{
  order: OrderItem
  showAddress?: boolean
}>(), {
  showAddress: true
})

const emit = defineEmits<{
  (e: 'pay', order: OrderItem): void
  (e: 'cancel', order: OrderItem): void
  (e: 'confirmReceive', order: OrderItem): void
  (e: 'delete', order: OrderItem): void
}>()

const router = useRouter()

const defaultImage = 'https://cube.elemecdn.com/6/74/3a6f1b1d4b7c3a2d2e1e2e3e4e5f6g7.png'

const goToDetail = () => {
  router.push(`/order/detail/${props.order.id}`)
}

const goToGoodsDetail = (goodsId: number) => {
  router.push(`/goods/detail/${goodsId}`)
}

const handlePay = () => {
  emit('pay', props.order)
}

const handleCancel = () => {
  emit('cancel', props.order)
}

const handleConfirmReceive = () => {
  emit('confirmReceive', props.order)
}

const handleDelete = () => {
  emit('delete', props.order)
}
</script>

<style lang="scss" scoped>
.order-item {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  transition: box-shadow 0.3s;

  &:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  }
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
}

.order-info {
  display: flex;
  gap: 16px;

  .order-no {
    font-size: 13px;
    color: #303133;
    font-weight: 500;
  }

  .order-time {
    font-size: 13px;
    color: #909399;
  }
}

.order-status {
  .status-text {
    font-size: 14px;
    font-weight: 500;

    &.status-0 { color: #909399; }
    &.status-1 { color: #409eff; }
    &.status-2 { color: #e6a23c; }
    &.status-3 { color: #67c23a; }
    &.status-4 { color: #303133; }
    &.status--1 { color: #f56c6c; }
  }
}

.order-goods {
  padding: 12px 0;
  cursor: pointer;
}

.goods-item {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  transition: opacity 0.3s;

  &:hover {
    opacity: 0.8;
  }

  .goods-image {
    width: 80px;
    height: 80px;
    border-radius: 4px;
    object-fit: cover;
    cursor: pointer;
  }

  .goods-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-between;

    .goods-name {
      font-size: 14px;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
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
    display: flex;
    align-items: center;
  }
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}

.order-amount {
  .label {
    font-size: 13px;
    color: #909399;
  }

  .amount {
    font-size: 18px;
    font-weight: bold;
    color: #f56c6c;
    margin-left: 4px;
  }

  .freight {
    font-size: 12px;
    color: #909399;
    margin-left: 8px;
  }
}

.order-actions {
  display: flex;
  gap: 8px;
}

.order-address {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
  font-size: 13px;
  color: #909399;
}
</style>
