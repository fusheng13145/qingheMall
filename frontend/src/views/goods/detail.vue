<template>
  <div class="goods-detail-page">
    <div class="container">
      <Breadcrumb :items="breadcrumbItems" />

      <div class="goods-detail" v-loading="loading">
        <!-- 商品图片 -->
        <div class="goods-images">
          <div class="main-image" @click="handleImagePreview">
            <el-image
              :src="activeImage || goods.images?.[0] || defaultImage"
              :zoom-rate="1.2"
              :preview-src-list="goods.images || [defaultImage]"
              fit="cover"
            />
            <div class="image-badge" v-if="goods.tags?.length">
              <el-tag type="danger" size="small">{{ goods.tags[0] }}</el-tag>
            </div>
          </div>
          <div class="thumbnail-list" v-if="goods.images?.length">
            <div
              v-for="(img, index) in goods.images"
              :key="index"
              :class="['thumbnail-item', { active: activeImage === img }]"
              @click="activeImage = img"
            >
              <el-image :src="img" fit="cover" />
            </div>
          </div>
          <div class="images-tip">
            <el-icon><ZoomIn /></el-icon>
            <span>点击图片可放大</span>
          </div>
        </div>

        <!-- 商品信息 -->
        <div class="goods-info">
          <h1 class="goods-title">{{ goods.name }}</h1>
          <p class="goods-desc" v-if="goods.description">{{ goods.description }}</p>

          <div class="goods-price-box">
            <div class="price-row">
              <span class="label">价格</span>
              <span class="current-price">¥{{ goods.price?.toFixed(2) }}</span>
              <span class="original-price" v-if="goods.originalPrice">
                ¥{{ goods.originalPrice?.toFixed(2) }}
              </span>
              <span class="discount" v-if="goods.originalPrice">
                {{ Math.round((goods.price / goods.originalPrice) * 10) }}折
              </span>
            </div>
            <div class="price-row" v-if="goods.tags?.length">
              <span class="label">促销</span>
              <div class="tags">
                <el-tag v-for="tag in goods.tags" :key="tag" size="small" type="danger">{{ tag }}</el-tag>
              </div>
            </div>
            <div class="price-row promotion-row" v-if="goods.promotion">
              <span class="label">优惠</span>
              <span class="promotion-text">{{ goods.promotion }}</span>
            </div>
          </div>

          <div class="goods-stats">
            <div class="stat-item">
              <span class="stat-label">销量</span>
              <span class="stat-value">{{ goods.sales || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">库存</span>
              <span class="stat-value" :class="{ 'low-stock': goods.stock < 10 }">
                {{ goods.stock || 0 }}
              </span>
            </div>
            <div class="stat-item" v-if="goods.rating">
              <span class="stat-label">评分</span>
              <el-rate v-model="goods.rating" disabled text-color="#ff9900" />
            </div>
          </div>

          <div class="sku-section" v-if="goods.categoryName">
            <span class="label">分类</span>
            <el-link type="primary">{{ goods.categoryName }}</el-link>
          </div>

          <div class="specs-section" v-if="goods.specs?.length">
            <span class="label">规格</span>
            <div class="specs-list">
              <el-tag
                v-for="spec in goods.specs"
                :key="spec"
                size="small"
                :type="selectedSpecs.includes(spec) ? 'primary' : 'info'"
                @click="toggleSpec(spec)"
              >
                {{ spec }}
              </el-tag>
            </div>
          </div>

          <div class="quantity-section">
            <span class="label">数量</span>
            <el-input-number
              v-model="quantity"
              :min="1"
              :max="goods.stock || 99"
              @change="handleQuantityChange"
            />
            <span class="stock-tip">件 (库存{{ goods.stock || 0 }}件)</span>
          </div>

          <div class="action-buttons">
            <el-button type="primary" size="large" @click="handleBuyNow">立即购买</el-button>
            <el-button size="large" @click="handleAddToCart">
              <el-icon><ShoppingCart /></el-icon>
              加入购物车
            </el-button>
            <el-button size="large" @click="handleCollect">
              <el-icon><Star /></el-icon>
            </el-button>
          </div>

          <div class="service-commitment">
            <div class="commitment-item">
              <el-icon color="#67c23a"><CircleCheck /></el-icon>
              <span>7天无理由退换</span>
            </div>
            <div class="commitment-item">
              <el-icon color="#67c23a"><CircleCheck /></el-icon>
              <span>正品保证</span>
            </div>
            <div class="commitment-item">
              <el-icon color="#67c23a"><CircleCheck /></el-icon>
              <span>急速发货</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 商品详情 -->
      <div class="goods-detail-content">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="商品详情" name="detail">
            <div class="detail-content" v-html="goodsDetailHtml"></div>
          </el-tab-pane>
          <el-tab-pane label="商品评价" name="review">
            <div class="review-list">
              <div class="review-summary">
                <div class="score-info">
                  <span class="score">{{ goods.rating || 4.8 }}</span>
                  <el-rate v-model="goods.rating" disabled show-score />
                  <span class="count">共 {{ reviewCount }} 条评价</span>
                </div>
              </div>
              <el-empty description="暂无评价" />
            </div>
          </el-tab-pane>
          <el-tab-pane label="售后保障" name="service">
            <div class="service-content">
              <div class="service-section">
                <h4><el-icon><CircleCheck /></el-icon> 7天无理由退换货</h4>
                <p>自商品签收之日起7天内，在商品完好的情况下可申请无理由退换货。退换货时请确保商品及其附属配件、赠品、发票等完整。</p>
              </div>
              <div class="service-section">
                <h4><el-icon><Wallet /></el-icon> 正品保证</h4>
                <p>青禾商城所有商品均为正品采购，我们承诺提供100%正品保障。如发现假冒商品，经核实后给予加倍赔偿。</p>
              </div>
              <div class="service-section">
                <h4><el-icon><Van /></el-icon> 配送说明</h4>
                <p>全场购物满99元包邮（港澳台及海外地区除外）。一般情况下，订单支付成功后48小时内发货，2-5个工作日送达。</p>
              </div>
              <div class="service-section">
                <h4><el-icon><Refrigerator /></el-icon> 特殊商品说明</h4>
                <p>部分商品（生鲜、食品、化妆品等）因商品特性不适用7天无理由退换货，请购买前仔细阅读商品详情页说明。</p>
              </div>
              <div class="service-section">
                <h4><el-icon><Service /></el-icon> 售后服务</h4>
                <p>如有售后问题，请联系在线客服或拨打客服热线：400-888-8888。我们将竭诚为您提供满意的售后服务。</p>
              </div>
              <div class="service-section">
                <h4><el-icon><Document /></el-icon> 退换货流程</h4>
                <ol>
                  <li>联系客服申请退换货</li>
                  <li>提交退换货原因及凭证</li>
                  <li>客服审核通过后寄回商品</li>
                  <li>商家收到商品后处理退款/换货</li>
                </ol>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 看了又看 -->
      <div class="recommend-section">
        <div class="section-header">
          <h3>看了又看</h3>
        </div>
        <div class="recommend-list">
          <div
            v-for="item in recommendList"
            :key="item.id"
            class="recommend-item"
            @click="goToDetail(item.id)"
          >
            <img :src="item.thumbnail" class="recommend-image" />
            <div class="recommend-info">
              <span class="recommend-name">{{ item.name }}</span>
              <span class="recommend-price">¥{{ item.price }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ZoomIn,
  ShoppingCart,
  Star,
  CircleCheck,
  Wallet,
  Van,
  Refrigerator,
  Service,
  Document
} from '@element-plus/icons-vue'
import Breadcrumb from '@/components/common/Breadcrumb.vue'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const loading = ref(false)
const quantity = ref(1)
const activeImage = ref('')
const activeTab = ref('detail')
const selectedSpecs = ref<string[]>([])
const reviewCount = ref(126)

const goods = ref<any>({
  id: 0,
  name: '',
  price: 0,
  originalPrice: 0,
  description: '',
  stock: 0,
  sales: 0,
  images: [],
  tags: [],
  categoryName: '',
  specs: [],
  promotion: ''
})

const defaultImage = 'https://cube.elemecdn.com/6/74/3a6f1b1d4b7c3a2d2e1e2e3e4e5f6g7.png'

const recommendList = ref([
  { id: 1, name: '推荐商品1', price: 99, thumbnail: 'https://picsum.photos/200/200?random=201' },
  { id: 2, name: '推荐商品2', price: 129, thumbnail: 'https://picsum.photos/200/200?random=202' },
  { id: 3, name: '推荐商品3', price: 159, thumbnail: 'https://picsum.photos/200/200?random=203' },
  { id: 4, name: '推荐商品4', price: 89, thumbnail: 'https://picsum.photos/200/200?random=204' },
  { id: 5, name: '推荐商品5', price: 199, thumbnail: 'https://picsum.photos/200/200?random=205' }
])

const breadcrumbItems = computed(() => [
  { title: '商品列表', path: '/goods/list' },
  { title: goods.value.categoryName || '商品分类', path: `/goods/list?categoryId=${goods.value.categoryId}` },
  { title: goods.value.name }
])

const goodsDetailHtml = ref(`
  <div style="padding: 20px; text-align: center; color: #909399;">
    商品详情加载中...
  </div>
`)

const fetchGoodsDetail = async () => {
  loading.value = true
  const id = Number(route.params.id)

  // 模拟数据
  goods.value = {
    id,
    name: `商品详情${id}`,
    price: 199,
    originalPrice: 399,
    description: '这是商品的详细描述，包含商品的特性、材质、使用方法等信息。优质面料，舒适穿着，适合各种场合。',
    stock: 100,
    sales: 1234,
    rating: 4.8,
    images: [
      'https://picsum.photos/600/600?random=100',
      'https://picsum.photos/600/600?random=101',
      'https://picsum.photos/600/600?random=102',
      'https://picsum.photos/600/600?random=103'
    ],
    tags: ['热销', '新品'],
    categoryName: '服装',
    categoryId: 1,
    specs: ['红色', '蓝色', '绿色', 'XL', 'L', 'M'],
    promotion: '满199减20元'
  }

  activeImage.value = goods.value.images[0]

  goodsDetailHtml.value = `
    <div style="padding: 20px;">
      <h3>商品介绍</h3>
      <p>${goods.value.description}</p>
      <h3>商品规格</h3>
      <table style="width: 100%; border-collapse: collapse;">
        <tr style="border-bottom: 1px solid #eee;">
          <td style="padding: 10px; color: #909399;">品牌</td>
          <td style="padding: 10px;">青禾商城</td>
        </tr>
        <tr style="border-bottom: 1px solid #eee;">
          <td style="padding: 10px; color: #909399;">产地</td>
          <td style="padding: 10px;">中国</td>
        </tr>
        <tr style="border-bottom: 1px solid #eee;">
          <td style="padding: 10px; color: #909399;">材质</td>
          <td style="padding: 10px;">优质面料</td>
        </tr>
        <tr style="border-bottom: 1px solid #eee;">
          <td style="padding: 10px; color: #909399;">适用人群</td>
          <td style="padding: 10px;">通用</td>
        </tr>
      </table>
      <h3>商品图片</h3>
      <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin-top: 10px;">
        <img src="https://picsum.photos/400/400?random=110" style="width: 100%; border-radius: 8px;" />
        <img src="https://picsum.photos/400/400?random=111" style="width: 100%; border-radius: 8px;" />
        <img src="https://picsum.photos/400/400?random=112" style="width: 100%; border-radius: 8px;" />
        <img src="https://picsum.photos/400/400?random=113" style="width: 100%; border-radius: 8px;" />
      </div>
    </div>
  `

  loading.value = false
}

const handleQuantityChange = (value: number) => {
  quantity.value = value
}

const toggleSpec = (spec: string) => {
  const index = selectedSpecs.value.indexOf(spec)
  if (index > -1) {
    selectedSpecs.value.splice(index, 1)
  } else {
    selectedSpecs.value.push(spec)
  }
}

const handleImagePreview = () => {
  // 图片预览由 el-image 组件自带处理
}

const handleAddToCart = async () => {
  try {
    await cartStore.addItem(goods.value.id, quantity.value)
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error('加入购物车失败')
  }
}

const handleBuyNow = () => {
  router.push({
    path: '/order/confirm',
    query: { goodsId: goods.value.id, quantity: quantity.value.toString() }
  })
}

const handleCollect = () => {
  ElMessage.success('已添加到收藏夹')
}

const goToDetail = (id: number) => {
  router.push(`/goods/detail/${id}`)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  fetchGoodsDetail()
})
</script>

<style lang="scss" scoped>
.goods-detail-page {
  background-color: #f5f7fa;
  min-height: 100vh;
  padding-bottom: 40px;
}

.container {
  width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}

.goods-detail {
  display: flex;
  gap: 40px;
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  margin-bottom: 20px;
}

.goods-images {
  width: 400px;
  flex-shrink: 0;
}

.main-image {
  width: 400px;
  height: 400px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
  margin-bottom: 16px;
  position: relative;
  cursor: pointer;

  :deep(.el-image) {
    width: 100%;
    height: 100%;
  }

  .image-badge {
    position: absolute;
    top: 16px;
    left: 16px;
  }
}

.thumbnail-list {
  display: flex;
  gap: 10px;

  .thumbnail-item {
    width: 68px;
    height: 68px;
    border-radius: 4px;
    overflow: hidden;
    cursor: pointer;
    border: 2px solid transparent;
    transition: border-color 0.3s;

    &.active {
      border-color: #409eff;
    }

    &:hover {
      border-color: #409eff;
    }
  }
}

.images-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}

