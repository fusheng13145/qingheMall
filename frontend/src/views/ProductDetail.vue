<template>
  <div v-if="product" class="product-detail">
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
        <div v-if="imageList.length > 1" class="thumbnail-list">
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
        <div v-if="summary && summary.ratingCount > 0" class="product-info-row">
          <span class="info-label">评分</span>
          <span class="info-value rating-value">
            <span class="stars" aria-hidden="true">
              <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= Math.round(summary.avgRating) }">★</span>
            </span>
            <span class="rating-num">{{ summary.avgRating }}</span>
            <span class="rating-count">（{{ summary.ratingCount }} 条评价）</span>
          </span>
        </div>
        <div v-if="product.productIntro && !isUrl(product.productIntro)" class="product-info-row">
          <span class="info-label">简介</span>
          <span class="info-value">{{ product.productIntro }}</span>
        </div>
        <div v-if="detailList.length > 0" class="size-section">
          <span class="info-label">规格</span>
          <div class="size-list">
            <button
              v-for="detail in detailList"
              :key="detail.id"
              class="size-btn"
              :class="{ active: selectedDetail && selectedDetail.id === detail.id }"
              @click="selectDetail(detail)"
            >
              {{ formatSize(detail.size) }}
            </button>
          </div>
        </div>
        <div v-if="selectedDetail" class="stock-info">
          <span class="info-label">库存</span>
          <span class="info-value stock-value" :class="{ 'low-stock': selectedDetail.stock < 10 }">
            {{ selectedDetail.stock }} 件
          </span>
        </div>
        <div v-if="selectedDetail && selectedDetail.stock > 0" class="qty-row">
          <span class="info-label">数量</span>
          <div class="qty-stepper">
            <button class="qty-btn" :disabled="buyQuantity <= 1" @click="buyQuantity--">−</button>
            <span class="qty-num">{{ buyQuantity }}</span>
            <button class="qty-btn" :disabled="buyQuantity >= selectedDetail.stock" @click="buyQuantity++">+</button>
          </div>
        </div>
        <div class="buy-section">
          <button
            class="btn-cart"
            :disabled="!selectedDetail || selectedDetail.stock <= 0 || addingCart"
            @click="handleAddCart"
          >
            {{ addingCart ? '添加中...' : '加入购物车' }}
          </button>
          <button
            class="btn-buy"
            :disabled="!selectedDetail || selectedDetail.stock <= 0 || buying"
            @click="handleBuy"
          >
            {{ buying ? '处理中...' : '立即购买' }}
          </button>
          <span v-if="!selectedDetail && detailList.length > 0" class="buy-tip">请选择规格</span>
          <span v-if="selectedDetail && selectedDetail.stock <= 0" class="buy-tip">该规格已售罄</span>
        </div>
      </div>
    </div>

    <!-- 用户评价区 -->
    <div class="comment-section">
      <div class="comment-header">
        <h2 class="comment-title">用户评价</h2>
        <span v-if="summary && summary.ratingCount > 0" class="comment-sub">
          好评率 {{ summary.ratingCount > 0 ? Math.round(summary.avgRating / 5 * 100) : 0 }}%
        </span>
      </div>
      <div v-if="comments.length > 0" class="comment-list">
        <div v-for="c in comments" :key="c.id" class="comment-item">
          <div class="comment-top">
            <span class="comment-user">{{ c.userNickName || '匿名用户' }}</span>
            <span class="comment-stars">
              <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= c.rating }">★</span>
            </span>
          </div>
          <p class="comment-content">{{ c.content }}</p>
          <span class="comment-time">{{ formatTime(c.gmtCreated) }}</span>
        </div>
      </div>
      <div v-else-if="!commentLoading" class="comment-empty">
        <p>暂无评价，快来抢首评吧</p>
      </div>
      <div v-if="commentTotalPages > 1" class="comment-more">
        <button class="comment-more-btn" @click="loadComments(commentPage + 1)">
          加载更多评价
        </button>
      </div>
    </div>

    <!-- 同品牌推荐（P3 简单推荐） -->
    <div v-if="relatedProducts.length > 0" class="comment-section">
      <div class="comment-header">
        <h2 class="comment-title">同品牌推荐</h2>
      </div>
      <div class="related-grid">
        <router-link
          v-for="p in relatedProducts"
          :key="p.id"
          :to="`/product/${p.id}`"
          class="related-card"
        >
          <div class="related-img">
            <img :src="firstImg(p.productImgs)" :alt="p.name" loading="lazy" />
          </div>
          <div class="related-info">
            <span class="related-name">{{ p.name }}</span>
            <span class="related-price">¥{{ Number(p.price || 0).toFixed(2) }}</span>
          </div>
        </router-link>
      </div>
    </div>
  </div>
  <div v-else class="loading-state">
    <div class="loading-spinner"></div>
    <span>加载中...</span>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get, getProductDetails, pageQuery } from '../api/product'
