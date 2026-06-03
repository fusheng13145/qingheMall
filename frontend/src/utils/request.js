import axios from 'axios'
import { useUserStore } from '../stores/user'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  withCredentials: true,
  timeout: 10000
})

request.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        const userStore = useUserStore()
        userStore.isLoggedIn = false
        userStore.userId = null
        userStore.userName = ''
        userStore.nickName = ''
        router.push('/login')
      } else if (status === 403) {
        router.push('/login')
      }
    }
    return Promise.reject(error)
  }
)

export default request
