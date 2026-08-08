-- ============================================================
-- v1.6 M4 优化：商品评价表 + 库存流水表
-- 执行：mysql -uroot -p1234 --default-character-set=utf8mb4 < v1.6_m4_comment_stocklog.sql
-- 幂等：表不存在才创建
-- ============================================================

USE qinghedb;

-- 商品评价表
CREATE TABLE IF NOT EXISTS `comment` (
    `id` varchar(64) NOT NULL COMMENT '主键ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `product_id` varchar(64) NOT NULL COMMENT '商品ID',
    `order_number` varchar(64) NOT NULL COMMENT '关联订单号（唯一，防重复评价）',
    `rating` int NOT NULL COMMENT '评分 1-5',
    `content` varchar(500) NOT NULL COMMENT '评价内容',
    `gmt_created` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY uk_order_number (`order_number`),
    KEY idx_product_id (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

-- 库存流水表（下单扣减/取消回滚/超时回滚留痕，可追溯对账）
CREATE TABLE IF NOT EXISTS `stock_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `product_detail_id` varchar(64) NOT NULL COMMENT '商品规格ID',
    `product_id` varchar(64) DEFAULT NULL COMMENT '商品ID（冗余，便于按商品查询）',
    `order_number` varchar(64) DEFAULT NULL COMMENT '关联订单号',
    `change_type` varchar(32) NOT NULL COMMENT '变动类型：ORDER_DEDUCT 下单扣减 / ORDER_RESTORE 取消回滚 / EXPIRE_RESTORE 超时回滚 / STOCK_SET 手动设置',
    `change_quantity` int NOT NULL COMMENT '变动数量（扣减为负数，回滚为正数）',
    `before_stock` int NOT NULL COMMENT '变动前库存',
    `after_stock` int NOT NULL COMMENT '变动后库存',
    `gmt_created` datetime NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY idx_product_detail_id (`product_detail_id`),
    KEY idx_order_number (`order_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';
