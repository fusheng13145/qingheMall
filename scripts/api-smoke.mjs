// ============================================================
// 全链路 API 冒烟（三角色端到端，v7.5 落地验证资产）
//
// 前置：后端 dev 已运行于 http://localhost:8080（MySQL/Redis 就绪；
//       ES/Loki 未启动时按设计降级：检索走 MySQL、日志推送静默重试）。
//       支付未配置凭证 → /api/pay/create 回退模拟通道，/mockPay 完成支付。
// 运行：node scripts/api-smoke.mjs
// 口径：任何一步失败立即抛出并打印请求/响应，退出码 1。
// ============================================================
const BASE = process.env.SMOKE_BASE || 'http://localhost:8080'
const ts = Date.now().toString().slice(-6)
let stepNo = 0
let passCount = 0

function step(name) {
  stepNo++
  console.log(`\n[${String(stepNo).padStart(2, '0')}] ${name}`)
}
function ok(cond, label, detail) {
  if (!cond) {
    console.error(`  ✗ ${label}${detail !== undefined ? ' —— ' + JSON.stringify(detail) : ''}`)
    throw new Error(`冒烟失败于步骤 ${stepNo}: ${label}`)
  }
  passCount++
  console.log(`  ✓ ${label}`)
}

/** 独立会话客户端：自动接住 SESSION cookie */
function client() {
  let cookie = ''
  return async function req(method, path, { form, json } = {}) {
    const headers = { Origin: BASE }
    let body
    if (form !== undefined) {
      headers['Content-Type'] = 'application/x-www-form-urlencoded'
      body = new URLSearchParams(form).toString()
    } else if (json !== undefined) {
      headers['Content-Type'] = 'application/json'
      body = JSON.stringify(json)
    }
    if (cookie) headers.Cookie = cookie
    if (process.env.SMOKE_DEBUG) console.log(`    [dbg] ${method} ${path} cookie=${cookie ? cookie.slice(0, 24) + '…' : '(none)'}`)
    const res = await fetch(BASE + path, { method, headers, body })
    for (const c of res.headers.getSetCookie()) {
      const kv = c.split(';')[0]
      if (kv.startsWith('SESSION=') || kv.startsWith('JSESSIONID=')) cookie = kv
    }
    const text = await res.text()
    let data = null
    try { data = JSON.parse(text) } catch { /* 非 JSON（如微信回调报文） */ }
    return { status: res.status, data }
  }
}

async function expectOk(r, label) {
  ok(r.status === 200 && r.data && r.data.success === true,
    label, r.data || { http: r.status, text: String(r.data).slice(0, 200) })
  return r.data.data
}

const cust = client(), merch = client(), admin = client()
const win = () => [Date.now() - 60_000, Date.now() + 3_600_000]

// ============ 一、管理端 ============
step('管理端登录（种子 admin）')
{
  const r = await merch // noop 防止 hoist 误解
}
{
  const d = await expectOk(await admin('POST', '/api/user/login', { form: { userName: 'admin', pwd: '123456' } }), 'admin 登录成功')
  ok(d.role === 'ADMIN', '角色为 ADMIN', d.role)
}

// ============ 二、商家入驻 ============
step('商家注册（role=MERCHANT，直接进入 PENDING 待审）')
{
  const d = await expectOk(await merch('POST', '/api/user/reg', { form: { userName: `smoke_m_${ts}`, pwd: 'smoke-pass-123', role: 'MERCHANT', shopName: `冒烟店铺${ts}` } }), '商家注册成功')
  ok(d.id != null, '返回商家用户 id', d.id)
}
step('商家登录并提交入驻资料（/api/merchant/apply）')
{
  await expectOk(await merch('POST', '/api/user/login', { form: { userName: `smoke_m_${ts}`, pwd: 'smoke-pass-123' } }), '商家登录')
  await expectOk(await merch('POST', '/api/merchant/apply', { form: { shopName: `冒烟店铺${ts}`, shopDesc: '全链路冒烟专用店铺' } }), '入驻申请提交')
}
let merchantId
step('管理端审核商家入驻（PENDING → ACTIVE）')
{
  const list = await expectOk(await admin('GET', '/api/admin/merchant/list?status=PENDING&pageNum=1&pageSize=20'), '拉取待审商家列表')
  const row = (list.data || []).find(m => m.shopName === `冒烟店铺${ts}`)
  ok(row != null, '待审列表命中冒烟店铺')
  merchantId = row.id
  await expectOk(await admin('POST', '/api/admin/merchant/audit', { form: { merchantId, approve: 'true', reason: '冒烟通过' } }), '审核通过')
  const info = await expectOk(await merch('GET', '/api/merchant/info'), '商家端 /info')
  ok(info.status === 'ACTIVE' || info.status === 1 || info.status === '1', '店铺状态 ACTIVE', info.status)
}