import { addOrder } from '../api/order'
import { addCart } from '../api/cart'
import { getCommentSummary, listProductComments } from '../api/comment'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const product = ref(null)
const detailList = ref([])
const selectedDetail = ref(null)
const currentImageIndex = ref(0)
const buyQuantity = ref(1)
const buying = ref(false)
const addingCart = ref(false)

// 评价
const summary = ref(null)
const comments = ref([])
const commentPage = ref(1)
const commentTotalPages = ref(1)
const commentLoading = ref(true)

// 同品牌推荐（P3）
const relatedProducts = ref([])

const imageList = computed(() => {
  return splitImgs(product.value && product.value.productImgs)
})

// 兼容后端以空格分隔、历史数据以分号分隔的图片串
function splitImgs(str) {
  if (!str) return []
  return str
    .split(/[;\s]+/)
    .map(s => s.trim())
    .filter(Boolean)
}

// 规格 size 为 Double（如 38.0 / 38.5），JS 数字转字符串会自动去掉多余的 .0
function formatSize(size) {
  if (size === null || size === undefined || size === '') return ''
  return String(Number(size))
}

// 存量种子数据的 product_intro 存的是图片 URL，非文字介绍时隐藏简介行
function isUrl(str) {
  return /^https?:\/\//i.test(String(str).trim())
}

const currentImage = computed(() => {
  if (imageList.value.length > 0) {
    return imageList.value[currentImageIndex.value]
  }
  return ''
})

function selectDetail(detail) {
  selectedDetail.value = detail
  buyQuantity.value = 1
}

async function handleAddCart() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  if (!selectedDetail.value || selectedDetail.value.stock <= 0) {
    return
  }
  addingCart.value = true
  try {
    await addCart(selectedDetail.value.id, buyQuantity.value)
    cartStore.refreshCount()
    alert('已加入购物车')
  } catch (error) {
    alert('加入购物车失败：' + (error.message || '请稍后重试'))
  } finally {
    addingCart.value = false
  }
}

async function loadProduct() {
  const productId = route.params.id
  try {
    const [productRes, detailRes] = await Promise.all([
      get(productId),
      getProductDetails(productId)
    ])
    product.value = productRes.data
    detailList.value = detailRes.data || []
    if (detailList.value.length === 1) {
      selectedDetail.value = detailList.value[0]
    }
  } catch (error) {
    console.error('加载商品详情失败:', error)
  }
  loadSummary(productId)
  loadComments(1)
  loadRelated()
}

async function loadSummary(productId) {
  try {
    const res = await getCommentSummary(productId)
    summary.value = res.data || {}
  } catch (error) {
    console.error('加载评分失败:', error)
  }
}

async function loadComments(page) {
  commentLoading.value = true
  try {
    const res = await listProductComments(route.params.id, page, 5)
    const paging = res.data || {}
    if (page <= 1) {
      comments.value = paging.data || []
    } else {
      comments.value = comments.value.concat(paging.data || [])
    }
    commentPage.value = page
    commentTotalPages.value = paging.totalPage || 1
  } catch (error) {
    console.error('加载评价失败:', error)
  } finally {
    commentLoading.value = false
  }
}

