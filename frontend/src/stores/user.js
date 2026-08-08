import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, logoutApi, checkLoginApi } from '../api/user'

export const useUserStore = defineStore('user', () => {
  const userId = ref(null)
  const userName = ref('')
  const nickName = ref('')
  const isLoggedIn = ref(false)
  const role = ref('')

  const isAdmin = computed(() => role.value === 'ADMIN')

  // 商家身份（M6）：登录角色为 MERCHANT 即可进入商家工作台（经营权限以后端 ACTIVE 校验为准）
  const isMerchant = computed(() => role.value === 'MERCHANT')

  function applyUser(data) {
    userId.value = data.id
    userName.value = data.userName
    nickName.value = data.nickName || data.userName
    role.value = data.role || ''
    isLoggedIn.value = true
  }

  function clearUser() {
    userId.value = null
    userName.value = ''
    nickName.value = ''
    role.value = ''
    isLoggedIn.value = false
  }

  async function login(userNameVal, pwd) {
    try {
      const res = await loginApi(userNameVal, pwd)
      applyUser(res.data)
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message || '网络错误，请稍后重试' }
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (e) {
      // 登出失败不阻塞本地清理
    }
    clearUser()
  }

  async function checkLogin() {
    try {
      const res = await checkLoginApi()
      if (res.data) {
        applyUser(res.data)
      } else {
        clearUser()
      }
    } catch (error) {
      clearUser()
    }
  }

  return {
    userId,
    userName,
    nickName,
    isLoggedIn,
    role,
    isAdmin,
    isMerchant,
    login,
    logout,
    checkLogin
  }
})