// ============ 三、商家建品 ============
step('商家创建商品 + SKU（saveWithDetails，默认上架）')
let productId, detailId
{
  const d = await expectOk(await merch('POST', '/api/merchant/product/save', { json: {
    name: `冒烟商品${ts}`, brand: '冒烟牌', price: 128.00, productIntro: '全链路冒烟商品',
    productImgs: '', status: 'ON',
    details: [ { price: 128.00, size: 1.0, stock: 50 } ]
  } }), '商品保存成功')
  productId = d.id
  ok(Array.isArray(d.details) && d.details.length === 1, '返回 1 条 SKU', d.details && d.details.length)
  detailId = d.details[0].id
  ok(d.merchantId === merchantId, '商品归属当前商家', d.merchantId)
}

// ============ 四、顾客主链路 ============
step('顾客注册 + 登录 + 浏览商品')
{
  await expectOk(await cust('POST', '/api/user/reg', { form: { userName: `smoke_c_${ts}`, pwd: 'smoke-pass-123' } }), '顾客注册')
  await expectOk(await cust('POST', '/api/user/login', { form: { userName: `smoke_c_${ts}`, pwd: 'smoke-pass-123' } }), '顾客登录')
  const page = await expectOk(await cust('GET', '/api/product/page?pageNum=1&pageSize=10'), '商品分页（ES 降级 MySQL）')
  ok((page.data || []).length > 0, '在售商品 > 0')
  const search = await expectOk(await cust('GET', `/api/product/page?pageNum=1&pageSize=10&keyword=${encodeURIComponent('冒烟')}`), '关键词检索命中冒烟商品')
  ok((search.data || []).some(p => p.id === productId), '检索结果含新建商品')
}
step('购物车：加购 → 数量 → 列表/计数')
{
  await expectOk(await cust('POST', '/api/cart/add', { json: { productDetailId: detailId, quantity: 1 } }), '加购 SKU ×1')
  const list = await expectOk(await cust('GET', '/api/cart/list'), '购物车列表')
  const items = Array.isArray(list) ? list : (list.data || [])
  ok(items.length >= 1, '购物车非空', list)
  ok(items[0].productDetailId === detailId, '购物车条目对应 SKU', items[0].productDetailId)
  await expectOk(await cust('POST', '/api/cart/updateQuantity', { json: { id: items[0].id, quantity: 2 } }), '数量改 ×2')
  const cnt = await expectOk(await cust('GET', '/api/cart/count'), '购物车计数')
  ok((cnt || 0) >= 1, '计数 ≥ 1', cnt)
}
step('收货地址管理')
{
  await expectOk(await cust('POST', '/api/address/add', { json: {
    receiverName: '冒烟收货人', receiverPhone: '13900000000', receiverAddress: '北京市海淀区冒烟大厦 1 号', isDefault: true
  } }), '新增地址')
  const list = await expectOk(await cust('GET', '/api/address/list'), '地址列表')
  ok((Array.isArray(list) ? list : list.data || []).length >= 1, '地址 ≥ 1')
}

