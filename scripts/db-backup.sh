#!/usr/bin/env bash
# ============================================================
# 青禾商城 数据库备份脚本（v1.10 方向 A：备份与恢复预案）
#
# 用法:  ./scripts/db-backup.sh [备份目录]        （默认 ./backups）
# 环境变量:
#   DB_HOST / DB_PORT / DB_USER   连接信息（默认 localhost / 3306 / root）
#   DB_PASSWORD                   MySQL 口令（必填）
#   DB_BACKUP_KEEP                保留份数（默认 7，超出自动轮转删除）
#
# 说明:
#   - --single-transaction：InnoDB 一致性快照，备份期间不锁写
#   - 含 routines/triggers；库「qinghedb」为固定备份对象
#   - 生产 compose 部署时可在宿主机执行本脚本（MySQL 已映射 127.0.0.1:3306），
#     或容器内执行：docker exec mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" qinghedb' > 备份文件
#   - 定时（生产 crontab 示例，每日 03:00）：0 3 * * * cd /data/qinghe && DB_PASSWORD=*** ./scripts/db-backup.sh /data/qinghe/backups >> /data/qinghe/logs/backup.log 2>&1
# ============================================================
set -euo pipefail

BACKUP_DIR="${1:-./backups}"
KEEP="${DB_BACKUP_KEEP:-7}"
TS="$(date +%Y%m%d_%H%M%S)"
OUT="$BACKUP_DIR/qinghedb_$TS.sql"

: "${DB_PASSWORD:?> 请通过环境变量 DB_PASSWORD 提供 MySQL 口令}"

mkdir -p "$BACKUP_DIR"
mysqldump --single-transaction --routines --triggers --default-character-set=utf8mb4 \
  -h "${DB_HOST:-localhost}" -P "${DB_PORT:-3306}" -u "${DB_USER:-root}" -p"$DB_PASSWORD" \
  qinghedb > "$OUT"

SIZE=$(du -h "$OUT" | cut -f1)
LINES=$(wc -l < "$OUT")
echo "[backup] $OUT ($SIZE, $LINES lines)"

# 轮转：仅保留最近 KEEP 份
ls -1t "$BACKUP_DIR"/qinghedb_*.sql 2>/dev/null | tail -n +$((KEEP + 1)) | while read -r old; do
  rm -f "$old"
  echo "[rotate] removed $old"
done
echo "[backup] done, keeping latest $KEEP"
