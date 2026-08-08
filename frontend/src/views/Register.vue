<template>
  <div class="register-page">
    <div class="register-card">
      <div class="card-header">
        <div class="card-icon">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/></svg>
        </div>
        <h2 class="card-title">创建账户</h2>
        <p class="card-subtitle">注册开启您的品质购物之旅</p>
      </div>
      <div class="form-group">
        <label class="form-label">注册身份</label>
        <div class="role-selector">
          <button type="button" class="role-option" :class="{ active: role === 'USER' }" @click="role = 'USER'">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/></svg>
            <span>顾客（购物）</span>
          </button>
          <button type="button" class="role-option" :class="{ active: role === 'MERCHANT' }" @click="role = 'MERCHANT'">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l1.5-5h15L21 9"/><path d="M3 9v11a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1V9"/><path d="M3 9h18a2 2 0 0 1 0 4H3a2 2 0 0 1 0-4z"/></svg>
            <span>商家（开店）</span>
          </button>
        </div>
      </div>
      <div v-if="role === 'MERCHANT'" class="form-group">
        <label class="form-label">店铺名称</label>
        <input
          v-model="shopName"
          type="text"
          class="form-input"
          placeholder="请输入店铺名称（入驻申请，平台审核通过后开店）"
        />
      </div>
      <div class="form-group">
        <label class="form-label">用户名</label>
        <div class="input-wrapper">
          <svg class="input-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          <input
            v-model="userName"
            type="text"
            class="form-input"
            placeholder="请输入用户名"
            @keyup.enter="handleRegister"
          />
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">密码</label>
        <div class="input-wrapper">
          <svg class="input-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
          <input
            v-model="pwd"
            type="password"
            class="form-input"
            placeholder="请输入密码（至少6位）"
            @keyup.enter="handleRegister"
          />
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">确认密码</label>
        <div class="input-wrapper">
          <svg class="input-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
          <input
            v-model="confirmPwd"
            type="password"
            class="form-input"
            placeholder="请再次输入密码"
            @keyup.enter="handleRegister"
          />
        </div>
      </div>
      <div v-if="errorMsg" class="error-message">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        {{ errorMsg }}
      </div>
      <div v-if="successMsg" class="success-message">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        {{ successMsg }}
      </div>
      <button class="btn-submit" :disabled="submitting" @click="handleRegister">
        {{ submitting ? '注册中...' : '注册' }}
      </button>
      <div class="form-footer">
        已有账号？<router-link to="/login" class="link">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { reg } from '../api/user'

const router = useRouter()

const userName = ref('')
const pwd = ref('')
const confirmPwd = ref('')
const role = ref('USER')
const shopName = ref('')
const errorMsg = ref('')
const successMsg = ref('')
const submitting = ref(false)

async function handleRegister() {
  errorMsg.value = ''
  successMsg.value = ''

  if (!userName.value.trim()) {
    errorMsg.value = '请输入用户名'
    return
  }
  if (!pwd.value) {
    errorMsg.value = '请输入密码'
    return
  }
  if (pwd.value.length < 6) {
    errorMsg.value = '密码长度不能少于6位'
    return
  }
  if (pwd.value !== confirmPwd.value) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  if (role.value === 'MERCHANT' && !shopName.value.trim()) {
    errorMsg.value = '商家注册请填写店铺名称'
    return
  }

  submitting.value = true
  try {
    await reg(userName.value.trim(), pwd.value, role.value, shopName.value.trim())
    successMsg.value = role.value === 'MERCHANT'
      ? '入驻申请已提交，等待平台审核通过后即可经营...'
      : '注册成功，即将跳转到登录页...'
    setTimeout(() => {
      router.push('/login')
    }, 1800)
  } catch (error) {
    errorMsg.value = error.message || '注册失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 520px;
  padding: 40px 20px;
}

.register-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 40px;
  width: 100%;
  max-width: 420px;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border-light);
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  background: var(--color-primary-50);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.card-title {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 6px;
}

.card-subtitle {
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.role-selector {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.role-option {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 8px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.role-option:hover {
  border-color: var(--color-primary);
}

.role-option.active {
  border-color: var(--color-primary);
  background: var(--color-primary-50);
  color: var(--color-primary);
  font-weight: 600;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  color: var(--color-text-tertiary);
  pointer-events: none;
  transition: color var(--transition-fast);
}

.input-wrapper:focus-within .input-icon {
  color: var(--color-primary);
}

.form-input {
  width: 100%;
  padding: 12px 16px 12px 44px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 15px;
  background: var(--color-bg);
  color: var(--color-text);
  transition: all var(--transition-fast);
}

.form-input::placeholder {
  color: var(--color-text-tertiary);
}

.form-input:focus {
  border-color: var(--color-primary);
  background: var(--color-bg-elevated);
  box-shadow: 0 0 0 3px var(--color-primary-50);
}

.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-danger-light);
  color: var(--color-danger);
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  margin-bottom: 16px;
}

.success-message {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-success-light);
  color: var(--color-success);
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  margin-bottom: 16px;
}

.btn-submit {
  width: 100%;
  padding: 13px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  transition: all var(--transition-base);
  margin-top: 4px;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);
}

.btn-submit:hover:not(:disabled) {
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.35);
  transform: translateY(-1px);
}

.btn-submit:active:not(:disabled) {
  transform: translateY(0);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.link {
  color: var(--color-primary);
  font-weight: 600;
  transition: opacity var(--transition-fast);
}

.link:hover {
  opacity: 0.8;
}
</style>