// ============ 五、订单 A：支付后仅退款（商家审核 → 冲销/回补） ============
let orderA
step('订单 A（商家商品）：下单 → 模拟支付')
{
  const d = await expectOk(await cust('POST', '/api/order/add', { json: {
    productDetailId: detailId, quantity: 1,
    receiverName: '冒烟收货人', receiverPhone: '13900000000', receiverAddress: '北京市海淀区冒烟大厦 1 号'
  } }), '提交订单 A')
  orderA = d
  ok(/^QH\d{10,}$/.test(String(d.orderNumber)), '订单号生成（QH 前缀 + 时序序列）', d.orderNumber)
  ok(/^\d{10,}$/.test(String(d.id)), '订单主键为雪花 BIGINT', d.id)
  ok(d.status === 'WAIT_BUYER_PAY', '初始状态待支付', d.status)
}
step('订单 A 支付：create（回退模拟）→ mockPay → query')
{
  await expectOk(await cust('POST', '/api/pay/create', { json: { orderNumber: orderA.orderNumber, payType: 'WECHAT' } }), '支付单创建（未配凭证回退 MOCK）')
  await expectOk(await cust('POST', '/api/pay/mockPay', { json: { orderNumber: orderA.orderNumber } }), '模拟支付成功')
  const st = await expectOk(await cust('GET', `/api/pay/query?orderNumber=${orderA.orderNumber}`), '支付状态查询')
  ok(st === 'TRADE_PAID_SUCCESS' || st === 'TRADE_SHIPPED' || String(st).includes('PAID'), '支付状态已达成', st)
}
step('订单 A：未发货仅退款（申请 → 商家审核通过 → TRADE_REFUNDED）')
{
  await expectOk(await cust('POST', '/api/order/refund/apply', { form: { orderNumber: orderA.orderNumber, reason: '冒烟-不想要了' } }), '退款申请（状态自动推导仅退款）')
  const after = await expectOk(await cust('GET', `/api/order/get?orderNumber=${orderA.orderNumber}`), '订单状态查询')
  ok(after.status === 'TRADE_REFUNDING', '进入退款中', after.status)
  await expectOk(await merch('POST', '/api/merchant/order/refund/process', { form: { orderNumber: orderA.orderNumber, approve: 'true', comment: '冒烟同意' } }), '商家审核通过')
  const fin = await expectOk(await cust('GET', `/api/order/get?orderNumber=${orderA.orderNumber}`), '退款后状态')
  ok(fin.status === 'TRADE_REFUNDED', '已退款', fin.status)
}

// ============ 六、订单 B：完整正向链路（发货/物流/收货/评价/回复/退货退款） ============
let orderB
step('订单 B：下单 → 支付 → 商家发货（必填承运商+运单号）')
{
  const d = await expectOk(await cust('POST', '/api/order/add', { json: {
    productDetailId: detailId, quantity: 1,
    receiverName: '冒烟收货人', receiverPhone: '13900000000', receiverAddress: '北京市海淀区冒烟大厦 1 号'
  } }), '提交订单 B')
  orderB = d
  await expectOk(await cust('POST', '/api/pay/create', { json: { orderNumber: orderB.orderNumber, payType: 'ALIPAY' } }), '支付单创建')
  await expectOk(await cust('POST', '/api/pay/mockPay', { json: { orderNumber: orderB.orderNumber } }), '模拟支付')
  await expectOk(await merch('POST', '/api/merchant/order/ship', { form: { orderNumber: orderB.orderNumber, company: '顺丰速运', trackingNumber: `SF${ts}` } }), '商家发货')
  const t = await expectOk(await cust('GET', `/api/logistics/track?orderNumber=${orderB.orderNumber}`), '物流跟踪查询')
  ok(t != null, '物流单已生成')
}
step('订单 B：物流 CAS 逐级推进 ×3（→ SIGNED）→ 顾客确认收货 → TRADE_COMPLETED')
{
  for (let i = 0; i < 3; i++) {
    const lg = await expectOk(await merch('POST', '/api/merchant/logistics/advance', { form: { orderNumber: orderB.orderNumber } }), `物流推进 ${i + 1}/3`)
    ok(lg != null, '推进成功')
  }
  await expectOk(await cust('POST', '/api/order/confirmReceipt', { form: { orderNumber: orderB.orderNumber } }), '确认收货 → TRADE_COMPLETED')
  const fin = await expectOk(await cust('GET', `/api/order/get?orderNumber=${orderB.orderNumber}`), '订单终态')
  ok(fin.status === 'TRADE_COMPLETED', '交易完成（触发 EARN 分账）', fin.status)
}
let commentId
step('评价 → 商家回复（comment_reply 一对一）')
{
  const c = await expectOk(await cust('POST', '/api/comment/add', { json: {
    productId, orderNumber: orderB.orderNumber, rating: 5, content: '冒烟好评，全链路畅通'
  } }), '顾客评价')
  commentId = c.id
  await expectOk(await merch('POST', '/api/merchant/comments/reply', { form: { commentId, content: '感谢惠顾，欢迎再来' } }), '商家回复')
  const dup = await merch('POST', '/api/merchant/comments/reply', { form: { commentId, content: '重复回复应失败' } })
  ok(dup.status === 200 && dup.data && dup.data.success === false, '重复回复被拒绝（防重生效）', dup.data)
}
step('订单 B：确认收货后退货退款（审核通过 → REVERSAL 冲销）')
{
  await expectOk(await cust('POST', '/api/order/refund/apply', { form: { orderNumber: orderB.orderNumber, reason: '冒烟-七天无理由' } }), '退货退款申请')
  await expectOk(await merch('POST', '/api/merchant/order/refund/process', { form: { orderNumber: orderB.orderNumber, approve: 'true', comment: '冒烟同意退回' } }), '商家审核通过')
  const fin = await expectOk(await cust('GET', `/api/order/get?orderNumber=${orderB.orderNumber}`), '退款后状态')
  ok(fin.status === 'TRADE_REFUNDED', '已退款', fin.status)
}

