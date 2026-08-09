-- ============================================================
-- 青禾商城 P2 数据库瘦身 迁移脚本 v2.4
-- 说明：对存量库执行；全新部署直接使用根目录 index.sql（已同步变更）
-- 变更内容：删除 cart 表冗余索引 idx_user_id
--          （uk_user_detail(user_id, product_detail_id) 左前缀已覆盖 user_id 查询，
--           冗余索引徒增写放大，无查询收益）
-- 幂等：通过 information_schema 判断，可重复执行
-- ============================================================

USE qinghedb;

SET @c = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='cart' AND INDEX_NAME='idx_user_id');
SET @s = IF(@c > 0, 'ALTER TABLE cart DROP INDEX idx_user_id', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
