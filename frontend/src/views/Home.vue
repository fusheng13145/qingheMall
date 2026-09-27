<template>
  <div class="home">
    <section class="hero">
      <div class="hero-bg">
        <div class="hero-gradient" :class="{ dark: isDark }"></div>
        <div class="hero-pattern"></div>
      </div>
      <div class="hero-content">
        <div class="hero-badge">品质甄选</div>
        <h1 class="hero-title">青禾商城</h1>
        <p class="hero-subtitle">品质生活，尽在青禾 — 精选好物，用心甄选</p>
        <router-link to="/products" class="hero-btn">
          <span>立即选购</span>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
        </router-link>
      </div>
    </section>

    <section class="features">
      <div class="feature-item">
        <div class="feature-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
        </div>
        <div class="feature-text">
          <h3>品质保障</h3>
          <p>严选好物，品质认证</p>
        </div>
      </div>
      <div class="feature-item">
        <div class="feature-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
        </div>
        <div class="feature-text">
          <h3>极速配送</h3>
          <p>闪电发货，快速送达</p>
        </div>
      </div>
      <div class="feature-item">
        <div class="feature-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>
        </div>
        <div class="feature-text">
          <h3>售后无忧</h3>
          <p>7天退换，安心购物</p>
        </div>
      </div>
      <div class="feature-item">
        <div class="feature-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>
        </div>
        <div class="feature-text">
          <h3>安全支付</h3>
          <p>加密交易，保障安全</p>
        </div>
      </div>
    </section>

    <section v-if="banners.length" class="banner-zone" aria-label="运营位">
      <div class="banner-stage" @mouseenter="pauseCarousel" @mouseleave="resumeCarousel">
        <component
          :is="bannerInternal(b) ? 'router-link' : 'a'"
          v-for="(b, i) in banners"
          v-show="i === bannerIndex"
          :key="b.id"
          class="banner-slide"
          :to="bannerInternal(b) ? b.linkUrl : undefined"
          :href="bannerInternal(b) ? undefined : b.linkUrl"
          :target="bannerInternal(b) ? undefined : '_blank'"
          rel="noopener"
        >
          <img :src="b.image" :alt="b.title" loading="lazy" />
          <span class="banner-title">{{ b.title }}</span>
        </component>
        <div v-if="banners.length > 1" class="banner-dots">
          <button
            v-for="(b, i) in banners"
            :key="'dot-' + b.id"
            class="banner-dot"
            :class="{ active: i === bannerIndex }"
            :aria-label="`切换到第 ${i + 1} 张运营位`"
            @click="bannerIndex = i"
          ></button>
        </div>
      </div>
    </section>

    <section class="featured">
      <div class="section-header">
        <h2 class="section-title">热销推荐</h2>
        <router-link to="/products" class="section-more">
          查看更多
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
        </router-link>
      </div>
      <div v-if="hotProducts.length > 0" class="product-grid">
        <ProductCard v-for="product in hotProducts" :key="product.id" :product="product" />
      </div>
      <div v-else-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>
      <div v-else class="empty-state">
        <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="8" y1="15" x2="16" y2="15"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/></svg>
        <p>暂无商品</p>
      </div>
    </section>

    <section class="featured">
      <div class="section-header">
        <h2 class="section-title">新品上架</h2>
        <router-link to="/products" class="section-more">
          查看更多
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
        </router-link>
      </div>
      <div v-if="newProducts.length > 0" class="product-grid">
        <ProductCard v-for="product in newProducts" :key="product.id" :product="product" />
      </div>
      <div v-else-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>
      <div v-else class="empty-state">
        <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="8" y1="15" x2="16" y2="15"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/></svg>
        <p>暂无新品</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { recommend } from '../api/product.js'
import { listBanners } from '../api/banner.js'
import ProductCard from '../components/ProductCard.vue'
import { useThemeStore } from '../stores/theme'

const themeStore = useThemeStore()
const isDark = computed(() => themeStore.getEffectiveTheme() === 'dark')

const hotProducts = ref([])
const newProducts = ref([])
const banners = ref([])
const loading = ref(true)

// 运营位轮播：5s 自动切换，悬停暂停
const bannerIndex = ref(0)
let carouselTimer = null
function startCarousel() {
  stopCarousel()
  if (banners.value.length > 1) {
    carouselTimer = setInterval(() => {
      bannerIndex.value = (bannerIndex.value + 1) % banners.value.length
    }, 5000)
  }
}
function stopCarousel() {
  if (carouselTimer) {
    clearInterval(carouselTimer)
    carouselTimer = null
  }
}
function pauseCarousel() { stopCarousel() }
function resumeCarousel() { startCarousel() }
function bannerInternal(b) {
  return !!(b.linkUrl && b.linkUrl.startsWith('/'))
}
onUnmounted(stopCarousel)

