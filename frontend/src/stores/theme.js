import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const theme = ref(localStorage.getItem('theme') || 'system')

  function getSystemTheme() {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }

  function getEffectiveTheme() {
    return theme.value === 'system' ? getSystemTheme() : theme.value
  }

  function applyTheme() {
    const effective = getEffectiveTheme()
    document.documentElement.setAttribute('data-theme', effective)
  }

  function setTheme(newTheme) {
    theme.value = newTheme
    localStorage.setItem('theme', newTheme)
    applyTheme()
  }

  function toggleTheme() {
    const effective = getEffectiveTheme()
    setTheme(effective === 'dark' ? 'light' : 'dark')
  }

  // 监听系统主题变化
  if (typeof window !== 'undefined') {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (theme.value === 'system') {
        applyTheme()
      }
    })
  }

  // 初始化
  applyTheme()

  return {
    theme,
    getEffectiveTheme,
    setTheme,
    toggleTheme,
    applyTheme
  }
})
