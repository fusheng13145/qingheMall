# DEVELOPMENT.md — 项目开发与发布流程

> 派生视图（权威源：[青禾商城手册.md](../青禾商城手册.md) v8.0 §2/§7/§8；协作规范见 [AGENTS.md](../AGENTS.md)）。

## 1. 环境准备

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| JDK / Maven | 17+ / 3.6+ | 后端构建 |
| Node | 20+ | 前端构建与测试 |
| MySQL 8 / Redis | 8 / 7.x（本地 7.4.10 对齐生产） | 本机服务或 Docker |

数据库初始化（二选一，双路径不可混用）：
- 全新库：`mysql -u root -p --default-character-set=utf8mb4 < backend/index.sql`（21 表 + 种子，管理员 `admin/123456`）。
- 存量库升级：依次执行 `backend/sql/migration/v*.sql`（幂等）。

## 2. 日常开发循环

```bash
# 后端（dev profile：MySQL root/1234 @ qinghedb，Redis 6379）
cd backend && mvn spring-boot:run          # http://localhost:8080，/actuator/health 验 UP

# 前端
cd frontend && npm install && npm run dev  # http://localhost:5173，/api /uploads 代理 8080

# 测试三连（提交前必须全绿）
cd backend  && mvn test                    # 1109 例 + JaCoCo 双门禁
cd frontend && npm run test && npm run lint && npm run build

# 全链路（后端运行时）
cd frontend && npm run e2e                 # Playwright 桌面 2 + 移动 1
node scripts/api-smoke.mjs                 # 三角色 25 步/161 断言
```

接口直连调试注意：带 `Origin: http://localhost:8080` 头（CSRF 白名单）；未配支付凭证自动回退 MOCK 通道。

## 3. 迭代交付流程（每轮固定动作）

1. **开工**：读手册第 15 章（待完成项唯一清单）与 §11.7 作战地图，选定本迭代范围。
2. **实现**：按 [AGENTS.md](../AGENTS.md) 技术约定 + [COMPONENT-GUIDELINES.md](COMPONENT-GUIDELINES.md) 编码；数据库变更 = 新增幂等迁移 `v2.x_*.sql` + 同步 `index.sql`。
3. **验证**：四件套 + 全链路（见 §1 命令）；覆盖率门禁余量不低于 30 分支。
4. **文档**：手册对应章节更新 + 附录 C 追加一条变更注 + 第 15 章增删待办 + 同步 docs/ 派生视图与本 TODO。
5. **发布**：提交（`feat(v1.x): …`）→ `git tag v1.x && git push origin main v1.x`。main 推送由 CD 构建并推送 GHCR 镜像。

## 4. CI 流水线（GitHub Actions，push/PR 触发）

| Job | 内容 | 门禁 |
| --- | --- | --- |
| backend | MySQL/Redis service → 初始化库 → `mvn test` → package | JaCoCo 行/分支双 0.85 |
| frontend | audit(high) → lint → vitest（覆盖率门禁）→ build | statements/lines ≥85、branches ≥80、functions ≥75 |
| e2e | MySQL/Redis service → 起后端 jar → Playwright（桌面 2 + 移动 1） | 3/3 通过（CD 前置） |
| 安全扫描 | OWASP 依赖扫描 | — |
| cd | main 双绿后构建并推送 GHCR 镜像 | — |

## 5. 版本与发布约定

- 版本号 = git 标签（v1.x）；**先合入并验证、后打标签、再推送**（含标签）。
- 数据库变更随版本发布：存量库发布时按序重放迁移（全部幂等，可安全重跑）。
- 主键/分区等大迁移须有「两步发布 + 回滚预案」（参考 v1.6 雪花迁移：应用先行兼容 → 停机窗口执行迁移 → 恢复备份回滚路径已演练）。

## 6. 质量红线（历史教训固化）

- 单测全绿 ≠ 全链路通：每轮必跑 e2e + 冒烟（v1.7 实证）。
- 新增协作方法必须回扫既有测试桩（v1.8 实证）。
- 「移动端可用」必须有可验证口径（v1.9 移动 e2e 实证）。
- 覆盖率门禁余量低于 30 分支时，先补测后开新功能（v1.10 复盘红线）。
