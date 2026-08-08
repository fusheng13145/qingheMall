-- v1.8_m5_coupon.sql
-- A 阶段优惠券：券模板表 + 用户券表 + order 表增量扩展（优惠券字段）
-- 增量、幂等；存量库手动执行，与 v1.1~v1.7 范式一致

-- ============ 1. 券模板 coupon ============
CREATE TABLE IF NOT EXISTS `coupon` (
  `id`            VARCHAR(32)   NOT NULL,
  `name`          VARCHAR(64)   NOT NULL COMMENT '券名称',
  `type`          VARCHAR(20)   NOT NULL COMMENT 'FULL_REDUCTION 满减 / DISCOUNT 折扣',
  `threshold`     DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '使用门槛（订单原价满额），0 表示无门槛',
  `amount`        DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '满减面额（满减券用）',
  `discount`      DECIMAL(4,2)  NOT NULL DEFAULT 1.00 COMMENT '折扣率（折扣券用，0.85=85折）',
  `max_discount`  DECIMAL(10,2) NULL     COMMENT '折扣封顶金额（折扣券可选）',
  `total`         INT           NOT NULL DEFAULT 0 COMMENT '发放总量',
  `issued`        INT           NOT NULL DEFAULT 0 COMMENT '已领取数',
  `per_limit`     INT           NOT NULL DEFAULT 1 COMMENT '每人限领张数',
  `start_time`    DATETIME      NOT NULL COMMENT '领取/可用开始时间',
  `end_time`      DATETIME      NOT NULL COMMENT '领取/可用结束时间',
  `status`        VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE 上架 / INACTIVE 下架',
  `gmt_created`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板';

-- ============ 2. 用户券 user_coupon ============
CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id`            VARCHAR(32)   NOT NULL,
  `user_id`       BIGINT        NOT NULL,
  `coupon_id`     VARCHAR(32)   NOT NULL,
  `status`        VARCHAR(16)   NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED 未使用 / USED 已使用 / EXPIRED 已过期 / RELEASED 已释放',
  `order_number`  VARCHAR(32)   NULL     COMMENT '核销绑定的订单号（主订单）',
  `used_time`     DATETIME      NULL,
  `gmt_created`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `status`),
  UNIQUE KEY `uk_user_coupon` (`user_id`, `coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户持有券';

-- ============ 3. order 表增量扩展（优惠券字段） ============
-- 幂等：仅当列不存在时才 ALTER，避免重复执行报错
SET @exist_coupon := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order' AND column_name = 'coupon_id');
SET @exist_discount := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order' AND column_name = 'discount_amount');

SET @sql := IF(@exist_coupon = 0 AND @exist_discount = 0,
  'ALTER TABLE `order` ADD COLUMN `coupon_id` VARCHAR(32) NULL COMMENT ''使用的用户券id'' AFTER `status`, ADD COLUMN `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''优惠券抵扣金额''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
