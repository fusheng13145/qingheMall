<template>
  <div class="order-confirm-page">
    <div class="container">
      <h1 class="page-title">确认订单</h1>

      <div class="order-content">
        <!-- 收货地址 -->
        <section class="section">
          <h2 class="section-title">
            <el-icon><Location /></el-icon>
            收货地址
          </h2>
          <div class="address-list" v-if="addresses.length > 0">
            <div
              v-for="addr in addresses"
              :key="addr.id"
              :class="['address-item', { active: selectedAddressId === addr.id }]"
              @click="selectedAddressId = addr.id"
            >
              <div class="address-info">
                <div class="address-header">
                  <span class="address-name">{{ addr.name }}</span>
                  <span class="address-phone">{{ addr.phone }}</span>
                </div>
                <p class="address-detail">{{ formatAddress(addr) }}</p>
              </div>
              <el-tag v-if="addr.isDefault" size="small" type="success">默认</el-tag>
            </div>
            <div class="address-add" @click="goToAddress">
              <el-icon><Plus /></el-icon>
              <span>添加新地址</span>
            </div>
          </div>
          <el-empty v-else description="暂无收货地址">
            <el-button type="primary" @click="goToAddress">添加地址</el-button>
          </el-empty>
        </section>

        <!-- 配送时间 -->
        <section class="section">
          <h2 class="section-title">
            <el-icon><Clock /></el-icon>
            配送时间
          </h2>
          <div class="delivery-time">
            <el-radio-group v-model="deliveryType" size="default">
              <el-radio-button label="normal">工作日配送</el-radio-button>
              <el-radio-button label="weekend">周末配送</el-radio-button>
              <el-radio-button label="specified">指定日期</el-radio-button>
            </el-radio-group>
            <el-date-picker
              v-if="deliveryType === 'specified'"
              v-model="specifiedDate"
              type="date"
              placeholder="选择日期"
              :disabled-date="disabledDate"
              size="default"
              style="margin-left: 16px;"
            />
          </div>
          <div class="delivery-hint">
            <el-icon><InfoFilled /></el-icon>
            <span>配送时间仅为意向时间，实际配送可能因物流情况有所调整</span>
          </div>
        </section>

        <!-- 商品清单 -->
        <section class="section">
          <h2 class="section-title">
            <el-icon><Goods /></el-icon>
            商品清单
          </h2>
          <div class="goods-list">
            <div
              class="goods-item"
              v-for="item in orderItems"
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
        </section>

        <!-- 优惠券 -->
        <section class="section">
          <h2 class="section-title">
            <el-icon><Tickets /></el-icon>
            优惠券
          </h2>
          <div class="coupon-section">
            <div class="coupon-select" @click="showCouponDialog = true">
              <span class="coupon-label">{{ selectedCoupon ? `-¥${selectedCoupon.discount.toFixed(2)} ${selectedCoupon.name}` : '暂不使用优惠券' }}</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
            <div class="coupon-hint" v-if="availableCoupons.length > 0">
              共 {{ availableCoupons.length }} 张优惠券可用
            </div>
          </div>
        </section>

        <!-- 订单备注 -->
        <section class="section">
          <h2 class="section-title">
            <el-icon><Comment /></el-icon>
            订单备注
          </h2>
          <el-input
            v-model="remark"
            type="textarea"
            :rows="2"
            placeholder="请输入订单备注（选填）"
            maxlength="200"
            show-word-limit
          />
        </section>

        <!-- 订单金额 -->
        <section class="section">
          <h2 class="section-title">
            <el-icon><Wallet /></el-icon>
            订单金额
          </h2>
          <div class="amount-detail">
            <div class="amount-row">
              <span class="label">商品总额</span>
              <span class="value">¥{{ goodsAmount.toFixed(2) }}</span>
            </div>
            <div class="amount-row" v-if="selectedCoupon">
              <span class="label">优惠券</span>
              <span class="value discount">-¥{{ selectedCoupon.discount.toFixed(2) }}</span>
            </div>
            <div class="amount-row">
              <span class="label">运费</span>
              <span class="value">{{ freight > 0 ? `¥${freight.toFixed(2)}` : '免费' }}</span>
            </div>
            <div class="amount-row total">
              <span class="label">应付总额</span>
              <span class="value">¥{{ totalAmount.toFixed(2) }}</span>
            </div>
          </div>
        </section>
      </div>

      <!-- 提交订单 -->
      <div class="order-footer">
        <div class="footer-info">
          <span>提交订单后，请于 <em>24小时</em> 内完成支付</span>
        </div>
        <div class="footer-actions">
          <el-button @click="goBack">返回购物车</el-button>
          <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
            提交订单
          </el-button>
        </div>
      </div>
    </div>

    <!-- 优惠券选择对话框 -->
    <el-dialog v-model="showCouponDialog" title="选择优惠券" width="500px">
      <div class="coupon-dialog">
        <div 
          :class="['coupon-item', { active: selectedCouponId === null }]"
          @click="selectedCouponId = null"
        >
          <div class="coupon-info">
            <span class="coupon-name">不使用优惠券</span>
          </div>
          <el-icon v-if="selectedCouponId === null"><Check /></el-icon>
        </div>
        <div 
          v-for="coupon in availableCoupons"
          :key="coupon.id"
          :class="['coupon-item', { active: selectedCouponId === coupon.id, disabled: coupon.minAmount > goodsAmount }]"
          @click="selectedCouponId = coupon.minAmount <= goodsAmount ? coupon.id : selectedCouponId"
        >
          <div class="coupon-info">
            <span class="coupon-price">¥{{ coupon.discount.toFixed(2) }}</span>
            <div class="coupon-detail">
              <span class="coupon-name">{{ coupon.name }}</span>
              <span class="coupon-condition" v-if="coupon.minAmount > 0">满{{ coupon.minAmount }}可用</span>
              <span class="coupon-expire">有效期至 {{ coupon.expireDate }}</span>
            </div>
          </div>
          <el-icon v-if="selectedCouponId === coupon.id"><Check /></el-icon>
        </div>
      </div>
      <template #footer>
        <el-button @click="showCouponDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmCoupon">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Location, Plus, Clock, InfoFilled, Goods, Tickets, ArrowRight, Comment, Wallet, Check } from '@element-plus/icons-vue'
