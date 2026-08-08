-- ============================================================
-- 青禾商城 M3 体验与工程化 数据库迁移脚本 v1.4
-- 说明：对存量库执行；全新部署直接使用根目录 index.sql（已含变更）
-- 变更内容：1) product 表新增 brand 品牌列并回填；2) 金额字段 DOUBLE → DECIMAL(10,2)
-- 幂等：可重复执行
-- ============================================================

USE qinghedb;

-- 1. product.brand 品牌列（缺失时补齐，幂等）
SET @has_brand = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='product' AND COLUMN_NAME='brand');
SET @sql1 = IF(@has_brand = 0, 'ALTER TABLE product ADD COLUMN brand VARCHAR(32) DEFAULT NULL COMMENT ''品牌'' AFTER `name`', 'SELECT 1');
PREPARE stmt1 FROM @sql1; EXECUTE stmt1; DEALLOCATE PREPARE stmt1;

-- 2. 按商品名回填品牌（幂等：仅填充仍为空的品牌）
UPDATE product SET brand = 'Nike'          WHERE brand IS NULL AND name LIKE 'Nike%';
UPDATE product SET brand = 'Adidas'        WHERE brand IS NULL AND name LIKE 'Adidas%';
UPDATE product SET brand = 'New Balance'   WHERE brand IS NULL AND name LIKE 'New Balance%';
UPDATE product SET brand = 'Converse'      WHERE brand IS NULL AND name LIKE 'Converse%';
UPDATE product SET brand = 'Vans'          WHERE brand IS NULL AND name LIKE 'Vans%';
UPDATE product SET brand = 'Puma'          WHERE brand IS NULL AND name LIKE 'Puma%';
UPDATE product SET brand = 'Reebok'        WHERE brand IS NULL AND name LIKE 'Reebok%';
UPDATE product SET brand = 'Jordan'        WHERE brand IS NULL AND name LIKE 'Jordan%';
UPDATE product SET brand = '其他'          WHERE brand IS NULL;

-- 3. 金额字段 DOUBLE → DECIMAL(10,2)（幂等：仅当仍为 double 类型时变更）
--    product.price / product_detail.price / order.total_price / qinghe_payment_record.amount
SET @p1 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='product' AND COLUMN_NAME='price' AND DATA_TYPE='double');
SET @sql2 = IF(@p1 > 0, 'ALTER TABLE product MODIFY COLUMN price DECIMAL(10,2) NOT NULL COMMENT ''商品参考价格''', 'SELECT 1');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

SET @p2 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='product_detail' AND COLUMN_NAME='price' AND DATA_TYPE='double');
SET @sql3 = IF(@p2 > 0, 'ALTER TABLE product_detail MODIFY COLUMN price DECIMAL(10,2) NOT NULL COMMENT ''价格''', 'SELECT 1');
PREPARE stmt3 FROM @sql3; EXECUTE stmt3; DEALLOCATE PREPARE stmt3;

SET @p3 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='order' AND COLUMN_NAME='total_price' AND DATA_TYPE='double');
SET @sql4 = IF(@p3 > 0, 'ALTER TABLE `order` MODIFY COLUMN total_price DECIMAL(10,2) NOT NULL COMMENT ''订单总价格（单价×数量）''', 'SELECT 1');
PREPARE stmt4 FROM @sql4; EXECUTE stmt4; DEALLOCATE PREPARE stmt4;

SET @p4 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='qinghe_payment_record' AND COLUMN_NAME='amount' AND DATA_TYPE='double');
SET @sql5 = IF(@p4 > 0, 'ALTER TABLE qinghe_payment_record MODIFY COLUMN amount DECIMAL(10,2) NOT NULL COMMENT ''支付金额''', 'SELECT 1');
PREPARE stmt5 FROM @sql5; EXECUTE stmt5; DEALLOCATE PREPARE stmt5;