// 同品牌推荐：按当前商品品牌查询，排除自身
async function loadRelated() {
  const brand = product.value && product.value.brand
  if (!brand) return
  try {
    const res = await pageQuery(1, 4, null, brand, null)
    relatedProducts.value = (res.data.data || []).filter(p => p.id !== route.params.id)
  } catch (error) {
    console.error('加载同品牌推荐失败:', error)
  }
}

function firstImg(str) {
  if (!str) return ''
  const parts = str.split(/[;\s]+/).map(s => s.trim()).filter(Boolean)
  return parts[0] || ''
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  if (isNaN(d.getTime())) return String(time)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
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
    const orderRes = await addOrder({
      productDetailId: selectedDetail.value.id,
      quantity: buyQuantity.value
    })
    // 下单成功：进入收银台选择支付方式
    router.push({ name: 'Pay', query: { orderNumber: orderRes.data.orderNumber } })
  } catch (error) {
    alert('创建订单失败：' + (error.message || '请稍后重试'))
  } finally {
    buying.value = false
  }
}

onMounted(() => {
  loadProduct()
})

// P2-8：相关推荐/页面内跳转同路由切换商品时重新加载，避免旧数据残留
watch(
  () => route.params.id,
  () => {
    selectedDetail.value = null
    loadProduct()
  }
)
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
  flex-wrap: wrap;
}

.qty-row {
  display: flex;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid var(--color-divider);
}

.qty-stepper {
  display: flex;
  align-items: center;
  gap: 4px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.qty-btn {
  width: 30px;
  height: 30px;
  background: var(--color-bg-sunken);
  color: var(--color-text-secondary);
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background var(--transition-fast);
}

.qty-btn:hover:not(:disabled) {
  background: var(--color-primary-50);
  color: var(--color-primary);
}

.qty-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.qty-num {
  width: 40px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.btn-cart {
  padding: 14px 36px;
  background: transparent;
  color: var(--color-primary);
  border: 2px solid var(--color-primary);
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.btn-cart:hover:not(:disabled) {
  background: var(--color-primary-50);
}

.btn-cart:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

/* ========== 用户评价区 ========== */
.comment-section {
  margin-top: 32px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 28px 32px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-light);
}

.comment-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-divider);
  margin-bottom: 8px;
}

.comment-title {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
}

.comment-sub {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.comment-list {
  display: flex;
  flex-direction: column;
}

.comment-item {
  padding: 18px 0;
  border-bottom: 1px solid var(--color-divider);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.comment-user {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.comment-stars {
  display: inline-flex;
  gap: 2px;
}

.star {
  color: var(--color-border);
  font-size: 15px;
}

.star.filled {
  color: #f5a623;
}

.comment-content {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.7;
  margin-bottom: 8px;
  word-break: break-word;
}

.comment-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.comment-empty {
  padding: 40px 0;
  text-align: center;
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.comment-more {
  padding-top: 16px;
  text-align: center;
}

.comment-more-btn {
  padding: 8px 28px;
  background: transparent;
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.comment-more-btn:hover {
  background: var(--color-primary-50);
}

.rating-value {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.rating-num {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-price);
}

.rating-count {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* ========== 同品牌推荐 ========== */
.related-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding-top: 16px;
}

.related-card {
  background: var(--color-bg-sunken);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 1px solid transparent;
  text-decoration: none;
}

.related-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-primary-50);
}

.related-img {
  aspect-ratio: 1;
  overflow: hidden;
  background: var(--color-bg-sunken);
}

.related-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.related-card:hover .related-img img {
  transform: scale(1.05);
}

.related-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
}

.related-name {
  font-size: 13px;
  color: var(--color-text);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.related-price {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-price);
}

@media (max-width: 900px) {
  .related-grid {
    grid-template-columns: repeat(2, 1fr);
  }
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
