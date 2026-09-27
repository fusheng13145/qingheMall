# ARCHITECTURE.md — 项目架构、目录结构与数据组织

> 派生视图（权威源：[青禾商城手册.md](../青禾商城手册.md) v8.0 §1.3/§3/§5/§7）。

## 1. 总体架构

```
浏览器（用户前台 /admin 商家 /merchant 管理）
   │  Nginx（静态 + /api /uploads 反代 + HTTPS 可选）
   ▼
Spring Boot 3.3.13（单体，模块化分层）
   ├─ Session 鉴权（Redis 共享）+ CSRF Origin 白名单 + per-user 限流（Redisson）
   ├─ 业务层：Controller(18) → Service(28+) → DAO(MyBatis XML)
   ├─ 数据层：MySQL 8（21 表，utf8mb4）        Redis（缓存/分布式锁/延迟队列/会话）
   ├─ 检索：Elasticsearch 8.15（双写 + 读降级 MySQL）
   └─ 可观测：Micrometer → Prometheus / Alertmanager / Grafana；logback → Loki
CI/CD：GitHub Actions（后端/前端/e2e/扫描 → GHCR 镜像）
```

单体但模块化：按域分包（user/order/product/coupon/seckill/merchant/settlement/refund/logistics/pay/admin/banner/shop），跨域读取收敛至协作服务（如 `UserAffinityService`）。

## 2. 目录结构

```
qingheMall
├── backend/
│   ├── index.sql                    # 全新库基线（21 表 + 种子）
│   ├── sql/migration/               # 存量库幂等迁移 v1.1~v2.11（21 个）
│   └── src/main/java/com/qinghe/mall/
│       ├── controller/   (18)       # REST 端点（105 个，契约快照钉死）
│       ├── service/ + impl/         # 业务逻辑（含事务模板/分布式锁）
│       ├── dao/ + resources/dao/    # MyBatis 接口与 XML
│       ├── dataobject/ model/       # DO（表映射）与领域模型
│       ├── config/                  # 22 类：Redisson/CORS/CSRF/限流/雪花/Vault/ES…
│       ├── elasticsearch/(4)        # ES 客户端/索引/文档/检索
│       └── exception/ util/
├── frontend/
│   ├── src/api/ (17 模块 + spec)    # 接口封装（薄，axios Result 包络）
│   ├── src/views/ (40 .vue)         # 前台 + admin/ + merchant/ 三端页面
│   ├── src/components/ (+common/)   # 6+3 公共组件（全 .vue 含 spec）
│   ├── src/stores/ utils/ router/ styles/
│   └── e2e/                         # Playwright 冒烟（桌面 + 移动双项目）
├── deploy/                          # Prometheus/Alertmanager/Grafana/Loki/SSL（10 文件）
├── scripts/                         # api-smoke.mjs / db-backup.sh / db-restore.sh
├── docs/                            # 本派生文档集
└── 青禾商城手册.md                   # 唯一权威源
```

## 3. 后端分层与关键机制

| 层 | 职责 | 关键机制 |
| --- | --- | --- |
| Controller | 参数校验 + 会话鉴权 + Result 包络 | 403/401 守卫；`@RateLimit` 注解限流 |
| Service | 业务规则与事务 | `TransactionTemplate` 锁内事务；Redisson 分布式锁（`order:lock:{sku}`、`seckill:user:*`） |
| DAO | MyBatis XML SQL | PageHelper 分页；聚合查询（品牌偏好/已购集合） |
| 数据 | MySQL + Redis | 订单雪花 BIGINT 主键 + 季度 RANGE 分区；库存 Cache-Aside（命中率指标） |

跨域约定：商品服务读取订单偏好经 `UserAffinityService`（协作服务），不允许直接注入他域 DAO。

## 4. 数据组织（21 表）

- **交易主链**：`user` / `product` + `product_detail` / `cart` / `order`（雪花 BIGINT + 季度分区）/ `qinghe_payment_record`。
- **营销**：`coupon` + `user_coupon` / `seckill_activity` + `seckill_order` / `home_banner`（运营位）。
- **商家与资金**：`merchant` / `settlement_ledger` + `settlement_bill`（EARN/REVERSAL 分账流水，Σ净额恒等）。
- **售后与内容**：`refund_request` / `logistics` + `logistics_trace` / `comment` + `comment_reply` / `address` / `stock_log`（库存流水）。

关键组织约定：
- **双路径迁移**：`index.sql`（全新库一次性基线）与 `sql/migration/v*.sql`（存量库幂等重放），**不可合并**。
- **金额**：DECIMAL(10,2)，分为最小运算单位（分摊算法以厘计整数化再回转）。
- **无外键**：一致性由 Service 层事务保证（学习项目约定，见手册 §10）。

## 5. 前端架构

- 请求层：axios 实例统一拦截——业务成功 resolve Result 包络、业务失败 reject Error（401 统一清登录态跳登录）。
- 状态：Pinia（user/cart/theme）；路由守卫 `await checkLogin()` 保证进页登录态就绪。
- 皮肤：三端 `data-skin` 令牌（见 [DESIGN.md](../DESIGN.md)）；移动端 ≤640px sheet 弹窗规范（`--sheet-*`）。
- 测试：组件/页面 63 个 spec 全覆盖；e2e 双视口（桌面 chromium + iPhone 视口）。

## 6. 架构决策记录（精选）

| 决策 | 理由 | 手册 |
| --- | --- | --- |
| 单体 + 模块化（不拆微服务） | 学习项目复杂度控制；MQ 拆分为备选项 | §9.3-B3 |
| 订单雪花 BIGINT + 季度分区 | 主键迁移与容量演进（v1.6 两步发布 + 回滚预案） | 附录 B-v2.8 |
| 分账「先重排后移已购」等金额语义 | 测试驱动澄清，恒等式单测钉死 | §5（v1.13） |
| 释放守卫（末单才还券） | 购物车级券部分退款不可整券归还 | v1.8 |
