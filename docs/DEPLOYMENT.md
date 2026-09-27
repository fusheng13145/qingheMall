# DEPLOYMENT.md — 项目构建与部署说明

> 派生视图（权威源：[青禾商城手册.md](../青禾商城手册.md) v8.0 §2/§7）。本项目部署目标是 **Docker Compose（9 服务）或裸机**；模板中的 Cloudflare 场景不适用，已按项目实际适配。

## 1. 构建产物

```bash
# 后端 Jar（Java 17）
cd backend && mvn -B package            # target/qinghe-mall-*.jar

# 前端 dist（Node 20+）
cd frontend && npm ci && npm run build  # dist/
```

## 2. Docker Compose 一键部署（推荐）

```bash
cp .env.example .env                    # 填写真实口令（DB/Redis/支付凭证/Vault 等）
docker compose up -d
```

9 服务拓扑：

| 服务 | 说明 |
| --- | --- |
| mysql | MySQL 8（库自动按 index.sql 初始化；数据卷 mysql-data） |
| redis | Redis 7（requirepass） |
| backend | Spring Boot（非 root UID 1001 + HEALTHCHECK；仅内网） |
| frontend | Nginx（静态 dist + /api /uploads 反代 + 可选 HTTPS；仅暴露 80/443） |
| elasticsearch | 8.15 单节点（商品检索） |
| loki | 日志聚合（后端 logback 直推） |
| prometheus / alertmanager / grafana | 指标、告警路由、看板 |

安全默认：MySQL/Redis 不映射宿主端口；backend 与监控面仅绑 127.0.0.1；口令全部走 `.env`（**必须覆盖默认值**）。

## 3. 裸机部署

1. 数据库：执行 `backend/index.sql`（全新库）或按序重放 `sql/migration/v*.sql`（存量库）。
2. 后端：`mvn package` 后 `java -jar target/qinghe-mall-*.jar --server.port=8080`（prod profile，环境变量见 `.env.example` 与手册 §7.4）。
3. 前端：`dist/` 交由 Nginx 托管（配置模板 `frontend/nginx.conf` / `nginx-ssl.conf`）。
4. 日志：结构化 JSON 落 `logs/` + Loki 直推（可选）。

## 4. HTTPS

- `SSL_ENABLED=true` 启用 443 + 80→443 强制跳转（缺证书 fail-closed）。
- 证书放置与 Nginx 细节见 `frontend/nginx-ssl.conf` 与手册 §7.5；域名/证书落定是当前待完成项 A4（手册第 15 章）。

## 5. 密钥管理

- dev：环境变量兜底。
- prod：`VAULT_*` 环境变量启用 HashiCorp Vault KV v2 取数（密钥不入仓/不入镜像）；生产端到端演练为待完成项 A2。

## 6. 备份与恢复（预案已演练）

```bash
DB_PASSWORD=*** ./scripts/db-backup.sh /data/qinghe/backups        # 一致性快照 + 7 份轮转
DB_PASSWORD=*** ./scripts/db-restore.sh <备份文件>                  # 演练：独立库逐表比对后清理
CONFIRM=yes DB_PASSWORD=*** ./scripts/db-restore.sh <备份文件> qinghedb   # 真实恢复（双确认）
```

- 定时：crontab 示例见脚本头注释（每日 03:00）。
- 上传目录另备：`tar -czf upload_backup_$(date +%F).tar.gz <upload 目录>`。
- v1.10 本机演练基线：21 表行数逐表一致。

## 7. 部署验证清单（上线后逐项）

1. `GET /actuator/health` → UP（ES 指示器 prod 默认开启，需 ES 可达）。
2. 前端首页可浏览、检索可用（ES 失败自动降级 MySQL 为预期行为）。
3. 顾客注册 → 下单 → 模拟支付 → 收货全链路（可用 `node scripts/api-smoke.mjs` 代跑）。
4. Grafana 看板有数据、Loki 可查 `app=qinghe-mall` 日志、Alertmanager 通知通道可达。
5. Vault 取数日志确认（A2 演练项）；HTTPS 跳转实测（A4 项）。
6. 备份任务首夜成功产出且演练通过。
