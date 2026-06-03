<template>
  <div class="product-detail" v-if="product">
    <div class="breadcrumb">
      <router-link to="/" class="breadcrumb-link">首页</router-link>
      <span class="breadcrumb-sep">/</span>
      <router-link to="/products" class="breadcrumb-link">商品</router-link>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-current">{{ product.name }}</span>
    </div>
    <div class="detail-main">
      <div class="detail-left">
        <div class="main-image">
          <img :src="currentImage" :alt="product.name" />
        </div>
        <div class="thumbnail-list" v-if="imageList.length > 1">
          <div
            v-for="(img, index) in imageList"
            :key="index"
            class="thumbnail-item"
            :class="{ active: currentImageIndex === index }"
            @click="currentImageIndex = index"
          >
            <img :src="img" :alt="`${product.name}-${index}`" />
          </div>
        </div>
      </div>
      <div class="detail-right">
        <h1 class="product-name">{{ product.name }}</h1>
        <div class="product-price-box">
          <span class="price-label">价格</span>
          <span class="product-price"><span class="price-symbol">¥</span>{{ product.price }}</span>
        </div>
        <div class="product-info-row">
          <span class="info-label">销量</span>
          <span class="info-value">{{ product.purchaseNum || 0 }} 件</span>
        </div>
        <div class="product-info-row" v-if="product.intro">
          <span class="info-label">简介</span>
          <span class="info-value">{{ product.intro }}</span>
        </div>
        <div class="size-section" v-if="detailList.length > 0">
          <span class="info-label">规格</span>
          <div class="size-list">
            <button
              v-for="detail in detailList"
              :key="detail.id"
              class="size-btn"
              :class="{ active: selectedDetail && selectedDetail.id === detail.id }"
              @click="selectDetail(detail)"
            >
              {{ detail.size }}
            </button>
          </div>
        </div>
        <div class="stock-info" v-if="selectedDetail">
          <span class="info-label">库存</span>
          <span class="info-value stock-value" :class="{ 'low-stock': selectedDetail.stock < 10 }">
            {{ selectedDetail.stock }} 件
          </span>
        </div>
        <div class="buy-section">
          <button
            class="btn-buy"
            @click="handleBuy"
            :disabled="!selectedDetail || selectedDetail.stock <= 0 || buying"
          >
            {{ buying ? '处理中...' : '立即购买' }}
          </button>
          <span class="buy-tip" v-if="!selectedDetail && detailList.length > 0">请选择规格</span>
          <span class="buy-tip" v-if="selectedDetail && selectedDetail.stock <= 0">该规格已售罄</span>
        </div>
      </div>
    </div>
  </div>
  <div class="loading-state" v-else>
    <div class="loading-spinner"></div>
    <span>加载中...</span>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get, getProductDetails } from '../api/product'
import { addOrder } from '../api/order'
import { payOrder } from '../api/payment'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const product = ref(null)
const detailList = ref([])
const selectedDetail = ref(null)
const currentImageIndex = ref(0)
const buying = ref(false)

const imageList = computed(() => {
  if (product.value && product.value.productImgs) {
    return product.value.productImgs.split(';').filter(img => img.trim())
  }
  return []
})

const currentImage = computed(() => {
  if (imageList.value.length > 0) {
    return imageList.value[currentImageIndex.value]
  }
  return ''
})

function selectDetail(detail) {
  selectedDetail.value = detail
}

async function loadProduct() {
  const productId = route.params.id
  try {
    const [productRes, detailRes] = await Promise.all([
      get(productId),
      getProductDetails(productId)
    ])
    if (productRes.data.code === 200) {
      product.value = productRes.data.data
    }
    if (detailRes.data.code === 200) {
      detailList.value = detailRes.data.data || []
      if (detailList.value.length === 1) {
        selectedDetail.value = detailList.value[0]
      }
    }
  } catch (error) {
    console.error('加载商品详情失败:', error)
  }
}

