// Mock 数据 - 当后端不可用时自动使用

const IMG_BASE = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image'

export const mockProducts = [
  {
    id: '1',
    name: '极简主义陶瓷花瓶 手工制作客厅装饰摆件',
    price: 168.00,
    purchaseNum: 236,
    intro: '手工拉坯成型，高温烧制，每一件都独一无二。适合搭配干花或鲜花，为家居增添一份雅致。',
    productImgs: `${IMG_BASE}?prompt=Minimalist%20ceramic%20vase%20on%20white%20marble%20table%2C%20soft%20natural%20lighting%2C%20scandinavian%20interior%20design%2C%20product%20photography&image_size=square_hd`
  },
  {
    id: '2',
    name: '北欧风格羊毛针织毯 加厚保暖沙发盖毯',
    price: 329.00,
    purchaseNum: 189,
    intro: '100%纯羊毛材质，亲肤柔软，透气保暖。经典北欧几何纹样，百搭各种家居风格。',
    productImgs: `${IMG_BASE}?prompt=Cozy%20wool%20knitted%20blanket%20on%20modern%20sofa%2C%20scandinavian%20living%20room%2C%20warm%20lighting%2C%20product%20photography&image_size=square_hd`
  },
  {
    id: '3',
    name: '日式手冲咖啡壶套装 硼硅玻璃耐热滤杯',
    price: 258.00,
    purchaseNum: 412,
    intro: '高硼硅玻璃材质，耐冷热冲击。细口壶嘴设计，精准控制水流，轻松冲泡专业级手冲咖啡。',
    productImgs: `${IMG_BASE}?prompt=Japanese%20pour%20over%20coffee%20maker%20set%20with%20borosilicate%20glass%2C%20minimalist%20kitchen%20counter%2C%20warm%20morning%20light%2C%20product%20photography&image_size=square_hd`
  },
  {
    id: '4',
    name: '真皮手工笔记本 A5复古皮质旅行手帐',
    price: 128.00,
    purchaseNum: 567,
    intro: '头层牛皮封面，手工缝线装订，可更换内芯。复古做旧工艺，越用越有味道。',
    productImgs: `${IMG_BASE}?prompt=Vintage%20leather%20journal%20A5%20on%20wooden%20desk%2C%20rustic%20craftsmanship%2C%20warm%20ambient%20light%2C%20product%20photography&image_size=square_hd`
  },
  {
    id: '5',
    name: '智能香薰机 超声波雾化静音扩香器',
    price: 199.00,
    purchaseNum: 328,
    intro: '超声波雾化技术，静音运行低于30分贝。七彩氛围灯，定时开关，让生活充满仪式感。',
    productImgs: `${IMG_BASE}?prompt=Smart%20aroma%20diffuser%20with%20soft%20LED%20light%2C%20modern%20bedroom%20nightstand%2C%20gentle%20mist%2C%20product%20photography&image_size=square_hd`
  },
  {
    id: '6',
    name: '天然乳胶枕头 泰国进口波浪形护颈枕',
    price: 299.00,
    purchaseNum: 891,
    intro: '泰国天然乳胶，人体工学波浪曲线设计，有效支撑颈椎。透气蜂窝结构，抑菌防螨。',
    productImgs: `${IMG_BASE}?prompt=Natural%20latex%20pillow%20on%20white%20bed%20linen%2C%20ergonomic%20wave%20design%2C%20clean%20bedroom%2C%20product%20photography&image_size=square_hd`
  },
  {
    id: '7',
    name: '便携式蓝牙音箱 复古收音机造型防水设计',
    price: 239.00,
    purchaseNum: 445,
    intro: '复古收音机造型，蓝牙5.0连接，IPX5防水等级。10小时超长续航，户外出行必备。',
    productImgs: `${IMG_BASE}?prompt=Retro%20radio%20style%20bluetooth%20speaker%2C%20vintage%20design%2C%20outdoor%20picnic%20setting%2C%20product%20photography&image_size=square_hd`
  },
  {
    id: '8',
    name: '不锈钢保温杯 316L医用级大容量水杯',
    price: 159.00,
    purchaseNum: 1023,
    intro: '316L医用级不锈钢内胆，12小时保温保冷。500ml大容量，一键弹盖，单手操作。',
    productImgs: `${IMG_BASE}?prompt=Premium%20stainless%20steel%20thermos%20bottle%2C%20matte%20finish%2C%20minimalist%20desk%20setting%2C%20product%20photography&image_size=square_hd`
  },
  {
    id: '9',
    name: '机械键盘 青轴87键复古打字机风格',
    price: 459.00,
    purchaseNum: 276,
    intro: '青轴段落手感，清脆打字声。复古圆形键帽，LED背光，全键无冲，即插即用。',
    productImgs: `${IMG_BASE}?prompt=Retro%20typewriter%20style%20mechanical%20keyboard%2C%20round%20keycaps%2C%20vintage%20desk%20setup%2C%20product%20photography&image_size=square_hd`
  },
  {
    id: '10',
    name: '植物精油礼盒 四季香氛薰衣草甜橙套装',
    price: 188.00,
    purchaseNum: 634,
    intro: '天然植物萃取，四季四种香型。薰衣草助眠、甜橙提神、茶树净化、桉树舒缓。',
    productImgs: `${IMG_BASE}?prompt=Essential%20oil%20gift%20set%20with%20four%20bottles%2C%20natural%20botanical%20style%2C%20elegant%20packaging%2C%20product%20photography&image_size=square_hd`
  },
  {
    id: '11',
    name: '无线充电底座 15W快充兼容多设备',
    price: 89.00,
    purchaseNum: 756,
    intro: '15W快速无线充电，兼容手机/耳机/手表。超薄设计，LED指示灯，智能识别设备。',
    productImgs: `${IMG_BASE}?prompt=Minimalist%20wireless%20charging%20pad%2C%20sleek%20white%20design%2C%20phone%20charging%2C%20clean%20desk%2C%20product%20photography&image_size=square_hd`
  },
  {
    id: '12',
    name: '手工编织收纳篮 水草天然材质多规格组合',
    price: 78.00,
    purchaseNum: 423,
    intro: '天然水草手工编织，环保无异味。三个规格组合，收纳整理两不误，让生活井井有条。',
    productImgs: `${IMG_BASE}?prompt=Handwoven%20seagrass%20storage%20baskets%20set%2C%20natural%20material%2C%20boho%20home%20decor%2C%20product%20photography&image_size=square_hd`
  }
]

