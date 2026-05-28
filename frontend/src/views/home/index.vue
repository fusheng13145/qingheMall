<template>
  <div class="home-page">
    <!-- 顶部促销横幅 -->
    <div class="promotion-bar">
      <div class="container">
        <div class="promotion-content">
          <div class="promotion-left">
            <el-icon><Bell /></el-icon>
            <span>公告：</span>
            <span class="promotion-text">全场满99元包邮，新用户首单立减10元</span>
          </div>
          <div class="promotion-right">
            <router-link to="/login">登录</router-link>
            <span class="divider">|</span>
            <router-link to="/register">注册</router-link>
            <span class="divider">|</span>
            <router-link to="/user/profile">我的订单</router-link>
            <span class="divider">|</span>
            <router-link to="/cart">购物车</router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索栏区域 -->
    <div class="search-section">
      <div class="container">
        <div class="search-content">
          <router-link to="/" class="logo">
            <span class="logo-icon">🌾</span>
            <span class="logo-text">青禾商城</span>
          </router-link>
          <div class="search-box">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索商品、品牌、店铺"
              size="large"
              class="search-input"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button :icon="Search" @click="handleSearch" class="search-btn">搜索</el-button>
              </template>
            </el-input>
            <div class="search-tags">
              <span
                v-for="tag in hotSearchTags"
                :key="tag"
                class="search-tag"
                @click="searchKeyword = tag"
              >
                {{ tag }}
              </span>
            </div>
          </div>
          <div class="cart-preview">
            <router-link to="/cart" class="cart-link">
              <el-badge :value="cartCount" :hidden="cartCount === 0" :max="99">
                <el-icon :size="24"><ShoppingCart /></el-icon>
              </el-badge>
              <span>我的购物车</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- 分类导航 -->
    <div class="category-section">
      <div class="container">
        <div class="category-content">
          <div class="category-menu">
            <div class="category-title">
              <el-icon><Grid /></el-icon>
              <span>全部商品分类</span>
            </div>
            <div class="category-list">
              <div
                v-for="category in categories"
                :key="category.id"
                class="category-item"
                @mouseenter="activeCategory = category.id"
                @mouseleave="activeCategory = null"
              >
                <span class="category-icon">{{ category.icon }}</span>
                <span class="category-name">{{ category.name }}</span>
                <el-icon class="arrow"><ArrowRight /></el-icon>
                <!-- 子分类悬浮面板 -->
                <div class="sub-category" v-if="activeCategory === category.id && category.children?.length">
                  <div
                    v-for="child in category.children"
                    :key="child.id"
                    class="sub-category-item"
                    @click="goToCategory(child.id)"
                  >
                    {{ child.name }}
                  </div>
                </div>
              </div>
            </div>
          </div>
          <nav class="main-nav">
            <router-link to="/" class="nav-item">首页</router-link>
            <router-link to="/goods/list" class="nav-item">全部商品</router-link>
            <router-link to="/goods/list?tag=hot" class="nav-item">热销</router-link>
            <router-link to="/goods/list?tag=new" class="nav-item">新品</router-link>
            <router-link to="/goods/list?tag=promotion" class="nav-item">促销</router-link>
          </nav>
        </div>
      </div>
    </div>

    <!-- 轮播图 -->
    <div class="banner-section">
      <el-carousel :interval="4000" type="card" height="400px">
        <el-carousel-item v-for="(banner, index) in banners" :key="index">
          <img :src="banner.image" :alt="banner.title" class="banner-image" />
        </el-carousel-item>
      </el-carousel>
    </div>

    <div class="container">
      <!-- 热门商品 -->
      <section class="goods-section">
        <div class="section-header">
          <h2 class="section-title">
            <el-icon class="title-icon"><HotWater /></el-icon>
            热门推荐
          </h2>
          <router-link to="/goods/list?sortBy=sales&order=desc" class="more-link">
            查看更多 <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>
        <div class="goods-grid">
          <GoodsCard v-for="goods in hotGoods" :key="goods.id" :goods="goods" />
        </div>
      </section>

      <!-- 新品上市 -->
      <section class="goods-section">
        <div class="section-header">
          <h2 class="section-title">
            <el-icon class="title-icon"><Present /></el-icon>
            新品上市
          </h2>
          <router-link to="/goods/list?sortBy=createdAt&order=desc" class="more-link">
            查看更多 <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>
        <div class="goods-grid">
          <GoodsCard v-for="goods in newGoods" :key="goods.id" :goods="goods" />
        </div>
      </section>

      <!-- 为您推荐 -->
      <section class="goods-section">
        <div class="section-header">
          <h2 class="section-title">
            <el-icon class="title-icon"><Star /></el-icon>
            为您推荐
          </h2>
        </div>
        <div class="goods-grid">
          <GoodsCard v-for="goods in recommendGoods" :key="goods.id" :goods="goods" />
        </div>
      </section>
    </div>

    <!-- 底部信息 -->
    <div class="home-bottom-info">
      <div class="container">
        <div class="info-grid">
          <div class="info-item">
            <el-icon :size="40" color="#409eff"><ShoppingCart /></el-icon>
            <div class="info-text">
              <h4>正品保障</h4>
              <p>全场商品正品保证</p>
            </div>
          </div>
          <div class="info-item">
            <el-icon :size="40" color="#409eff"><Van /></el-icon>
            <div class="info-text">
              <h4>快速配送</h4>
              <p>全场满99元包邮</p>
            </div>
          </div>
          <div class="info-item">
            <el-icon :size="40" color="#409eff"><Service /></el-icon>
            <div class="info-text">
              <h4>售后服务</h4>
              <p>7天无理由退换货</p>
            </div>
          </div>
          <div class="info-item">
            <el-icon :size="40" color="#409eff"><Headset /></el-icon>
            <div class="info-text">
              <h4>在线客服</h4>
              <p>24小时在线解答</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Search,
  Bell,
  Grid,
  ShoppingCart,
  HotWater,
  Present,
  Star,
  Van,
  Service,
  Headset
} from '@element-plus/icons-vue'
import GoodsCard from '@/components/business/GoodsCard.vue'
import { useCartStore } from '@/stores/cart'
import { storeToRefs } from 'pinia'

