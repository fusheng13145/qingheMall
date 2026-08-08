<template>
  <div class="checkout-page">
    <div class="page-header">
      <h1 class="page-title">确认订单</h1>
      <p class="page-desc">请核对商品信息并填写收货地址</p>
    </div>

    <div class="checkout-main">
      <!-- 商品清单 -->
      <section class="panel">
        <h2 class="panel-title">商品清单</h2>
        <div class="goods-list">
          <div v-for="item in items" :key="item.id" class="goods-item">
            <img class="goods-img" :src="item.productImg" :alt="item.productName" loading="lazy" />
            <div class="goods-info">
              <span class="goods-name">{{ item.productName || '商品' }}</span>
              <span class="goods-spec">规格: {{ formatSize(item.size) }} × {{ item.quantity }}</span>
            </div>
            <span class="goods-price">¥{{ formatPrice((item.price || 0) * item.quantity) }}</span>
          </div>
          <div v-if="items.length === 0" class="goods-empty">
            购物车中没有选中的商品，<router-link to="/products" class="link">去选购</router-link>
          </div>
        </div>
        <div class="goods-total">
          <span>共 {{ totalCount }} 件</span>
          <span>合计：<b class="total-price">¥{{ formatPrice(totalPrice) }}</b></span>
          <span v-if="selectedDiscount > 0" class="discount-line">优惠券抵扣：<b>-¥{{ formatPrice(selectedDiscount) }}</b></span>
        </div>
      </section>

      <!-- 优惠券（仅单笔订单可用） -->
      <section v-if="couponEnabled" class="panel">
        <h2 class="panel-title">优惠券</h2>
        <div v-if="availableList.length === 0" class="coupon-empty">暂无可用优惠券</div>
        <div v-else class="coupon-list">
          <label
            v-for="c in availableList"
            :key="c.id"
            class="coupon-item"
            :class="{ active: selectedId === c.id }"
          >
            <input v-model="selectedId" type="radio" name="coupon" :value="c.id" />
            <div class="coupon-info">
              <span class="coupon-name">{{ c.couponName }}</span>
              <span class="coupon-rule">{{ couponRuleText(c) }}</span>
            </div>
            <span class="coupon-discount">-¥{{ formatPrice(calcCouponDiscount(c, totalPrice)) }}</span>
          </label>
        </div>
      </section>

      <!-- 收货地址 -->
      <section class="panel">
        <h2 class="panel-title">收货地址</h2>
        <div class="form-grid">
          <div class="form-group">
            <label>收货人 <span class="req">*</span></label>
            <input v-model.trim="receiver.name" type="text" placeholder="请输入收货人姓名" maxlength="20" />
          </div>
          <div class="form-group">
            <label>联系电话 <span class="req">*</span></label>
            <input v-model.trim="receiver.phone" type="text" placeholder="请输入手机号" maxlength="11" />
          </div>
          <div class="form-group full">
            <label>收货地址 <span class="req">*</span></label>
            <textarea v-model.trim="receiver.address" rows="2" placeholder="请输入详细收货地址（省市区 + 街道门牌）" maxlength="120"></textarea>
          </div>
        </div>
      </section>

      <!-- 提交 -->
      <div class="submit-bar">
        <div class="submit-left">
          <span>应付金额：</span>
          <b class="total-price">¥{{ formatPrice(payableAmount) }}</b>
          <span v-if="selectedDiscount > 0" class="discount-hint">已优惠 ¥{{ formatPrice(selectedDiscount) }}</span>
        </div>
        <button class="btn-submit" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? '提交中...' : '提交订单' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listCart, clearSelectedCart } from '../api/cart'
import { batchAddOrders } from '../api/order'
import { getDefaultAddress } from '../api/address'
import { availableCoupons } from '../api/coupon'
import { calcCouponDiscount, couponRuleText } from '../utils/coupon'
import { useCartStore } from '../stores/cart'

const router = useRouter()
const cartStore = useCartStore()

const items = ref([])
const submitting = ref(false)
const receiver = ref({ name: '', phone: '', address: '' })

// 优惠券（仅单笔订单可用）
const availableList = ref([])
const selectedId = ref('')
const couponEnabled = computed(() => items.value.length === 1)
const selectedCoupon = computed(() => availableList.value.find(c => c.id === selectedId.value) || null)
const selectedDiscount = computed(() =>
  selectedCoupon.value ? calcCouponDiscount(selectedCoupon.value, totalPrice.value) : 0)
const payableAmount = computed(() => Math.max(0, totalPrice.value - selectedDiscount.value))