import { useCartStore } from '@/stores/cart'
import { formatAddress } from '@/utils/format'
import type { Address } from '@/api/modules/order'

interface Coupon {
  id: number
  name: string
  discount: number
  minAmount: number
  expireDate: string
}

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()

const submitting = ref(false)
const selectedAddressId = ref<number | null>(null)
const remark = ref('')
const defaultImage = 'https://cube.elemecdn.com/6/74/3a6f1b1d4b7c3a2d2e1e2e3e4e5f6g7.png'

// 配送时间
const deliveryType = ref<'normal' | 'weekend' | 'specified'>('normal')
const specifiedDate = ref<Date | null>(null)

// 优惠券
const showCouponDialog = ref(false)
const selectedCouponId = ref<number | null>(null)
const selectedCoupon = computed(() => availableCoupons.value.find(c => c.id === selectedCouponId.value) || null)

// 模拟优惠券数据
const availableCoupons = ref<Coupon[]>([
  { id: 1, name: '新人专享券', discount: 10, minAmount: 100, expireDate: '2024-12-31' },
  { id: 2, name: '满减券', discount: 20, minAmount: 200, expireDate: '2024-12-31' },
  { id: 3, name: '限时折扣券', discount: 50, minAmount: 500, expireDate: '2024-12-31' }
])

const addresses = ref<Address[]>([])
const orderItems = ref<any[]>([])

