-- 青禾商城 迁移脚本 v2.10（D2，v1.7 首页运营位）
-- 新建 home_banner 运营位配置表，幂等可执行。
-- 存量库手动执行；与 index.sql 最新结构一致。

-- ============ 1. 首页运营位表 ============
CREATE TABLE IF NOT EXISTS `home_banner` (
    `id` varchar(32) NOT NULL COMMENT '主键ID（UUID）',
    `title` varchar(64) NOT NULL COMMENT '运营位标题',
    `image` varchar(512) NOT NULL COMMENT '图片URL（站内 /uploads/ 或外链）',
    `link_url` varchar(512) DEFAULT NULL COMMENT '跳转链接（以 / 开头按站内路由跳转，否则按外链新窗打开；NULL 不可点击）',
    `sort_order` int NOT NULL DEFAULT 0 COMMENT '展示顺序（数值小在前）',
    `status` varchar(16) NOT NULL DEFAULT 'ON' COMMENT '状态：ON 上架 / OFF 下架',
    `gmt_created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页运营位配置表';
