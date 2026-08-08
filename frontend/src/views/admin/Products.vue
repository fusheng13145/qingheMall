<template>
  <div class="products-page">
    <div class="page-header">
      <h1 class="page-title">商品管理</h1>
      <button class="btn-add" @click="openAddModal">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        新增商品
      </button>
    </div>

    <div class="table-section">
      <div class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>商品名称</th>
              <th>价格</th>
              <th>销量</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="product in products" :key="product.id">
              <td class="product-name">{{ product.name }}</td>
              <td class="price">&yen;{{ Number(product.price || 0).toFixed(2) }}</td>
              <td>{{ product.purchaseNum }}</td>
              <td class="actions">
                <button class="btn-action btn-edit" @click="openEditModal(product)">编辑</button>
                <button class="btn-action btn-delete" @click="handleDelete(product)">删除</button>
              </td>
            </tr>
            <tr v-if="products.length === 0">
              <td colspan="4" class="empty">暂无商品数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 模态框 -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ isEditing ? '编辑商品' : '新增商品' }}</h3>
          <button class="modal-close" @click="closeModal">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>商品名称</label>
            <input v-model="form.name" type="text" placeholder="请输入商品名称" />
          </div>
          <div class="form-group">
            <label>价格</label>
            <input v-model.number="form.price" type="number" step="0.01" placeholder="请输入价格" />
          </div>
          <div class="form-group">
            <label>商品简介</label>
            <textarea v-model="form.productIntro" rows="3" placeholder="请输入商品简介"></textarea>
          </div>
          <div class="form-group">
            <label>商品图片URL（多图用分号/空格分隔）</label>
            <div class="img-upload-row">
              <input v-model="form.productImgs" type="text" placeholder="https://... 或 /uploads/xxx.jpg" />
              <label class="btn-upload">
                上传图片
                <input type="file" accept="image/*" hidden @change="handleUpload" />
              </label>
            </div>
            <span v-if="uploading" class="upload-tip">上传中...</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeModal">取消</button>
          <button class="btn-confirm" @click="handleSubmit">{{ isEditing ? '保存' : '添加' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getProductList, addProduct, updateProduct, deleteProduct } from '../../api/admin'
import { uploadImage } from '../../api/file'

const products = ref([])
const showModal = ref(false)
const isEditing = ref(false)
const uploading = ref(false)
const form = ref({
  id: '',
  name: '',
  price: null,
  productIntro: '',
  productImgs: ''
})

onMounted(async () => {
  await loadProducts()
})

async function loadProducts() {
  try {
    const res = await getProductList()
    // res.data 为 Paging<Product>，列表在 data 字段
    products.value = res.data.data || []
  } catch (e) {
    alert('加载商品列表失败：' + (e.message || '请稍后重试'))
  }
}

function openAddModal() {
  isEditing.value = false
  form.value = { id: '', name: '', price: null, productIntro: '', productImgs: '' }
  showModal.value = true
}

function openEditModal(product) {
  isEditing.value = true
  form.value = { ...product }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

// 上传图片：成功后将返回 URL 追加到图片字段（多图用分号分隔）
async function handleUpload(event) {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  uploading.value = true
  try {
    const res = await uploadImage(file)
    const url = res.data.url
    form.value.productImgs = form.value.productImgs
      ? form.value.productImgs.trim() + ';' + url
      : url
  } catch (e) {
    alert('上传失败：' + (e.message || '请稍后重试'))
  } finally {
    uploading.value = false
  }
}

async function handleSubmit() {
  if (!form.value.name) {
    alert('请填写商品名称')
    return
  }
  if (form.value.price == null || form.value.price === '') {
    alert('请填写商品价格')
    return
  }
  try {
    if (isEditing.value) {
      await updateProduct(form.value)
    } else {
      await addProduct(form.value)
    }
    await loadProducts()
    closeModal()
  } catch (e) {
    alert('保存失败：' + (e.message || '请稍后重试'))
  }
}

async function handleDelete(product) {
  if (!confirm(`确定要删除商品「${product.name}」吗？`)) return
  try {
    await deleteProduct(product.id)
    await loadProducts()
  } catch (e) {
    alert('删除失败：' + (e.message || '请稍后重试'))
  }
}
</script>

<style scoped>
.products-page {
  max-width: 1200px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
}

.btn-add {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  transition: all var(--transition-fast);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);
}

.btn-add:hover {
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.35);
  transform: translateY(-1px);
}

.table-section {
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  overflow: hidden;
}

.table-wrapper {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  text-align: left;
  padding: 14px 24px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  background: var(--color-bg-sunken);
  border-bottom: 1px solid var(--color-border);
}

.data-table td {
  padding: 14px 24px;
  font-size: 14px;
  color: var(--color-text);
  border-bottom: 1px solid var(--color-divider);
}

.data-table tbody tr:nth-child(even) {
  background: var(--color-bg-sunken);
}

.data-table tbody tr:hover {
  background: var(--color-surface-hover);
}

.product-name {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price {
  color: var(--color-price);
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 8px;
}

.btn-action {
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.btn-edit {
  color: var(--color-primary);
  background: var(--color-primary-50);
}

.btn-edit:hover {
  background: var(--color-primary);
  color: #fff;
}

.btn-delete {
  color: var(--color-danger);
  background: var(--color-danger-light);
}

.btn-delete:hover {
  background: var(--color-danger);
  color: #fff;
}

.empty {
  text-align: center;
  color: var(--color-text-tertiary);
  padding: 40px 24px !important;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 300;
  padding: 20px;
}

.modal {
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  width: 100%;
  max-width: 520px;
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--color-divider);
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}

.modal-close:hover {
  background: var(--color-bg-overlay);
  color: var(--color-text);
}

.modal-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.form-group input,
.form-group textarea {
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 14px;
  transition: border-color var(--transition-fast);
  outline: none;
  font-family: var(--font-sans);
}

.form-group input:focus,
.form-group textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-50);
}

.form-group textarea {
  resize: vertical;
}

.img-upload-row {
  display: flex;
  gap: 8px;
}

.img-upload-row input {
  flex: 1;
}

.btn-upload {
  flex-shrink: 0;
  padding: 10px 16px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
  background: var(--color-primary-50);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-upload:hover {
  background: var(--color-primary);
  color: #fff;
}

.upload-tip {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--color-divider);
}

.btn-cancel {
  padding: 10px 20px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border);
  transition: all var(--transition-fast);
}

.btn-cancel:hover {
  background: var(--color-bg-overlay);
  color: var(--color-text);
}

.btn-confirm {
  padding: 10px 20px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  transition: all var(--transition-fast);
}

.btn-confirm:hover {
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.35);
  transform: translateY(-1px);
}
</style>
