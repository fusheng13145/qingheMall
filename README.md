# 青禾商城（qingheMall）

> 一个定位为**全品类一站式综合购物平台**的全栈电商实战项目，覆盖数码家电、服饰美妆、食品生鲜、家居百货等日常消费场景。前后端分离架构，已打通「浏览商品 → 下单 → 支付 → 订单管理 → 商家入驻 → 后台管理」完整业务闭环。

## 项目简介

青禾商城是一个基于 **Spring Boot + Vue 3** 的**平台型综合性电商**系统实战项目：平台 + 入驻商家 + 顾客三方结构，商品覆盖全品类（种子数据已含数码、个护、服饰、家居、食品、美妆与潮鞋等）。项目采用前后端完全分离的开发模式：

- **后端**：Spring Boot 2.7（Java 8）+ MyBatis + MySQL，提供 RESTful API 与 Session 鉴权
- **前端**：Vue 3（Composition API）+ Vite + Pinia + Vue Router，含亮/暗双主题与响应式布局

系统包含**用户端**（浏览、下单、支付、订单查询）与**管理端**（数据看板、商品/订单/用户管理）两套界面，覆盖电商核心业务链路。

## 技术栈

| 层次 | 技术 | 说明 |
| --- | --- | --- |
| 后端框架 | Spring Boot 2.7.18 | Web 服务 |
| ORM | MyBatis 2.3.1 + PageHelper 1.4.7 | XML 映射 + 分页插件 |
| 连接池 | Druid 1.2.16 | 含 SQL 监控台（/druid） |
| 分布式组件 | Redisson 3.17.0 | 分布式锁（防超卖）、订单号原子生成 |
| 数据库 | MySQL 8.0 | 库名 `qinghedb`，utf8mb4 |
| 前端框架 | Vue 3.3 + Vite 4 | Composition API + SFC |
| 状态管理 | Pinia 2.1 | 用户态、主题态 |
| 路由 | Vue Router 4 | 路由守卫鉴权 |
| HTTP | Axios 1.5 | 统一请求封装 + 拦截器 |

## 功能清单

### 用户端

- [x] 用户注册 / 登录 / 登出（BCrypt 密码存储，Session 会话）
- [x] 首页：品牌 Hero、服务特性、热门商品（真实接口）
- [x] 商品列表：分页浏览、**关键词搜索、品牌筛选、销量/价格/上新排序**（真实接口）
- [x] 商品详情：多图轮播、规格（尺码）选择、库存展示、售罄禁用（真实接口）
- [x] 购物车：加入购物车、数量增减、勾选/全选、删除、合计
- [x] 结算：商品清单 + 收货地址（收货人/电话/地址）+ 批量下单，成功后清空购物车
- [x] 下单：选规格 → 创建订单（Redisson 分布式锁扣库存）→ 收银台支付
- [x] 收银台：支付宝扫码支付（沙箱可测，个人可申请）/ 微信扫码支付（接口已保留）/ 模拟支付回退（渠道未配置时自动切换）
- [x] 支付回调：微信异步通知验签（RSA-SHA256 + AES-GCM 解密）+ 幂等落库
- [x] 订单中心：按状态 Tab 筛选、**分页加载**、取消订单（回滚库存）、订单详情弹窗
- [x] **商品评价：已支付订单可评价（1-5 星 + 内容，每单一次），商品详情页展示评分汇总与评价列表（分页）**
- [x] 超时自动关单：超时未付订单（默认 30 分钟）自动关闭并回滚库存
- [x] **库存流水留痕：下单扣减 / 取消回滚 / 超时回滚均落 stock_log，可追溯对账**
- [x] 支付成功页（支付成功后商品销量自动累加）
- [x] 个人中心：昵称/头像资料编辑、收货地址管理（默认地址自动带入结算）
- [x] 商品图片本地上传（管理端上传 → `/uploads/**` 静态访问）
- [x] 亮 / 暗 / 跟随系统 三态主题切换（localStorage 持久化）

