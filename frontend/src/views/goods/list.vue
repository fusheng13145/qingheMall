<template>
  <div class="goods-list-page">
    <div class="container">
      <Breadcrumb :items="breadcrumbItems" />

      <div class="content-wrapper">
        <!-- 左侧分类导航 -->
        <aside class="left-sidebar">
          <div class="sidebar-section">
            <h3 class="sidebar-title">
              <el-icon><Grid /></el-icon>
              商品分类
            </h3>
            <div class="category-tree">
              <div
                v-for="category in categories"
                :key="category.id"
                class="category-parent"
              >
                <div
                  :class="['category-node', { active: filters.categoryId === category.id }]"
                  @click="handleCategoryClick(category.id)"
                >
                  <span class="category-icon">{{ category.icon }}</span>
                  <span class="category-name">{{ category.name }}</span>
                </div>
                <div class="category-children" v-if="category.children?.length">
                  <div
                    v-for="child in category.children"
                    :key="child.id"
                    :class="['category-child', { active: filters.categoryId === child.id }]"
                    @click="handleCategoryClick(child.id)"
                  >
                    {{ child.name }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="sidebar-section">
            <h3 class="sidebar-title">
              <el-icon><Filter /></el-icon>
              筛选条件
            </h3>
            <div class="filter-options">
              <div class="filter-item">
                <span class="filter-label">价格区间</span>
                <el-input
                  v-model="priceRange.min"
                  placeholder="最低价"
                  size="small"
                  @change="handleFilterChange"
                />
                <span class="filter-separator">-</span>
                <el-input
                  v-model="priceRange.max"
                  placeholder="最高价"
                  size="small"
                  @change="handleFilterChange"
                />
              </div>
              <div class="filter-item">
                <span class="filter-label">标签</span>
                <div class="tag-list">
                  <el-check-tag
                    v-for="tag in availableTags"
                    :key="tag"
                    :checked="selectedTags.includes(tag)"
                    @change="handleTagToggle(tag)"
                  >
                    {{ tag }}
                  </el-check-tag>
                </div>
              </div>
            </div>
          </div>

          <div class="sidebar-section">
            <h3 class="sidebar-title">
              <el-icon><TrendCharts /></el-icon>
              销量排行
            </h3>
            <div class="sales-rank">
              <div
                v-for="(item, index) in salesRankList"
                :key="item.id"
                class="rank-item"
                @click="goToDetail(item.id)"
              >
                <span :class="['rank-num', { top: index < 3 }]">{{ index + 1 }}</span>
                <img :src="item.thumbnail" class="rank-image" />
                <div class="rank-info">
                  <span class="rank-name">{{ item.name }}</span>
                  <span class="rank-price">¥{{ item.price }}</span>
                </div>
              </div>
            </div>
          </div>
        </aside>

        <!-- 右侧商品列表 -->
        <main class="main-content">
          <!-- 搜索结果栏 -->
          <div class="search-result-bar">
            <div class="result-info">
              <span v-if="filters.keyword">搜索关键词: <strong>{{ filters.keyword }}</strong></span>
              <span>共找到 <strong>{{ total }}</strong> 件商品</span>
            </div>
            <div class="sort-bar">
              <span class="sort-label">排序:</span>
              <el-radio-group v-model="filters.sortBy" size="small" @change="handleSortChange">
                <el-radio-button label="">综合</el-radio-button>
                <el-radio-button label="sales">销量</el-radio-button>
                <el-radio-button label="price">价格</el-radio-button>
                <el-radio-button label="createdAt">新品</el-radio-button>
              </el-radio-group>
              <el-checkbox
                v-model="filters.order"
                true-value="asc"
                false-value="desc"
                v-if="filters.sortBy === 'price'"
                class="price-order"
              >
                {{ filters.order === 'asc' ? '从低到高' : '从高到低' }}
              </el-checkbox>
            </div>
          </div>

          <!-- 快捷筛选标签 -->
          <div class="quick-filter">
            <span class="quick-label">快速筛选:</span>
            <el-check-tag
              v-for="tag in availableTags"
              :key="tag"
              :checked="selectedTags.includes(tag)"
              @change="handleTagToggle(tag)"
              class="quick-tag"
            >
              {{ tag }}
            </el-check-tag>
            <el-button size="small" text type="primary" @click="clearFilters" v-if="hasActiveFilters">
              清除筛选
            </el-button>
          </div>

          <!-- 商品列表 -->
          <div class="goods-list" v-loading="loading">
            <div class="goods-grid" v-if="goodsList.length > 0">
              <GoodsCard v-for="goods in goodsList" :key="goods.id" :goods="goods" />
            </div>
            <el-empty v-else description="暂无商品" />
          </div>

          <!-- 分页 -->
          <div class="pagination-wrapper" v-if="total > 0">
            <Pagination
              :total="total"
              :page="filters.page"
              :limit="filters.pageSize"
              @pagination="handlePagination"
            />
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Grid, Filter, TrendCharts } from '@element-plus/icons-vue'
import Breadcrumb from '@/components/common/Breadcrumb.vue'
import Pagination from '@/components/common/Pagination.vue'
import GoodsCard from '@/components/business/GoodsCard.vue'
import type { Goods } from '@/api/modules/goods'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const goodsList = ref<Goods[]>([])
const total = ref(0)

