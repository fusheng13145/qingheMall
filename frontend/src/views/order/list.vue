<template>
  <div class="order-list-page">
    <div class="container">
      <h1 class="page-title">我的订单</h1>

      <div class="order-tabs">
        <el-tabs v-model="activeStatus" @tab-change="handleStatusChange">
          <el-tab-pane label="全部" :name="undefined" />
          <el-tab-pane label="待付款" :name="1" />
          <el-tab-pane label="已付款" :name="2" />
          <el-tab-pane label="已发货" :name="3" />
          <el-tab-pane label="已完成" :name="4" />
          <el-tab-pane label="已取消" :name="-1" />
        </el-tabs>
      </div>

      <div class="order-list" v-loading="loading">
        <OrderItem
          v-for="order in orderList"
          :key="order.id"
          :order="order"
          @pay="handlePay"
          @cancel="handleCancel"
          @confirmReceive="handleConfirmReceive"
          @delete="handleDelete"
        />

        <el-empty v-if="orderList.length === 0 && !loading" description="暂无订单">
          <el-button type="primary" @click="goShopping">去逛逛</el-button>
        </el-empty>
      </div>

      <div class="pagination-wrapper" v-if="total > 0">
        <Pagination
          :total="total"
          :page="page"
          :limit="pageSize"
          @pagination="handlePagination"
        />
      </div>
    </div>

    <!-- 取消订单对话框 -->
    <el-dialog v-model="cancelDialogVisible" title="取消订单" width="400px">
      <div class="cancel-reason">
        <p class="reason-title">请选择取消原因：</p>
        <el-radio-group v-model="cancelReason">
          <el-radio label="不想要了">不想要了</el-radio>
          <el-radio label="信息填写错误">信息填写错误</el-radio>
          <el-radio label="商品价格偏高">商品价格偏高</el-radio>
          <el-radio label="重复下单">重复下单</el-radio>
          <el-radio label="其他原因">其他原因</el-radio>
        </el-radio-group>
        <el-input
          v-if="cancelReason === '其他原因'"
          v-model="cancelReasonDetail"
          type="textarea"
          :rows="2"
          placeholder="请输入其他原因"
          style="margin-top: 12px;"
        />
      </div>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">返回</el-button>
        <el-button type="danger" @click="confirmCancel">确认取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import OrderItem from '@/components/business/OrderItem.vue'
import Pagination from '@/components/common/Pagination.vue'
import { useOrderStore } from '@/stores/order'
import type { OrderItem as OrderItemType } from '@/api/modules/order'

const router = useRouter()
const orderStore = useOrderStore()

