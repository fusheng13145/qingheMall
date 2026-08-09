import request from '../utils/request'

// 上传图片：POST /api/file/upload (multipart) → { url: "/uploads/xxx.jpg" }
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  // P2：不手动指定 Content-Type——浏览器/axios 自动生成带 boundary 的 multipart 头，手动覆盖可能缺 boundary
  return request.post('/file/upload', formData)
}
