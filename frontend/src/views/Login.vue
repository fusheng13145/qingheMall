<template>
  <div class="login-page">
    <div class="login-card">
      <div class="card-header">
        <div class="card-icon">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg>
        </div>
        <h2 class="card-title">欢迎回来</h2>
        <p class="card-subtitle">登录您的账户继续购物</p>
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
            @keyup.enter="handleLogin"
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
            placeholder="请输入密码"
            @keyup.enter="handleLogin"
          />
        </div>
      </div>
      <div class="error-message" v-if="errorMsg">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        {{ errorMsg }}
      </div>
      <button class="btn-submit" @click="handleLogin" :disabled="submitting">
        <span v-if="!submitting">登录</span>
        <span v-else class="btn-loading">登录中...</span>
      </button>
      <div class="form-footer">
        还没有账号？<router-link to="/register" class="link">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const userName = ref('')
const pwd = ref('')
const errorMsg = ref('')
const submitting = ref(false)

async function handleLogin() {
  errorMsg.value = ''

  if (!userName.value.trim()) {
    errorMsg.value = '请输入用户名'
    return
  }
  if (!pwd.value) {
    errorMsg.value = '请输入密码'
    return
  }

  submitting.value = true
  try {
    const result = await userStore.login(userName.value.trim(), pwd.value)
    if (result.success) {
      const redirect = route.query.redirect || '/'
      router.push(redirect)
    } else {
      errorMsg.value = result.message
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 520px;
  padding: 40px 20px;
}

.login-card {
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

.form-input:focus + .input-icon,
.form-input:focus ~ .input-icon {
  color: var(--color-primary);
}

.input-wrapper:focus-within .input-icon {
  color: var(--color-primary);
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

.btn-loading {
  display: inline-flex;
  align-items: center;
  gap: 6px;
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
