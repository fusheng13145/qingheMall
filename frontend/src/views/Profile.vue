<template>
  <div class="profile-page">
    <div class="page-header">
      <h1 class="page-title">个人中心</h1>
      <p class="page-desc">管理您的个人资料与收货地址</p>
    </div>

    <div class="profile-grid">
      <!-- 个人信息 -->
      <section class="panel">
        <h2 class="panel-title">个人资料</h2>
        <div class="profile-info">
          <div class="avatar-box">
            <img v-if="profile.avatar" :src="profile.avatar" alt="头像" class="avatar-img" />
            <div v-else class="avatar-placeholder">{{ (profile.nickName || 'U').charAt(0) }}</div>
          </div>
          <div class="profile-fields">
            <div class="field-row">
              <span class="field-label">用户名</span>
              <span class="field-value">{{ profile.userName }}</span>
            </div>
            <div class="field-row">
              <span class="field-label">角色</span>
              <span class="field-value">{{ roleText(profile.role) }}</span>
            </div>
            <div class="field-row">
              <span class="field-label">昵称</span>
              <div class="field-edit">
                <input v-model="nickName" class="field-input" maxlength="20" placeholder="请输入昵称" />
                <button class="btn-small btn-primary" :disabled="saving" @click="saveProfile">
                  {{ saving ? '保存中...' : '保存昵称' }}
                </button>
              </div>
            </div>
            <div class="field-row">
              <span class="field-label">头像URL</span>
              <div class="field-edit">
                <input v-model="avatar" class="field-input" placeholder="请输入头像图片 URL（可留空）" />
                <button class="btn-small btn-plain" @click="clearAvatar">清空</button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 收货地址 -->
      <section class="panel">
        <div class="panel-head">
          <h2 class="panel-title no-border">收货地址</h2>
          <button class="btn-add" @click="openEdit(null)">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            新增地址
          </button>
        </div>

        <div v-if="addresses.length > 0" class="address-list">
          <div v-for="addr in addresses" :key="addr.id" class="address-card">
            <div class="address-main">
              <div class="address-line1">
                <span class="addr-name">{{ addr.receiverName }}</span>
                <span class="addr-phone">{{ addr.receiverPhone }}</span>
                <span v-if="addr.isDefault" class="addr-default">默认</span>
              </div>
              <div class="addr-detail">{{ addr.receiverAddress }}</div>
            </div>
            <div class="address-actions">
              <button v-if="!addr.isDefault" class="btn-text" @click="handleSetDefault(addr)">设为默认</button>
              <button class="btn-text" @click="openEdit(addr)">编辑</button>
              <button class="btn-text danger" @click="handleDelete(addr)">删除</button>
            </div>
          </div>
        </div>
        <div v-else class="address-empty">
          还没有收货地址，点击右上角「新增地址」添加
        </div>
      </section>

      <!-- 商家入驻 -->
      <section class="panel">
        <h2 class="panel-title">商家入驻</h2>
        <div class="merchant-box">
          <template v-if="merchant && merchant.status === 'ACTIVE'">
            <p class="merchant-tip">您已是入驻商家，可进入商家工作台管理商品与订单。</p>
            <router-link to="/merchant" class="btn-add">进入商家工作台</router-link>
          </template>
          <template v-else-if="merchant && merchant.status === 'PENDING'">
            <p class="merchant-tip">您的店铺「{{ merchant.shopName }}」入驻申请已提交，平台审核中。</p>
            <span class="merchant-badge">审核中</span>
          </template>
          <template v-else-if="merchant && merchant.status === 'REJECTED'">
            <p class="merchant-tip">入驻申请未通过：{{ merchant.rejectReason || '请联系平台' }}</p>
            <button class="btn-add" @click="openApply">重新申请</button>
          </template>
          <template v-else>
            <p class="merchant-tip">开通您的专属店铺，向全平台用户售卖商品。</p>
            <button class="btn-add" @click="openApply">我要开店</button>
          </template>
        </div>
      </section>
    </div>

    <!-- 地址编辑弹窗 -->
    <div v-if="showEdit" class="modal-overlay" @click.self="showEdit = false">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ editingId ? '编辑地址' : '新增地址' }}</h3>
          <button class="modal-close" @click="showEdit = false">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>收货人 <span class="req">*</span></label>
            <input v-model.trim="editForm.receiverName" type="text" placeholder="请输入收货人姓名" maxlength="20" />
          </div>
          <div class="form-group">
            <label>联系电话 <span class="req">*</span></label>
            <input v-model.trim="editForm.receiverPhone" type="text" placeholder="请输入手机号" maxlength="11" />
          </div>
          <div class="form-group">
            <label>收货地址 <span class="req">*</span></label>
            <textarea v-model.trim="editForm.receiverAddress" rows="2" placeholder="省市区 + 详细地址" maxlength="120"></textarea>
          </div>
          <label class="default-check">
            <input v-model="editForm.isDefault" type="checkbox" />
            <span>设为默认地址</span>
          </label>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showEdit = false">取消</button>
          <button class="btn-confirm" :disabled="saving" @click="saveAddress">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 开店申请弹窗 -->
    <div v-if="showApply" class="modal-overlay" @click.self="showApply = false">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ merchant && merchant.status === 'REJECTED' ? '重新申请开店' : '我要开店' }}</h3>
          <button class="modal-close" @click="showApply = false">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>店铺名称 <span class="req">*</span></label>
            <input v-model.trim="applyForm.shopName" type="text" placeholder="请输入店铺名称" maxlength="40" />
          </div>
          <div class="form-group">
            <label>店铺简介</label>
            <textarea v-model.trim="applyForm.shopDesc" rows="3" placeholder="简要描述您的店铺（选填）" maxlength="200"></textarea>
          </div>
          <div class="form-group">
            <label>店铺Logo URL</label>
            <input v-model.trim="applyForm.shopLogo" type="text" placeholder="店铺 Logo 图片 URL（选填）" maxlength="255" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showApply = false">取消</button>
          <button class="btn-confirm" :disabled="applying || !applyForm.shopName" @click="submitApply">
            {{ applying ? '提交中...' : '提交申请' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { toast, apiError } from '../utils/toast'
import { useUserStore } from '../stores/user'
import { updateProfileApi } from '../api/user'
import { getMerchantInfo, applyMerchant } from '../api/merchant'
import { listAddress, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '../api/address'

const userStore = useUserStore()

const profile = ref({})
const nickName = ref('')
const avatar = ref('')
const saving = ref(false)

const addresses = ref([])
const showEdit = ref(false)
const editingId = ref(null)
const editForm = reactive({ receiverName: '', receiverPhone: '', receiverAddress: '', isDefault: false })

const merchant = ref(null)
const showApply = ref(false)
const applying = ref(false)
const applyForm = reactive({ shopName: '', shopDesc: '', shopLogo: '' })

// 角色文案（P3-8：商家角色展示）
function roleText(role) {
  if (role === 'ADMIN') return '管理员'
  if (role === 'MERCHANT') return '入驻商家'
  return '普通用户'
}

function loadProfile() {
  profile.value = {
    userName: userStore.userName,
    nickName: userStore.nickName,
    role: userStore.role,
    avatar: ''
  }
  nickName.value = userStore.nickName
}

async function saveProfile() {
  saving.value = true
  try {
    const res = await updateProfileApi(nickName.value || null, avatar.value || null)
    if (res.data) {
      profile.value = { ...profile.value, nickName: res.data.nickName, avatar: res.data.avatar || '' }
      userStore.nickName = res.data.nickName || userStore.userName
    }
    toast.success('资料已更新')
  } catch (e) {
    apiError(e, '保存失败')
  } finally {
    saving.value = false
  }
}

function clearAvatar() {
  avatar.value = ''
}

// 加载我的商家入驻状态；审核已通过但会话角色仍为 USER 时，同步为 MERCHANT 使商家入口立即可见
function loadMerchant() {
  return getMerchantInfo()
    .then((res) => {
      merchant.value = res.data || null
      if (merchant.value && merchant.value.status === 'ACTIVE' && userStore.role !== 'MERCHANT') {
        userStore.role = 'MERCHANT'
      }
    })
    .catch(() => {
      merchant.value = null
    })
}

function openApply() {
  applyForm.shopName = merchant.value ? (merchant.value.shopName || '') : ''
  applyForm.shopDesc = merchant.value ? (merchant.value.shopDesc || '') : ''
  applyForm.shopLogo = merchant.value ? (merchant.value.shopLogo || '') : ''
  showApply.value = true
}

async function submitApply() {
  if (!applyForm.shopName) return toast.warning('请填写店铺名称')
  applying.value = true
  try {
    await applyMerchant(applyForm.shopName, applyForm.shopLogo, applyForm.shopDesc)
    showApply.value = false
    await loadMerchant()
    toast.success('开店申请已提交，平台审核通过后即可经营')
  } catch (e) {
    apiError(e, '提交失败')
  } finally {
    applying.value = false
  }
}

async function loadAddresses() {
  try {
    const res = await listAddress()
    addresses.value = res.data || []
  } catch (e) {
    apiError(e, '加载地址失败')
  }
}

function openEdit(addr) {
  editingId.value = addr ? addr.id : null
  editForm.receiverName = addr ? addr.receiverName : ''
  editForm.receiverPhone = addr ? addr.receiverPhone : ''
  editForm.receiverAddress = addr ? addr.receiverAddress : ''
  editForm.isDefault = addr ? !!addr.isDefault : false
  showEdit.value = true
}

async function saveAddress() {
  if (!editForm.receiverName) return toast.warning('请填写收货人')
  if (!/^1\d{10}$/.test(editForm.receiverPhone)) return toast.warning('请输入正确的手机号')
  if (!editForm.receiverAddress) return toast.warning('请填写收货地址')
  saving.value = true
  try {
    if (editingId.value) {
      await updateAddress({ id: editingId.value, ...editForm })
    } else {
      await addAddress({ ...editForm })
    }
    showEdit.value = false
    await loadAddresses()
  } catch (e) {
    apiError(e, '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSetDefault(addr) {
  try {
    await setDefaultAddress(addr.id)
    await loadAddresses()
  } catch (e) {
    apiError(e, '操作失败')
  }
}

async function handleDelete(addr) {
  if (!confirm(`确定要删除该收货地址吗？`)) return
  try {
    await deleteAddress(addr.id)
    await loadAddresses()
  } catch (e) {
    apiError(e, '删除失败')
  }
}

onMounted(() => {
  loadProfile()
  loadMerchant()
  loadAddresses()
})
</script>

<style scoped>
.profile-page {
  padding: 20px 0;
  max-width: 960px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}

.page-desc {
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.profile-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs);
  padding: 24px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-divider);
}

.panel-title.no-border {
  border-bottom: none;
  padding-bottom: 0;
  margin-bottom: 0;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.profile-info {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.avatar-box {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--color-primary-50);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  font-size: 32px;
  font-weight: 700;
}

.profile-fields {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.field-label {
  width: 72px;
  flex-shrink: 0;
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.field-value {
  font-size: 14px;
  color: var(--color-text);
}

.field-edit {
  display: flex;
  gap: 8px;
  flex: 1;
}

.field-input {
  flex: 1;
  max-width: 300px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 14px;
  outline: none;
  transition: border-color var(--transition-fast);
}

.field-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-50);
}

.btn-small {
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  transition: all var(--transition-fast);
}

.btn-primary {
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: #fff;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-plain {
  background: var(--color-bg-sunken);
  color: var(--color-text-secondary);
}

.btn-add {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  transition: all var(--transition-fast);
}

.btn-add:hover {
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.35);
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.address-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-sm);
  transition: border-color var(--transition-fast);
}

.address-card:hover {
  border-color: var(--color-primary);
}

.address-main {
  min-width: 0;
}

.address-line1 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.addr-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

.addr-phone {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.addr-default {
  font-size: 11px;
  color: var(--color-primary);
  background: var(--color-primary-50);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-weight: 600;
}

.addr-detail {
  font-size: 13px;
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.address-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.btn-text {
  font-size: 13px;
  color: var(--color-primary);
  transition: opacity var(--transition-fast);
}

.btn-text:hover {
  opacity: 0.75;
}

.btn-text.danger {
  color: var(--color-danger);
}

.address-empty {
  text-align: center;
  color: var(--color-text-tertiary);
  padding: 32px 0;
  font-size: 14px;
}

.merchant-box {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: flex-start;
}

.merchant-tip {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.merchant-badge {
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  color: var(--color-primary);
  background: var(--color-primary-50);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-weight: 600;
}

/* 弹窗 */
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
  max-width: 480px;
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

.req {
  color: var(--color-danger);
}

.form-group input,
.form-group textarea {
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 14px;
  outline: none;
  transition: border-color var(--transition-fast);
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

.default-check {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--color-text-secondary);
  cursor: pointer;
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

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