const filters = reactive({
  categoryId: undefined as number | undefined,
  keyword: '',
  sortBy: '',
  order: 'desc' as 'asc' | 'desc',
  page: 1,
  pageSize: 20
})

const priceRange = reactive({
  min: '',
  max: ''
})

const selectedTags = ref<string[]>([])

const categories = ref([
  {
    id: 1,
    name: '服装',
    icon: '👗',
    children: [
      { id: 11, name: '女装' },
      { id: 12, name: '男装' },
      { id: 13, name: '童装' }
    ]
  },
  {
    id: 2,
    name: '数码',
    icon: '📱',
    children: [
      { id: 21, name: '手机' },
      { id: 22, name: '电脑' },
      { id: 23, name: '平板' }
    ]
  },
  {
    id: 3,
    name: '食品',
    icon: '🍎',
    children: [
      { id: 31, name: '水果' },
      { id: 32, name: '零食' },
      { id: 33, name: '粮油' }
    ]
  },
  {
    id: 4,
    name: '美妆',
    icon: '💄',
    children: [
      { id: 41, name: '护肤' },
      { id: 42, name: '彩妆' },
      { id: 43, name: '香水' }
    ]
  },
  {
    id: 5,
    name: '家居',
    icon: '🏠',
    children: [
      { id: 51, name: '家具' },
      { id: 52, name: '家纺' },
      { id: 53, name: '厨具' }
    ]
  },
  {
    id: 6,
    name: '母婴',
    icon: '🍼',
    children: [
      { id: 61, name: '奶粉' },
      { id: 62, name: '纸尿裤' },
      { id: 63, name: '玩具' }
    ]
  }
])

const availableTags = ['热销', '新品', '促销', '推荐']

const salesRankList = ref([
  { id: 1, name: '热门商品1', price: 199, thumbnail: 'https://picsum.photos/80/80?random=101' },
  { id: 2, name: '热门商品2', price: 159, thumbnail: 'https://picsum.photos/80/80?random=102' },
  { id: 3, name: '热门商品3', price: 129, thumbnail: 'https://picsum.photos/80/80?random=103' },
  { id: 4, name: '热门商品4', price: 99, thumbnail: 'https://picsum.photos/80/80?random=104' },
  { id: 5, name: '热门商品5', price: 79, thumbnail: 'https://picsum.photos/80/80?random=105' }
])

const breadcrumbItems = computed(() => {
  const items = [{ title: '商品列表' }]
  if (filters.keyword) {
    items.unshift({ title: `搜索: ${filters.keyword}`, path: '/goods/list' })
  }
  return items
})

const hasActiveFilters = computed(() => {
  return filters.categoryId !== undefined || filters.keyword || selectedTags.value.length > 0
})

const fetchGoodsList = async () => {
  loading.value = true
  try {
    // 模拟数据
    total.value = 40
    goodsList.value = Array.from({ length: 20 }, (_, i) => ({
      id: i + 1,
      name: `商品${i + 1}`,
      price: 99 + (i % 10) * 20,
      originalPrice: 199 + (i % 10) * 20,
      thumbnail: `https://picsum.photos/300/300?random=${i + 50}`,
      sales: 500 + i * 50,
      stock: 100,
      tags: i % 3 === 0 ? ['热销'] : i % 3 === 1 ? ['新品'] : []
    }))
  } finally {
    loading.value = false
  }
}

