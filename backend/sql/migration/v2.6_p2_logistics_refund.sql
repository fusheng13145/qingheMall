-- ============================================================
-- 青禾商城 P2-18 退货退款 + 物流跟踪模块 迁移脚本 v2.6
-- 说明：对存量库执行；全新部署直接使用根目录 index.sql（已同步变更）
-- 背景：此前退款仅限「未发货订单」且无退货流程；发货为纯状态变更，
--       无承运商/运单号，用户侧无物流轨迹可查。
-- 变更内容：
--   1) logistics 表：一单一物流记录（承运商/运单号/状态）
--   2) logistics_trace 表：物流轨迹事件时间线
--   3) refund_request 表：退款/退货申请单（含审核留痕与回退状态）
-- 幂等：全部通过 information_schema 判断，可重复执行
-- ============================================================

USE qinghedb;

-- 1. logistics 物流主表
SET @t1 = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='logistics');
SET @s1 = IF(@t1 = 0,
  'CREATE TABLE logistics (
    `id` varchar(64) NOT NULL COMMENT ''主键ID'',
    `order_number` varchar(64) NOT NULL COMMENT ''订单编号'',
    `company` varchar(64) NOT NULL COMMENT ''承运商'',
    `tracking_number` varchar(64) NOT NULL COMMENT ''运单号'',
    `status` varchar(32) NOT NULL COMMENT ''物流状态 SHIPPED/IN_TRANSIT/DELIVERING/SIGNED'',
    `gmt_created` datetime NOT NULL COMMENT ''创建时间'',
    `gmt_modified` datetime NOT NULL COMMENT ''修改时间'',
    PRIMARY KEY (`id`),
    UNIQUE KEY uk_order_number (`order_number`),
    KEY idx_tracking_number (`tracking_number`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''物流表''',
  'SELECT 1');
PREPARE st1 FROM @s1; EXECUTE st1; DEALLOCATE PREPARE st1;

-- 2. logistics_trace 物流轨迹表
SET @t2 = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='logistics_trace');
SET @s2 = IF(@t2 = 0,
  'CREATE TABLE logistics_trace (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
    `order_number` varchar(64) NOT NULL COMMENT ''订单编号'',
    `description` varchar(255) NOT NULL COMMENT ''轨迹描述'',
    `trace_time` datetime NOT NULL COMMENT ''轨迹时间'',
    `gmt_created` datetime NOT NULL COMMENT ''创建时间'',
    PRIMARY KEY (`id`),
    KEY idx_order_number (`order_number`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''物流轨迹表''',
  'SELECT 1');
PREPARE st2 FROM @s2; EXECUTE st2; DEALLOCATE PREPARE st2;

-- 3. refund_request 退款/退货申请表
SET @t3 = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='refund_request');
SET @s3 = IF(@t3 = 0,
  'CREATE TABLE refund_request (
    `id` varchar(64) NOT NULL COMMENT ''主键ID'',
    `order_number` varchar(64) NOT NULL COMMENT ''订单编号'',
    `user_id` bigint NOT NULL COMMENT ''申请用户ID'',
    `type` varchar(32) NOT NULL COMMENT ''申请类型 REFUND_ONLY=仅退款 / RETURN_REFUND=退货退款'',
    `reason` varchar(255) NOT NULL COMMENT ''申请原因'',
    `status` varchar(32) NOT NULL COMMENT ''审核状态 PENDING/APPROVED/REJECTED'',
    `previous_order_status` varchar(32) DEFAULT NULL COMMENT ''申请前订单状态（驳回时回退用）'',
    `review_comment` varchar(255) DEFAULT NULL COMMENT ''审核意见'',
    `gmt_created` datetime NOT NULL COMMENT ''创建时间'',
    `gmt_modified` datetime NOT NULL COMMENT ''修改时间'',
    PRIMARY KEY (`id`),
    KEY idx_order_number (`order_number`),
    KEY idx_user_id (`user_id`),
    KEY idx_status (`status`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''退款/退货申请表''',
  'SELECT 1');
PREPARE st3 FROM @s3; EXECUTE st3; DEALLOCATE PREPARE st3;