const router = useRouter()
const cartStore = useCartStore()
const { totalCount: cartCount } = storeToRefs(cartStore)

const searchKeyword = ref('')
const activeCategory = ref<number | null>(null)

const hotSearchTags = ['手机', '电脑', '服装', '美妆', '食品', '家居']

const hotGoods = ref<any[]>([])
const newGoods = ref<any[]>([])
const recommendGoods = ref<any[]>([])

const banners = ref([
  {
    title: '夏季特惠',
    image: 'https://picsum.photos/1200/400?random=1'
  },
  {
    title: '新品上市',
    image: 'https://picsum.photos/1200/400?random=2'
  },
  {
    title: '限时折扣',
    image: 'https://picsum.photos/1200/400?random=3'
  }
])

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
  },
  {
    id: 7,
    name: '运动',
    icon: '⚽',
    children: [
      { id: 71, name: '运动鞋' },
      { id: 72, name: '运动服' },
      { id: 73, name: '健身器材' }
    ]
  },
  {
    id: 8,
    name: '图书',
    icon: '📚',
    children: [
      { id: 81, name: '小说' },
      { id: 82, name: '儿童读物' },
      { id: 83, name: '教辅' }
    ]
  }
])

const goToCategory = (categoryId: number) => {
  router.push(`/goods/list?categoryId=${categoryId}`)
}

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/goods/list', query: { keyword: searchKeyword.value } })
  } else {
    router.push('/goods/list')
  }
}

onMounted(async () => {
  // 模拟数据
  hotGoods.value = Array.from({ length: 8 }, (_, i) => ({
    id: i + 1,
    name: `热门商品${i + 1}`,
    price: 99 + i * 10,
    originalPrice: 199 + i * 10,
    thumbnail: `https://picsum.photos/300/300?random=${i + 10}`,
    sales: 1000 + i * 100,
    tags: i === 0 ? ['热销'] : []
  }))

  newGoods.value = Array.from({ length: 8 }, (_, i) => ({
    id: i + 100,
    name: `新品商品${i + 1}`,
    price: 79 + i * 8,
    thumbnail: `https://picsum.photos/300/300?random=${i + 20}`,
    tags: i === 0 ? ['新品'] : []
  }))

  recommendGoods.value = Array.from({ length: 8 }, (_, i) => ({
    id: i + 200,
    name: `推荐商品${i + 1}`,
    price: 129 + i * 12,
    thumbnail: `https://picsum.photos/300/300?random=${i + 30}`,
    description: '优质商品，限时特惠'
  }))
})
</script>

<style lang="scss" scoped>
.home-page {
  background-color: #f5f7fa;
}