const totalCount = computed(() => items.value.reduce((sum, i) => sum + i.quantity, 0))
const totalPrice = computed(() => items.value.reduce((sum, i) => sum + (i.price || 0) * i.quantity, 0))

function formatPrice(p) {
  return Number(p || 0).toFixed(2)
}

function formatSize(size) {
  if (size === null || size === undefined || size === '') return ''
  return String(Number(size))
}

onMounted(async () => {
  try {
    const res = await listCart()
    items.value = (res.data || []).filter(i => i.selected)
    // 单笔订单时拉取可用券（多商品不支持用券，避免跨单分摊）
    if (items.value.length === 1) {
      try {
        const cRes = await availableCoupons(totalPrice.value)
        availableList.value = cRes.data || []
      } catch (e) { /* 优惠券非强依赖，忽略 */ }
    }
  } catch (e) {
    alert('加载购物车失败：' + (e.message || '请稍后重试'))
  }
  // 有默认收货地址时自动填充（M3-3 用户中心地址簿）
  try {
    const addrRes = await getDefaultAddress()
    if (addrRes.data) {
      receiver.value = {
        name: addrRes.data.receiverName,
        phone: addrRes.data.receiverPhone,
        address: addrRes.data.receiverAddress
      }
    }
  } catch (e) { /* 无地址时保持手填 */ }
})

function validate() {
  if (!receiver.value.name) return '请填写收货人姓名'
  if (!receiver.value.phone) return '请填写联系电话'
  if (!/^1\d{10}$/.test(receiver.value.phone)) return '联系电话格式不正确'
  if (!receiver.value.address) return '请填写收货地址'
  return ''
}

async function handleSubmit() {
  const err = validate()
  if (err) {
    alert(err)
    return
  }
  if (items.value.length === 0) {
    alert('没有可提交的商品')
    return
  }
  submitting.value = true
  try {
    const orders = items.value.map(i => ({
      productDetailId: i.productDetailId,
      quantity: i.quantity,
      receiverName: receiver.value.name,
      receiverPhone: receiver.value.phone,
      receiverAddress: receiver.value.address
    }))
    // 单笔订单可用券：将券与优惠额附加到该订单（后端二次校验）
    if (selectedCoupon.value) {
      orders[0].couponId = selectedCoupon.value.id
      orders[0].discountAmount = selectedDiscount.value
    }
    await batchAddOrders(orders)
    // 下单成功：清空购物车已勾选条目
    try {
      await clearSelectedCart()
    } catch (e) { /* 清理失败不影响主流程 */ }
    cartStore.refreshCount()
    alert('下单成功！请前往订单中心完成支付')
    router.push('/orders')
  } catch (e) {
    alert('下单失败：' + (e.message || '请稍后重试'))
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.checkout-page {
  padding: 20px 0;
  max-width: 860px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}

.page-desc {
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.checkout-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs);
  padding: 24px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-divider);
}

.goods-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.goods-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.goods-img {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  background: var(--color-bg-sunken);
  flex-shrink: 0;
}

.goods-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.goods-name {
  font-size: 14px;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-spec {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.goods-price {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-price);
  flex-shrink: 0;
}

.goods-empty {
  text-align: center;
  color: var(--color-text-tertiary);
  padding: 20px 0;
  font-size: 14px;
}

.link {
  color: var(--color-primary);
  font-weight: 600;
}

.coupon-empty {
  font-size: 14px;
  color: var(--color-text-tertiary);
  padding: 8px 0;
}

.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.coupon-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.coupon-item.active {
  border-color: var(--color-primary);
  background: var(--color-primary-50);
}

.coupon-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.coupon-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.coupon-rule {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.coupon-discount {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-price);
}

.discount-line {
  color: var(--color-success);
  font-weight: 600;
}

.discount-hint {
  font-size: 13px;
  color: var(--color-success);
  font-weight: 600;
  margin-left: 8px;
}

.goods-total {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--color-divider);
  font-size: 14px;
  color: var(--color-text-secondary);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group.full {
  grid-column: 1 / -1;
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
.form-group textarea {
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 14px;
  transition: border-color var(--transition-fast);
  outline: none;
  font-family: var(--font-sans);
}

.form-group input:focus,
.form-group textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-50);
}

.form-group textarea {
  resize: vertical;
}

.submit-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 24px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-light);
  padding: 16px 24px;
  box-shadow: var(--shadow-md);
}

.submit-left {
  font-size: 15px;
  color: var(--color-text-secondary);
}

.total-price {
  font-size: 26px;
  font-weight: 800;
  color: var(--color-price);
}

.btn-submit {
  padding: 12px 40px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);
}

.btn-submit:hover:not(:disabled) {
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.35);
  transform: translateY(-1px);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