.goods-info {
  flex: 1;
}

.goods-title {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 12px;
  line-height: 1.4;
}

.goods-desc {
  font-size: 14px;
  color: #909399;
  margin-bottom: 20px;
  line-height: 1.6;
}

.goods-price-box {
  background: #fff8f6;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  border: 1px solid #fdf0ee;

  .price-row {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;

    &:last-child {
      margin-bottom: 0;
    }

    .label {
      font-size: 14px;
      color: #909399;
      width: 60px;
    }

    .current-price {
      font-size: 28px;
      font-weight: bold;
      color: #f56c6c;
    }

    .original-price {
      font-size: 14px;
      color: #c0c4cc;
      text-decoration: line-through;
    }

    .discount {
      background: #f56c6c;
      color: #fff;
      padding: 2px 8px;
      font-size: 12px;
      border-radius: 2px;
    }

    .tags {
      display: flex;
      gap: 8px;
    }

    .promotion-text {
      color: #f56c6c;
      font-size: 14px;
    }
  }

  .promotion-row {
    background: #fff;
    padding: 8px 12px;
    border-radius: 4px;
    margin-top: 8px;
  }
}

.goods-stats {
  display: flex;
  gap: 32px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f2f5;

  .stat-item {
    display: flex;
    align-items: center;
    gap: 8px;

    .stat-value {
      color: #303133;
      font-weight: bold;

      &.low-stock {
        color: #f56c6c;
      }
    }
  }
}

