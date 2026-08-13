-- 青禾商城 迁移脚本 v2.7（#39 商家端营销管理）
-- 为 coupon / seckill_activity 表加 merchant_id 归属列（NULL=平台级），幂等可执行。
-- 存量库手动执行；与 index.sql 最新结构一致。

-- ============ 1. coupon 表增量加 merchant_id ============
SET @exist_coupon_m := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'coupon' AND column_name = 'merchant_id');
SET @sql := IF(@exist_coupon_m = 0,
  'ALTER TABLE `coupon` ADD COLUMN `merchant_id` BIGINT DEFAULT NULL COMMENT ''归属商家ID(NULL=平台券)'' AFTER `status`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- coupon 表加商家索引（幂等：先查再建）
SET @exist_coupon_idx := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'coupon' AND index_name = 'idx_merchant_id');
SET @sql := IF(@exist_coupon_idx = 0,
  'ALTER TABLE `coupon` ADD KEY `idx_merchant_id` (`merchant_id`)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============ 2. seckill_activity 表增量加 merchant_id ============
SET @exist_sk_m := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'seckill_activity' AND column_name = 'merchant_id');
SET @sql := IF(@exist_sk_m = 0,
  'ALTER TABLE `seckill_activity` ADD COLUMN `merchant_id` BIGINT DEFAULT NULL COMMENT ''归属商家ID(NULL=平台秒杀)'' AFTER `status`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist_sk_idx := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'seckill_activity' AND index_name = 'idx_merchant_id');
SET @sql := IF(@exist_sk_idx = 0,
  'ALTER TABLE `seckill_activity` ADD KEY `idx_merchant_id` (`merchant_id`)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
