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
 * - 401（HTTP 层或业务层 code，P1-8 后两者同语义）：清除登录态并跳转登录页，
 *   error.handled=true 提示调用方无需重复弹窗（P2-13）
 * - HTTP 403：仅 reject（可能是「无管理员权限」，由页面决定如何提示），不强制跳转
 */

/** 401 统一处理：清登录态 + 跳转登录页，返回展示文案（防重复跳转） */
function handleUnauthorized(serverMessage) {
  const userStore = useUserStore()
  // P2-15：走统一清理入口（同步重置购物车本地状态，防跨用户残留）
  userStore.clearUser()
  const current = router.currentRoute.value
  // P2：防重复跳转（并发 401 只跳一次）
  if (current.name !== 'Login') {
    router.push({
      name: 'Login',
      query: { redirect: current.fullPath }
    })
  }
  return serverMessage || '登录已过期，请重新登录'
}

request.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result && result.code !== undefined && result.code !== 200) {
      const error = new Error(result.message || '请求失败')
      error.code = result.code
      error.data = result.data
      // P1-8/P2-13：业务层 401（AuthException 未登录）与 HTTP 401 同等处理
      if (result.code === 401) {
        error.message = handleUnauthorized(result.message)
        error.handled = true
      }
      return Promise.reject(error)
    }
    return result
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      const serverMessage = error.response.data && error.response.data.message
      if (status === 401) {
        // P2：标记已全局处理（清登录态+跳转），调用方 catch 可据此不再重复提示
        error.handled = true
        error.message = handleUnauthorized(serverMessage)
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
