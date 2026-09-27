#!/usr/bin/env bash
# ============================================================
# 青禾商城 数据库恢复脚本（v1.10 方向 A：备份与恢复预案）
#
# 用法:
#   ./scripts/db-restore.sh <备份文件>              # 演练模式（默认）：恢复到独立演练库
#                                                   # qinghedb_restore_<ts> 并与源库逐表比对行数，
#                                                   # 结束后删除演练库——绝不触碰 qinghedb 本体
#   CONFIRM=yes ./scripts/db-restore.sh <备份文件> qinghedb
#                                                   # 真实恢复：显式指定目标库 + CONFIRM=yes 才执行
#
# 环境变量: DB_HOST / DB_PORT / DB_USER / DB_PASSWORD（同 db-backup.sh）
# ============================================================
set -euo pipefail

FILE="${1:?用法: db-restore.sh <备份文件> [目标库]（真实恢复须 CONFIRM=yes）}"
TARGET="${2:-}"
CONFIRM="${CONFIRM:-no}"

: "${DB_PASSWORD:?> 请通过环境变量 DB_PASSWORD 提供 MySQL 口令}"
MYSQL_CLIENT="mysql --default-character-set=utf8mb4 -h ${DB_HOST:-localhost} -P ${DB_PORT:-3306} -u ${DB_USER:-root} -p$DB_PASSWORD"
DUMP_CLIENT="mysqldump --single-transaction --routines --triggers --default-character-set=utf8mb4 -h ${DB_HOST:-localhost} -P ${DB_PORT:-3306} -u ${DB_USER:-root} -p$DB_PASSWORD"

[ -f "$FILE" ] || { echo "[restore] 备份文件不存在: $FILE"; exit 1; }

# ---------- 真实恢复模式 ----------
if [ -n "$TARGET" ]; then
  if [ "$CONFIRM" != "yes" ]; then
    echo "[restore] 拒绝执行：真实恢复会覆盖目标库 $TARGET 的全部数据。"
    echo "[restore] 如确认无误，请以 CONFIRM=yes 重跑。演练模式请省略目标库参数。"
    exit 1
  fi
  echo "[restore] 真实恢复 $FILE -> $TARGET ..."
  $MYSQL_CLIENT -e "CREATE DATABASE IF NOT EXISTS \`$TARGET\` DEFAULT CHARSET utf8mb4"
  $MYSQL_CLIENT "$TARGET" < "$FILE"
  echo "[restore] 完成。请重启应用并抽查关键业务数据。"
  exit 0
fi

# ---------- 演练模式（默认） ----------
TS="$(date +%Y%m%d_%H%M%S)"
DRILL_DB="qinghedb_restore_$TS"
echo "[drill] 演练恢复 $FILE -> $DRILL_DB（结束后删除演练库，不影响 qinghedb）"
cleanup() { $MYSQL_CLIENT -e "DROP DATABASE IF EXISTS \`$DRILL_DB\`" >/dev/null 2>&1 || true; }
trap cleanup EXIT

$MYSQL_CLIENT -e "CREATE DATABASE \`$DRILL_DB\` DEFAULT CHARSET utf8mb4"
$MYSQL_CLIENT "$DRILL_DB" < "$FILE"

# 逐表比对源库与演练库行数
TABLES=$($MYSQL_CLIENT -N -e "SELECT table_name FROM information_schema.tables WHERE table_schema='qinghedb' ORDER BY table_name")
FAIL=0
for t in $TABLES; do
  t=$(echo "$t" | tr -d '')   # Windows 客户端输出含 CR，剥除防表名污染
  SRC=$($MYSQL_CLIENT -N -e "SELECT COUNT(*) FROM \`qinghedb\`.\`$t\`")
  DST=$($MYSQL_CLIENT -N -e "SELECT COUNT(*) FROM \`$DRILL_DB\`.\`$t\`")
  if [ "$SRC" = "$DST" ]; then
    echo "[drill] ✓ $t: $SRC"
  else
    echo "[drill] ✗ $t: source=$SRC restored=$DST"
    FAIL=1
  fi
done

if [ "$FAIL" = "0" ]; then
  echo "[drill] 恢复演练通过：备份文件可用，全部表行数与源库一致。"
else
  echo "[drill] 恢复演练失败：存在行数不一致的表，请检查备份文件。"
  exit 1
fi
