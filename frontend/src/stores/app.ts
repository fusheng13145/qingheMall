import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarOpened = ref(true)
  const loading = ref(false)
  const device = ref<'desktop' | 'mobile'>('desktop')

  const toggleSidebar = () => {
    sidebarOpened.value = !sidebarOpened.value
  }

  const setLoading = (status: boolean) => {
    loading.value = status
  }

  const setDevice = (d: 'desktop' | 'mobile') => {
    device.value = d
  }

  return {
    sidebarOpened,
    loading,
    device,
    toggleSidebar,
    setLoading,
    setDevice
  }
}, {
  persist: {
    key: 'qinghe-app',
    paths: ['sidebarOpened', 'device']
  }
})
