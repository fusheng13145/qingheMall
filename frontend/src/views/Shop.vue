<template>
  <div class="shop-page">
    <div v-if="error" class="shop-error">{{ error }}</div>

    <template v-else-if="shop">
      <!-- 店铺头部 -->
      <div class="shop-header">
        <div class="shop-logo">{{ (shop.shopName || '店').slice(0, 1) }}</div>
        <div class="shop-meta">
          <h2 class="shop-name">{{ shop.shopName }}</h2>
          <p class="shop-desc">{{ shop.shopDesc || '这家店铺还没有简介' }}</p>
        </div>
        <div class="shop-stats">
          <div class="stat">
            <span class="stat-num">{{ stats.productCount ?? 0 }}</span>
            <span class="stat-label">在售商品</span>
          </div>
          <div class="stat">
            <span class="stat-num">{{ stats.avgRating ?? 0 }}</span>
            <span class="stat-label">店铺评分</span>
          </div>
          <div class="stat">
            <span class="stat-num">{{ stats.totalSales ?? 0 }}</span>
            <span class="stat-label">累计销量</span>
          </div>
        </div>
      </div>

      <!-- 店铺券 / 秒杀入口 -->
      <div v-if="coupons.length || seckills.length" class="shop-promos">
        <div v-for="c in coupons" :key="'c' + c.id" class="promo-card">
          <span class="promo-tag">店铺券</span>
          <span>{{ c.name || '满减券' }}</span>
        </div>
        <div v-for="s in seckills" :key="'s' + s.id" class="promo-card seckill">
          <span class="promo-tag">限时秒杀</span>
          <span>秒杀进行中，¥{{ s.seckillPrice }}</span>
        </div>
      </div>

      <!-- 在售商品 -->
      <div class="shop-goods-header">
        <h3>全部商品</h3>
        <input v-model.trim="keyword" class="shop-search" type="text" placeholder="搜索本店商品" @keyup.enter="loadGoods(1)" />
      </div>
      <div v-if="goods.length" class="shop-grid">
        <div v-for="p in goods" :key="p.id" class="product-card" @click="goDetail(p.id)">
          <img class="product-img" :src="firstImg(p)" :alt="p.name" loading="lazy" />
          <div class="product-name">{{ p.name }}</div>
          <div class="product-price">¥{{ formatPrice(p.price) }}</div>
        </div>
      </div>
      <div v-else class="shop-empty">该店铺暂无在售商品</div>

      <!-- 分页 -->
      <div v-if="totalPage > 1" class="shop-pager">
        <button class="pager-btn" :disabled="pageNum <= 1" @click="loadGoods(pageNum - 1)">上一页</button>
        <span>{{ pageNum }} / {{ totalPage }}</span>
        <button class="pager-btn" :disabled="pageNum >= totalPage" @click="loadGoods(pageNum + 1)">下一页</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getShopHome } from '../api/shop'
import { formatPrice } from '../utils/format'

const route = useRoute()
const router = useRouter()

const shop = ref(null)
const stats = ref({})
const goods = ref([])
const coupons = ref([])
const seckills = ref([])
const keyword = ref('')
const pageNum = ref(1)
const error = ref('')

const totalPage = computed(() => Number(goods.value.totalPage || 0))

function firstImg(p) {
  return (p.productImgs || '').split(';')[0] || ''
}

function goDetail(id) {
  router.push(`/product/${id}`)
}

async function loadGoods(page = 1) {
  pageNum.value = page
  const res = await getShopHome(route.params.merchantId, page, 12, keyword.value)
  const data = res.data || {}
  shop.value = data.shop
  stats.value = data.stats || {}
  coupons.value = data.coupons || []
  seckills.value = data.seckills || []
  goods.value = data.products && Array.isArray(data.products.data) ? data.products.data : []
  goods.value.totalPage = data.products ? data.products.totalPage : 0
}

onMounted(async () => {
  try {
    await loadGoods(1)
  } catch (e) {
    error.value = '店铺不存在或未营业'
  }
})
</script>

<style scoped>
.shop-page { max-width: 1200px; margin: 0 auto; padding: 24px 16px 60px; }
.shop-error { text-align: center; color: #ef4444; padding: 80px 0; }
.shop-header { display: flex; align-items: center; gap: 20px; padding: 24px; border-radius: 14px; background: linear-gradient(135deg, #ecfdf5, #f0fdf4); }
.shop-logo { width: 64px; height: 64px; border-radius: 14px; background: #10b981; color: #fff; font-size: 28px; display: flex; align-items: center; justify-content: center; }
.shop-meta { flex: 1; }
.shop-name { margin: 0 0 4px; font-size: 22px; }
.shop-desc { margin: 0; color: #6b7280; font-size: 13px; }
.shop-stats { display: flex; gap: 28px; }
.stat { text-align: center; }
.stat-num { display: block; font-size: 20px; font-weight: 700; color: #10b981; }
.stat-label { font-size: 12px; color: #6b7280; }
.shop-promos { display: flex; gap: 12px; flex-wrap: wrap; margin: 16px 0; }
.promo-card { display: inline-flex; align-items: center; gap: 8px; padding: 8px 14px; border: 1px dashed #10b981; border-radius: 10px; font-size: 13px; color: #065f46; }
.promo-card.seckill { border-color: #f59e0b; color: #92400e; }
.promo-tag { font-weight: 700; }
.shop-goods-header { display: flex; justify-content: space-between; align-items: center; margin: 20px 0 12px; }
.shop-search { border: 1px solid #d1d5db; border-radius: 8px; padding: 7px 12px; font-size: 13px; width: 220px; }
.shop-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; }
.product-card { border: 1px solid #f3f4f6; border-radius: 12px; overflow: hidden; cursor: pointer; background: #fff; }
.product-card:hover { box-shadow: 0 6px 18px rgba(16, 185, 129, 0.15); }
.product-img { width: 100%; aspect-ratio: 1; object-fit: cover; }
.product-name { padding: 10px 12px 2px; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-price { padding: 0 12px 12px; color: #10b981; font-weight: 700; }
.shop-empty { text-align: center; color: #9ca3af; padding: 48px 0; }
.shop-pager { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 20px; }
.pager-btn { border: 1px solid #d1d5db; background: #fff; border-radius: 8px; padding: 6px 14px; cursor: pointer; }
.pager-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
