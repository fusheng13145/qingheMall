-- 青禾商城平台化：入驻商家子系统（M6，v2.0）
-- 变更内容：
--   1) 新建 merchant 表（入驻商家：店铺资料 + 审核状态机 PENDING/ACTIVE/REJECTED/DISABLED）
--   2) product 增加 merchant_id（归属商家，NULL=平台自营）+ status（上下架 ON/OFF，默认 ON）
--   3) order 增加冗余 merchant_id（按商品归属回填存量）+ 索引
-- 全部幂等，可重复执行。

USE qinghedb;

-- ========== 1. merchant 表 ==========
CREATE TABLE IF NOT EXISTS `merchant` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `user_id` bigint NOT NULL COMMENT '店主用户ID（1:1）',
    `shop_name` varchar(64) NOT NULL COMMENT '店铺名称',
    `shop_logo` varchar(200) DEFAULT NULL COMMENT '店铺Logo',
    `shop_desc` varchar(255) DEFAULT NULL COMMENT '店铺简介',
    `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待审/ACTIVE正常/REJECTED驳回/DISABLED禁用',
    `reject_reason` varchar(255) DEFAULT NULL COMMENT '驳回原因',
    `gmt_created` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入驻商家表';

-- ========== 2. product.merchant_id（NULL=平台自营） ==========
SET @has_pm = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='product' AND COLUMN_NAME='merchant_id');
SET @sql1 = IF(@has_pm=0,
    'ALTER TABLE product ADD COLUMN merchant_id bigint DEFAULT NULL COMMENT ''归属商家ID(NULL=平台自营)'' AFTER brand',
    'SELECT 1');
PREPARE s1 FROM @sql1; EXECUTE s1; DEALLOCATE PREPARE s1;

-- ========== 3. product.status（上下架，默认在售） ==========
SET @has_ps = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='product' AND COLUMN_NAME='status');
SET @sql2 = IF(@has_ps=0,
    'ALTER TABLE product ADD COLUMN status varchar(16) NOT NULL DEFAULT ''ON'' COMMENT ''上架状态：ON在售/OFF下架'' AFTER merchant_id',
    'SELECT 1');
PREPARE s2 FROM @sql2; EXECUTE s2; DEALLOCATE PREPARE s2;

-- product 索引
SET @has_pm_idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='product' AND INDEX_NAME='idx_merchant_id');
SET @sql3 = IF(@has_pm_idx=0, 'ALTER TABLE product ADD KEY idx_merchant_id (merchant_id)', 'SELECT 1');
PREPARE s3 FROM @sql3; EXECUTE s3; DEALLOCATE PREPARE s3;

-- ========== 4. order.merchant_id（冗余，便于商家订单查询/结算） ==========
SET @has_om = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='order' AND COLUMN_NAME='merchant_id');
SET @sql4 = IF(@has_om=0,
    'ALTER TABLE `order` ADD COLUMN merchant_id bigint DEFAULT NULL COMMENT ''归属商家ID(NULL=平台自营)'' AFTER user_id',
    'SELECT 1');
PREPARE s4 FROM @sql4; EXECUTE s4; DEALLOCATE PREPARE s4;

SET @has_om_idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='order' AND INDEX_NAME='idx_merchant_id');
SET @sql5 = IF(@has_om_idx=0, 'ALTER TABLE `order` ADD KEY idx_merchant_id (merchant_id)', 'SELECT 1');
PREPARE s5 FROM @sql5; EXECUTE s5; DEALLOCATE PREPARE s5;

-- ========== 5. 存量回填：order.merchant_id 由 product 推导（平台自营为 NULL） ==========
UPDATE `order` o
JOIN product_detail pd ON o.product_detail_id = pd.id
JOIN product p ON pd.product_id = p.id
SET o.merchant_id = p.merchant_id
WHERE o.merchant_id IS NULL;