async function handleBuy() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }

  if (!selectedDetail.value || selectedDetail.value.stock <= 0) {
    return
  }

  buying.value = true
  try {
    const orderRes = await addOrder(selectedDetail.value.id)
    if (orderRes.data.code === 200) {
      const orderData = orderRes.data.data
      const orderId = orderData.id || orderData.orderId || orderData
      try {
        const payRes = await payOrder({
          orderId: orderId,
          payType: 1
        })
        if (payRes.data.code === 200) {
          router.push({ name: 'PaySuccess', query: { orderId: orderId } })
        } else {
          alert('支付失败：' + (payRes.data.msg || '请稍后重试'))
        }
      } catch (payError) {
        alert('支付请求失败，请稍后重试')
      }
    } else {
      alert('创建订单失败：' + (orderRes.data.msg || '请稍后重试'))
    }
  } catch (error) {
    alert('操作失败，请稍后重试')
  } finally {
    buying.value = false
  }
}

onMounted(() => {
  loadProduct()
})
</script>

<style scoped>
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  font-size: 13px;
}

.breadcrumb-link {
  color: var(--color-text-tertiary);
  transition: color var(--transition-fast);
}

.breadcrumb-link:hover {
  color: var(--color-primary);
}

.breadcrumb-sep {
  color: var(--color-text-tertiary);
  opacity: 0.5;
}

.breadcrumb-current {
  color: var(--color-text-secondary);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-main {
  display: flex;
  gap: 48px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 32px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-light);
}

.detail-left {
  flex-shrink: 0;
  width: 480px;
}

.main-image {
  width: 480px;
  height: 480px;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-bg-sunken);
  margin-bottom: 12px;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.main-image:hover img {
  transform: scale(1.03);
}

.thumbnail-list {
  display: flex;
  gap: 8px;
}

.thumbnail-item {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all var(--transition-fast);
  background: var(--color-bg-sunken);
}

.thumbnail-item.active {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary-50);
}

.thumbnail-item:hover {
  border-color: var(--color-primary);
}

.thumbnail-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-right {
  flex: 1;
  min-width: 0;
}

.product-name {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.4;
  margin-bottom: 24px;
}

.product-price-box {
  background: var(--color-primary-50);
  padding: 20px 24px;
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.price-label {
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.product-price {
  font-size: 36px;
  font-weight: 800;
  color: var(--color-price);
  line-height: 1;
}

.price-symbol {
  font-size: 18px;
  font-weight: 700;
  margin-right: 2px;
}

.product-info-row {
  display: flex;
  align-items: flex-start;
  padding: 14px 0;
  border-bottom: 1px solid var(--color-divider);
}

.info-label {
  font-size: 14px;
  color: var(--color-text-tertiary);
  width: 60px;
  flex-shrink: 0;
}

.info-value {
  font-size: 14px;
  color: var(--color-text);
}

.size-section {
  display: flex;
  align-items: flex-start;
  padding: 14px 0;
  border-bottom: 1px solid var(--color-divider);
}

.size-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.size-btn {
  padding: 8px 24px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.size-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.size-btn.active {
  border-color: var(--color-primary);
  background: var(--color-primary-50);
  color: var(--color-primary);
  font-weight: 600;
  box-shadow: 0 0 0 2px var(--color-primary-50);
}

.stock-info {
  display: flex;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid var(--color-divider);
}

.stock-value {
  font-weight: 600;
}

.low-stock {
  color: var(--color-danger);
}

.buy-section {
  margin-top: 32px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.btn-buy {
  padding: 14px 64px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  transition: all var(--transition-base);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.btn-buy:hover:not(:disabled) {
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4);
  transform: translateY(-1px);
}

.btn-buy:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

.buy-tip {
  font-size: 14px;
  color: var(--color-danger);
  font-weight: 500;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 80px 0;
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 900px) {
  .detail-main {
    flex-direction: column;
  }

  .detail-left {
    width: 100%;
  }

  .main-image {
    width: 100%;
    height: auto;
    aspect-ratio: 1;
  }
}
</style>