const loading = ref(false)
const orderList = ref<OrderItemType[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const activeStatus = ref<number | undefined>(undefined)

// 取消订单
const cancelDialogVisible = ref(false)
const cancelReason = ref('不想要了')
const cancelReasonDetail = ref('')
const currentCancelOrder = ref<OrderItemType | null>(null)

const goShopping = () => {
  router.push('/goods/list')
}

const handleStatusChange = (status: number | undefined) => {
  activeStatus.value = status
  page.value = 1
  fetchOrderList()
}

const handlePagination = ({ page: p, limit }: { page: number; limit: number }) => {
  page.value = p
  pageSize.value = limit
  fetchOrderList()
}

const fetchOrderList = async () => {
  loading.value = true
  try {
    // 模拟数据
    total.value = 8
    const mockOrders: OrderItemType[] = [
      {
        id: 1,
        orderNo: 'ORDER20240001',
        status: 1,
        statusText: '待付款',
        totalAmount: 299,
        payAmount: 299,
        freight: 0,
        createdAt: new Date(Date.now() - 0 * 86400000).toISOString(),
        items: [{
          id: 1,
          goodsId: 1,
          goodsName: '示例商品1',
          image: 'https://picsum.photos/80/80?random=1',
          price: 299,
          quantity: 1
        }],
        address: {
          id: 1,
          name: '张三',
          phone: '13800138000',
          province: '广东省',
          city: '深圳市',
          district: '南山区',
          detail: '科技园路1号',
          isDefault: true
        }
      },
      {
        id: 2,
        orderNo: 'ORDER20240002',
        status: 2,
        statusText: '已付款',
        totalAmount: 598,
        payAmount: 598,
        freight: 0,
        createdAt: new Date(Date.now() - 1 * 86400000).toISOString(),
        items: [{
          id: 2,
          goodsId: 2,
          goodsName: '示例商品2',
          image: 'https://picsum.photos/80/80?random=2',
          price: 299,
          quantity: 2
        }],
        address: {
          id: 1,
          name: '张三',
          phone: '13800138000',
          province: '广东省',
          city: '深圳市',
          district: '南山区',
          detail: '科技园路1号',
          isDefault: true
        }
      },
      {
        id: 3,
        orderNo: 'ORDER20240003',
        status: 3,
        statusText: '已发货',
        totalAmount: 399,
        payAmount: 409,
        freight: 10,
        createdAt: new Date(Date.now() - 3 * 86400000).toISOString(),
        shipAt: new Date(Date.now() - 1 * 86400000).toISOString(),
        items: [{
          id: 3,
          goodsId: 3,
          goodsName: '示例商品3',
          image: 'https://picsum.photos/80/80?random=3',
          price: 399,
          quantity: 1
        }],
        address: {
          id: 1,
          name: '张三',
          phone: '13800138000',
          province: '广东省',
          city: '深圳市',
          district: '南山区',
          detail: '科技园路1号',
          isDefault: true
        }
      },
      {
        id: 4,
        orderNo: 'ORDER20240004',
        status: 4,
        statusText: '已完成',
        totalAmount: 199,
        payAmount: 199,
        freight: 0,
        createdAt: new Date(Date.now() - 7 * 86400000).toISOString(),
        payAt: new Date(Date.now() - 6 * 86400000).toISOString(),
        shipAt: new Date(Date.now() - 5 * 86400000).toISOString(),
        receiveAt: new Date(Date.now() - 2 * 86400000).toISOString(),
        items: [{
          id: 4,
          goodsId: 4,
          goodsName: '示例商品4',
          image: 'https://picsum.photos/80/80?random=4',
          price: 199,
          quantity: 1
        }],
        address: {
          id: 1,
          name: '张三',
          phone: '13800138000',
          province: '广东省',
          city: '深圳市',
          district: '南山区',
          detail: '科技园路1号',
          isDefault: true
        }
      },
      {
        id: 5,
        orderNo: 'ORDER20240005',
        status: -1,
        statusText: '已取消',
        totalAmount: 599,
        payAmount: 599,
        freight: 0,
        createdAt: new Date(Date.now() - 10 * 86400000).toISOString(),
        items: [{
          id: 5,
          goodsId: 5,
          goodsName: '示例商品5',
          image: 'https://picsum.photos/80/80?random=5',
          price: 599,
          quantity: 1
        }],
        address: {
          id: 1,
          name: '张三',
          phone: '13800138000',
          province: '广东省',
          city: '深圳市',
          district: '南山区',
          detail: '科技园路1号',
          isDefault: true
        }
      }
    ]

    // 根据状态过滤
    let filteredOrders = mockOrders
    if (activeStatus.value !== undefined) {
      filteredOrders = mockOrders.filter(o => o.status === activeStatus.value)
    }
    
    orderList.value = filteredOrders
    total.value = filteredOrders.length
  } finally {
    loading.value = false
  }
}

const handlePay = (order: OrderItemType) => {
  router.push({
    path: '/payment',
    query: { orderNo: order.orderNo }
  })
}

const handleCancel = (order: OrderItemType) => {
  currentCancelOrder.value = order
  cancelReason.value = '不想要了'
  cancelReasonDetail.value = ''
  cancelDialogVisible.value = true
}

const confirmCancel = async () => {
  if (!currentCancelOrder.value) return
  
  const reason = cancelReason.value === '其他原因' ? cancelReasonDetail.value : cancelReason.value
  if (!reason) {
    ElMessage.warning('请选择或输入取消原因')
    return
  }

  try {
    // 模拟取消订单
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    const order = orderList.value.find(o => o.id === currentCancelOrder.value!.id)
    if (order) {
      order.status = -1
      order.statusText = '已取消'
    }
    
    ElMessage.success('订单已取消')
    cancelDialogVisible.value = false
  } catch {
    ElMessage.error('取消失败')
  }
}

const handleConfirmReceive = async (order: OrderItemType) => {
  try {
    await ElMessageBox.confirm('确认收到货物吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 模拟确认收货
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    order.status = 4
    order.statusText = '已完成'
    order.receiveAt = new Date().toISOString()
    
    ElMessage.success('确认收货成功')
  } catch {
    // 取消
  }
}

const handleDelete = async (order: OrderItemType) => {
  try {
    await ElMessageBox.confirm('确定要删除该订单吗？删除后不可恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 模拟删除订单
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    orderList.value = orderList.value.filter(o => o.id !== order.id)
    ElMessage.success('订单已删除')
  } catch {
    // 取消
  }
}

onMounted(() => {
  fetchOrderList()
})
</script>

<style lang="scss" scoped>
.order-list-page {
  background-color: #f5f7fa;
  min-height: 100vh;
  padding-bottom: 40px;
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

.order-tabs {
  background: #fff;
  border-radius: 8px;
  padding: 0 20px;
  margin-bottom: 20px;
}

.order-list {
  min-height: 300px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.cancel-reason {
  .reason-title {
    font-size: 14px;
    color: #606266;
    margin-bottom: 16px;
  }

  .el-radio-group {
    display: flex;
    flex-direction: column;
    gap: 12px;

    :deep(.el-radio) {
      margin-right: 0;
    }
  }
}
</style>
