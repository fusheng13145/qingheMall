import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Products from './Products.vue'
import { toasts } from '../../utils/toast'

// vi.hoisted：mock 工厂与测试体共享同一组 api 引用
const { listMerchantProducts, saveMerchantProduct, toggleMerchantProduct } = vi.hoisted(() => ({
  listMerchantProducts: vi.fn(),
  saveMerchantProduct: vi.fn(),
  toggleMerchantProduct: vi.fn()
}))
const { getProductDetails } = vi.hoisted(() => ({ getProductDetails: vi.fn() }))
const { uploadImage } = vi.hoisted(() => ({ uploadImage: vi.fn() }))

vi.mock('../../api/merchant', () => ({
  listMerchantProducts: (...a) => listMerchantProducts(...a),
  saveMerchantProduct: (...a) => saveMerchantProduct(...a),
  toggleMerchantProduct: (...a) => toggleMerchantProduct(...a)
}))
vi.mock('../../api/product', () => ({ getProductDetails: (...a) => getProductDetails(...a) }))
vi.mock('../../api/file', () => ({ uploadImage: (...a) => uploadImage(...a) }))

function makeProduct(overrides = {}) {
  return {
    id: 1,
    name: '青禾 Tee',
    brand: '青禾',
    price: 99,
    purchaseNum: 6,
    status: 'ON',
    productIntro: '',
    productImgs: '',
    details: [],
    ...overrides
  }
}

/** res.data 为 Paging<Product>：列表在 data 字段，另带 totalPage / totalCount */
function mockPage(list, paging = {}) {
  listMerchantProducts.mockResolvedValue({
    data: { data: list, totalPage: 1, totalCount: list.length, ...paging }
  })
}

function mountPage() {
  return mount(Products)
}

function expectToast(type, message) {
  expect(toasts.some((t) => t.type === type && t.message === message)).toBe(true)
}

function actionButton(wrapper, text) {
  return wrapper.findAll('.row-actions .link-btn').find((b) => b.text() === text)
}

beforeEach(() => {
  vi.clearAllMocks()
  global.confirm = vi.fn(() => true)
  toasts.splice(0, toasts.length)
})

