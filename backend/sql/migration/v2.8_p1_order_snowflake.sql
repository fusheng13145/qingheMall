-- ============================================================
-- 青禾商城：订单 order.id  UUID → BIGINT 雪花 ID 迁移脚本（B2，v1.6 执行版）
-- ============================================================
-- 【状态】ACTIVE（v1.6 转正执行；原规划见 git 历史 planned/v2.8_p1_order_snowflake.sql）
-- 【执行条件】① 应用侧 T5 落地（OrderDO.id: String→Long + SnowflakeIdGenerator，本版本已合入）
--            ② 停机维护窗口（应用停写）
--            ③ 先备份：mysqldump -uroot -p qinghedb "order" > order_backup_$(date +%F).sql
-- 【幂等】可重复执行：order.id 已为 BIGINT 时全部步骤跳过。
-- 【与冻结规划的差异（执行细则修正）】
--   1) 回填方式由「应用侧批量 UPDATE 临时列」改为**纯 SQL 雪花回填**（窗口函数构造，
--      时间取行内 gmt_created、(worker,seq) 由全局行号分解），可回放、无需应用参与；
--      uk_id_new 唯一约束兜底，任何冲突即整句失败可发现。
--   2) 分区前置修正：MySQL 要求唯一键含分区键，故 uk_order_number 由
--      (order_number) 复合化为 (order_number, gmt_created)——order_number 生成含
--      时间戳+序列，跨日碰撞概率极低，且保留「同日不重」强约束（P1-5 兜底语义弱化，
--      见手册 §3.2）。
-- 【影响面】仅 order 表自身；关联表（qinghe_payment_record / comment / stock_log /
--        logistics / logistics_trace / refund_request / seckill_order）均经
--        order_number 关联，不引用 order.id。
-- 【回滚】见手册 §7.8 发布预案：停写 → DROP TABLE `order` → 从备份恢复旧表结构
--        与数据 → 回滚应用版本（OrderDO.id String 分支）。雪花 ID 与 UUID 无格式
--        依赖（关联全走 order_number），回滚后业务可继续。
-- ============================================================

-- 步骤 0：幂等守卫——order.id 已是 BIGINT 则跳过全部 DDL
SET @is_bigint := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'order'
      AND column_name = 'id' AND data_type IN ('bigint'));

-- 步骤 1：加临时列（未迁移时）
SET @sql := IF(@is_bigint = 0,
  'ALTER TABLE `order` ADD COLUMN `id_new` BIGINT UNSIGNED NULL COMMENT ''雪花ID临时列（迁移中）''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 步骤 2：纯 SQL 雪花回填（窗口函数；仅未迁移时执行）
--   结构：(gmt_created 毫秒 - EPOCH) << 22 | (rn % 1024) << 12 | ((rn DIV 1024) % 4096)
--   同毫秒内 (worker, seq) 组合在 1024*4096 = 4M 行内不重复，覆盖任何现实量级。
SET @sql := IF(@is_bigint = 0,
  'UPDATE `order` o
     JOIN (SELECT id, ROW_NUMBER() OVER (ORDER BY gmt_created, id) AS rn FROM `order`) t ON o.id = t.id
      SET o.id_new =
          ((CAST(UNIX_TIMESTAMP(o.gmt_created) AS UNSIGNED) * 1000 - 1767225600000) << 22)
          | ((t.rn % 1024) << 12)
          | ((t.rn DIV 1024) % 4096)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 步骤 3：唯一约束兜底（冲突即失败，回填质量门禁；未迁移时执行）
SET @sql := IF(@is_bigint = 0,
  'ALTER TABLE `order` ADD UNIQUE KEY `uk_id_new` (`id_new`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 步骤 4：切换主键（停写窗口内原子操作）
SET @sql := IF(@is_bigint = 0,
  'ALTER TABLE `order` DROP PRIMARY KEY',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@is_bigint = 0,
  'ALTER TABLE `order` CHANGE COLUMN `id` `id_old` varchar(64) NULL, CHANGE COLUMN `id_new` `id` BIGINT UNSIGNED NOT NULL COMMENT ''主键ID(雪花)''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@is_bigint = 0,
  'ALTER TABLE `order` ADD PRIMARY KEY (`id`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 步骤 5：uk_order_number 复合化（分区前置条件；幂等：唯一键最后一列已是 gmt_created 则跳过）
SET @uk_last_col := (SELECT column_name FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'order' AND index_name = 'uk_order_number'
    ORDER BY sequence DESC LIMIT 1);
SET @sql := IF(@uk_last_col IS NOT NULL AND @uk_last_col <> 'gmt_created',
  'ALTER TABLE `order` DROP INDEX `uk_order_number`, ADD UNIQUE KEY `uk_order_number` (`order_number`, `gmt_created`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 步骤 6a：主键复合化（分区前置条件：PK 必须含分区键；已是复合键则跳过）
SET @pk_last_col := (SELECT column_name FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'order' AND index_name = 'PRIMARY'
    ORDER BY sequence DESC LIMIT 1);
SET @sql := IF(@pk_last_col IS NOT NULL AND @pk_last_col <> 'gmt_created',
  'ALTER TABLE `order` DROP PRIMARY KEY, ADD PRIMARY KEY (`id`, `gmt_created`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 步骤 6b：按季度桶分区（幂等：已分区则跳过）
SET @is_partitioned := (SELECT COUNT(DISTINCT partition_name) FROM information_schema.partitions
    WHERE table_schema = DATABASE() AND table_name = 'order' AND partition_name IS NOT NULL);
SET @sql := IF(@is_partitioned = 0,
  'ALTER TABLE `order` PARTITION BY RANGE COLUMNS (gmt_created) (\
      PARTITION p_hist   VALUES LESS THAN (''2026-10-01''),\
      PARTITION p2026Q4  VALUES LESS THAN (''2027-01-01''),\
      PARTITION p2027Q1  VALUES LESS THAN (''2027-04-01''),\
      PARTITION p2027Q2  VALUES LESS THAN (''2027-07-01''),\
      PARTITION pmax     VALUES LESS THAN MAXVALUE\
   )',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 步骤 7：清理旧 UUID 列与临时唯一键（确认迁移成功后；id_old 已随 CHANGE 删除，此步兜底清理临时键）
SET @sql := IF(@is_bigint = 0,
  'ALTER TABLE `order` DROP INDEX `uk_id_new`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
