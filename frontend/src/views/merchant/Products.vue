<template>
  <div class="products">
    <div class="page-header">
      <h2 class="page-title">商品管理</h2>
      <button class="btn-primary" @click="openCreate">+ 新增商品</button>
    </div>

    <div class="filter-bar">
      <input v-model="keyword" class="filter-input" placeholder="搜索商品名称" @keyup.enter="load(1)" />
      <select v-model="statusFilter" class="filter-select" @change="load(1)">
        <option value="">全部状态</option>
        <option value="ON">在售</option>
        <option value="OFF">下架</option>
      </select>
      <button class="btn-ghost" @click="load(1)">查询</button>
    </div>

    <div class="table-card">
      <table class="data-table">
        <thead>
          <tr>
            <th>商品</th>
            <th>价格</th>
            <th>库存</th>
            <th>销量</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in products" :key="p.id">
            <td>
              <div class="cell-product">
                <img v-if="firstImg(p.productImgs)" :src="firstImg(p.productImgs)" class="thumb" alt="" />
                <div class="thumb thumb-empty" v-else></div>
                <span class="p-name">{{ p.name }}</span>
              </div>
            </td>
            <td>¥{{ Number(p.price).toFixed(2) }}</td>
            <td>{{ stockOf(p.id) }}</td>
            <td>{{ p.purchaseNum || 0 }}</td>
            <td>
              <span class="tag" :class="p.status === 'ON' ? 'tag-on' : 'tag-off'">
                {{ p.status === 'ON' ? '在售' : '下架' }}
              </span>
            </td>
            <td>
              <div class="row-actions">
                <button class="link-btn" @click="openEdit(p)">编辑</button>
                <button class="link-btn" @click="toggle(p)">
                  {{ p.status === 'ON' ? '下架' : '上架' }}
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!loading && products.length === 0">
            <td colspan="6" class="empty-row">暂无商品，点击右上角「新增商品」上架第一款商品</td>
          </tr>
        </tbody>
      </table>
      <div class="pager">
        <button class="btn-ghost" :disabled="pageNum <= 1" @click="load(pageNum - 1)">上一页</button>
        <span class="page-info">第 {{ pageNum }} / {{ totalPage || 1 }} 页（共 {{ totalCount }} 件）</span>
        <button class="btn-ghost" :disabled="pageNum >= totalPage" @click="load(pageNum + 1)">下一页</button>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div v-if="modalOpen" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <h3 class="modal-title">{{ editingId ? '编辑商品' : '新增商品' }}</h3>
        <div class="form-grid">
          <label class="form-label">商品名称 *</label>
          <input v-model="form.name" class="form-input" placeholder="商品名称" />
          <label class="form-label">品牌</label>
          <input v-model="form.brand" class="form-input" placeholder="品牌（可选）" />
          <label class="form-label">参考价（元）*</label>
          <input v-model.number="form.price" type="number" step="0.01" min="0" class="form-input" placeholder="0.00" />
          <label class="form-label">商品介绍</label>
          <textarea v-model="form.productIntro" class="form-input" rows="2" placeholder="商品介绍（可选）"></textarea>
          <label class="form-label">商品图片（URL，多个用空格分隔）</label>
          <input v-model="form.productImgs" class="form-input" placeholder="http://... 第一张为缩略图" />
        </div>

        <div class="sku-header">
          <span class="form-label">规格（SKU）*：价格 / 尺码 / 库存</span>
          <button class="link-btn" @click="addSku">+ 添加规格</button>
        </div>
        <div v-for="(sku, idx) in form.details" :key="idx" class="sku-row">
          <input v-model.number="sku.price" type="number" step="0.01" min="0" class="form-input sku-price" placeholder="价格" />
          <input v-model.number="sku.size" type="number" step="0.5" class="form-input sku-size" placeholder="尺码" />
          <input v-model.number="sku.stock" type="number" min="0" class="form-input sku-stock" placeholder="库存" />
          <button class="link-btn danger" @click="form.details.splice(idx, 1)">删除</button>
        </div>

        <div class="modal-actions">
          <button class="btn-ghost" @click="closeModal">取消</button>
          <button class="btn-primary" :disabled="saving" @click="save">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listMerchantProducts, saveMerchantProduct, toggleMerchantProduct } from '../../api/merchant'
import { getProductDetails } from '../../api/product'

const products = ref([])
const detailsMap = reactive({})
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const totalPage = ref(1)
const totalCount = ref(0)
const keyword = ref('')
const statusFilter = ref('')

const modalOpen = ref(false)
const editingId = ref('')
const saving = ref(false)
const form = reactive({ name: '', brand: '', price: null, productIntro: '', productImgs: '', details: [] })

function firstImg(imgs) {
  if (!imgs) return ''
  const parts = String(imgs).split(/[;\s]+/).filter(Boolean)
  return parts[0] || ''
}

function stockOf(productId) {
  return detailsMap[productId] ?? '-'
}

