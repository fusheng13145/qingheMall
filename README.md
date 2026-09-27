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

JDK 17+、Maven 3.6+、Node 20+、MySQL 8、Redis 5+（Docker 20.10+ 可选）。

### 1. 初始化数据库

```bash
mysql -u root -p --default-character-set=utf8mb4 < backend/index.sql
```

脚本自动创建 `qinghedb` 库、21 张业务表，并写入 16 个示例商品与 48 条规格数据（含种子管理员 `admin/123456`）。

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
│   ├── index.sql                  # 数据库全量初始化脚本（建库建表 + 种子数据，21 表，order 为雪花 BIGINT 分区表）
│   ├── sql/migration/             # 存量库增量迁移脚本 v1.1~v2.10（幂等，20 个）
│   ├── src/main/java/com/qinghe/mall/   # controller / service / dao / model / config / util
│   ├── src/main/resources/        # application*.properties + MyBatis XML + seed-images
│   └── src/test/java/             # 单元测试（后端 1031 + 前端 520 用例，另含 API 契约快照与 Playwright e2e 冒烟，JaCoCo 行/分支双 ≥85% 门禁；订单主键为雪花 BIGINT 分区表）
├── frontend/                      # 前端应用（Vue 3 / Vite）
│   ├── src/api/                   # 接口封装（17 个模块）
│   ├── src/components/ stores/ styles/ utils/ router/   # 组件（含 common/ConsoleSidebar、LogisticsTimeline）/ Pinia / 三端皮肤令牌 / 工具 / 路由
│   ├── src/views/                 # 页面 33 个 .vue（前台 + admin/ + merchant/）；另含 components/ 6 个公共组件 + App.vue，前端共 40 个 .vue 文件，前端测试 520 用例
│   ├── vite.config.js  nginx.conf  nginx-ssl.conf  Dockerfile
│   └── package-lock.json          # 依赖锁定
├── deploy/                        # Prometheus / Alertmanager / Grafana / Loki 监控告警与 HTTPS 证书配置（10 文件，含 Grafana provisioning 告警三件套）
├── perf/                          # 容量压测资产：REPORT.md / perf_summary.md + locustfile*/verify* 脚本（11 文件，结论见手册 §11.4）
├── scripts/                       # 全链路 API 冒烟资产：api-smoke.mjs（三角色 23 步/135 断言，node scripts/api-smoke.mjs，需后端 8080 已启动）
├── .github/workflows/ci.yml       # CI/CD：后端 test+package / 前端 audit+lint+test+build / e2e 冒烟(Playwright) / OWASP 扫描 / main 推送 GHCR 镜像
├── docker-compose.yml             # 一键部署（9 服务：mysql/redis/backend/frontend/prometheus/alertmanager/grafana/elasticsearch/loki）
├── .env.example                   # 部署环境变量模板（复制为 .env 填写真实值）
└── 青禾商城手册.md                # ★ 唯一文档源（见下）
```

## 文档

> 📖 **唯一文档源：[青禾商城手册.md](青禾商城手册.md)**（**v7.6**，2026-09-27）—— 涵盖项目总览、目录说明、环境配置、快速开始、数据模型、API 大全、核心设计、支付接入、部署运维与上线流程、测试验收、版本路线图（含 **§9.3 阶段三迭代路线图：v1.4/v1.5/v1.6 已落地，阶段三主线收官** 与 **§9.4 阶段四路线图：上线验证与增长（v1.7 = E1 前端单测补齐 + D2 运营位/推荐位已落地，B1 随商户凭证）**）、**项目完成度评估与就绪度（第 11 章，含内联 SVG 可视化）**、专题设计（第 12 章）、常见问题 FAQ（第 13 章）、**源码研读全景（第 14 章，含 §14.9 v1.4~v1.6 增量文件索引）**、附录 A/B。**后续文档变更一律并入手册，不再另开独立文档。**
>
> ✅ **手册自包含（v6.9）**：原两份配套文档《青禾商城就绪评估_v6.7.html》《青禾商城项目研读总结.md》经 2026-09-09 时效性核验（基准 v6.7/代码 v1.3 与当前 HEAD 一致、业务代码零漂移）判定仍有效，其**全部内容已并入手册**——就绪评估（含九维度雷达、规划完成度雷达、模块落地率图）并入 **§11.5**，源码级研读（逐目录/逐文件 + 行号证据）并入 **第 14 章**；两份原件按项目惯例**整合废止**，归档于 `.workbuddy/backup/已整合废止_*`，仅供追溯、不再维护。

## 已知限制

详见[手册 §10 已知问题与限制](青禾商城手册.md)：真实支付需配置凭证（未配置回退模拟）、退款不对接真实支付网关原路退回（已发货订单支持退货退款审核，v5.7）、优惠券仅单品订单可用等。

## License

本项目为学习实战项目，未指定开源协议，仅供学习交流使用。