.sku-section,
.specs-section,
.quantity-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;

  .label {
    font-size: 14px;
    color: #909399;
    width: 60px;
  }
}

.specs-section {
  .specs-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    :deep(.el-tag) {
      cursor: pointer;
    }
  }
}

.stock-tip {
  font-size: 13px;
  color: #909399;
}

.action-buttons {
  display: flex;
  gap: 16px;
  margin-top: 30px;

  :deep(.el-button) {
    flex: 1;
    height: 48px;
    font-size: 16px;
  }
}

.service-commitment {
  display: flex;
  gap: 24px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px dashed #f0f2f5;

  .commitment-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #606266;
  }
}

.goods-detail-content {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.detail-content {
  min-height: 300px;
}

.review-list {
  .review-summary {
    padding: 20px;
    background: #f5f7fa;
    border-radius: 8px;
    margin-bottom: 20px;

    .score-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .score {
        font-size: 36px;
        font-weight: bold;
        color: #f56c6c;
      }

      .count {
        font-size: 13px;
        color: #909399;
      }
    }
  }
}

.service-content {
  .service-section {
    padding: 20px;
    border-bottom: 1px solid #f0f2f5;

    &:last-child {
      border-bottom: none;
    }

    h4 {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      color: #303133;
      margin-bottom: 12px;
    }

    p {
      font-size: 14px;
      color: #606266;
      line-height: 1.8;
      padding-left: 28px;
    }

    ol {
      padding-left: 28px;
      font-size: 14px;
      color: #606266;
      line-height: 2;

      li {
        list-style: decimal;
      }
    }
  }
}

.recommend-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;

  .section-header {
    margin-bottom: 20px;

    h3 {
      font-size: 18px;
      color: #303133;
      padding-left: 12px;
      border-left: 4px solid #409eff;
    }
  }

  .recommend-list {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 16px;
  }

  .recommend-item {
    cursor: pointer;
    transition: transform 0.3s;

    &:hover {
      transform: translateY(-4px);
    }

    .recommend-image {
      width: 100%;
      aspect-ratio: 1;
      object-fit: cover;
      border-radius: 8px;
      margin-bottom: 10px;
    }

    .recommend-info {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .recommend-name {
        font-size: 13px;
        color: #606266;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .recommend-price {
        font-size: 16px;
        color: #f56c6c;
        font-weight: bold;
      }
    }
  }
}
</style>
