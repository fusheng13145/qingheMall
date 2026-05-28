import { defineStore } from 'pinia'
import { ref } from 'vue'
import { setToken, removeToken } from '@/utils/auth'
import { login as loginApi, getUserInfo, logout as logoutApi } from '@/api/modules/user'

export interface UserInfo {
  id: number
  username: string
  nickname?: string
  avatar?: string
  email?: string
  phone?: string
  gender?: number
  birthday?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)

  const setUserToken = (newToken: string) => {
    token.value = newToken
    setToken(newToken)
  }

  const login = async (username: string, password: string) => {
    const result = await loginApi({ username, password })
    setUserToken(result.token)
    await fetchUserInfo()
    return result
  }

  const fetchUserInfo = async () => {
    try {
      const info = await getUserInfo()
      userInfo.value = info
      return info
    } catch (error) {
      console.error('获取用户信息失败:', error)
      return null
    }
  }

  const logout = async () => {
    try {
      await logoutApi()
    } catch (error) {
      console.error('登出失败:', error)
    } finally {
      token.value = ''
      userInfo.value = null
      removeToken()
    }
  }

  const updateUserInfo = (info: Partial<UserInfo>) => {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...info }
    }
  }

  return {
    token,
    userInfo,
    login,
    fetchUserInfo,
    logout,
    updateUserInfo,
    setUserToken
  }
}, {
  persist: {
    key: 'qinghe-user',
    paths: ['token', 'userInfo']
  }
})
