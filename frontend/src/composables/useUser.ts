import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { storeToRefs } from 'pinia'

export const useUser = () => {
  const userStore = useUserStore()
  const { token, userInfo } = storeToRefs(userStore)

  const isLoggedIn = computed(() => !!token.value)

  const checkLogin = () => {
    if (!isLoggedIn.value) {
      return false
    }
    return true
  }

  const getUserId = () => {
    return userInfo.value?.id
  }

  const getUsername = () => {
    return userInfo.value?.username || userInfo.value?.nickname || '用户'
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    checkLogin,
    getUserId,
    getUsername,
    login: userStore.login,
    logout: userStore.logout,
    fetchUserInfo: userStore.fetchUserInfo,
    updateUserInfo: userStore.updateUserInfo
  }
}
