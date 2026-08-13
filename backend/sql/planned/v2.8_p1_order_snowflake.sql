-- ============================================================
-- 青禾商城：订单 order.id  UUID → BIGINT 雪花 ID 迁移脚本
-- ============================================================
-- 影响面：仅 order 表自身。所有关联表（qinghe_payment_record / comment /
--        stock_log / logistics / logistics_trace / refund_request /
--        seckill_order）均经 order_number 关联，不引用 order.id，故迁移
--        不影响关联表（详见 index.sql）。
-- 风险：高（改主键类型）。需停机窗口 + staging 全量回放演练。
-- 前置：先对 qinghedb 做物理备份。
-- ============================================================

-- 步骤 1：备份（运维执行，非本脚本）
--   mysqldump -uroot -p qinghedb `order` > order_backup_$(date +%F).sql

-- 步骤 2：应用层回填雪花 ID
--   说明：MySQL 无法在纯 SQL 中生成雪花 ID，须由应用侧 SnowflakeIdGenerator
--         批量 UPDATE 到临时列 id_new。在维护窗口、应用停写时执行：
--   for each row in `order`:
--       newId = snowflake.nextId();
--       UPDATE `order` SET id_new = newId WHERE id = <uuid>;
--   回填后务必校验：id_new 无 0、无重复（见步骤 3 唯一约束）。

-- 步骤 3：DDL 切换（停写状态下执行）
ALTER TABLE `order`
    ADD COLUMN `id_new` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '雪花ID临时列';
-- 回填完成后建立唯一约束，确保无冲突
ALTER TABLE `order` ADD UNIQUE KEY `uk_id_new` (`id_new`);

-- 步骤 4：替换为新主键
ALTER TABLE `order` DROP PRIMARY KEY;
ALTER TABLE `order` CHANGE COLUMN `id_new` `id` BIGINT UNSIGNED NOT NULL COMMENT '主键ID(雪花)';
ALTER TABLE `order` ADD PRIMARY KEY (`id`);

-- 步骤 5：按月分区
--   注意：MySQL 要求分区表的所有唯一索引必须包含分区键。
--   采用 (id, gmt_created) 复合主键 + RANGE COLUMNS(gmt_created) 满足约束。
ALTER TABLE `order` DROP PRIMARY KEY, ADD PRIMARY KEY (`id`, `gmt_created`);
ALTER TABLE `order` PARTITION BY RANGE COLUMNS (gmt_created) (
    PARTITION p2026Q1 VALUES LESS THAN ('2026-04-01'),
    PARTITION p2026Q2 VALUES LESS THAN ('2026-07-01'),
    PARTITION p2026Q3 VALUES LESS THAN ('2026-10-01'),
    PARTITION p2026Q4 VALUES LESS THAN ('2027-01-01'),
    PARTITION pmax     VALUES LESS THAN MAXVALUE
);

-- 步骤 6：Java 侧改造（见重评报告第十一章 T5 清单）
--   Order.id: String → Long；新增 SnowflakeIdGenerator；
--   写入订单处改生成雪花；所有 order.getId() 调用方类型随之调整；
--   前端若展示 order.id 需 Long 兼容；order_number 不变仍为业务主键。
