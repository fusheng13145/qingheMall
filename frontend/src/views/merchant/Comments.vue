<template>
  <div class="comments-admin">
    <div class="page-header">
      <h2 class="page-title">评价管理</h2>
      <p class="page-desc">查看本店商品评价并回复，每条评价仅可回复一次</p>
    </div>

    <div v-for="c in comments" :key="c.id" class="comment-card">
      <div class="comment-head">
        <span class="comment-user">{{ c.userNickName || '匿名买家' }}</span>
        <span class="comment-stars">{{ '★'.repeat(c.rating || 0) }}{{ '☆'.repeat(5 - (c.rating || 0)) }}</span>
        <span class="comment-product">{{ c.productName || '商品' }}</span>
        <span class="comment-time">{{ formatTime(c.gmtCreated) }}</span>
      </div>
      <p class="comment-content">{{ c.content }}</p>

      <!-- 商家回复 -->
      <div v-if="c.replyContent" class="reply-box">
        <span class="reply-tag">商家回复</span>{{ c.replyContent }}
      </div>
      <div v-else class="reply-editor">
        <input
          v-model.trim="replyDrafts[c.id]"
          class="reply-input"
          type="text"
          maxlength="200"
          placeholder="回复该评价（200 字以内）"
          @keyup.enter="submitReply(c)"
        />
        <button class="reply-btn" :disabled="replying" @click="submitReply(c)">回复</button>
      </div>
    </div>

    <div v-if="!comments.length" class="comments-empty">暂无本店评价</div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listMerchantComments, replyMerchantComment } from '../../api/merchant'
import { formatTime } from '../../utils/format'
import { toast } from '../../utils/toast'

const comments = ref([])
const replyDrafts = reactive({})
const replying = ref(false)

async function load() {
  try {
    const res = await listMerchantComments(1, 20)
    comments.value = (res.data && res.data.data) || []
  } catch (e) {
    toast.error('评价加载失败')
  }
}

async function submitReply(comment) {
  const content = replyDrafts[comment.id]
  if (!content) {
    toast.error('回复内容不能为空')
    return
  }
  replying.value = true
  try {
    await replyMerchantComment(comment.id, content)
    comment.replyContent = content
    replyDrafts[comment.id] = ''
    toast.success('回复成功')
  } catch (e) {
    toast.error('回复失败')
  } finally {
    replying.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-desc { color: #6b7280; font-size: 13px; margin: 4px 0 0; }
.comment-card { background: #fff; border: 1px solid #f3f4f6; border-radius: 12px; padding: 14px 16px; margin-bottom: 12px; }
.comment-head { display: flex; align-items: center; gap: 12px; font-size: 13px; }
.comment-user { font-weight: 600; }
.comment-stars { color: #f59e0b; }
.comment-product { color: #6b7280; }
.comment-time { margin-left: auto; color: #9ca3af; font-size: 12px; }
.comment-content { margin: 8px 0; font-size: 14px; }
.reply-box { background: #f9fafb; border-radius: 8px; padding: 8px 12px; font-size: 13px; color: #374151; }
.reply-tag { color: #d97706; font-weight: 700; margin-right: 8px; }
.reply-editor { display: flex; gap: 8px; }
.reply-input { flex: 1; border: 1px solid #d1d5db; border-radius: 8px; padding: 7px 12px; font-size: 13px; }
.reply-btn { background: #d97706; color: #fff; border: none; border-radius: 8px; padding: 7px 16px; cursor: pointer; }
.comments-empty { text-align: center; color: #9ca3af; padding: 48px 0; }
</style>
