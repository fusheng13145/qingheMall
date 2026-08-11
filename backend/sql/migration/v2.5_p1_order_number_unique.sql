-- ============================================================
-- 青禾商城 P1-5 order_number 唯一约束 迁移脚本 v2.5
-- 说明：对存量库执行；全新部署直接使用根目录 index.sql（已同步变更）
-- 背景：订单号由「时间戳 + Redis 自增 seq % 10000」拼接，seq 回绕到同一毫秒段
--       理论可碰撞；原 order 表仅普通索引 idx_order_number，无唯一性兜底。
-- 变更内容：
--   1) order 表 order_number 增加唯一索引 uk_order_number（数据库层防碰撞兜底）
--   2) 删除冗余普通索引 idx_order_number（uk 左前缀已覆盖其查询能力，减少写放大）
-- 幂等：全部通过 information_schema 判断，可重复执行
-- 注意：若存量 order 表存在重复 order_number，uk 建立会失败，需先人工核查去重：
--       SELECT order_number, COUNT(*) FROM `order` GROUP BY order_number HAVING COUNT(*)>1;
-- ============================================================

USE qinghedb;

-- 1. order.order_number 唯一索引
SET @c1 = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='order' AND INDEX_NAME='uk_order_number');
SET @s1 = IF(@c1 = 0, 'ALTER TABLE `order` ADD UNIQUE INDEX uk_order_number (order_number)', 'SELECT 1');
PREPARE st1 FROM @s1; EXECUTE st1; DEALLOCATE PREPARE st1;

-- 2. 删除冗余普通索引（仅当唯一索引已就位时才删，避免中间态无索引可用）
SET @uk  = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='order' AND INDEX_NAME='uk_order_number');
SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='order' AND INDEX_NAME='idx_order_number');
SET @s2 = IF(@uk > 0 AND @idx > 0, 'ALTER TABLE `order` DROP INDEX idx_order_number', 'SELECT 1');
PREPARE st2 FROM @s2; EXECUTE st2; DEALLOCATE PREPARE st2;
