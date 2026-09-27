# REGISTRY.md — 构建产物、校验与分发说明

> 派生视图（权威源：[青禾商城手册.md](../青禾商城手册.md) v8.0 §7.7/§8）。本项目的「注册与分发」对象不是 npm 组件库，而是**四类受版本控制的制品**：后端 Jar、前端 dist、GHCR 容器镜像、契约快照。

## 1. 制品清单

| 制品 | 产生方式 | 存放/分发 | 校验 |
| --- | --- | --- | --- |
| 后端 Jar | `mvn package`（CI backend job） | GitHub Actions artifact `qinghe-mall-backend-jar`；CD 推送 GHCR 镜像 | `mvn test` 1109 例 + JaCoCo 行/分支双 0.85 门禁 |
| 前端 dist | `npm run build`（CI frontend job） | artifact `qinghe-mall-frontend-dist`；生产由 Nginx 容器挂载 | vitest 541 例 + lint + 覆盖率门禁 |
| 容器镜像 | CD（main 推送，双绿前置） | GHCR：`ghcr.io/<owner>/qinghe-mall-backend|frontend` | e2e 3/3 为 CD 前置门禁 |
| 契约快照 | `mvn test -Dtest=ApiContractSnapshotTest -Dcontract.update=true` | `backend/src/test/resources/api-contract-snapshot.json`（105 端点） | 每次全量测试自动比对，防端点漂移 |

## 2. 契约快照（API 注册表的等价物）

- 后端 15 个 Controller 的全部端点（方法 + 路径）被反射扫描并与快照逐条比对——**任何新增/删除/改动端点而未重建快照，CI 即红**。
- 端点有意变更后的流程：`mvn test -Dtest=ApiContractSnapshotTest -Dcontract.update=true` 重建 → 变更注记录 → 前端 `src/api/*` 同步。
- 当前口径：**105 端点**（v1.7 建 98 → v1.9 运营位 6 + recommend 1 → 105）。

## 3. 数据库 Schema 的注册与分发

- 全新库基线：`backend/index.sql`（21 表 + 种子，Docker 首次启动自动执行）。
- 存量库增量：`backend/sql/migration/v*.sql`（21 个，幂等可重放，按版本序执行）。
- 两者**双路径不可合并**（运维契约），schema 变更必须同步两侧。

## 4. 校验工具链

| 工具 | 校验对象 | 命令 |
| --- | --- | --- |
| mvn test + JaCoCo | 后端行为与覆盖率 | `cd backend && mvn test` |
| vitest + 覆盖率 | 前端行为与覆盖率 | `cd frontend && npm run test:coverage` |
| ESLint | 前端代码风格 | `npm run lint` |
| Playwright | 真浏览器全链路（桌面/移动） | `npm run e2e` |
| api-smoke.mjs | 三角色 25 步 API 全链路 | `node scripts/api-smoke.mjs`（需后端已启动） |
| npm audit | 依赖漏洞（high 级阻断） | CI 强制 |

## 5. 分发边界

- 镜像与制品仅分发至 GitHub（Actions artifact + GHCR）；未使用公共 npm 注册表（前端无组件库发布需求）。
- 版本化规则：镜像随 main CD 产出（latest）；可回溯版本 = git 标签 v1.x 对应的源码树 + 契约快照。
