import request from '../utils/request'

// 注册：POST /api/user/reg（form-urlencoded）
// role=USER 注册即生效；role=MERCHANT 创建商家账号 + 入驻申请（待平台审核），需带 shopName
export function reg(userName, pwd, role = 'USER', shopName) {
  const params = new URLSearchParams()
  params.append('userName', userName)
  params.append('pwd', pwd)
  params.append('role', role)
  if (shopName) params.append('shopName', shopName)
  return request.post('/user/reg', params)
}

// 登录：POST /api/user/login（form-urlencoded）
export function loginApi(userName, pwd) {
  const params = new URLSearchParams()
  params.append('userName', userName)
  params.append('pwd', pwd)
  return request.post('/user/login', params)
}

export function logoutApi() {
  return request.get('/user/logout')
}

export function checkLoginApi() {
  return request.get('/user/checkLogin')
}

// 更新个人资料（昵称/头像）：POST /api/user/updateProfile
export function updateProfileApi(nickName, avatar) {
  return request.post('/user/updateProfile', null, { params: { nickName, avatar } })
}
