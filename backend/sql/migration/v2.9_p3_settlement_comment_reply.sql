-- 青禾商城 迁移脚本 v2.9（A2/A4，v1.5 商家结算分账 + 评价回复）
-- 新建 settlement_ledger / settlement_bill / comment_reply 三表，幂等可执行。
-- 存量库手动执行；与 index.sql 最新结构一致。

-- ============ 1. 分账流水表 ============
CREATE TABLE IF NOT EXISTS `settlement_ledger` (
    `id` varchar(64) NOT NULL COMMENT '主键ID',
    `order_number` varchar(64) NOT NULL COMMENT '关联订单号',
    `merchant_id` bigint NOT NULL COMMENT '分账商家ID',
    `type` varchar(16) NOT NULL COMMENT '类型：EARN 确认收货分账 / REVERSAL 退款冲销',
    `gross` decimal(10,2) NOT NULL COMMENT '订单实付金额（冲销时为被冲销的实付额）',
    `commission_rate` decimal(5,4) NOT NULL COMMENT '平台佣金费率快照（分账时点）',
    `commission` decimal(10,2) NOT NULL COMMENT '平台佣金（恒正，冲销时随净额一并冲回）',
    `net` decimal(10,2) NOT NULL COMMENT '商家净入（EARN 为正，REVERSAL 为负）',
    `bill_id` varchar(64) DEFAULT NULL COMMENT '纳入的结算单ID（NULL=未结算）',
    `gmt_created` datetime NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_type` (`order_number`, `type`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_bill_id` (`bill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家分账流水表';

-- ============ 2. 结算单表 ============
CREATE TABLE IF NOT EXISTS `settlement_bill` (
    `id` varchar(64) NOT NULL COMMENT '主键ID',
    `merchant_id` bigint NOT NULL COMMENT '商家ID',
    `total_gross` decimal(10,2) NOT NULL COMMENT '汇总实付金额',
    `total_commission` decimal(10,2) NOT NULL COMMENT '汇总平台佣金',
    `total_net` decimal(10,2) NOT NULL COMMENT '汇总商家净入（应放款额）',
    `entry_count` int NOT NULL COMMENT '包含流水笔数',
    `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待审核 / PAID已放款 / REJECTED已驳回（流水退回未结算）',
    `review_note` varchar(255) DEFAULT NULL COMMENT '放款备注/驳回原因',
    `reviewer_id` bigint DEFAULT NULL COMMENT '审核管理员ID',
    `gmt_created` datetime NOT NULL COMMENT '生成时间',
    `gmt_reviewed` datetime DEFAULT NULL COMMENT '审核时间',
    PRIMARY KEY (`id`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家结算单表';

-- ============ 3. 评价回复表 ============
CREATE TABLE IF NOT EXISTS `comment_reply` (
    `id` varchar(64) NOT NULL COMMENT '主键ID',
    `comment_id` varchar(64) NOT NULL COMMENT '关联评价ID',
    `merchant_id` bigint NOT NULL COMMENT '回复商家ID',
    `content` varchar(200) NOT NULL COMMENT '回复内容',
    `gmt_created` datetime NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_id` (`comment_id`),
    KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价回复表';
