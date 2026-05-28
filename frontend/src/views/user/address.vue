<template>
  <div class="user-address-page">
    <div class="page-header">
      <h2 class="page-title">收货地址</h2>
      <el-button type="primary" @click="handleAdd">新增地址</el-button>
    </div>

    <div class="address-list" v-loading="loading">
      <div
        class="address-item"
        v-for="address in addressList"
        :key="address.id"
      >
        <div class="address-info">
          <div class="address-main">
            <span class="name">{{ address.name }}</span>
            <span class="phone">{{ formatPhone(address.phone) }}</span>
            <el-tag v-if="address.isDefault" size="small" type="success">默认</el-tag>
          </div>
          <p class="address-detail">{{ formatAddress(address) }}</p>
        </div>
        <div class="address-actions">
          <el-button text type="primary" @click="handleEdit(address)">编辑</el-button>
          <el-button text type="danger" @click="handleDelete(address.id)" v-if="!address.isDefault">
            删除
          </el-button>
          <el-button text @click="handleSetDefault(address.id)" v-if="!address.isDefault">
            设为默认
          </el-button>
        </div>
      </div>

      <el-empty v-if="addressList.length === 0 && !loading" description="暂无收货地址">
        <el-button type="primary" @click="handleAdd">添加地址</el-button>
      </el-empty>
    </div>

    <!-- 地址编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑地址' : '新增地址'"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="收货人" prop="name">
          <el-input v-model="form.name" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="form.province" placeholder="请输入省份" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="form.city" placeholder="请输入城市" />
        </el-form-item>
        <el-form-item label="区县" prop="district">
          <el-input v-model="form.district" placeholder="请输入区县" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input
            v-model="form.detail"
            type="textarea"
            :rows="2"
            placeholder="请输入详细地址"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.isDefault">设为默认地址</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatPhone, formatAddress } from '@/utils/format'
import type { Address } from '@/api/modules/order'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const addressList = ref<Address[]>([])

const form = reactive({
  id: 0,
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false
})

const rules = {
  name: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (address: Address) => {
  isEdit.value = true
  Object.assign(form, address)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    // 模拟提交
    await new Promise(resolve => setTimeout(resolve, 1000))

    if (isEdit.value) {
      const index = addressList.value.findIndex(a => a.id === form.id)
      if (index !== -1) {
        addressList.value[index] = { ...form }
      }
      ElMessage.success('修改成功')
    } else {
      const newAddress = {
        ...form,
        id: Date.now()
      }
      addressList.value.unshift(newAddress)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
  } catch {
    // 验证失败
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该地址吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    addressList.value = addressList.value.filter(a => a.id !== id)
    ElMessage.success('删除成功')
  } catch {
    // 取消
  }
}

const handleSetDefault = async (id: number) => {
  addressList.value.forEach(addr => {
    addr.isDefault = addr.id === id
  })
  ElMessage.success('设置成功')
}

const resetForm = () => {
  Object.assign(form, {
    id: 0,
    name: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    isDefault: false
  })
  formRef.value?.resetFields()
}

const fetchAddressList = async () => {
  loading.value = true
  try {
    // 模拟数据
    addressList.value = [
      {
        id: 1,
        name: '张三',
        phone: '13800138000',
        province: '广东省',
        city: '深圳市',
        district: '南山区',
        detail: '科技园路1号',
        isDefault: true
      }
    ]
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchAddressList()
})
</script>

<style lang="scss" scoped>
.user-address-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .page-title {
      font-size: 18px;
      font-weight: bold;
      color: #303133;
    }
  }

  .address-list {
    min-height: 200px;
  }

  .address-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    margin-bottom: 16px;
    transition: all 0.3s;

    &:hover {
      border-color: #409eff;
    }

    .address-info {
      .address-main {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;

        .name {
          font-size: 15px;
          font-weight: bold;
          color: #303133;
        }

        .phone {
          font-size: 14px;
          color: #606266;
        }
      }

      .address-detail {
        font-size: 13px;
        color: #909399;
      }
    }

    .address-actions {
      display: flex;
      gap: 8px;
    }
  }
}
</style>
