import type { Directive } from 'vue'
import { useUserStore } from '@/stores/user'

export const vPermission: Directive = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const { userInfo } = userStore

    const permission = binding.value

    if (permission && userInfo) {
      // 检查用户是否有该权限
      const hasPermission = checkPermission(permission, userInfo)
      if (!hasPermission) {
        el.parentNode?.removeChild(el)
      }
    }
  }
}

function checkPermission(permission: string | string[], userInfo: any): boolean {
  if (!permission) return true

  if (typeof permission === 'string') {
    return userInfo?.permissions?.includes(permission) || false
  }

  if (Array.isArray(permission)) {
    return permission.some(p => userInfo?.permissions?.includes(p))
  }

  return false
}

export default vPermission
