<template>
  <div class="users-page">
    <h1 class="page-title">用户管理</h1>

    <div class="table-section">
      <div class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>昵称</th>
              <th>角色</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>{{ user.id }}</td>
              <td>{{ user.userName }}</td>
              <td>{{ user.nickName }}</td>
              <td>
                <span class="role-badge" :class="user.role === 'ADMIN' ? 'admin' : 'user'">
                  {{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}
                </span>
              </td>
              <td class="time">{{ formatTime(user.gmtCreated) }}</td>
              <td class="actions">
                <button
                  v-if="user.role === 'USER'"
                  class="btn-action btn-promote"
                  @click="handleRoleChange(user, 'ADMIN')"
                >
                  设为管理员
                </button>
                <button
                  v-else
                  class="btn-action btn-demote"
                  @click="handleRoleChange(user, 'USER')"
                >
                  设为普通用户
                </button>
              </td>
            </tr>
            <tr v-if="users.length === 0">
              <td colspan="6" class="empty">暂无用户数据</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="totalPage > 1" class="pagination-bar">
        <button class="page-btn" :disabled="pagination <= 1" @click="goPrev">上一页</button>
        <span class="page-info">第 {{ pagination }} / {{ totalPage }} 页（共 {{ totalCount }} 条）</span>
        <button class="page-btn" :disabled="pagination >= totalPage" @click="goNext">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserList, updateUserRole } from '../../api/admin'

const users = ref([])
// P1-11：服务端分页状态
const pagination = ref(1)
const pageSize = ref(20)
const totalCount = ref(0)
const totalPage = ref(1)

onMounted(async () => {
  await loadUsers()
})

async function loadUsers() {
  try {
    const res = await getUserList(pagination.value, pageSize.value)
    const paging = res.data || {}
    users.value = paging.data || []
    totalCount.value = paging.totalCount || 0
    totalPage.value = paging.totalPage || 1
  } catch (e) {
    alert('加载用户列表失败：' + (e.message || '请稍后重试'))
  }
}

async function goPrev() {
  if (pagination.value <= 1) return
  pagination.value--
  await loadUsers()
}

async function goNext() {
  if (pagination.value >= totalPage.value) return
  pagination.value++
  await loadUsers()
}

function formatTime(time) {
  if (!time) return ''
  // 兼容 LocalDateTime 的 ISO 格式（2026-08-06T15:00:00）与 Date 格式
  const d = new Date(time)
  if (isNaN(d.getTime())) return String(time).replace('T', ' ')
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

async function handleRoleChange(user, newRole) {
  const label = newRole === 'ADMIN' ? '管理员' : '普通用户'
  if (!confirm(`确定要将用户「${user.nickName}」角色修改为「${label}」吗？`)) return
  try {
    await updateUserRole(user.id, newRole)
    user.role = newRole
  } catch (e) {
    alert('角色修改失败：' + (e.message || '请稍后重试'))
  }
}
</script>

<style scoped>
.users-page {
  max-width: 1200px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 24px;
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

.time {
  color: var(--color-text-secondary);
  font-size: 13px;
}

.role-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}

.role-badge.admin {
  background: var(--color-primary-50);
  color: var(--color-primary);
}

.role-badge.user {
  background: var(--color-bg-overlay);
  color: var(--color-text-secondary);
}

.actions {
  min-width: 120px;
}

.btn-action {
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.btn-promote {
  color: var(--color-primary);
  background: var(--color-primary-50);
}

.btn-promote:hover {
  background: var(--color-primary);
  color: #fff;
}

.btn-demote {
  color: var(--color-warning);
  background: var(--color-warning-light);
}

.btn-demote:hover {
  background: var(--color-warning);
  color: #fff;
}

.empty {
  text-align: center;
  color: var(--color-text-tertiary);
  padding: 40px 24px !important;
}

/* P1-11：分页控件 */
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 4px 0;
}
.page-btn {
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text);
  cursor: pointer;
}
.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.page-info {
  font-size: 13px;
  color: var(--color-text-secondary);
}
</style>
