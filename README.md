# 青禾商城（qingheMall）

> 全品类一站式综合购物平台实战项目：平台运营 + 入驻商家 + 顾客三方结构，覆盖数码家电、服饰美妆、食品生鲜、家居百货等消费场景，已打通「浏览商品 → 下单 → 支付 → 订单管理 → 商家入驻 → 后台管理」完整业务闭环。

## 技术栈

| 层次 | 技术 | 说明 |
| --- | --- | --- |
| 后端 | **Spring Boot 3.3.13（Java 17）** + MyBatis 3.0.4 + PageHelper 2.1.0 + fastjson2 | RESTful API + Session 鉴权（jakarta） |
| 中间件 | MySQL 8（`qinghedb`，utf8mb4）+ Redis（Redisson 3.27.2 分布式锁/延迟队列 + Spring Session 共享） | 防超卖、订单号生成、多实例会话 |
| 前端 | Vue 3.3 + Vite 6 + Pinia + Vue Router + Axios | Composition API、亮/暗主题、响应式 |
| 工程化 | Docker Compose 一键部署（含 Prometheus/Grafana 监控）、GitHub Actions CI、JaCoCo 覆盖率门禁、Vitest + ESLint | |

## 快速启动（本地开发）

### 环境要求

JDK 17+、Maven 3.6+、Node 16+、MySQL 8、Redis 5+（Docker 20.10+ 可选）。

### 1. 初始化数据库

```bash
mysql -u root -p --default-character-set=utf8mb4 < backend/index.sql
```

脚本自动创建 `qinghedb` 库、14 张业务表，并写入 16 个示例商品与 48 条规格数据（含种子管理员 `admin/123456`）。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run            # 默认 dev profile，数据库口令见 src/main/resources/application-dev.properties
```

后端默认运行于 `http://localhost:8080`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev                    # http://localhost:5173，/api 与 /uploads 已代理至 8080
```

## 项目结构

```
qingheMall
├── backend/                       # 后端服务（Spring Boot / Maven）
│   ├── index.sql                  # 数据库全量初始化脚本（建库建表 + 种子数据，14 表）
│   ├── sql/migration/             # 存量库增量迁移脚本 v1.1~v2.4（幂等，14 个）
│   ├── src/main/java/com/qinghe/mall/   # controller / service / dao / model / config / util
│   ├── src/main/resources/        # application*.properties + MyBatis XML + seed-images
│   └── src/test/java/             # 单元测试（470 用例）
├── frontend/                      # 前端应用（Vue 3 / Vite）
│   ├── src/api/                   # 接口封装（12 个模块）
│   ├── src/components/ stores/ styles/ utils/ router/   # 组件（含 common/ConsoleSidebar）/ Pinia / 三端皮肤令牌 / 工具 / 路由
│   ├── src/views/                 # 页面（前台 + admin/ + merchant/）
│   ├── vite.config.js  nginx.conf  Dockerfile
│   └── package-lock.json          # 依赖锁定
├── deploy/                        # Prometheus / Grafana 监控配置
├── .github/workflows/ci.yml       # CI：后端 test+package / 前端 lint+test+build
├── docker-compose.yml             # 一键部署（mysql/redis/backend/frontend/prometheus/grafana）
├── .env.example                   # 部署环境变量模板（复制为 .env 填写真实值）
└── 青禾商城手册.md                # ★ 唯一文档源（见下）
```

## 文档

> 📖 **唯一文档源：[青禾商城手册.md](青禾商城手册.md)**（v4.6）—— 涵盖项目总览、目录说明、环境配置、快速开始、数据模型、API 大全、核心设计、支付接入、部署运维与上线流程、测试验收、版本路线图、已知问题与 FAQ。**后续文档变更一律并入手册，不再另开独立文档。**

## 已知限制

详见[手册 §10 已知问题与限制](青禾商城手册.md)：真实支付需配置凭证（未配置回退模拟）、已发货订单暂不支持退款、优惠券仅单品订单可用等。

## License

本项目为学习实战项目，未指定开源协议，仅供学习交流使用。
