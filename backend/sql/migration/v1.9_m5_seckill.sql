-- 青禾商城 迁移脚本 v1.9（M5 营销与高并发 - 秒杀子系统）
-- 秒杀活动表 + 秒杀订单表；幂等可执行（CREATE TABLE IF NOT EXISTS）。
-- 说明：秒杀订单复用既有普通 order 表（秒杀_order 仅作去重与库存对账），
--       故本脚本不改动 order 表结构（沿用 v1.8 已加的可空 coupon 列）。

CREATE TABLE IF NOT EXISTS `seckill_activity` (
  `id`                VARCHAR(32)   NOT NULL,
  `product_detail_id` VARCHAR(32)   NOT NULL COMMENT '绑定的 SKU',
  `seckill_price`     DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
  `total_stock`       INT           NOT NULL COMMENT '活动总库存',
  `remain_stock`      INT           NOT NULL COMMENT '剩余库存（CAS 扣减，防超卖主防线）',
  `start_time`        DATETIME      NOT NULL COMMENT '活动开始时间',
  `end_time`          DATETIME      NOT NULL COMMENT '活动结束时间',
  `status`            VARCHAR(16)   NOT NULL DEFAULT 'NOT_START' COMMENT 'NOT_START / ONGOING / ENDED / CLOSED',
  `gmt_created`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status_time` (`status`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动表';

CREATE TABLE IF NOT EXISTS `seckill_order` (
  `id`                VARCHAR(32)   NOT NULL,
  `user_id`           BIGINT        NOT NULL,
  `activity_id`       VARCHAR(32)   NOT NULL,
  `product_detail_id` VARCHAR(32)   NOT NULL,
  `quantity`          INT           NOT NULL DEFAULT 1,
  `order_number`      VARCHAR(32)   NOT NULL,
  `status`            VARCHAR(16)   NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED（待支付）/ CANCELLED（超时或取消回滚）',
  `gmt_created`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_activity` (`user_id`, `activity_id`) COMMENT '同一用户同一活动只能抢一次',
  KEY `idx_order_number` (`order_number`),
  KEY `idx_activity` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表（去重 + 库存对账）';