async function load(page) {
  loading.value = true
  try {
    const res = await listMerchantProducts(page, pageSize.value, keyword.value.trim(), statusFilter.value)
    pageNum.value = page
    totalPage.value = res.data.totalPage
    totalCount.value = res.data.totalCount
    products.value = res.data.data
    detailsMap.__sync = Date.now()
    for (const p of products.value) {
      const skus = p.details || []
      let total = 0
      skus.forEach((s) => { total += Number(s.stock || 0) })
      detailsMap[p.id] = total
    }
  } catch (e) {
    alert(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = ''
  form.name = ''
  form.brand = ''
  form.price = null
  form.productIntro = ''
  form.productImgs = ''
  form.details = [{ price: null, size: null, stock: null }]
  modalOpen.value = true
}

async function openEdit(p) {
  editingId.value = p.id
  form.name = p.name
  form.brand = p.brand || ''
  form.price = p.price
  form.productIntro = p.productIntro || ''
  form.productImgs = p.productImgs || ''
  form.details = [{ price: null, size: null, stock: null }]
  try {
    const skus = (await getProductDetails(p.id)).data || []
    if (skus.length) {
      form.details = skus.map((s) => ({
        price: s.price,
        size: s.size,
        stock: s.stock
      }))
    }
  } catch (e) {
    // 规格拉取失败时保持空白行，由用户补充
  }
  modalOpen.value = true
}

function addSku() {
  form.details.push({ price: null, size: null, stock: null })
}

function closeModal() {
  modalOpen.value = false
}

async function save() {
  if (!form.name.trim()) {
    alert('请填写商品名称')
    return
  }
  if (!form.price || form.price <= 0) {
    alert('请填写正确的参考价')
    return
  }
  // P2-9：价格/库存须为有效数值（>0 / >=0），拦截空串与 0
  const details = form.details.filter(
    (s) => s.price != null && Number(s.price) > 0 && s.stock != null && Number(s.stock) >= 0
  )
  if (details.length === 0) {
    alert('请至少填写一条完整的规格（价格>0、库存>=0）')
    return
  }
  if (form.details.some((s) => s.price != null && s.stock != null
    && (Number(s.price) <= 0 || Number(s.stock) < 0))) {
    alert('规格价格必须大于 0，库存不能为负数')
    return
  }
  saving.value = true
  try {
    await saveMerchantProduct({
      id: editingId.value || null,
      name: form.name.trim(),
      brand: form.brand.trim() || null,
      price: form.price,
      productIntro: form.productIntro,
      productImgs: form.productImgs.trim() || null,
      status: 'ON',
      details
    })
    alert(editingId.value ? '保存成功' : '商品已上架')
    modalOpen.value = false
    load(pageNum.value)
  } catch (e) {
    alert(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggle(p) {
  const target = p.status === 'ON' ? 'OFF' : 'ON'
  if (!confirm(`确定要${target === 'ON' ? '上架' : '下架'}「${p.name}」吗？`)) return
  try {
    await toggleMerchantProduct(p.id, target)
    load(pageNum.value)
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-input {
  flex: 1;
  max-width: 280px;
  padding: 9px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg-elevated);
  color: var(--color-text);
  font-size: 14px;
}

.filter-select {
  padding: 9px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg-elevated);
  color: var(--color-text);
  font-size: 14px;
}

.table-card {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px 16px;
  text-align: left;
  font-size: 14px;
  border-bottom: 1px solid var(--color-border);
}

.data-table th {
  color: var(--color-text-tertiary);
  font-weight: 500;
  font-size: 13px;
  background: var(--color-bg);
}

.cell-product {
  display: flex;
  align-items: center;
  gap: 10px;
}

.thumb {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  background: var(--color-bg);
}

.thumb-empty {
  border: 1px dashed var(--color-border);
}

.p-name {
  font-weight: 500;
  color: var(--color-text);
}

.tag {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.tag-on {
  background: var(--color-success-light, #E1F5EE);
  color: var(--color-primary);
}

.tag-off {
  background: var(--color-bg-overlay);
  color: var(--color-text-tertiary);
}

.row-actions {
  display: flex;
  gap: 10px;
}

.link-btn {
  color: var(--color-primary);
  font-size: 13px;
  padding: 2px 0;
}

.link-btn.danger {
  color: var(--color-danger);
}

.link-btn:hover {
  opacity: 0.75;
}

.empty-row {
  text-align: center;
  color: var(--color-text-tertiary);
  padding: 40px !important;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 14px;
}

.page-info {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.btn-primary {
  padding: 9px 18px;
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: 14px;
  transition: opacity var(--transition-fast);
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.85;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-ghost {
  padding: 9px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  font-size: 14px;
  background: transparent;
}

.btn-ghost:hover:not(:disabled) {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.btn-ghost:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 300;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 60px 16px;
  overflow-y: auto;
}

.modal {
  width: 100%;
  max-width: 620px;
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-lg);
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: 110px 1fr;
  gap: 12px 16px;
  align-items: center;
  margin-bottom: 16px;
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.form-input {
  padding: 9px 12px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 14px;
  width: 100%;
}

.sku-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 10px;
}

.sku-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr 48px;
  gap: 10px;
  margin-bottom: 8px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 18px;
}
</style>
