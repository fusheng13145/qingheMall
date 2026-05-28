import request from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userId: number
  username: string
}

export interface RegisterParams {
  username: string
  password: string
  phone: string
  code: string
}

export const login = (params: LoginParams) => {
  return request.post<LoginResult>('/user/auth/login', params)
}

export const logout = () => {
  return request.post('/user/logout')
}

export const getUserInfo = () => {
  return request.get('/user/info')
}

export const updateUserInfo = (data: any) => {
  return request.put('/user/info', data)
}

export const register = (data: RegisterParams) => {
  return request.post('/user/auth/register', data)
}

export const changePassword = (data: { oldPassword: string; newPassword: string }) => {
  return request.post('/user/changePassword', data)
}

export const getAddressList = () => {
  return request.get('/user/address/list')
}

export const addAddress = (data: any) => {
  return request.post('/user/address/add', data)
}

export const updateAddress = (data: any) => {
  return request.put('/user/address/update', data)
}

export const deleteAddress = (id: number) => {
  return request.delete(`/user/address/${id}`)
}

export const setDefaultAddress = (id: number) => {
  return request.put(`/user/address/default/${id}`)
}
