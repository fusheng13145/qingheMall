<template>
  <div class="products-page">
    <div class="page-header">
      <h1 class="page-title">全部商品</h1>
      <p class="page-desc">发现您心仪的好物</p>
    </div>

    <!-- 搜索 / 品牌筛选 / 排序 -->
    <div class="filter-bar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input
          v-model.trim="keyword"
          type="text"
          placeholder="搜索商品名称"
          @keyup.enter="search"
        />
        <button class="btn-search" @click="search">搜索</button>
      </div>
      <select v-model="brand" class="filter-select" @change="search">
        <option value="">全部品牌</option>
        <option v-for="b in brands" :key="b" :value="b">{{ b }}</option>
      </select>
      <select v-model="sort" class="filter-select" @change="search">
        <option value="">默认（上新）</option>
        <option value="sales_desc">销量优先</option>
        <option value="price_asc">价格从低到高</option>
        <option value="price_desc">价格从高到低</option>
      </select>
    </div>

    <div v-if="products.length > 0" class="product-grid">
      <ProductCard v-for="product in products" :key="product.id" :product="product" />
    </div>
    <div v-else-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>
    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="8" y1="15" x2="16" y2="15"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/></svg>
      <p>暂无商品</p>
    </div>
    <div v-if="totalPages > 1" class="pagination">
      <button
        class="page-btn"
        :disabled="currentPage <= 1"
        @click="changePage(currentPage - 1)"
      >
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
        上一页
      </button>
      <div class="page-indicator">
        <span class="page-current">{{ currentPage }}</span>
        <span class="page-sep">/</span>
        <span class="page-total">{{ totalPages }}</span>
      </div>
      <button
        class="page-btn"
        :disabled="currentPage >= totalPages"
        @click="changePage(currentPage + 1)"
      >
        下一页
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { pageQuery, listBrands } from '../api/product.js'
import ProductCard from '../components/ProductCard.vue'

const products = ref([])
const loading = ref(true)
const currentPage = ref(1)
const totalPages = ref(1)
const pageSize = 12

// 搜索 / 筛选 / 排序条件
const keyword = ref('')
const brand = ref('')
const sort = ref('')
const brands = ref([])

async function loadProducts(page) {
  loading.value = true
  try {
    const res = await pageQuery(page, pageSize, {
      keyword: keyword.value,
      brand: brand.value,
      sort: sort.value
    })
    // res.data 为 Paging<Product>：{ pageNum, pageSize, totalPage, totalCount, data }
    products.value = res.data.data || []
    totalPages.value = res.data.totalPage || 1
  } catch (error) {
    console.error('加载商品失败:', error)
  } finally {
    loading.value = false
  }
}

// 变更条件后回到第一页重新查询
function search() {
  currentPage.value = 1
  loadProducts(1)
}

function changePage(page) {
  currentPage.value = page
  loadProducts(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  loadProducts(1)
  try {
    const res = await listBrands()
    brands.value = res.data || []
  } catch (e) {
    // 品牌列表加载失败不阻塞页面
  }
})
</script>

<style scoped>
.products-page {
  padding: 20px 0;
}

.page-header {
  margin-bottom: 32px;
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

/* ========== 搜索/筛选/排序 ========== */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 8px 0 12px;
  color: var(--color-text-tertiary);
  transition: border-color var(--transition-fast);
  flex: 1;
  min-width: 240px;
  max-width: 360px;
}

.search-box:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-50);
}

.search-box input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  padding: 10px 0;
  font-size: 14px;
  color: var(--color-text);
}

.btn-search {
  padding: 8px 16px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  border-radius: 6px;
  transition: all var(--transition-fast);
}

.btn-search:hover {
  opacity: 0.9;
}

.filter-select {
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text);
  font-size: 14px;
  outline: none;
  cursor: pointer;
  transition: border-color var(--transition-fast);
}

.filter-select:focus {
  border-color: var(--color-primary);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
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

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 80px 0;
  color: var(--color-text-tertiary);
}

.empty-state svg {
  opacity: 0.4;
}

.empty-state p {
  font-size: 15px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 48px;
  padding: 20px 0;
}

.page-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.page-btn:hover:not(:disabled) {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: var(--color-primary-50);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.page-current {
  font-weight: 700;
  color: var(--color-primary);
  font-size: 16px;
}

.page-sep {
  color: var(--color-text-tertiary);
}

.page-total {
  color: var(--color-text-tertiary);
}

@media (max-width: 1024px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
