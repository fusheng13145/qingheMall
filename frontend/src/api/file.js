import request from '../utils/request'

// 上传图片：POST /api/file/upload (multipart) → { url: "/uploads/xxx.jpg" }
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