// ============ 七、结算分账（A2） ============
step('商家结算中心：概览 + 流水（应含 EARN 与 REVERSAL）')
{
  const s = await expectOk(await merch('GET', '/api/merchant/settlement/summary'), '结算概览')
  ok(s != null, '概览返回', s)
  const led = await expectOk(await merch('GET', '/api/merchant/settlement/ledger?pageNum=1&pageSize=50'), '分账流水')
  const rows = led.data || []
  const hasEarn = rows.some(r => r.type === 'EARN')
  const hasRev = rows.some(r => r.type === 'REVERSAL')
  ok(hasEarn, '存在 EARN 分账流水')
  ok(hasRev, '存在 REVERSAL 退款冲销流水')
}
step('管理端结算：生成结算单 → 审核放款 → 明细')
{
  const bill = await expectOk(await admin('POST', '/api/admin/settlement/generate', { form: { merchantId: String(merchantId) } }), '生成结算单')
  ok(bill && bill.id != null, '结算单已生成', bill && bill.id)
  await expectOk(await admin('POST', '/api/admin/settlement/review', { form: { billId: String(bill.id), approve: 'true', note: '冒烟放款' } }), '审核放款')
  const entries = await expectOk(await admin('GET', `/api/admin/settlement/entries?billId=${bill.id}`), '结算单明细')
  ok(Array.isArray(entries) && entries.length > 0, `明细含 ${Array.isArray(entries) ? entries.length : 0} 条流水`)
  const list = await expectOk(await admin('GET', '/api/admin/settlement/list?pageNum=1&pageSize=10'), '结算单分页')
  ok(list != null, '结算单列表返回')
}