const goodsAmount = computed(() => {
  return orderItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

const freight = computed(() => {
  return goodsAmount.value >= 99 ? 0 : 10
})

const totalAmount = computed(() => {
  const discount = selectedCoupon.value?.discount || 0
  return Math.max(0, goodsAmount.value - discount + freight.value)
})

const disabledDate = (time: Date) => {
  return time.getTime() < Date.now() - 8.64e7
}

const goBack = () => {
  router.push('/cart')
}

const goToAddress = () => {
  router.push('/user/address')
}

const goToGoods = (goodsId: number) => {
  router.push(`/goods/detail/${goodsId}`)
}

const confirmCoupon = () => {
  showCouponDialog.value = false
}

const handleSubmit = async () => {
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }

  if (orderItems.value.length === 0) {
    ElMessage.warning('订单商品不能为空')
    return
  }

  if (deliveryType.value === 'specified' && !specifiedDate.value) {
    ElMessage.warning('请选择配送日期')
    return
  }

  submitting.value = true
  try {
    // 模拟提交订单
    await new Promise(resolve => setTimeout(resolve, 1000))
    const orderNo = `ORDER${Date.now()}`
    ElMessage.success('订单提交成功')
    
    // 清除已购买的购物车商品
    const selectedItems = cartStore.items.filter(i => i.selected)
    if (selectedItems.length > 0) {
      await cartStore.removeItem(selectedItems.map(i => i.id))
    }
    
    router.push({
      path: '/payment',
      query: { orderNo }
    })
  } catch (error) {
    ElMessage.error('订单提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  // 获取地址列表 - 模拟数据
  addresses.value = [
    {
      id: 1,
      name: '张三',
      phone: '13800138000',
      province: '广东省',
      city: '深圳市',
      district: '南山区',
      detail: '科技园路1号',
      isDefault: true
    },
    {
      id: 2,
      name: '李四',
      phone: '13900139000',
      province: '广东省',
      city: '广州市',
      district: '天河区',
      detail: '体育西路123号',
      isDefault: false
    }
  ]
  selectedAddressId.value = addresses.value.find(a => a.isDefault)?.id || addresses.value[0]?.id || null

  // 获取订单商品
  const goodsId = route.query.goodsId
  const quantity = route.query.quantity

  if (goodsId) {
    // 直接购买
    orderItems.value = [{
      id: Date.now(),
      goodsId: Number(goodsId),
      goodsName: `商品${goodsId}`,
      price: 199,
      quantity: Number(quantity) || 1,
      image: `https://picsum.photos/100/100?random=${goodsId}`
    }]
  } else {
    // 从购物车结算
    const selectedItems = cartStore.items.filter(i => i.selected)
    if (selectedItems.length > 0) {
      orderItems.value = selectedItems.map(item => ({
        ...item,
        image: item.image
      }))
    }
  }

  if (orderItems.value.length === 0) {
    // 默认模拟数据
    orderItems.value = [{
      id: 1,
      goodsId: 1,
      goodsName: '示例商品',
      price: 199,
      quantity: 1,
      image: defaultImage
    }]
  }
})
</script>

<style lang="scss" scoped>
.order-confirm-page {
  background-color: #f5f7fa;
  min-height: 100vh;
  padding-bottom: 100px;
}

.container {
  width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  padding: 20px 0;
}

.order-content {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.section {
  padding: 20px 0;
  border-bottom: 1px solid #f0f2f5;

  &:last-child {
    border-bottom: none;
  }
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;

  .el-icon {
    color: #409eff;
  }
}

.address-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.address-item {
  width: 280px;
  padding: 16px;
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

  .address-info {
    .address-header {
      display: flex;
      gap: 12px;
      margin-bottom: 8px;

      .address-name {
        font-size: 14px;
        font-weight: bold;
        color: #303133;
      }

      .address-phone {
        font-size: 14px;
        color: #606266;
      }
    }

    .address-detail {
      font-size: 13px;
      color: #909399;
    }
  }
}

.address-add {
  width: 280px;
  padding: 16px;
  border: 2px dashed #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #909399;
  transition: all 0.3s;

  &:hover {
    border-color: #409eff;
    color: #409eff;
  }

  .el-icon {
    font-size: 24px;
  }

  span {
    font-size: 14px;
  }
}

.delivery-time {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.delivery-hint {
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 6px;

  .el-icon {
    color: #409eff;
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

.coupon-section {
  .coupon-select {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      border-color: #409eff;
    }

    .coupon-label {
      font-size: 14px;
      color: #606266;
    }

    .el-icon {
      color: #909399;
    }
  }

  .coupon-hint {
    margin-top: 8px;
    font-size: 13px;
    color: #409eff;
  }
}

.coupon-dialog {
  .coupon-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    margin-bottom: 12px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      border-color: #409eff;
    }

    &.active {
      border-color: #409eff;
      background: #f0f7ff;
    }

    &.disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .coupon-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .coupon-price {
        font-size: 20px;
        font-weight: bold;
        color: #f56c6c;
      }

      .coupon-detail {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .coupon-name {
          font-size: 14px;
          color: #303133;
        }

        .coupon-condition {
          font-size: 12px;
          color: #909399;
        }

        .coupon-expire {
          font-size: 12px;
          color: #c0c4cc;
        }
      }
    }

    .el-icon {
      color: #409eff;
      font-size: 18px;
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
        font-size: 20px;
        color: #f56c6c;
      }
    }

    .discount {
      color: #67c23a;
    }
  }
}

.order-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.08);
  padding: 16px 0;
  z-index: 100;

  .container {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .footer-info {
    font-size: 13px;
    color: #909399;

    em {
      color: #f56c6c;
      font-style: normal;
    }
  }

  .footer-actions {
    display: flex;
    gap: 12px;
  }
}
</style>
