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

  async function login(userNameVal, pwd) {
    try {
      const res = await loginApi(userNameVal, pwd)
      if (res.data.code === 200) {
        const data = res.data.data
        userId.value = data.userId
        userName.value = data.userName
        nickName.value = data.nickName || data.userName
        role.value = data.role || ''
        isLoggedIn.value = true
        return { success: true }
      } else {
        return { success: false, message: res.data.msg || '登录失败' }
      }
    } catch (error) {
      return { success: false, message: error.response?.data?.msg || '网络错误，请稍后重试' }
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (e) {
      // ignore
    }
    userId.value = null
    userName.value = ''
    nickName.value = ''
    role.value = ''
    isLoggedIn.value = false
  }

  async function checkLogin() {
    try {
      const res = await checkLoginApi()
      if (res.data.code === 200 && res.data.data) {
        const data = res.data.data
        userId.value = data.userId
        userName.value = data.userName
        nickName.value = data.nickName || data.userName
        role.value = data.role || ''
        isLoggedIn.value = true
      } else {
        userId.value = null
        userName.value = ''
        nickName.value = ''
        role.value = ''
        isLoggedIn.value = false
      }
    } catch (error) {
      userId.value = null
      userName.value = ''
      nickName.value = ''
      role.value = ''
      isLoggedIn.value = false
    }
  }

  return {
    userId,
    userName,
    nickName,
    isLoggedIn,
    role,
    isAdmin,
    login,
    logout,
    checkLogin
  }
})