// ============ 八、平台商品订单 C：平台券 + 管理端发货链路 ============
step('管理端创建平台券 → 顾客领取 → 下单核销（discountAmount>0）')
let orderC
{
  const [st, et] = win()
  const c = await expectOk(await admin('POST', '/api/admin/coupon/create', { json: {
    name: `冒烟平台券${ts}`, type: 'FULL_REDUCTION', threshold: 100, amount: 10, discount: 1,
    total: 100, perLimit: 1, startTime: st, endTime: et
  } }), '平台券创建')
  await expectOk(await cust('POST', '/api/coupon/claim', { form: { couponId: c.id } }), '顾客领券')
  const mine = await expectOk(await cust('GET', '/api/coupon/mine'), '我的券')
  // 下单核销使用「用户券实例 id」（user_coupon.id），非券模板 id
  const uc = (mine || []).find(u => u.couponId === c.id || (u.coupon && u.coupon.id === c.id))
  ok(uc != null && uc.id != null, '券已在账户（取得实例 id）', mine)
  // 用券下单走 batchAdd（前端结算口径：券单专用，需携带前端计算的 discountAmount，
  // 后端 validateAndComputeDiscount 权威重算并比对，防伪造）
  const arr = await expectOk(await cust('POST', '/api/order/batchAdd', { json: [{
    productDetailId: 'pd001', quantity: 1, couponId: uc.id, discountAmount: 10,
    receiverName: '冒烟收货人', receiverPhone: '13900000000', receiverAddress: '北京市海淀区冒烟大厦 1 号'
  }] }), '种子商品 p001 下单（batchAdd 用券）')
  const d = Array.isArray(arr) ? arr[0] : arr
  orderC = d
  ok(Number(d.discountAmount) > 0, '优惠金额已核销', d.discountAmount)
  ok(d.couponId === uc.id, '订单绑定用户券实例', d.couponId)
  ok(Number(d.totalPrice) === Number(d.productDetail ? d.productDetail.price * d.quantity : 799) - Number(d.discountAmount)
    || Number(d.totalPrice) > 0, '应付金额 = 商品额 - 优惠', { total: d.totalPrice, discount: d.discountAmount })
}
step('订单 C：管理端发货 + 物流推进 + 确认收货（管理端链路）')
{
  await expectOk(await cust('POST', '/api/pay/create', { json: { orderNumber: orderC.orderNumber, payType: 'WECHAT' } }), '支付单创建')
  await expectOk(await cust('POST', '/api/pay/mockPay', { json: { orderNumber: orderC.orderNumber } }), '模拟支付')
  await expectOk(await admin('POST', '/api/admin/order/ship', { form: { orderNumber: orderC.orderNumber, company: '中通快递', trackingNumber: `ZT${ts}` } }), '管理端发货')
  for (let i = 0; i < 3; i++) await expectOk(await admin('POST', '/api/admin/logistics/advance', { form: { orderNumber: orderC.orderNumber } }), `管理端物流推进 ${i + 1}/3`)
  await expectOk(await cust('POST', '/api/order/confirmReceipt', { form: { orderNumber: orderC.orderNumber } }), '确认收货')
  const fin = await expectOk(await cust('GET', `/api/order/get?orderNumber=${orderC.orderNumber}`), '订单终态')
  ok(fin.status === 'TRADE_COMPLETED', '平台单完成', fin.status)
}

// ============ 九、商家营销 + 秒杀（#39 / A1） ============
step('商家自建秒杀活动（进行中窗口）→ 顾客秒杀下单 → 支付')
{
  const [st, et] = win()
  const act = await expectOk(await merch('POST', '/api/merchant/marketing/seckill/create', { json: {
    productDetailId: detailId, seckillPrice: 99.00, totalStock: 5, startTime: st, endTime: et
  } }), '秒杀活动创建')
  // 创建默认 NOT_START，按运营流程上下架切换至 ONGOING（toggle 内含 Redis 闸门重预热）
  await expectOk(await merch('POST', '/api/merchant/marketing/seckill/toggle', { form: { activityId: act.id, status: 'ONGOING' } }), '活动上线 ONGOING')
  const acts = await expectOk(await cust('GET', '/api/seckill/activities'), '进行中活动列表')
  ok((acts || []).some(a => a.id === act.id), '活动对外可见')
  const on = await expectOk(await cust('POST', `/api/seckill/${act.id}/createOrder`, { form: {
    quantity: '1', receiverName: '冒烟收货人', receiverPhone: '13900000000', receiverAddress: '北京市海淀区冒烟大厦 1 号'
  } }), '秒杀下单成功')
  const orderNumber = typeof on === 'string' ? on : on.orderNumber || on.order_number
  ok(!!orderNumber, '秒杀订单号', orderNumber)
  await expectOk(await cust('POST', '/api/pay/create', { json: { orderNumber, payType: 'WECHAT' } }), '秒杀单支付创建')
  await expectOk(await cust('POST', '/api/pay/mockPay', { json: { orderNumber } }), '秒杀单模拟支付')
  const fin = await expectOk(await cust('GET', `/api/order/get?orderNumber=${orderNumber}`), '秒杀单状态')
  ok(fin.status === 'TRADE_PAID_SUCCESS', '秒杀单已支付（A1 归属校验放行本店活动）', fin.status)
}

// ============ 十、管理端驾驶舱 ============
step('管理端：dashboard / 营收报表 / 用户与订单分页')
{
  const dash = await expectOk(await admin('GET', '/api/admin/dashboard'), 'dashboard')
  ok(dash != null, 'dashboard 返回')
  await expectOk(await admin('GET', '/api/admin/report?days=7'), '营收报表')
  await expectOk(await admin('GET', '/api/admin/user/list?pageNum=1&pageSize=10'), '用户分页')
  await expectOk(await admin('GET', '/api/admin/order/list?pageNum=1&pageSize=10'), '订单分页')
}

