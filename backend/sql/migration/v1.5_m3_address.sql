-- ============================================================
-- 青禾商城 M3 体验与工程化 数据库迁移脚本 v1.5
-- 说明：对存量库执行；全新部署直接使用根目录 index.sql（已含变更）
-- 变更内容：新增 address 收货地址表（M3-3 用户中心）
-- 幂等：可重复执行
-- ============================================================

USE qinghedb;

CREATE TABLE IF NOT EXISTS `address` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `receiver_name` varchar(32) NOT NULL COMMENT '收货人',
    `receiver_phone` varchar(20) NOT NULL COMMENT '收货电话',
    `receiver_address` varchar(255) NOT NULL COMMENT '收货地址',
    `is_default` tinyint NOT NULL DEFAULT 0 COMMENT '是否默认 1-是 0-否',
    `gmt_created` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';
