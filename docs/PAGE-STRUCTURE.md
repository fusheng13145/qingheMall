# PAGE-STRUCTURE.md — 页面结构与路由说明

> 派生视图（权威源：[青禾商城手册.md](../青禾商城手册.md) v8.0 §1.5/§4/§9-10）。路由定义：`frontend/src/router/index.js`（含登录态守卫与角色守卫）。

## 1. 路由总表

### 1.1 用户前台（AppHeader/Footer 布局，皮肤 `user`）

| 路由 | 页面（views/） | 结构要点 | 主要 API |
| --- | --- | --- | --- |
| `/` | Home.vue | Hero + 特性四宫格 + 运营位轮播（登录「为你推荐」/未登录「热销推荐」+ 新品上架双区块） | product/recommend, banner/list |
| `/products` | Products.vue | 筛选（品牌/关键字/排序）+ 商品分页网格 | product/page |
| `/product/:id` | ProductDetail.vue | 图集/规格选择/加购/立即购买 + 本店优惠区块 + 评价列表（含商家回复） | product/get, productdetail, coupon, comment |
| `/seckill` | Seckill.vue | 进行中活动卡片（倒计时/进度条）+ 详情抢购弹窗（sheet 化） | seckill/activities |
| `/coupons` | Coupons.vue | 领券中心 | coupon/list, coupon/claim |
| `/my-coupons` | MyCoupons.vue | 我的券（状态页签） | coupon/mine |
| `/cart` | Cart.vue | 勾选/数量/结算入口 | cart/* |
| `/checkout` | Checkout.vue | 收货信息 + 优惠券选择（购物车级）+ 提交 | order/batchAdd, address, coupon/available |
| `/pay` `/pay-success` | Pay.vue / PaySuccess.vue | 支付发起（通道解析/模拟）+ 结果页 | pay/create, pay/mockPay, pay/query |
| `/orders` | Orders.vue | 订单列表（状态页签）+ 详情/取消/支付/收货/评价/退款弹窗（sheet 化） | order/*, refund, logistics |
| `/shop/:merchantId` | Shop.vue | 店铺主页：信息/评分销量 + 商品分页 + 店铺券/秒杀 | shop/{id} |
| `/profile` | Profile.vue | 资料/地址簿/我要开店（申请弹窗） | user, merchant/apply |
| `/login` `/register` | Login / Register | 表单语义（URLSearchParams）；注册含角色切换与店名 | user/login, user/reg |
| `/:pathMatch(.*)*` | NotFound.vue | 404 兜底（双出口） | — |

### 1.2 管理后台（`/admin`，AdminLayout + 皮肤 `admin`）

| 子路由 | 页面 | 要点 |
| --- | --- | --- |
| `` | Dashboard.vue | 统计卡片与图表 |
| `products` | Products.vue | 商品治理（弹窗编辑，sheet 化） |
| `orders` | Orders.vue | 订单治理 + 发货 + 状态机改单（sheet 化） |
| `coupons` / `seckills` | Coupons.vue / Seckill.vue | 平台券/秒杀管理 |
| `merchants` | Merchants.vue | 商家审核 |
| `users` | Users.vue | 用户与角色（白名单校验） |
| `settlement` | Settlement.vue | 结算单生成/审核放款/明细 |
| `banners` | BannerManage.vue | 首页运营位 CRUD/上下架 |

### 1.3 商家工作台（`/merchant`，MerchantLayout + 皮肤 `merchant`）

| 子路由 | 页面 | 要点 |
| --- | --- | --- |
| `` | Overview.vue | 工作台概览 |
| `products` | Products.vue | 商品 + SKU 管理（saveWithDetails 事务整体替换） |
| `orders` | Orders.vue | 订单发货/物流推进/退款审核/评价回复（三弹窗 sheet 化） |
| `marketing` | Marketing.vue | 自建券/秒杀（双页签表单 + 列表） |
| `comments` | Comments.vue | 评价管理与回复（一对一防重） |
| `settlement` | Settlement.vue | 结算概览/流水/结算单 |

## 2. 组件结构说明

| 组件 | 职责 | 关键契约 |
| --- | --- | --- |
| AppHeader | 全局头部：logo + 7 导航图标 + 主题切换 + 认证区 | ≤640px 紧凑化（logo 仅图标/导航自横滑）；登录态显示用户 |
| AppFooter | 全局页脚 | 静态链接组 |
| ProductCard | 商品卡片（首页/列表/推荐复用） | props: product；点击跳详情 |
| common/ConsoleSidebar | 控制台侧栏容器 | props: title/sidebarOpen；emits: close；logo/nav 双插槽 |
| common/AppToast | 全局提示容器 | 消费 utils/toast 响应式队列；点击关闭 |
| common/LogisticsTimeline | 物流时间轴 | props: 轨迹列表 |

## 3. 布局与守卫规则

- 路由守卫：`await checkLogin()` 后按 `requiresAuth`（登录）与 `requiresRole`（ADMIN/MERCHANT）分流；未授权跳登录并带 `redirect`。
- 三端皮肤：App 根 `data-skin="user"`、AdminLayout `admin`、MerchantLayout `merchant`（令牌见 DESIGN.md）。
- 404：未知路由统一落 NotFound（唯一无 spec 缺口已补齐，当前全页面覆盖）。
