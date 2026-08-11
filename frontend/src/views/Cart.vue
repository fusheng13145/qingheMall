<template>
  <div class="cart-page">
    <div class="page-header">
      <h1 class="page-title">购物车</h1>
      <p class="page-desc">共 {{ items.length }} 件商品</p>
    </div>

    <!-- 空态 -->
    <div v-if="!loading && items.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
      <p>购物车还是空的</p>
      <router-link to="/products" class="btn-go">去逛逛</router-link>
    </div>

    <template v-else>
      <!-- 商品列表 -->
      <div class="cart-list">
        <div v-for="item in items" :key="item.id" class="cart-item">
          <label class="checkbox" :class="{ checked: item.selected }" @click="toggleSelect(item)">
            <svg v-if="item.selected" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
          </label>
          <router-link :to="`/product/${item.productId}`" class="item-img">
            <img :src="item.productImg" :alt="item.productName" loading="lazy" />
          </router-link>
          <div class="item-info">
            <router-link :to="`/product/${item.productId}`" class="item-name">{{ item.productName || '商品' }}</router-link>
            <span class="item-spec">规格: {{ formatSize(item.size) }}</span>
            <div class="item-bottom">
              <span class="item-price">¥{{ formatPrice(item.price) }}</span>
              <div class="qty-stepper">
                <button class="qty-btn" :disabled="item.quantity <= 1" @click="changeQty(item, -1)">−</button>
                <span class="qty-num">{{ item.quantity }}</span>
                <button class="qty-btn" :disabled="item.quantity >= item.stock" @click="changeQty(item, 1)">+</button>
              </div>
              <button class="item-del" title="删除" @click="handleRemove(item)">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部结算栏 -->
      <div class="checkout-bar">
        <label class="checkbox" :class="{ checked: allSelected }" @click="toggleAll">
          <svg v-if="allSelected" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
          <span class="all-label">全选</span>
        </label>
        <div class="total-box">
          <span class="total-label">合计：</span>
          <span class="total-price">¥{{ formatPrice(totalPrice) }}</span>
        </div>
        <button class="btn-checkout" :disabled="selectedItems.length === 0" @click="goCheckout">
          去结算 ({{ selectedItems.length }})
        </button>
      </div>
    </template>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { apiError } from '../utils/toast'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'

const router = useRouter()
// P2-15：购物车列表与变更动作统一由 store 管理（与 Checkout/角标共享同一数据源）
const cartStore = useCartStore()

const items = computed(() => cartStore.items)
const loading = computed(() => cartStore.loading)
const allSelected = computed(() => cartStore.allSelected)
const selectedItems = computed(() => cartStore.selectedItems)
const totalPrice = computed(() => cartStore.selectedTotalPrice)

function formatPrice(p) {
  return Number(p || 0).toFixed(2)
}

function formatSize(size) {
  if (size === null || size === undefined || size === '') return ''
  return String(Number(size))
}

async function toggleSelect(item) {
  try {
    await cartStore.toggleSelected(item)
  } catch (e) {
    apiError(e, '操作失败')
  }
}

async function toggleAll() {
  try {
    await cartStore.setAllSelected(!allSelected.value)
  } catch (e) {
    apiError(e, '操作失败')
  }
}

async function changeQty(item, delta) {
  const next = item.quantity + delta
  if (next < 1 || next > item.stock) return
  try {
    await cartStore.updateQuantity(item, next)
  } catch (e) {
    apiError(e, '修改数量失败')
  }
}

async function handleRemove(item) {
  if (!confirm('确定要从购物车删除该商品吗？')) return
  try {
    await cartStore.removeItem(item.id)
  } catch (e) {
    apiError(e, '删除失败')
  }
}

function goCheckout() {
  router.push({ name: 'Checkout' })
}

onMounted(() => {
  cartStore.fetchItems().catch(e => apiError(e, '加载购物车失败'))
})
</script>

<style scoped>
.cart-page {
  padding: 20px 0;
}

.page-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
}

.page-desc {
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.cart-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  box-shadow: var(--shadow-xs);
  border: 1px solid var(--color-border-light);
  transition: box-shadow var(--transition-fast);
}

.cart-item:hover {
  box-shadow: var(--shadow-md);
}

.checkbox {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--transition-fast);
  background: var(--color-bg);
}

.checkbox.checked {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.item-img {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--color-bg-sunken);
}

.item-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color var(--transition-fast);
}

.item-name:hover {
  color: var(--color-primary);
}

.item-spec {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.item-bottom {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 4px;
}

.item-price {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-price);
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
  width: 28px;
  height: 28px;
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
  width: 36px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.item-del {
  margin-left: auto;
  color: var(--color-text-tertiary);
  padding: 6px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  display: flex;
}

.item-del:hover {
  color: var(--color-danger);
  background: var(--color-danger-light);
}

.checkout-bar {
  position: sticky;
  bottom: 0;
  margin-top: 24px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  padding: 16px 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border-light);
}

.all-label {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-left: 4px;
}

.total-box {
  margin-left: auto;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.total-label {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.total-price {
  font-size: 26px;
  font-weight: 800;
  color: var(--color-price);
}

.btn-checkout {
  padding: 12px 32px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);
}

.btn-checkout:hover:not(:disabled) {
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.35);
  transform: translateY(-1px);
}

.btn-checkout:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 100px 0;
  color: var(--color-text-tertiary);
}

.empty-state svg {
  opacity: 0.4;
}

.empty-state p {
  font-size: 15px;
}

.btn-go {
  padding: 10px 28px;
  background: var(--color-primary-50);
  color: var(--color-primary);
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 600;
  transition: all var(--transition-fast);
}

.btn-go:hover {
  background: var(--color-primary);
  color: #fff;
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

@media (max-width: 640px) {
  .item-img {
    width: 56px;
    height: 56px;
  }
  .checkout-bar {
    flex-wrap: wrap;
  }
}
</style>
