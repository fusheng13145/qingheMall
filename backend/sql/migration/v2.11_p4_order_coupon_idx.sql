-- 青禾商城 迁移脚本 v2.11（v1.8 购物车级优惠券）
-- order 表补 coupon_id 索引：购物车级用券后，退款/取消的「同券在途订单」守卫查询按 coupon_id 定位。
-- 幂等可执行；存量库手动执行；与 index.sql 最新结构一致。

-- ============ 1. order.coupon_id 索引（幂等：先查再建） ============
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'order'
      AND index_name = 'idx_coupon_id'
);
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `order` ADD INDEX `idx_coupon_id` (`coupon_id`)',
    'SELECT ''idx_coupon_id already exists'' AS note');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