export const mockProductDetails = {
  '1': [
    { id: 'd1', productId: '1', price: 168.00, size: 15, stock: 56 },
    { id: 'd2', productId: '1', price: 198.00, size: 25, stock: 23 }
  ],
  '2': [
    { id: 'd3', productId: '2', price: 329.00, size: 150, stock: 45 },
    { id: 'd4', productId: '2', price: 459.00, size: 200, stock: 18 }
  ],
  '3': [
    { id: 'd5', productId: '3', price: 258.00, size: 350, stock: 67 },
    { id: 'd6', productId: '3', price: 328.00, size: 600, stock: 34 }
  ],
  '4': [
    { id: 'd7', productId: '4', price: 128.00, size: 0, stock: 120 }
  ],
  '5': [
    { id: 'd8', productId: '5', price: 199.00, size: 0, stock: 89 },
    { id: 'd9', productId: '5', price: 259.00, size: 0, stock: 45 }
  ],
  '6': [
    { id: 'd10', productId: '6', price: 299.00, size: 0, stock: 156 }
  ],
  '7': [
    { id: 'd11', productId: '7', price: 239.00, size: 0, stock: 78 }
  ],
  '8': [
    { id: 'd12', productId: '8', price: 159.00, size: 500, stock: 234 },
    { id: 'd13', productId: '8', price: 189.00, size: 750, stock: 112 }
  ],
  '9': [
    { id: 'd14', productId: '9', price: 459.00, size: 0, stock: 43 }
  ],
  '10': [
    { id: 'd15', productId: '10', price: 188.00, size: 0, stock: 167 }
  ],
  '11': [
    { id: 'd16', productId: '11', price: 89.00, size: 0, stock: 345 }
  ],
  '12': [
    { id: 'd17', productId: '12', price: 78.00, size: 0, stock: 198 }
  ]
}

export const mockOrders = [
  {
    id: 'order001',
    orderNo: 'QH20240601001',
    productName: '极简主义陶瓷花瓶 手工制作客厅装饰摆件',
    size: '15cm',
    totalPrice: 168.00,
    status: 0,
    createTime: '2024-06-01 14:30:00'
  },
  {
    id: 'order002',
    orderNo: 'QH20240531002',
    productName: '北欧风格羊毛针织毯 加厚保暖沙发盖毯',
    size: '150x200cm',
    totalPrice: 329.00,
    status: 1,
    createTime: '2024-05-31 10:15:00'
  },
  {
    id: 'order003',
    orderNo: 'QH20240530003',
    productName: '日式手冲咖啡壶套装 硼硅玻璃耐热滤杯',
    size: '350ml',
    totalPrice: 258.00,
    status: 1,
    createTime: '2024-05-30 09:20:00'
  },
  {
    id: 'order004',
    orderNo: 'QH20240528004',
    productName: '真皮手工笔记本 A5复古皮质旅行手帐',
    size: 'A5',
    totalPrice: 128.00,
    status: 2,
    createTime: '2024-05-28 16:45:00'
  },
  {
    id: 'order005',
    orderNo: 'QH20240527005',
    productName: '智能香薰机 超声波雾化静音扩香器',
    totalPrice: 199.00,
    status: 0,
    createTime: '2024-05-27 20:10:00'
  },
  {
    id: 'order006',
    orderNo: 'QH20240525006',
    productName: '天然乳胶枕头 泰国进口波浪形护颈枕',
    totalPrice: 299.00,
    status: 1,
    createTime: '2024-05-25 11:30:00'
  }
]

// 模拟分页返回
export function getMockProductPage(page, pageSize) {
  const start = (page - 1) * pageSize
  const end = start + pageSize
  const records = mockProducts.slice(start, end)
  return {
    code: 200,
    data: {
      records,
      total: mockProducts.length,
      pages: Math.ceil(mockProducts.length / pageSize),
      current: page
    }
  }
}

// 模拟单个商品
export function getMockProduct(id) {
  const product = mockProducts.find(p => p.id === id)
  return {
    code: 200,
    data: product || null
  }
}

// 模拟商品详情
export function getMockProductDetails(productId) {
  const details = mockProductDetails[productId] || []
  return {
    code: 200,
    data: details
  }
}

// 模拟订单列表
export function getMockOrders() {
  return {
    code: 200,
    data: mockOrders
  }
}