### 管理端（需 ADMIN 角色）

- [x] 数据看板：商品数、订单数、用户数、总收入统计（SQL 聚合）
- [x] 商品管理：新增 / 编辑 / 删除商品（支持图片上传）
- [x] 订单管理：全量订单列表、状态流转
- [x] 用户管理：用户列表、角色调整（USER ↔ ADMIN）

### 工程化

- [x] 后端单元测试：下单并发不超卖、注册登录、限流切面、订单创建/取消状态机与库存回滚、购物车、地址（56 用例，7 个测试类）
- [x] 前端 Vitest 单元测试（17 用例：格式化工具 + 组件冒烟）+ ESLint/Prettier 规范（0 error）
- [x] 全局统一异常处理（@RestControllerAdvice → Result<T>）
- [x] 接口限流防刷：登录/注册（5 次/秒）、下单/支付（20 次/秒），Redisson 分布式令牌桶，超限返回 code 429
- [x] 订单超时关单：Redisson 延迟队列（下单即入队、到期自动关闭）+ 5 分钟轮询兜底
- [x] 监控：Actuator + Prometheus 指标（`/actuator/prometheus`，含 QPS/耗时/错误率/JVM）+ traceId 链路追踪（响应头与日志关联）
- [x] 管理端销售报表：近 7/30 天按日聚合销售额与订单数（SQL）
- [x] 多实例部署：Spring Session Redis 共享会话（SESSION Cookie）
- [x] 金额字段 DECIMAL(10,2)，无浮点误差
- [x] Docker Compose 一键部署（含 Prometheus/Grafana）+ GitHub Actions CI 流水线
- [x] 部署文档（详见 [docs/青禾商城手册.md](docs/青禾商城手册.md) §7 部署与运维）

### 未完成 / 待完善（详见 [docs/青禾商城手册.md](docs/青禾商城手册.md) §9.2 后续演进）

- [ ] 真实支付启用：配齐支付宝沙箱凭证（个人可申请）或微信商户凭证（需营业执照），接入步骤见 [docs/青禾商城手册.md](docs/青禾商城手册.md) §6；未配置时收银台走模拟支付
- [ ] 已发货订单退货退款（逆向物流状态）、商家货款结算/平台佣金分账
- [ ] ES 全文搜索（数据量上来后）、Redis 商品缓存、消息队列削峰、日志聚合

## 快速启动

### 环境要求

| 依赖 | 版本要求 |
| --- | --- |
| JDK | 1.8+ |
| Maven | 3.6+（或使用项目自带 `mvnw`） |
| Node.js | 16+ |
| MySQL | 8.0 |
| Redis | 5.0+（Redisson 依赖，默认 `127.0.0.1:6379`） |

### 1. 初始化数据库

```bash
mysql -u root -p < backend/index.sql
```

脚本会自动创建 `qinghedb` 库、5 张业务表，并写入 8 个示例商品与 38 条规格数据。

### 2. 启动后端

```bash
cd backend
# 数据库账号密码在 src/main/resources/application-dev.properties（或环境变量）中配置，按需修改
mvn spring-boot:run
# 或使用自带 wrapper
./mvnw spring-boot:run
```

后端默认运行于 `http://localhost:8080`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行于 `http://localhost:5173`，已配置 `/api` 代理转发到 `8080` 端口。

### 4. 管理员账号

初始化脚本自带种子管理员账号，可直接登录管理后台：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `123456` | ADMIN |

> 存量库升级：执行 `backend/sql/migration/v1.2_m1_seed_admin.sql`（幂等）即可补齐 `role` 列并创建 admin 账号。
> 新注册的普通用户角色为 `USER`，可在管理端「用户管理」中提升为 `ADMIN`。

## 项目结构