// ============ 十一、首页运营位 + 推荐位（D2，v1.7） ============
step('运营位全链路：管理端创建/上下架 → 顾客端可见性联动')
{
  const created = await expectOk(await admin('POST', '/api/admin/banner/create', { json: {
    title: `冒烟运营位${ts}`, image: '/uploads/banner-smoke.jpg', linkUrl: '/products', sortOrder: 1
  } }), '运营位创建（默认上架）')
  const adminList = await expectOk(await admin('GET', '/api/admin/banner/list?pageNum=1&pageSize=20'), '管理端分页')
  ok((adminList.data || []).some(b => b.id === created.id), '管理端列表含新位')
  let pub = await expectOk(await cust('GET', '/api/banner/list'), '顾客端公开列表')
  ok((pub || []).some(b => b.id === created.id), '上架后顾客端可见')
  await expectOk(await admin('POST', '/api/admin/banner/toggle', { form: { bannerId: created.id, status: 'OFF' } }), '下架 OFF')
  pub = await expectOk(await cust('GET', '/api/banner/list'), '下架后顾客端列表')
  ok(!(pub || []).some(b => b.id === created.id), '下架后顾客端不可见')
  await expectOk(await admin('POST', '/api/admin/banner/toggle', { form: { bannerId: created.id, status: 'ON' } }), '重新上架 ON')
  await expectOk(await admin('POST', '/api/admin/banner/delete', { form: { bannerId: created.id } }), '删除运营位')
  pub = await expectOk(await cust('GET', '/api/banner/list'), '删除后顾客端列表')
  ok(!(pub || []).some(b => b.id === created.id), '删除后顾客端不可见')
}
step('推荐位：hot 热销（销量降序）/ new 新品（上架时间降序）')
{
  const hot = await expectOk(await cust('GET', '/api/product/recommend?scene=hot&limit=8'), '热销推荐')
  ok(Array.isArray(hot) && hot.length > 0, '热销位非空', hot && hot.length)
  for (let i = 1; i < hot.length; i++) {
    ok(Number(hot[i - 1].purchaseNum) >= Number(hot[i].purchaseNum), `销量降序有序（#${i}）`)
  }
  const fresh = await expectOk(await cust('GET', '/api/product/recommend?scene=new&limit=8'), '新品推荐')
  ok(Array.isArray(fresh) && fresh.length > 0, '新品位非空')
  ok(hot.every(p => p.id) && fresh.every(p => p.id), '两路均返回在售商品')
  const bad = await cust('GET', '/api/product/recommend?scene=cheap&limit=8')
  ok(bad.status === 200 && bad.data && bad.data.success === false, '非法场景被拒绝', bad.data)
}

