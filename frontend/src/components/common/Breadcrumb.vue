<template>
  <div class="breadcrumb">
    <el-breadcrumb :separator="separator">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item v-for="(item, index) in items" :key="index">
        <router-link v-if="item.path" :to="item.path">{{ item.title }}</router-link>
        <span v-else>{{ item.title }}</span>
      </el-breadcrumb-item>
    </el-breadcrumb>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  items: { title: string; path?: string }[]
  separator?: string
}>(), {
  separator: '/'
})
</script>

<style lang="scss" scoped>
.breadcrumb {
  padding: 10px 0;
}
</style>