onMounted(async () => {
  try {
    // request.js 拦截器约定：业务成功直接 resolve Result 包络，res.data 即业务数据
    const [hotRes, newRes] = await Promise.all([
      recommend('hot', 8),
      recommend('new', 8)
    ])
    hotProducts.value = (hotRes && hotRes.data) || []
    newProducts.value = (newRes && newRes.data) || []
  } catch (error) {
    console.error('加载推荐商品失败:', error)
  } finally {
    loading.value = false
  }
  try {
    const bannerRes = await listBanners()
    banners.value = (bannerRes && bannerRes.data) || []
    startCarousel()
  } catch (error) {
    console.error('加载运营位失败:', error)
  }
})
</script>

<style scoped>
/* ========== Hero 区域 ========== */
.hero {
  position: relative;
  padding: 80px 20px 100px;
  text-align: center;
  overflow: hidden;
  margin: -24px -20px 0;
}

.hero-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.hero-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #059669 0%, #10b981 30%, #34d399 60%, #6ee7b7 100%);
}

.hero-gradient.dark {
  background: linear-gradient(135deg, #064e3b 0%, #065f46 30%, #047857 60%, #059669 100%);
}

.hero-pattern {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 20% 50%, rgba(255,255,255,0.1) 0%, transparent 50%),
                    radial-gradient(circle at 80% 20%, rgba(255,255,255,0.08) 0%, transparent 40%),
                    radial-gradient(circle at 40% 80%, rgba(255,255,255,0.06) 0%, transparent 45%);
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 600px;
  margin: 0 auto;
}

.hero-badge {
  display: inline-block;
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: var(--radius-full);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 1px;
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.25);
}

.hero-title {
  font-family: var(--font-display);
  font-size: 52px;
  font-weight: 800;
  color: #fff;
  margin-bottom: 16px;
  letter-spacing: 4px;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.1);
}

.hero-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 36px;
  line-height: 1.6;
}

.hero-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  color: #059669;
  padding: 14px 36px;
  border-radius: var(--radius-full);
  font-size: 16px;
  font-weight: 600;
  transition: all var(--transition-base);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.hero-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
}

/* ========== 特性区域 ========== */
.features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: -40px;
  position: relative;
  z-index: 2;
  margin-bottom: 48px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--color-surface);
  padding: 20px;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--color-border-light);
  transition: all var(--transition-base);
}

.feature-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.feature-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-sm);
  background: var(--color-primary-50);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-text h3 {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 2px;
}

.feature-text p {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

/* ========== 运营位轮播 ========== */
.banner-zone {
  margin-bottom: 48px;
}

.banner-stage {
  position: relative;
  border-radius: var(--radius-lg, 16px);
  overflow: hidden;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--color-border-light);
  aspect-ratio: 21 / 7;
  min-height: 160px;
  background: var(--color-surface);
}

.banner-slide {
  position: absolute;
  inset: 0;
  display: block;
  text-decoration: none;
}

.banner-slide img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.banner-title {
  position: absolute;
  left: 24px;
  bottom: 20px;
  padding: 8px 18px;
  border-radius: var(--radius-full, 999px);
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  max-width: calc(100% - 48px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.banner-dots {
  position: absolute;
  right: 20px;
  bottom: 16px;
  display: flex;
  gap: 8px;
  z-index: 1;
}

.banner-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: none;
  padding: 0;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.banner-dot.active {
  background: #fff;
  transform: scale(1.15);
}

/* ========== 热门商品 ========== */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-title {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
}

.section-more {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: var(--color-text-tertiary);
  font-weight: 500;
  transition: color var(--transition-fast);
}

.section-more:hover {
  color: var(--color-primary);
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

/* ========== 响应式 ========== */
@media (max-width: 1024px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .features {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 36px;
  }

  .hero-subtitle {
    font-size: 15px;
  }

  .hero {
    padding: 60px 20px 80px;
  }

  .banner-zone {
    margin-bottom: 32px;
  }

  .banner-title {
    left: 12px;
    bottom: 12px;
    font-size: 13px;
  }

  .banner-stage {
    aspect-ratio: 16 / 9;
    min-height: 120px;
  }

  .product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .features {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .product-grid {
    grid-template-columns: 1fr;
  }

  .features {
    grid-template-columns: 1fr;
  }

  .hero-title {
    font-size: 28px;
  }
}
</style>