```
qingheMall
├── backend                          # 后端服务（Spring Boot）
│   ├── index.sql                    # 数据库初始化脚本（建库建表 + 示例数据）
│   ├── pom.xml                      # Maven 依赖与构建配置
│   └── src/main
│       ├── java/com/qinghe/mall
│       │   ├── Application.java     # 启动类
│       │   ├── config/CorsConfig.java   # 跨域配置
│       │   ├── controller/          # 接口层：User/Product/Order/Pay/Admin
│       │   ├── service/             # 业务层：接口 + impl 实现
│       │   ├── dao/                 # MyBatis DAO 接口
│       │   ├── dataobject/          # 数据库实体
│       │   ├── model/               # 业务模型（Result/Paging/Order 等）
│       │   ├── param/               # 请求参数封装
│       │   └── util/                # 工具类（MD5/UUID）
│       └── resources
│           ├── application.properties       # 应用配置
│           └── com/qinghe/mall/dao/*.xml    # MyBatis 映射文件
└── frontend                         # 前端应用（Vue 3）
    └── src
        ├── api/                     # 接口封装（user/product/order/payment/admin）
        ├── components/              # 公共组件（Header/Footer/ProductCard）
        ├── router/                  # 路由 + 鉴权守卫
        ├── stores/                  # Pinia 状态（user/theme）
        ├── utils/request.js         # Axios 封装
        └── views/                   # 页面（含 admin/ 管理端）
```

## 数据库设计

| 表名 | 说明 | 关键字段 |
| --- | --- | --- |
| `user` | 用户表 | `user_name`、`pwd`（BCrypt）、`role`（USER/ADMIN）、`nick_name`、`avatar` |
| `product` | 商品表 | `name`、`brand`、`price`（DECIMAL）、`purchase_num`（销量）、`product_imgs`（多图，分号分隔） |
| `product_detail` | 商品规格表 | `product_id`、`size`（尺码）、`stock`（库存） |
| `cart` | 购物车表 | `user_id`、`product_detail_id`、`quantity`、`selected` |
| `order` | 订单表 | `order_number`、`user_id`、`product_detail_id`、`quantity`、`total_price`、`status`、收货三字段 |
| `address` | 收货地址表 | `user_id`、`receiver_name/phone/address`、`is_default` |
| `comment` | 商品评价表 | `user_id`、`product_id`、`order_number`（唯一防重复）、`rating`（1-5）、`content` |
| `stock_log` | 库存流水表 | `product_detail_id`、`order_number`、`change_type`、`change_quantity`、`before/after_stock` |
| `qinghe_payment_record` | 支付流水表 | `order_number`、`amount`、`pay_type`、`pay_status` |

订单状态机：`WAIT_BUYER_PAY`（待付款）→ `TRADE_PAID_SUCCESS`（已付款）/ `TRADE_CLOSED`（已关闭）/ `TRADE_PAID_FAILED`（支付失败）。

## 文档索引

> 📖 **唯一文档源：[docs/青禾商城手册.md](docs/青禾商城手册.md)** —— 已整合项目总览、快速开始、数据模型、API 大全、核心设计、支付接入、部署运维、测试验收、路线图与已知限制。后续文档变更一律并入手册。
>
> 历史分散文档已归档至 [docs/archive/](docs/archive/)（仅作追溯，不再维护）。

## 已知限制

1. 支付为**模拟实现**（渠道代码已就绪，配置支付宝沙箱或微信商户凭证即启用真实支付），未配置时收银台走模拟支付。
2. 已发货订单不支持退款（退货流程未实现）；优惠券仅单品订单可用。
3. 商家货款结算/平台佣金分账、店铺主页（顾客视角）等平台化延伸未实施。
4. 生产增强项尚未接入：消息队列削峰、Redis 商品缓存、Elasticsearch 检索（详见手册 §9.2 后续演进）。

## License

本项目为学习实战项目，未指定开源协议，仅供学习交流使用。
