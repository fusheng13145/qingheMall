-- ============================================================
-- 青禾商城 P1-9 数据库关键索引补齐 迁移脚本 v2.2
-- 说明：对存量库执行；全新部署直接使用根目录 index.sql（已同步变更）
-- 变更内容：
--   1) order 表补 (status, gmt_created) 联合索引——超时关单/销售报表/状态聚合全表扫描优化
--   2) user 表补 user_name 唯一索引——登录/注册全表扫描优化 + 业务唯一性约束
--   3) product 表补 status/brand 索引——在售列表/品牌筛选优化
--   4) qinghe_payment_record 表补 (pay_status, gmt_created) 索引——支付对账优化
-- 幂等：全部通过 information_schema 判断，可重复执行
-- 注意：若存量 user 表存在重复 user_name，uk_user_name 建立会失败，需先人工去重
-- ============================================================

USE qinghedb;

-- 1. order.status + gmt_created 联合索引
SET @c1 = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='order' AND INDEX_NAME='idx_status_created');
SET @s1 = IF(@c1 = 0, 'ALTER TABLE `order` ADD INDEX idx_status_created (status, gmt_created)', 'SELECT 1');
PREPARE st1 FROM @s1; EXECUTE st1; DEALLOCATE PREPARE st1;

-- 2. user.user_name 唯一索引
SET @c2 = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='user' AND INDEX_NAME='uk_user_name');
SET @s2 = IF(@c2 = 0, 'ALTER TABLE `user` ADD UNIQUE INDEX uk_user_name (user_name)', 'SELECT 1');
PREPARE st2 FROM @s2; EXECUTE st2; DEALLOCATE PREPARE st2;

-- 3. product.status 索引
SET @c3 = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='product' AND INDEX_NAME='idx_status');
SET @s3 = IF(@c3 = 0, 'ALTER TABLE product ADD INDEX idx_status (status)', 'SELECT 1');
PREPARE st3 FROM @s3; EXECUTE st3; DEALLOCATE PREPARE st3;

-- 4. product.brand 索引
SET @c4 = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='product' AND INDEX_NAME='idx_brand');
SET @s4 = IF(@c4 = 0, 'ALTER TABLE product ADD INDEX idx_brand (brand)', 'SELECT 1');
PREPARE st4 FROM @s4; EXECUTE st4; DEALLOCATE PREPARE st4;

-- 5. qinghe_payment_record.pay_status + gmt_created 联合索引
SET @c5 = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='qinghe_payment_record' AND INDEX_NAME='idx_pay_status_created');
SET @s5 = IF(@c5 = 0, 'ALTER TABLE qinghe_payment_record ADD INDEX idx_pay_status_created (pay_status, gmt_created)', 'SELECT 1');
PREPARE st5 FROM @s5; EXECUTE st5; DEALLOCATE PREPARE st5;
