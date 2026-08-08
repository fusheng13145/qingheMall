<template>
  <div class="product-card" @click="goToDetail">
    <div class="card-image">
      <img :src="imageUrl" :alt="product.name" loading="lazy" />
      <div v-if="product.purchaseNum > 50" class="card-badge">热销</div>
    </div>
    <div class="card-info">
      <h3 class="card-name">{{ product.name }}</h3>
      <div class="card-bottom">
        <span class="card-price">
          <span class="price-symbol">¥</span>{{ product.price }}
        </span>
        <span class="card-sales">{{ product.purchaseNum || 0 }}人购买</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
})

const router = useRouter()

const imageUrl = computed(() => {
  const imgs = splitImgs(props.product.productImgs)
  return imgs[0] || ''
})

// 兼容后端以空格分隔、历史数据以分号分隔的图片串
function splitImgs(str) {
  if (!str) return []
  return str
    .split(/[;\s]+/)
    .map(s => s.trim())
    .filter(Boolean)
}

function goToDetail() {
  router.push(`/product/${props.product.id}`)
}
</script>

<style scoped>
.product-card {
  background: var(--color-surface);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
  box-shadow: var(--shadow-xs);
  border: 1px solid var(--color-border-light);
}

.product-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg);
  border-color: transparent;
}

.card-image {
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  position: relative;
  background: var(--color-bg-sunken);
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.product-card:hover .card-image img {
  transform: scale(1.08);
}

.card-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 10px;
  background: linear-gradient(135deg, #ef4444, #f97316);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: var(--radius-full);
  letter-spacing: 0.5px;
}

.card-info {
  padding: 14px 16px 18px;
}

.card-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  line-height: 1.5;
  height: 42px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 10px;
  transition: color var(--transition-fast);
}

.product-card:hover .card-name {
  color: var(--color-primary);
}

.card-bottom {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.card-price {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-price);
  line-height: 1;
}

.price-symbol {
  font-size: 13px;
  font-weight: 600;
  margin-right: 1px;
}

.card-sales {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
</style>