describe('merchant/Products 商品管理', () => {
  it('加载并渲染商品列表与库存合计', async () => {
    mockPage([
      makeProduct({ details: [{ stock: 5 }, { stock: 7 }] }),
      makeProduct({ id: 2, name: '青禾帽', price: 59.5, status: 'OFF' })
    ])
    const wrapper = mountPage()
    await flushPromises()

    expect(listMerchantProducts).toHaveBeenCalledWith(1, 10, '', '')
    expect(wrapper.text()).toContain('青禾 Tee')
    expect(wrapper.text()).toContain('青禾帽')
    expect(wrapper.text()).toContain('99.00')
    expect(wrapper.text()).toContain('59.50')
    // 库存为 SKU stock 合计：5 + 7 = 12
    expect(wrapper.text()).toContain('12')
    expect(wrapper.text()).toContain('在售')
    expect(wrapper.text()).toContain('下架')
    expect(wrapper.text()).toContain('共 2 件')
  })

  it('空列表显示空态文案', async () => {
    mockPage([])
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无商品，点击右上角「新增商品」上架第一款商品')
  })

  it('加载失败 toast 提示', async () => {
    listMerchantProducts.mockRejectedValue(new Error('net'))
    mountPage()
    await flushPromises()

    expectToast('error', '加载失败：net')
  })

  it('新增：填写表单与规格提交成功并刷新列表', async () => {
    mockPage([])
    saveMerchantProduct.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.page-header .btn-primary').trigger('click')
    expect(wrapper.find('.modal-title').text()).toBe('新增商品')

    await wrapper.find('input[placeholder="商品名称"]').setValue('新商品')
    await wrapper.find('input[placeholder="0.00"]').setValue('19.9')
    await wrapper.find('.sku-price').setValue('59')
    await wrapper.find('.sku-stock').setValue('10')
    await wrapper.find('.modal-actions .btn-primary').trigger('click')
    await flushPromises()

    expect(saveMerchantProduct).toHaveBeenCalledTimes(1)
    expect(saveMerchantProduct.mock.calls[0][0]).toMatchObject({
      id: null,
      name: '新商品',
      price: 19.9,
      status: 'ON',
      details: [{ price: 59, size: null, stock: 10 }]
    })
    expectToast('success', '商品已上架')
    // 提交后重新拉取列表（onMounted 1 次 + 提交后 1 次）
    expect(listMerchantProducts).toHaveBeenCalledTimes(2)
    // 模态框关闭
    expect(wrapper.find('.modal-overlay').exists()).toBe(false)
  })

  it('新增校验：缺少名称不调用接口', async () => {
    mockPage([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.page-header .btn-primary').trigger('click')
    await wrapper.find('.modal-actions .btn-primary').trigger('click')
    await flushPromises()

    expectToast('warning', '请填写商品名称')
    expect(saveMerchantProduct).not.toHaveBeenCalled()
  })

  it('新增校验：参考价无效不调用接口', async () => {
    mockPage([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.page-header .btn-primary').trigger('click')
    await wrapper.find('input[placeholder="商品名称"]').setValue('仅有名称')
    await wrapper.find('.modal-actions .btn-primary').trigger('click')
    await flushPromises()

    expectToast('warning', '请填写正确的参考价')
    expect(saveMerchantProduct).not.toHaveBeenCalled()
  })

  it('新增校验：无完整规格不调用接口', async () => {
    mockPage([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.page-header .btn-primary').trigger('click')
    await wrapper.find('input[placeholder="商品名称"]').setValue('新商品')
    await wrapper.find('input[placeholder="0.00"]').setValue('19.9')
    // 默认规格行价格/库存均为空，不满足「价格>0、库存>=0」
    await wrapper.find('.modal-actions .btn-primary').trigger('click')
    await flushPromises()

    expectToast('warning', '请至少填写一条完整的规格（价格>0、库存>=0）')
    expect(saveMerchantProduct).not.toHaveBeenCalled()
  })

  it('新增校验：规格价格非法不调用接口', async () => {
    mockPage([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.page-header .btn-primary').trigger('click')
    await wrapper.find('input[placeholder="商品名称"]').setValue('新商品')
    await wrapper.find('input[placeholder="0.00"]').setValue('19.9')
    await wrapper.find('.sku-price').setValue('59')
    await wrapper.find('.sku-stock').setValue('10')
    // 添加第二条规格：价格 0 非法
    await wrapper.findAll('.link-btn').find((b) => b.text() === '+ 添加规格').trigger('click')
    const priceInputs = wrapper.findAll('.sku-price')
    const stockInputs = wrapper.findAll('.sku-stock')
    expect(priceInputs.length).toBe(2)
    await priceInputs[1].setValue('0')
    await stockInputs[1].setValue('5')
    await wrapper.find('.modal-actions .btn-primary').trigger('click')
    await flushPromises()

    expectToast('warning', '规格价格必须大于 0，库存不能为负数')
    expect(saveMerchantProduct).not.toHaveBeenCalled()
  })

  it('编辑：拉取规格回填并以原 id 保存', async () => {
    mockPage([makeProduct({ id: 7, name: '原名', price: 88 })])
    getProductDetails.mockResolvedValue({ data: [{ price: 59, size: 40, stock: 8 }] })
    saveMerchantProduct.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await actionButton(wrapper, '编辑').trigger('click')
    await flushPromises()

    expect(getProductDetails).toHaveBeenCalledWith(7)
    expect(wrapper.find('.modal-title').text()).toBe('编辑商品')
    expect(wrapper.find('input[placeholder="商品名称"]').element.value).toBe('原名')
    expect(wrapper.find('.sku-price').element.value).toBe('59')

    await wrapper.find('input[placeholder="商品名称"]').setValue('改名')
    await wrapper.find('.modal-actions .btn-primary').trigger('click')
    await flushPromises()

    expect(saveMerchantProduct).toHaveBeenCalledTimes(1)
    expect(saveMerchantProduct.mock.calls[0][0]).toMatchObject({
      id: 7,
      name: '改名',
      details: [{ price: 59, size: 40, stock: 8 }]
    })
    expectToast('success', '保存成功')
  })

  it('编辑：规格拉取失败仍打开弹窗并保留空白规格行', async () => {
    mockPage([makeProduct({ id: 7 })])
    getProductDetails.mockRejectedValue(new Error('net'))
    const wrapper = mountPage()
    await flushPromises()

    await actionButton(wrapper, '编辑').trigger('click')
    await flushPromises()

    expect(getProductDetails).toHaveBeenCalledWith(7)
    expect(wrapper.find('.modal-overlay').exists()).toBe(true)
    expect(wrapper.findAll('.sku-row').length).toBe(1)
    expect(wrapper.find('.sku-price').element.value).toBe('')
  })

  it('保存失败 toast 提示且不关闭模态框', async () => {
    mockPage([])
    saveMerchantProduct.mockRejectedValue(new Error('服务端错误'))
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.page-header .btn-primary').trigger('click')
    await wrapper.find('input[placeholder="商品名称"]').setValue('新商品')
    await wrapper.find('input[placeholder="0.00"]').setValue('19.9')
    await wrapper.find('.sku-price').setValue('59')
    await wrapper.find('.sku-stock').setValue('10')
    await wrapper.find('.modal-actions .btn-primary').trigger('click')
    await flushPromises()

    expectToast('error', '保存失败：服务端错误')
    expect(wrapper.find('.modal-overlay').exists()).toBe(true)
  })

  it('下架：确认后调用接口并刷新列表', async () => {
    mockPage([makeProduct({ id: 1, status: 'ON' })])
    toggleMerchantProduct.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await actionButton(wrapper, '下架').trigger('click')
    await flushPromises()

    expect(global.confirm).toHaveBeenCalled()
    expect(toggleMerchantProduct).toHaveBeenCalledWith(1, 'OFF')
    expect(listMerchantProducts).toHaveBeenCalledTimes(2)
  })

  it('上架：下架商品以 ON 状态调用接口', async () => {
    mockPage([makeProduct({ id: 1, status: 'OFF' })])
    toggleMerchantProduct.mockResolvedValue({})
    const wrapper = mountPage()
    await flushPromises()

    await actionButton(wrapper, '上架').trigger('click')
    await flushPromises()

    expect(toggleMerchantProduct).toHaveBeenCalledWith(1, 'ON')
    expect(listMerchantProducts).toHaveBeenCalledTimes(2)
  })

  it('上下架：取消确认不调用接口', async () => {
    global.confirm = vi.fn(() => false)
    mockPage([makeProduct({ id: 1, status: 'ON' })])
    const wrapper = mountPage()
    await flushPromises()

    await actionButton(wrapper, '下架').trigger('click')
    await flushPromises()

    expect(toggleMerchantProduct).not.toHaveBeenCalled()
  })

  it('上下架失败 toast 提示', async () => {
    mockPage([makeProduct({ id: 1, status: 'ON' })])
    toggleMerchantProduct.mockRejectedValue(new Error('权限不足'))
    const wrapper = mountPage()
    await flushPromises()

    await actionButton(wrapper, '下架').trigger('click')
    await flushPromises()

    expectToast('error', '操作失败：权限不足')
  })

  it('筛选：关键字与状态随查询传递', async () => {
    mockPage([])
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.find('.filter-input').setValue('青禾')
    await wrapper.find('.filter-bar .btn-ghost').trigger('click')
    await flushPromises()
    expect(listMerchantProducts).toHaveBeenLastCalledWith(1, 10, '青禾', '')

    await wrapper.find('.filter-select').setValue('ON')
    await flushPromises()
    expect(listMerchantProducts).toHaveBeenLastCalledWith(1, 10, '青禾', 'ON')
  })
})

describe('merchant/Products 图片上传（P2-17）', () => {
  async function openCreateModal() {
    mockPage([])
    const wrapper = mountPage()
    await flushPromises()
    await wrapper.find('.page-header .btn-primary').trigger('click')
    return wrapper
  }

  function selectFile(wrapper) {
    const fileInput = wrapper.find('input[type="file"]')
    const file = new File(['img'], 'a.jpg', { type: 'image/jpeg' })
    Object.defineProperty(fileInput.element, 'files', { value: [file], configurable: true })
    return { fileInput, file }
  }

  it('上传成功后 URL 以空格分隔追加到图片字段', async () => {
    uploadImage.mockResolvedValue({ data: { url: '/uploads/a.jpg' } })
    const wrapper = await openCreateModal()

    const { fileInput, file } = selectFile(wrapper)
    await fileInput.trigger('change')
    await flushPromises()

    expect(uploadImage).toHaveBeenCalledWith(file)
    const urlInput = wrapper.find('.img-upload-row .form-input')
    expect(urlInput.element.value).toBe('/uploads/a.jpg')

    // 第二张图追加（空格分隔）
    uploadImage.mockResolvedValue({ data: { url: '/uploads/b.jpg' } })
    Object.defineProperty(fileInput.element, 'files', { value: [file], configurable: true })
    await fileInput.trigger('change')
    await flushPromises()

    expect(urlInput.element.value).toBe('/uploads/a.jpg /uploads/b.jpg')
  })

  it('已有 URL 时上传结果追加在末尾', async () => {
    uploadImage.mockResolvedValue({ data: { url: '/uploads/new.jpg' } })
    const wrapper = await openCreateModal()
    await wrapper.find('.img-upload-row .form-input').setValue('http://cdn/x.jpg')

    const { fileInput } = selectFile(wrapper)
    await fileInput.trigger('change')
    await flushPromises()

    expect(wrapper.find('.img-upload-row .form-input').element.value)
      .toBe('http://cdn/x.jpg /uploads/new.jpg')
  })

  it('上传失败 toast 提示', async () => {
    uploadImage.mockRejectedValue(new Error('仅支持 jpg/png/webp/gif 格式图片'))
    const wrapper = await openCreateModal()

    const { fileInput } = selectFile(wrapper)
    await fileInput.trigger('change')
    await flushPromises()

    expectToast('error', '上传失败：仅支持 jpg/png/webp/gif 格式图片')
  })
})
