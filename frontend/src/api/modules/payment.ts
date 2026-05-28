import request from '../request'

export interface PayParams {
  orderNo: string
  payMethod: 'alipay' | 'wechat' | 'unionpay'
}

export interface PayResult {
  orderNo: string
  payUrl?: string
  qrCode?: string
  paySign?: string
  timeStamp?: string
  nonceStr?: string
}

export const createPayOrder = (data: PayParams) => {
  return request.post<PayResult>('/payment/create', data)
}

export const getPayStatus = (orderNo: string) => {
  return request.get<{ status: 'pending' | 'paid' | 'failed'; payTime?: string }>(`/payment/status/${orderNo}`)
}

export const queryPayResult = (orderNo: string) => {
  return request.get<{ paid: boolean }>(`/payment/query/${orderNo}`)
}
