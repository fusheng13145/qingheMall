import request from '../utils/request'

export function reg(userName, pwd) {
  return request.post('/user/reg', {
    userName,
    pwd
  })
}

export function loginApi(userName, pwd) {
  return request.post('/user/login', {
    userName,
    pwd
  })
}

export function logoutApi() {
  return request.get('/user/logout')
}

export function checkLoginApi() {
  return request.get('/user/checkLogin')
}
