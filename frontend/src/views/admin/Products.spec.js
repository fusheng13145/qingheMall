import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Products from './Products.vue'
import { toasts } from '../../utils/toast'

// vi.hoisted：mock 工厂与测试体共享同一组 api 引用
const { getProductList, addProduct, updateProduct, deleteProduct } = vi.hoisted(() => ({
  getProductList: vi.fn(),
  addProduct: vi.fn(),
  updateProduct: vi.fn(),
  deleteProduct: vi.fn()
}))
const { uploadImage } = vi.hoisted(() => ({ uploadImage: vi.fn() }))

vi.mock('../../api/admin', () => ({
  getProductList: (...a) => getProductList(...a),
  addProduct: (...a) => addProduct(...a),
  updateProduct: (...a) => updateProduct(...a),
  deleteProduct: (...a) => deleteProduct(...a)
}))
vi.mock('../../api/file', () => ({ uploadImage: (...a) => uploadImage(...a) }))

function makeProduct(overrides = {}) {
  return { id: 1, name: '青禾 Tee', price: 99, purchaseNum: 10, productIntro: '', productImgs: '', ...overrides }
}

/** res.data 为 Paging<Product>，列表在 data 字段 */
function mockList(list) {
  getProductList.mockResolvedValue({ data: { data: list } })
}

function mountPage() {
  return mount(Products)
}

function expectToast(type, message) {
  expect(toasts.some(t => t.type === type && t.message === message)).toBe(true)
}

beforeEach(() => {
  vi.clearAllMocks()
  global.confirm = vi.fn(() => true)
  toasts.splice(0, toasts.length)
})

describe('admin/Products 商品管理', () => {
  it('加载并渲染商品列表', async () => {
    mockList([makeProduct(), makeProduct({ id: 2, name: '青禾帽', price: 59.5 })])
    const wrapper = mountPage()
    await flushPromises()

    expect(getProductList).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('青禾 Tee')
    expect(wrapper.text()).toContain('青禾帽')
    expect(wrapper.text()).toContain('59.50')
  })

  it('空列表显示空态文案', async () => {
    mockList([])
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无商品数据')
  })

  it('加载失败 toast 提示', async () => {
    getProductList.mockRejectedValue(new Error('net'))
    mountPage()
    await flushPromises()

    expectToast('error', '加载商品列表失败：net')
  })

  it('新增：填写表单提交成功并刷新列表', async () => {
    mockList([])
    addProduct.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-add').trigger('click')
    expect(wrapper.find('.modal-header h3').text()).toBe('新增商品')

    await wrapper.find('input[placeholder="请输入商品名称"]').setValue('新商品')
    await wrapper.find('input[placeholder="请输入价格"]').setValue('19.9')
    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expect(addProduct).toHaveBeenCalledTimes(1)
    expect(addProduct.mock.calls[0][0]).toMatchObject({ name: '新商品', price: 19.9 })
    // 提交后重新拉取列表（onMounted 1 次 + 提交后 1 次）
    expect(getProductList).toHaveBeenCalledTimes(2)
    // 模态框关闭
    expect(wrapper.find('.modal-overlay').exists()).toBe(false)
  })

  it('新增校验：缺少名称不调用接口', async () => {
    mockList([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-add').trigger('click')
    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expectToast('warning', '请填写商品名称')
    expect(addProduct).not.toHaveBeenCalled()
  })

  it('新增校验：缺少价格不调用接口', async () => {
    mockList([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-add').trigger('click')
    await wrapper.find('input[placeholder="请输入商品名称"]').setValue('仅有名称')
    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expectToast('warning', '请填写商品价格')
    expect(addProduct).not.toHaveBeenCalled()
  })

  it('编辑：回填表单并调用 updateProduct', async () => {
    mockList([makeProduct({ name: '原名', price: 88 })])
    updateProduct.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-edit').trigger('click')
    expect(wrapper.find('.modal-header h3').text()).toBe('编辑商品')
    expect(wrapper.find('input[placeholder="请输入商品名称"]').element.value).toBe('原名')

    await wrapper.find('input[placeholder="请输入商品名称"]').setValue('改名')
    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expect(updateProduct).toHaveBeenCalledTimes(1)
    expect(updateProduct.mock.calls[0][0]).toMatchObject({ id: 1, name: '改名' })
    expect(addProduct).not.toHaveBeenCalled()
  })

  it('保存失败 toast 提示且不关闭模态框', async () => {
    mockList([])
    addProduct.mockRejectedValue(new Error('服务端错误'))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-add').trigger('click')
    await wrapper.find('input[placeholder="请输入商品名称"]').setValue('x')
    await wrapper.find('input[placeholder="请输入价格"]').setValue('1')
    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expectToast('error', '保存失败：服务端错误')
    expect(wrapper.find('.modal-overlay').exists()).toBe(true)
  })

  it('删除：确认后调用接口并刷新', async () => {
    mockList([makeProduct()])
    deleteProduct.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-delete').trigger('click')
    await flushPromises()

    expect(global.confirm).toHaveBeenCalled()
    expect(deleteProduct).toHaveBeenCalledWith(1)
    expect(getProductList).toHaveBeenCalledTimes(2)
  })

  it('删除：取消确认不调用接口', async () => {
    global.confirm = vi.fn(() => false)
    mockList([makeProduct()])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-delete').trigger('click')
    await flushPromises()

    expect(deleteProduct).not.toHaveBeenCalled()
  })

  it('上传图片成功后 URL 追加到图片字段（分号分隔）', async () => {
    mockList([])
    uploadImage.mockResolvedValue({ data: { url: '/uploads/a.jpg' } })
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-add').trigger('click')
    const fileInput = wrapper.find('input[type="file"]')
    const file = new File(['img'], 'a.jpg', { type: 'image/jpeg' })
    Object.defineProperty(fileInput.element, 'files', { value: [file], configurable: true })
    await fileInput.trigger('change')
    await flushPromises()

    expect(uploadImage).toHaveBeenCalledWith(file)
    expect(wrapper.find('input[placeholder^="https://"]').element.value).toBe('/uploads/a.jpg')

    // 第二张图追加
    uploadImage.mockResolvedValue({ data: { url: '/uploads/b.jpg' } })
    Object.defineProperty(fileInput.element, 'files', { value: [file], configurable: true })
    await fileInput.trigger('change')
    await flushPromises()

    expect(wrapper.find('input[placeholder^="https://"]').element.value).toBe('/uploads/a.jpg;/uploads/b.jpg')
  })

  it('上传失败 toast 提示', async () => {
    mockList([])
    uploadImage.mockRejectedValue(new Error('文件过大'))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.btn-add').trigger('click')
    const fileInput = wrapper.find('input[type="file"]')
    const file = new File(['img'], 'a.jpg', { type: 'image/jpeg' })
    Object.defineProperty(fileInput.element, 'files', { value: [file], configurable: true })
    await fileInput.trigger('change')
    await flushPromises()

    expectToast('error', '上传失败：文件过大')
  })
})
