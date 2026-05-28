<template>
  <div class="pagination">
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      :background="background"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  total: number
  page?: number
  limit?: number
  background?: boolean
}>(), {
  page: 1,
  limit: 10,
  background: true
})

const emit = defineEmits<{
  (e: 'update:page', value: number): void
  (e: 'update:limit', value: number): void
  (e: 'pagination', value: { page: number; limit: number }): void
}>()

const currentPage = computed({
  get: () => props.page,
  set: (val) => emit('update:page', val)
})

const pageSize = computed({
  get: () => props.limit,
  set: (val) => emit('update:limit', val)
})

const handleSizeChange = (val: number) => {
  emit('update:limit', val)
  emit('pagination', { page: currentPage.value, limit: val })
}

const handleCurrentChange = (val: number) => {
  emit('update:page', val)
  emit('pagination', { page: val, limit: pageSize.value })
}
</script>

<style lang="scss" scoped>
.pagination {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}
</style>
