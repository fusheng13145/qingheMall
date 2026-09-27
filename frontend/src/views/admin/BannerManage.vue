<template>
  <div class="admin-banner">
    <div class="toolbar">
      <h2 class="page-title">首页运营位</h2>
      <button class="btn-new" @click="openCreate">新建运营位</button>
    </div>

    <form v-if="formOpen" class="banner-form" @submit.prevent="submit">
      <div class="form-row">
        <label>标题<b>*</b></label>
        <input v-model="form.title" maxlength="64" placeholder="运营位标题（≤64 字符）" />
      </div>
      <div class="form-row">
        <label>图片 URL<b>*</b></label>
        <input v-model="form.image" placeholder="/uploads/banner.jpg 或 https://..." />
      </div>
      <div class="form-row">
        <label>跳转链接</label>
        <input v-model="form.linkUrl" placeholder="以 / 开头按站内路由跳转（如 /products），否则按外链新窗打开" />
      </div>
      <div class="form-row">
        <label>排序</label>
        <input v-model.number="form.sortOrder" type="number" min="0" placeholder="数值小在前，默认 0" />
      </div>
      <div class="form-actions">
        <button type="submit" class="btn-primary" :disabled="saving">{{ editing ? '保存修改' : '创建' }}</button>
        <button type="button" class="btn-cancel" @click="closeForm">取消</button>
      </div>
    </form>

    <table class="data-table">
      <thead>
        <tr>
          <th>标题</th><th>预览</th><th>跳转链接</th><th>排序</th><th>状态</th><th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="b in banners" :key="b.id">
          <td class="cell-title">{{ b.title }}</td>
          <td><img class="thumb" :src="b.image" :alt="b.title" loading="lazy" /></td>
          <td class="cell-link">{{ b.linkUrl || '-' }}</td>
          <td>{{ b.sortOrder }}</td>
          <td><span class="status-tag" :class="b.status === 'ON' ? 'on' : 'off'">{{ b.status === 'ON' ? '上架' : '下架' }}</span></td>
          <td>
            <button class="btn-edit" @click="openEdit(b)">编辑</button>
            <button class="btn-toggle" @click="toggle(b)">{{ b.status === 'ON' ? '下架' : '上架' }}</button>
            <button class="btn-delete" @click="remove(b)">删除</button>
          </td>
        </tr>
        <tr v-if="!banners.length">
          <td colspan="6" class="empty-cell">暂无运营位</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  adminListBanners,
  adminCreateBanner,
  adminUpdateBanner,
  adminDeleteBanner,
  adminToggleBanner
} from '../../api/banner'
import { toast } from '../../utils/toast'

const banners = ref([])
const formOpen = ref(false)
const editing = ref(false)
const saving = ref(false)
const form = ref({ id: '', title: '', image: '', linkUrl: '', sortOrder: 0 })

function emptyForm() {
  return { id: '', title: '', image: '', linkUrl: '', sortOrder: 0 }
}

async function load() {
  try {
    const res = await adminListBanners()
    banners.value = (res.data && res.data.data) || []
  } catch (e) {
    toast.error('运营位加载失败')
  }
}

function openCreate() {
  editing.value = false
  form.value = emptyForm()
  formOpen.value = true
}

function openEdit(b) {
  editing.value = true
  form.value = { id: b.id, title: b.title, image: b.image, linkUrl: b.linkUrl || '', sortOrder: b.sortOrder ?? 0 }
  formOpen.value = true
}

function closeForm() {
  formOpen.value = false
  form.value = emptyForm()
}

async function submit() {
  if (!form.value.title.trim()) {
    toast.error('请填写运营位标题')
    return
  }
  if (!form.value.image.trim()) {
    toast.error('请填写图片 URL')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await adminUpdateBanner(form.value)
      toast.success('运营位已更新')
    } else {
      await adminCreateBanner(form.value)
      toast.success('运营位已创建')
    }
    closeForm()
    await load()
  } catch (e) {
    toast.error(editing.value ? '更新失败' : '创建失败')
  } finally {
    saving.value = false
  }
}

async function toggle(b) {
  try {
    await adminToggleBanner(b.id, b.status === 'ON' ? 'OFF' : 'ON')
    toast.success(b.status === 'ON' ? '已下架' : '已上架')
    await load()
  } catch (e) {
    toast.error('操作失败')
  }
}

async function remove(b) {
  try {
    await adminDeleteBanner(b.id)
    toast.success('已删除')
    await load()
  } catch (e) {
    toast.error('删除失败')
  }
}

onMounted(() => load())
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.page-title { font-size: 18px; margin: 0; }
.btn-new { background: #3b82f6; color: #fff; border: none; border-radius: 8px; padding: 7px 16px; cursor: pointer; }
.banner-form { border: 1px solid #e5e7eb; border-radius: 10px; padding: 14px; margin-bottom: 16px; display: grid; gap: 10px; }
.form-row { display: flex; align-items: center; gap: 10px; }
.form-row label { width: 84px; font-size: 13px; color: #4b5563; flex-shrink: 0; }
.form-row label b { color: #ef4444; margin-left: 2px; }
.form-row input { flex: 1; border: 1px solid #d1d5db; border-radius: 8px; padding: 7px 10px; font-size: 13px; }
.form-actions { display: flex; gap: 8px; }
.btn-primary { background: #10b981; color: #fff; border: none; border-radius: 8px; padding: 7px 18px; cursor: pointer; }
.btn-cancel { background: #f3f4f6; color: #4b5563; border: none; border-radius: 8px; padding: 7px 18px; cursor: pointer; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th, .data-table td { border-bottom: 1px solid #f0f0f0; padding: 9px 10px; text-align: left; }
.cell-title { font-weight: 600; }
.cell-link { color: #6b7280; word-break: break-all; max-width: 260px; }
.thumb { width: 96px; height: 36px; object-fit: cover; border-radius: 6px; display: block; }
.status-tag { border-radius: 999px; padding: 2px 10px; font-size: 12px; }
.status-tag.on { background: #d1fae5; color: #059669; }
.status-tag.off { background: #f3f4f6; color: #9ca3af; }
.btn-edit { background: #3b82f6; color: #fff; border: none; border-radius: 6px; padding: 5px 12px; cursor: pointer; margin-right: 6px; }
.btn-toggle { background: #f59e0b; color: #fff; border: none; border-radius: 6px; padding: 5px 12px; cursor: pointer; margin-right: 6px; }
.btn-delete { background: #ef4444; color: #fff; border: none; border-radius: 6px; padding: 5px 12px; cursor: pointer; }
.empty-cell { text-align: center; color: #9ca3af; }
</style>
