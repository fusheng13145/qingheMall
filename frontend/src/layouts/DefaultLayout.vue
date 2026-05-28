<template>
  <div class="default-layout">
    <TheHeader />
    <main class="main-content">
      <router-view />
    </main>
    <TheFooter />
  </div>
</template>

<script setup lang="ts">
import TheHeader from '@/components/common/TheHeader.vue'
import TheFooter from '@/components/common/TheFooter.vue'
</script>

<style lang="scss" scoped>
.default-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding: 20px 0;
}
</style>
