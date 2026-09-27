# AGENTS.md — 项目协作与代码开发规范

> 适用范围：人类协作者与 AI 编码代理在本仓库的一切开发活动。
> 权威源：[青禾商城手册.md](青禾商城手册.md)（v8.0）。本文件为派生视图——规则细节以手册为准，冲突时先修手册再同步本文件。
> 代码基线：**v1.14**（后端 1109 + 前端 541 用例全绿；JaCoCo 行 92.49% / 分支 86.80% 双门禁）。

## 1. 质量铁律（每次交付必须全部满足）

1. **四件套**：代码 + 单测/契约 + 文档并入手册 + 全量回归绿。
2. **全链路验证**：四件套之外必须通过 e2e（桌面 2 + 移动 1）与三角色 API 冒烟（`node scripts/api-smoke.mjs`，需后端 8080 已启动）。历史教训：单测绿 ≠ 全链路通（v1.7 Home 取值层级缺陷由 e2e 拦截）。
3. **分支门禁余量**：改动新增分支必须同步补测，余量不得低于 30（当前 39；复盘 §11.7-P1）。
4. **验收命令**：`cd backend && mvn test`；`cd frontend && npm run test && npm run lint && npm run build`；e2e `npm run e2e`。

## 2. 分支、提交与里程碑

- 迭代直接在 `main` 开发（项目自 v1.4 起的既定节奏），提交信息格式：
  - 功能：`feat(v1.x): 主题——要点列表`
  - 修复：`fix(v1.x): 主题`
  - 纯测试：`test(v1.x): 主题`
  - 文档：`docs: 主题`
- 每个迭代收敛后**打标签并推送**（`git tag v1.x && git push origin main v1.x`，§7.7 铁律——v1.7~v1.10 曾漏打，复盘补齐）。
- 手册随每次迭代升版：功能迭代次版本 +1（v7.x），结构重编撰主版本 +1（v8.0）。

## 3. 文档治理

- **唯一权威源**：`青禾商城手册.md`。先改手册对应章节，再同步根级与 `docs/` 派生视图（AGENTS/DESIGN/CHANGELOG/TODO 与 docs/ 七件）。
- 历史变更注（v3.1~v7.14）归档于手册**附录 C**，其 § 编号为当时口径。
- 待完成/扩展项的唯一现行清单是手册**第 15 章**；不在正文其它位置新开待办。

## 4. 技术约定速查

| 主题 | 约定 | 详见 |
| --- | --- | --- |
| 后端 | Spring Boot 3.3.13 / Java 17 / MyBatis / jakarta；统一 `Result{success,code,message,data}` 包络 | 手册 §1.2/§4.0 |
| 前端 | Vue 3.3 `<script setup>` + Pinia + Vue Router；拦截器返回 Result 包络，业务数据在 `res.data` | 手册 §5.12 |
| 前端样式 | 三端皮肤令牌（`data-skin`），禁止硬编码角色色 | [DESIGN.md](DESIGN.md) |
| 组件 | 全部 .vue 必须有配套 `.spec.js`（63 文件全覆盖） | [docs/COMPONENT-GUIDELINES.md](docs/COMPONENT-GUIDELINES.md) |
| 数据库 | 21 表；迁移双路径：全新库 `index.sql`，存量库 `sql/migration/v*.sql`（幂等）；订单为雪花 BIGINT 季度分区表 | 手册 §3 |
| 鉴权 | Session；注册/登录为 form 语义，其余 JSON；CSRF 校验 Origin/Referer（API 工具直连需带 `Origin: http://localhost:8080`） | 手册 §4.0 |
| 支付 | 未配凭证自动回退 MOCK 通道（`/api/pay/mockPay`） | 手册 §6 |

## 5. 已知环境坑（Windows 本机）

- 打包前先杀 java 进程；`SERVER__PORT=0` 环境变量覆盖陷阱（须显式 `--server.port`）。
- 脚本写文件用明确 Windows 路径（托管 node 的 `/tmp` 解析不一致）。
- mysql 客户端输出含 `\r`，脚本消费需 `tr -d '\r'`。
- 改文件脚本必须「读全 → 校验 → 写临时 → 原子替换」（v1.8 曾截断冒烟脚本）。
- 构建产物/临时目录删除用 PowerShell `[System.IO.Directory]::Delete(path,$true)`（个人文件勿用）。

## 6. AI 协作补充约定

- 迭代开工先读手册**第 15 章**（待完成项唯一清单）与 §11.7 作战地图；不新开清单。
- 结构化代码追加（新增方法/类）禁手工拼串改写既有文件——用干净脚本或整文件重写。
- 每轮迭代收尾：手册变更注并入附录 C 一条 + 第 15 章增删待办 + 打标签推送。
