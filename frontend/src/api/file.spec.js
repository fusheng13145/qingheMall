import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '../utils/request'
import { uploadImage } from './file'

vi.mock('../utils/request', () => ({
  default: { get: vi.fn(), post: vi.fn() }
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api/file', () => {
  it('uploadImage 以 FormData 上传并透传返回值', async () => {
    request.post.mockResolvedValue({ data: { url: '/uploads/a.jpg' } })
    const file = new File(['content'], 'a.jpg', { type: 'image/jpeg' })
    const res = await uploadImage(file)

    expect(request.post).toHaveBeenCalledTimes(1)
    const [url, formData] = request.post.mock.calls[0]
    expect(url).toBe('/file/upload')
    expect(formData).toBeInstanceOf(FormData)
    const uploaded = formData.get('file')
    expect(uploaded).toBeInstanceOf(File)
    expect(uploaded.name).toBe('a.jpg')
    expect(res).toEqual({ data: { url: '/uploads/a.jpg' } })
  })
})
