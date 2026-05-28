<template>
  <div class="cart-page">
    <div class="container">
      <h1 class="page-title">购物车</h1>

      <div class="cart-content" v-loading="loading">
        <!-- 购物车列表 -->
        <div class="cart-table" v-if="cartItems.length > 0">
          <el-table
            ref="tableRef"
            :data="cartItems"
            @selection-change="handleSelectionChange"
            style="width: 100%"
          >
            <el-table-column type="selection" width="55" />
            
            <el-table-column label="商品" min-width="400">
              <template #default="{ row }">
                <div class="goods-cell" @click="goToGoods(row.goodsId)">
                  <el-image :src="row.image || defaultImage" class="goods-image" fit="cover" />
                  <div class="goods-info">
                    <p class="goods-name">{{ row.goodsName }}</p>
                    <p class="goods-stock">库存: {{ row.stock }}</p>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="单价" width="150" align="center">
              <template #default="{ row }">
                <span class="price">¥{{ row.price.toFixed(2) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="数量" width="180" align="center">
              <template #default="{ row }">
                <el-input-number
                  :model-value="row.quantity"
                  :min="1"
                  :max="row.stock"
                  size="default"
                  @change="(val: number) => handleQuantityChange(row.id, val)"
                />
              </template>
            </el-table-column>

            <el-table-column label="小计" width="150" align="center">
              <template #default="{ row }">
                <span class="subtotal">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button type="danger" text @click="handleRemove(row.id)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table-column>
        </div>

        <!-- 空购物车 -->
        <el-empty v-else description="购物车是空的">
          <el-button type="primary" @click="goShopping">去逛逛</el-button>
        </el-empty>

        <!-- 购物车底部 -->
        <div class="cart-footer" v-if="cartItems.length > 0">
          <div class="footer-left">
            <el-checkbox 
              v-model="isSelectAll" 
              @change="handleSelectAll"
              :indeterminate="isIndeterminate"
            >
              全选
            </el-checkbox>
            <el-button text @click="handleClearSelected" :disabled="selectedIds.length === 0">
              删除选中
            </el-button>
          </div>
          <div class="footer-right">
            <div class="total-info">
              <span class="total-label">已选 {{ selectedCount }} 件商品</span>
              <span class="total-amount">
                合计: <em>¥{{ totalAmount.toFixed(2) }}</em>
              </span>
            </div>
            <el-button 
              type="primary" 
              size="large" 
              :disabled="selectedCount === 0" 
              @click="handleCheckout"
            >
              去结算
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type ElTable } from 'element-plus'
import { useCartStore, type CartItem } from '@/stores/cart'
import { storeToRefs } from 'pinia'

const router = useRouter()
const cartStore = useCartStore()
const { items: cartItems, totalAmount, selectedCount } = storeToRefs(cartStore)

const loading = ref(false)
const tableRef = ref<InstanceType<typeof ElTable>>()
const selectedIds = ref<number[]>([])
const defaultImage = 'https://cube.elemecdn.com/6/74/3a6f1b1d4b7c3a2d2e1e2e3e4e5f6g7.png'

// 全选状态
const isSelectAll = computed({
  get: () => cartItems.value.length > 0 && cartItems.value.every(item => item.selected),
  set: (val: boolean) => handleSelectAll(val)
})

// 半选状态
const isIndeterminate = computed(() => {
  const selectedCount = cartItems.value.filter(item => item.selected).length
  return selectedCount > 0 && selectedCount < cartItems.value.length
})

const goShopping = () => {
  router.push('/goods/list')
}

const goToGoods = (goodsId: number) => {
  router.push(`/goods/detail/${goodsId}`)
}

const handleSelectionChange = (selection: CartItem[]) => {
  selectedIds.value = selection.map(item => item.id)
}

const handleSelectAll = async (selected: boolean) => {
  cartStore.selectAll(selected)
}

const handleQuantityChange = async (id: number, quantity: number) => {
  await cartStore.updateQuantity(id, quantity)
}

const handleRemove = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cartStore.removeItem([id])
    ElMessage.success('删除成功')
  } catch {
    // 取消删除
  }
}

const handleClearSelected = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择商品')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 件商品吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cartStore.removeItem(selectedIds.value)
    ElMessage.success('删除成功')
    selectedIds.value = []
  } catch {
    // 取消删除
  }
}

const handleCheckout = () => {
  if (selectedCount.value === 0) {
    ElMessage.warning('请先选择商品')
    return
  }
  router.push('/order/confirm')
}

// 同步table选中状态
watch(() => cartItems.value, () => {
  nextTick(() => {
    cartItems.value.forEach(item => {
      const row = cartItems.value.find(i => i.id === item.id)
      if (row) {
        tableRef.value?.toggleRowSelection(row, item.selected)
      }
    })
  })
}, { deep: true })

onMounted(() => {
  cartStore.fetchCartList()
})
</script>

<style lang="scss" scoped>
.cart-page {
  background-color: #f5f7fa;
  min-height: 100vh;
  padding-bottom: 80px;
}

.container {
  width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  padding: 20px 0;
}

.cart-content {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.cart-table {
  :deep(.el-table) {
    .el-table__header {
      th {
        background-color: #f5f7fa;
        color: #606266;
        font-weight: 600;
      }
    }

    .el-table__row {
      &:hover {
        background-color: #f5f7fa;
      }
    }
  }

  .goods-cell {
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: pointer;

    .goods-image {
      width: 80px;
      height: 80px;
      border-radius: 4px;
      flex-shrink: 0;
    }

    .goods-info {
      flex: 1;
      overflow: hidden;

      .goods-name {
        font-size: 14px;
        color: #303133;
        overflow: hidden;
        text-overflow: ellipsis;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        margin-bottom: 4px;
      }

      .goods-stock {
        font-size: 12px;
        color: #909399;
      }
    }
  }

  .price {
    font-size: 14px;
    color: #303133;
  }

  .subtotal {
    font-size: 16px;
    font-weight: bold;
    color: #f56c6c;
  }
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  margin-top: 20px;
  border-top: 1px solid #f0f2f5;

  .footer-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .footer-right {
    display: flex;
    align-items: center;
    gap: 20px;
  }

  .total-info {
    text-align: right;

    .total-label {
      display: block;
      font-size: 13px;
      color: #909399;
    }

    .total-amount {
      font-size: 14px;
      color: #303133;

      em {
        font-size: 20px;
        font-weight: bold;
        color: #f56c6c;
        font-style: normal;
      }
    }
  }
}
</style>
