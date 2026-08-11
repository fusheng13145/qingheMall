<script setup>
/**
 * 404 页面（P2-14）：未知路由兜底，替代 vue-router 默认空白/告警行为。
 */
</script>

<template>
  <div class="not-found">
    <p class="code">404</p>
    <h1 class="title">页面走丢了</h1>
    <p class="desc">您访问的页面不存在或已被移除，去别处逛逛吧。</p>
    <div class="actions">
      <router-link class="btn btn-primary" :to="{ name: 'Home' }">返回首页</router-link>
      <router-link class="btn btn-ghost" :to="{ name: 'Products' }">浏览商品</router-link>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 48px 20px;
}

.code {
  font-size: 96px;
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin-bottom: 12px;
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 8px;
}

.desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 28px;
}

.actions {
  display: flex;
  gap: 12px;
}

.btn {
  display: inline-block;
  padding: 10px 24px;
  border-radius: var(--radius-full, 9999px);
  font-size: 14px;
  transition: opacity var(--transition-fast, 0.15s ease);
}

.btn:hover {
  opacity: 0.85;
}

.btn-primary {
  background: var(--color-primary);
  color: #fff;
}

.btn-ghost {
  border: 1px solid var(--color-border);
  color: var(--color-text);
  background: var(--color-surface);
}
</style>
