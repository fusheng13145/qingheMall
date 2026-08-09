import axios from 'axios'
import { useUserStore } from '../stores/user'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  withCredentials: true,
  timeout: 10000
})

/**
 * 统一响应解析：适配后端 Result<T> 结构 { success, code, message, data }
 *
 * 约定：
 * - 业务成功（code === 200）：直接返回 Result 对象，调用方用 res.code / res.data 取值
 * - 业务失败（success === false）：reject 一个 Error，error.message 为后端 message
 * - HTTP 401：清除登录态并跳转登录页
 * - HTTP 403：仅 reject（可能是「无管理员权限」，由页面决定如何提示），不强制跳转
 */
request.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result && result.code !== undefined && result.code !== 200) {
      const error = new Error(result.message || '请求失败')
      error.code = result.code
      error.data = result.data
      return Promise.reject(error)
    }
    return result
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      const serverMessage = error.response.data && error.response.data.message
      if (status === 401) {
        const userStore = useUserStore()
        userStore.isLoggedIn = false
        userStore.userId = null
        userStore.userName = ''
        userStore.nickName = ''
        userStore.role = ''
        const current = router.currentRoute.value
        // P2：防重复跳转（并发 401 只跳一次）
        if (current.name !== 'Login') {
          router.push({
            name: 'Login',
            query: { redirect: current.fullPath }
          })
        }
        // P2：标记已全局处理（清登录态+跳转），调用方 catch 可据此不再重复 alert
        error.handled = true
        error.message = serverMessage || '登录已过期，请重新登录'
      } else if (status === 403) {
        // 无权限：交由调用方提示，不跳转（管理接口被普通用户访问时不应重定向到登录页）
        const err = new Error(serverMessage || '无权限访问')
        err.code = 403
        return Promise.reject(err)
      }
      error.message = serverMessage || error.message
    }
    return Promise.reject(error)
  }
)

export default request