const handlePagination = ({ page, limit }: { page: number; limit: number }) => {
  filters.page = page
  filters.pageSize = limit
  fetchGoodsList()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleCategoryClick = (categoryId: number) => {
  if (filters.categoryId === categoryId) {
    filters.categoryId = undefined
  } else {
    filters.categoryId = categoryId
  }
  filters.page = 1
  fetchGoodsList()
}

const handleSortChange = () => {
  filters.page = 1
  fetchGoodsList()
}

const handleFilterChange = () => {
  filters.page = 1
  fetchGoodsList()
}

const handleTagToggle = (tag: string) => {
  const index = selectedTags.value.indexOf(tag)
  if (index > -1) {
    selectedTags.value.splice(index, 1)
  } else {
    selectedTags.value.push(tag)
  }
  filters.page = 1
  fetchGoodsList()
}

const clearFilters = () => {
  filters.categoryId = undefined
  filters.keyword = ''
  filters.sortBy = ''
  filters.order = 'desc'
  selectedTags.value = []
  priceRange.min = ''
  priceRange.max = ''
  filters.page = 1
  fetchGoodsList()
}

const goToDetail = (id: number) => {
  router.push(`/goods/detail/${id}`)
}

const initFiltersFromQuery = () => {
  if (route.query.categoryId) {
    filters.categoryId = Number(route.query.categoryId)
  }
  if (route.query.keyword) {
    filters.keyword = route.query.keyword as string
  }
  if (route.query.sortBy) {
    filters.sortBy = route.query.sortBy as string
  }
  if (route.query.tag) {
    selectedTags.value = [route.query.tag as string]
  }
}

onMounted(() => {
  initFiltersFromQuery()
  fetchGoodsList()
})

watch(() => route.query, () => {
  initFiltersFromQuery()
  fetchGoodsList()
})
</script>

<style lang="scss" scoped>
.goods-list-page {
  background-color: #f5f7fa;
  min-height: 100vh;
}

.container {
  width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}

.content-wrapper {
  display: flex;
  gap: 20px;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

// 左侧边栏
.left-sidebar {
  width: 220px;
  flex-shrink: 0;
}

.sidebar-section {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f2f5;

  &:last-child {
    border-bottom: none;
    margin-bottom: 0;
    padding-bottom: 0;
  }
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #409eff;
}

// 分类树
.category-tree {
  .category-parent {
    margin-bottom: 4px;
  }

  .category-node {
    display: flex;
    align-items: center;
    padding: 10px 12px;
    cursor: pointer;
    border-radius: 4px;
    transition: all 0.3s;

    &:hover,
    &.active {
      background: #ecf5ff;
      color: #409eff;
    }

    .category-icon {
      font-size: 16px;
      margin-right: 8px;
    }

    .category-name {
      flex: 1;
      font-size: 14px;
    }
  }

  .category-children {
    padding-left: 32px;

    .category-child {
      padding: 8px 12px;
      font-size: 13px;
      color: #606266;
      cursor: pointer;
      border-radius: 4px;
      transition: all 0.3s;

      &:hover,
      &.active {
        background: #f5f7fa;
        color: #409eff;
      }

      &.active {
        &::before {
          content: '';
          display: inline-block;
          width: 3px;
          height: 3px;
          background: #409eff;
          border-radius: 50%;
          margin-right: 6px;
        }
      }
    }
  }
}

// 筛选选项
.filter-options {
  .filter-item {
    margin-bottom: 16px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .filter-label {
    display: block;
    font-size: 13px;
    color: #909399;
    margin-bottom: 10px;
  }

  .filter-separator {
    padding: 0 8px;
    color: #c0c4cc;
  }

  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
}

// 销量排行
.sales-rank {
  .rank-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 0;
    cursor: pointer;
    border-bottom: 1px dashed #f0f2f5;
    transition: opacity 0.3s;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      opacity: 0.8;
    }

    .rank-num {
      width: 18px;
      height: 18px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      background: #f5f7fa;
      color: #909399;
      border-radius: 2px;

      &.top {
        background: #ff6b6b;
        color: #fff;
      }
    }

    .rank-image {
      width: 50px;
      height: 50px;
      border-radius: 4px;
      object-fit: cover;
    }

    .rank-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 4px;

      .rank-name {
        font-size: 13px;
        color: #303133;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .rank-price {
        font-size: 14px;
        color: #f56c6c;
        font-weight: bold;
      }
    }
  }
}

// 主内容区
.main-content {
  flex: 1;
  min-width: 0;
}

.search-result-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f2f5;
  margin-bottom: 16px;

  .result-info {
    font-size: 14px;
    color: #606266;

    strong {
      color: #409eff;
    }
  }

  .sort-bar {
    display: flex;
    align-items: center;
    gap: 12px;

    .sort-label {
      font-size: 14px;
      color: #909399;
    }

    .price-order {
      margin-left: 8px;
    }
  }
}

.quick-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  margin-bottom: 16px;

  .quick-label {
    font-size: 13px;
    color: #909399;
  }

  .quick-tag {
    margin-right: 4px;
  }
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.pagination-wrapper {
  margin-top: 20px;
}

@media (max-width: 1200px) {
  .content-wrapper {
    flex-direction: column;
  }

  .left-sidebar {
    width: 100%;
    display: flex;
    gap: 20px;
    overflow-x: auto;

    .sidebar-section {
      flex-shrink: 0;
      width: 200px;
    }
  }

  .goods-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