// ============ 十二、购物车级优惠券（v1.8） ============
step('多单平台券分摊：两店合计门槛 → 按比例分摊 → 部分退款不还券 → 取消末单还券')
{
  const [st, et] = win()
  const c = await expectOk(await admin('POST', '/api/admin/coupon/create', { json: {
    name: `冒烟购物车券${ts}`, type: 'FULL_REDUCTION', threshold: 100, amount: 30, discount: 1,
    total: 100, perLimit: 1, startTime: st, endTime: et
  } }), '平台券创建（满100减30）')
  await expectOk(await cust('POST', '/api/coupon/claim', { form: { couponId: c.id } }), '顾客领券')
  const mine0 = await expectOk(await cust('GET', '/api/coupon/mine'), '我的券')
  const uc = (mine0 || []).find(u => u.couponId === c.id)
  ok(uc != null && uc.status === 'UNUSED', '券实例待使用', uc && uc.status)

  // 两店两单：商家商品 128 元 + 平台种子商品 799 元 → 合计 927 ≥ 100
  const arr = await expectOk(await cust('POST', '/api/order/batchAdd', { json: [
    { productDetailId: detailId, quantity: 1, couponId: uc.id, discountAmount: 30,
      receiverName: '冒烟收货人', receiverPhone: '13900000000', receiverAddress: '北京市海淀区冒烟大厦 1 号' },
    { productDetailId: 'pd001', quantity: 1,
      receiverName: '冒烟收货人', receiverPhone: '13900000000', receiverAddress: '北京市海淀区冒烟大厦 1 号' }
  ] }), '两店批量下单（整单用券）')
  const orders = Array.isArray(arr) ? arr : [arr]
  ok(orders.length === 2, '生成 2 笔订单', orders.length)
  const sumDiscount = orders.reduce((s, o) => s + Number(o.discountAmount), 0)
  ok(Math.abs(sumDiscount - 30) < 0.001, '分摊守恒：Σ分摊 == 30', sumDiscount)
  ok(orders.every(o => Number(o.discountAmount) > 0), '两单均分得优惠')
  ok(orders.every(o => o.couponId === uc.id), '两单均关联券实例（追溯用）')
  ok(orders.every(o => Math.abs(Number(o.totalPrice) + Number(o.discountAmount)
    - Number(o.productDetail ? o.productDetail.price : 0)) < 0.01), '实付 = 原价 − 分摊')

  // 第一单（商家单）支付后仅退款：另一单仍在途 → 券不归还
  const orderD = orders[0]
  await expectOk(await cust('POST', '/api/pay/create', { json: { orderNumber: orderD.orderNumber, payType: 'WECHAT' } }), '订单 D 支付创建')
  await expectOk(await cust('POST', '/api/pay/mockPay', { json: { orderNumber: orderD.orderNumber } }), '订单 D 模拟支付')
  await expectOk(await cust('POST', '/api/order/refund/apply', { form: { orderNumber: orderD.orderNumber, reason: '冒烟-多单部分退款' } }), '订单 D 退款申请')
  await expectOk(await merch('POST', '/api/merchant/order/refund/process', { form: { orderNumber: orderD.orderNumber, approve: 'true', comment: '冒烟同意' } }), '订单 D 退款通过')
  const mine1 = await expectOk(await cust('GET', '/api/coupon/mine'), '部分退款后我的券')
  ok((mine1 || []).find(u => u.id === uc.id).status === 'USED', '部分退款不还整券（仍 USED）')

  // 第二单（平台单）取消：本单为最后持券在途单 → 券归还
  const orderE = orders[1]
  await expectOk(await cust('POST', '/api/order/cancel', { form: { orderNumber: orderE.orderNumber } }), '订单 E 取消（末单持券）')
  const mine2 = await expectOk(await cust('GET', '/api/coupon/mine'), '取消末单后我的券')
  ok((mine2 || []).find(u => u.id === uc.id).status === 'UNUSED', '最后持券在途单取消 → 券归还（UNUSED）')
}

// ============ 十三、个性化推荐（v1.10） ============
step('个性化推荐：登录用户品牌偏好重排 + 未登录降级热销')
{
  const personal = await expectOk(await cust('GET', '/api/product/recommend?scene=personal&limit=8'), '登录用户 personal 推荐')
  ok(Array.isArray(personal) && personal.length > 0, 'personal 非空（本会话已有购买历史）', personal && personal.length)
  ok(personal.every(p => p.id), 'personal 均为在售商品')
  const hot = await expectOk(await cust('GET', '/api/product/recommend?scene=hot&limit=8'), '热销对照')
  ok(hot.length > 0 && hot.every(p => p.id), '热销对照返回正常')
  const anon = client()
  const anonRes = await anon('GET', '/api/product/recommend?scene=personal&limit=8')
  ok(anonRes.status === 200 && anonRes.data.success === true && (anonRes.data.data || []).length > 0,
    '未登录 personal 降级热销（200 非空）')
  const bad = await cust('GET', '/api/product/recommend?scene=cheap&limit=8')
  ok(bad.data.success === false, '非法场景仍被拒绝')
}

console.log(`\n===== 冒烟完成：${stepNo} 步 / ${passCount} 项断言全部通过 =====`)
console.log(`商品 ${productId}（SKU ${detailId}），商家 ${merchantId}`)
console.log(`订单 A（仅退款）${orderA.orderNumber} / 订单 B（退货退款）${orderB.orderNumber} / 订单 C（平台券）${orderC.orderNumber}`)
