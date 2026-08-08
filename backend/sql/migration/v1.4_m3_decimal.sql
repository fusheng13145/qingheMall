-- ============================================================
-- 青禾商城 M3 质量与工程化 数据库迁移脚本 v1.4
-- 说明：对存量库执行；全新部署直接使用根目录 index.sql（已含变更）
-- 变更内容：金额字段 DOUBLE → DECIMAL(10,2)，消除浮点误差（M2-7）
-- 幂等：可重复执行
-- ============================================================

USE qinghedb;

ALTER TABLE `product`              MODIFY COLUMN `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品参考价格';
ALTER TABLE `product_detail`       MODIFY COLUMN `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '价格';
ALTER TABLE `order`                MODIFY COLUMN `total_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单总价格（单价×数量）';
ALTER TABLE `qinghe_payment_record` MODIFY COLUMN `amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额';
