-- ============================================================
-- 青禾商城 M2 交易闭环 数据库迁移脚本 v1.3
-- 说明：对存量库执行；全新部署直接使用根目录 index.sql（已含变更）
-- 变更内容：1) 新增 cart 购物车表；2) order 表新增收货地址字段
-- 幂等：可重复执行
-- ============================================================

USE qinghedb;

-- 1. 购物车表（不存在时创建，幂等）
CREATE TABLE IF NOT EXISTS cart (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_detail_id VARCHAR(64) NOT NULL COMMENT '商品规格ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    selected TINYINT NOT NULL DEFAULT 1 COMMENT '是否勾选 1-是 0-否',
    gmt_created DATETIME NOT NULL COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_detail (user_id, product_detail_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 2. order 表收货地址字段（缺失时补齐，幂等）
SET @has_qty = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='order' AND COLUMN_NAME='quantity');
SET @sql0 = IF(@has_qty = 0, 'ALTER TABLE `order` ADD COLUMN `quantity` INT NOT NULL DEFAULT 1 COMMENT ''购买数量'' AFTER `product_detail_id`', 'SELECT 1');
PREPARE stmt0 FROM @sql0; EXECUTE stmt0; DEALLOCATE PREPARE stmt0;

SET @has_name = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='order' AND COLUMN_NAME='receiver_name');
SET @sql1 = IF(@has_name = 0, 'ALTER TABLE `order` ADD COLUMN `receiver_name` VARCHAR(32) DEFAULT NULL COMMENT ''收货人'' AFTER `status`', 'SELECT 1');
PREPARE stmt1 FROM @sql1; EXECUTE stmt1; DEALLOCATE PREPARE stmt1;

SET @has_phone = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='order' AND COLUMN_NAME='receiver_phone');
SET @sql2 = IF(@has_phone = 0, 'ALTER TABLE `order` ADD COLUMN `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT ''收货电话'' AFTER `receiver_name`', 'SELECT 1');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

SET @has_addr = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='order' AND COLUMN_NAME='receiver_address');
SET @sql3 = IF(@has_addr = 0, 'ALTER TABLE `order` ADD COLUMN `receiver_address` VARCHAR(255) DEFAULT NULL COMMENT ''收货地址'' AFTER `receiver_phone`', 'SELECT 1');
PREPARE stmt3 FROM @sql3; EXECUTE stmt3; DEALLOCATE PREPARE stmt3;
