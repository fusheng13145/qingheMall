<template>
  <div class="goods-card" @click="handleClick">
    <div class="goods-image">
      <img :src="goods.thumbnail || goods.images?.[0] || defaultImage" :alt="goods.name" />
      <span class="goods-tag" v-if="goods.tags?.[0]">{{ goods.tags[0] }}</span>
    </div>
    <div class="goods-info">
      <h3 class="goods-name">{{ goods.name }}</h3>
      <div class="goods-desc" v-if="goods.description">{{ goods.description }}</div>
      <div class="goods-bottom">
        <div class="goods-price">
          <span class="current-price">¥{{ goods.price.toFixed(2) }}</span>
          <span class="original-price" v-if="goods.originalPrice">
            ¥{{ goods.originalPrice.toFixed(2) }}
          </span>
        </div>
        <div class="goods-meta">
          <span class="sales" v-if="goods.sales">销量 {{ goods.sales }}</span>
        </div>
      </div>
      <div class="goods-actions">
        <el-button type="primary" size="small" @click.stop="addToCart">加入购物车</el-button>
        <el-button size="small" @click.stop="buyNow">立即购买</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { ElMessage } from 'element-plus'
import type { Goods } from '@/api/modules/goods'

const props = defineProps<{
  goods: Goods
}>()

const router = useRouter()
const cartStore = useCartStore()

const defaultImage = 'https://cube.elemecdn.com/6/74/3a6f1b1d4b7c3a2d2e1e2e3e4e5f6g7.png'

const handleClick = () => {
  router.push(`/goods/detail/${props.goods.id}`)
}

const addToCart = async () => {
  try {
    await cartStore.addItem(props.goods.id, 1)
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error('加入购物车失败')
  }
}

const buyNow = () => {
  router.push(`/goods/detail/${props.goods.id}`)
}
</script>

<style lang="scss" scoped>
.goods-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid transparent;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
    transform: translateY(-2px);
  }
}

.goods-image {
  position: relative;
  width: 100%;
  padding-top: 100%;
  background: #f5f7fa;
  overflow: hidden;

  img {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .goods-tag {
    position: absolute;
    top: 10px;
    left: 10px;
    padding: 2px 8px;
    background: #f56c6c;
    color: #fff;
    font-size: 12px;
    border-radius: 2px;
  }
}

.goods-info {
  padding: 12px;
}

.goods-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-desc {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.goods-price {
  display: flex;
  align-items: baseline;
  gap: 6px;

  .current-price {
    font-size: 18px;
    font-weight: bold;
    color: #f56c6c;
  }

  .original-price {
    font-size: 12px;
    color: #c0c4cc;
    text-decoration: line-through;
  }
}

.goods-meta {
  font-size: 12px;
  color: #909399;

  .sales {
    display: block;
  }
}

.goods-actions {
  display: flex;
  gap: 8px;

  :deep(.el-button) {
    flex: 1;
    padding: 6px 12px;
  }
}
</style>