// 顶部促销横幅
.promotion-bar {
  background: linear-gradient(90deg, #ff6b6b, #ff8e53);
  color: #fff;
  font-size: 13px;

  .container {
    width: 1200px;
    margin: 0 auto;
    padding: 0 16px;
  }

  .promotion-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 36px;
  }

  .promotion-left {
    display: flex;
    align-items: center;
    gap: 8px;

    .promotion-text {
      opacity: 0.95;
    }
  }

  .promotion-right {
    display: flex;
    align-items: center;
    gap: 8px;

    a {
      color: #fff;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }

    .divider {
      opacity: 0.5;
    }
  }
}

// 搜索栏区域
.search-section {
  background: #fff;
  padding: 20px 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);

  .container {
    width: 1200px;
    margin: 0 auto;
    padding: 0 16px;
  }

  .search-content {
    display: flex;
    align-items: center;
    gap: 40px;
  }

  .logo {
    display: flex;
    align-items: center;
    text-decoration: none;
    gap: 8px;

    .logo-icon {
      font-size: 32px;
    }

    .logo-text {
      font-size: 24px;
      font-weight: bold;
      color: #409eff;
    }
  }

  .search-box {
    flex: 1;
    max-width: 600px;

    .search-input {
      :deep(.el-input__wrapper) {
        border-radius: 20px 0 0 20px;
      }

      :deep(.el-input-group__append) {
        border-radius: 0 20px 20px 0;
        background: #409eff;
        border-color: #409eff;

        .search-btn {
          color: #fff;
          font-size: 16px;
        }
      }
    }

    .search-tags {
      display: flex;
      gap: 12px;
      margin-top: 8px;
      padding-left: 16px;

      .search-tag {
        font-size: 12px;
        color: #909399;
        cursor: pointer;

        &:hover {
          color: #409eff;
        }
      }
    }
  }

  .cart-preview {
    .cart-link {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      border: 1px solid #ddd;
      border-radius: 20px;
      text-decoration: none;
      color: #303133;
      transition: all 0.3s;

      &:hover {
        border-color: #409eff;
        color: #409eff;
      }
    }
  }
}

// 分类导航区域
.category-section {
  background: #fff;
  margin-bottom: 20px;

  .container {
    width: 1200px;
    margin: 0 auto;
    padding: 0 16px;
  }

  .category-content {
    display: flex;
  }

  .category-menu {
    width: 200px;
    position: relative;

    .category-title {
      display: flex;
      align-items: center;
      gap: 8px;
      height: 44px;
      background: #409eff;
      color: #fff;
      padding: 0 16px;
      font-size: 15px;
      font-weight: bold;
      border-radius: 4px 4px 0 0;
    }

    .category-list {
      background: #fff;
      border: 1px solid #e8e8e8;
      border-top: none;
      border-radius: 0 0 4px 4px;
    }

    .category-item {
      display: flex;
      align-items: center;
      padding: 10px 16px;
      cursor: pointer;
      transition: background 0.3s;
      position: relative;

      &:hover {
        background: #f5f7fa;

        .arrow {
          display: block;
        }
      }

      .category-icon {
        font-size: 18px;
        margin-right: 10px;
      }

      .category-name {
        flex: 1;
        font-size: 14px;
        color: #303133;
      }

      .arrow {
        font-size: 12px;
        color: #c0c4cc;
        display: none;
      }

      .sub-category {
        position: absolute;
        left: 100%;
        top: 0;
        min-width: 150px;
        background: #fff;
        border: 1px solid #e8e8e8;
        border-radius: 4px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        z-index: 100;
        display: flex;
        flex-direction: column;

        .sub-category-item {
          padding: 10px 16px;
          font-size: 13px;
          color: #606266;
          transition: all 0.3s;

          &:hover {
            background: #409eff;
            color: #fff;
          }
        }
      }
    }
  }

  .main-nav {
    display: flex;
    align-items: center;
    flex: 1;
    padding-left: 40px;

    .nav-item {
      padding: 12px 24px;
      font-size: 15px;
      color: #303133;
      text-decoration: none;
      transition: color 0.3s;

      &:hover,
      &.router-link-active {
        color: #409eff;
      }
    }
  }
}

// 轮播图区域
.banner-section {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 16px;

  .banner-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 8px;
  }

  :deep(.el-carousel__item--card) {
    border-radius: 8px;
  }
}

.container {
  width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}

// 商品区域
.goods-section {
  margin-bottom: 40px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 22px;
    font-weight: bold;
    color: #303133;

    .title-icon {
      color: #f56c6c;
    }
  }

  .more-link {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #909399;
    font-size: 14px;
    text-decoration: none;

    &:hover {
      color: #409eff;
    }
  }
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

// 底部信息
.home-bottom-info {
  background: #fff;
  padding: 40px 0;
  margin-top: 40px;

  .info-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 40px;
  }

  .info-item {
    display: flex;
    align-items: center;
    gap: 16px;

    .info-text {
      h4 {
        font-size: 16px;
        color: #303133;
        margin-bottom: 4px;
      }

      p {
        font-size: 13px;
        color: #909399;
      }
    }
  }
}
</style>
