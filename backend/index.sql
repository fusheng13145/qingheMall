-- 青禾商城数据库初始化脚本
CREATE DATABASE IF NOT EXISTS qinghedb DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE qinghedb;

-- 用户表
CREATE TABLE `user` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `user_name` varchar(20) NOT NULL COMMENT '用户名',
    `pwd` varchar(60) NOT NULL COMMENT '密码(BCrypt哈希，兼容旧MD5)',
    `nick_name` varchar(20) DEFAULT NULL COMMENT '昵称',
    `avatar` varchar(200) DEFAULT NULL COMMENT '头像url',
    `gmt_created` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_name` (`user_name`) COMMENT 'P1-9：用户名唯一（登录/注册走索引）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品表（M6：merchant_id 归属商家、NULL=平台自营；status 上下架）
CREATE TABLE product (
    id VARCHAR(32) NOT NULL COMMENT '主键',
    name VARCHAR(64) NOT NULL COMMENT '商品名称',
    brand VARCHAR(32) DEFAULT NULL COMMENT '品牌',
    merchant_id bigint DEFAULT NULL COMMENT '归属商家ID(NULL=平台自营)',
    status varchar(16) NOT NULL DEFAULT 'ON' COMMENT '上架状态：ON在售/OFF下架',
    price DECIMAL(10,2) NOT NULL COMMENT '商品参考价格',
    purchase_num INT(11) NOT NULL DEFAULT 0 COMMENT '商品销量',
    product_intro VARCHAR(256) NOT NULL COMMENT '商品介绍',
    product_imgs VARCHAR(1024) NOT NULL COMMENT '商品轮播图片，多个URL用空格隔开，第一张是缩略图',
    gmt_created DATETIME NOT NULL COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL COMMENT '修改日期',
    PRIMARY KEY (id),
    KEY idx_merchant_id (merchant_id) COMMENT '商家商品索引',
    KEY idx_status (status) COMMENT 'P1-9：在售列表过滤',
    KEY idx_brand (brand) COMMENT 'P1-9：品牌筛选',
    FULLTEXT KEY ft_name_intro (name, product_intro) WITH PARSER ngram COMMENT 'P1-20：商品全文搜索(ngram)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 商品详情表（规格）
CREATE TABLE product_detail (
    id VARCHAR(32) NOT NULL COMMENT '主键',
    product_id VARCHAR(32) NOT NULL COMMENT '关联商品',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    size DOUBLE(8,2) NOT NULL COMMENT '尺码',
    stock INT(11) NOT NULL COMMENT '库存',
    gmt_created DATETIME NOT NULL COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL COMMENT '修改日期',
    PRIMARY KEY (id),
    KEY idx_product_id (product_id) COMMENT '关联商品的索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品详情表';

-- 订单表（M6：merchant_id 归属商家；A2：coupon_id/discount_amount 优惠券）
-- 订单表（B2/v1.6：主键改雪花 BIGINT，按 gmt_created 季度桶分区；
-- uk_order_number 复合化以满足「唯一键须含分区键」的 MySQL 约束）
CREATE TABLE `order` (
    `id` bigint unsigned NOT NULL COMMENT '主键ID(雪花)',
    `order_number` varchar(64) NOT NULL COMMENT '订单编号',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `merchant_id` bigint DEFAULT NULL COMMENT '归属商家ID(NULL=平台自营)',
    `product_detail_id` varchar(64) NOT NULL COMMENT '商品详情ID',
    `quantity` int NOT NULL DEFAULT 1 COMMENT '购买数量',
    `total_price` decimal(10,2) NOT NULL COMMENT '订单总价格（单价×数量）',
    `status` varchar(32) NOT NULL COMMENT '订单状态',
    `coupon_id` varchar(32) DEFAULT NULL COMMENT '使用的用户券id',
    `discount_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠券抵扣金额',
    `receiver_name` varchar(32) DEFAULT NULL COMMENT '收货人',
    `receiver_phone` varchar(20) DEFAULT NULL COMMENT '收货电话',
    `receiver_address` varchar(255) DEFAULT NULL COMMENT '收货地址',
    `gmt_created` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`, `gmt_created`),
    UNIQUE KEY uk_order_number (`order_number`, `gmt_created`) COMMENT 'P1-5：订单号唯一约束（复合化含分区键，防生成碰撞兜底）',
    KEY idx_coupon_id (`coupon_id`) COMMENT 'v1.8 购物车级用券：同券在途订单守卫查询',
    KEY idx_user_id (`user_id`),
    KEY idx_merchant_id (`merchant_id`) COMMENT '商家订单索引',
    KEY idx_status_created (`status`, `gmt_created`) COMMENT 'P1-9：超时关单/报表/状态聚合'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表'
PARTITION BY RANGE COLUMNS (gmt_created) (
    PARTITION p_hist  VALUES LESS THAN ('2026-10-01'),
    PARTITION p2026Q4 VALUES LESS THAN ('2027-01-01'),
    PARTITION p2027Q1 VALUES LESS THAN ('2027-04-01'),
    PARTITION p2027Q2 VALUES LESS THAN ('2027-07-01'),
    PARTITION pmax    VALUES LESS THAN MAXVALUE
);

-- 购物车表
CREATE TABLE cart (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_detail_id VARCHAR(64) NOT NULL COMMENT '商品规格ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    selected TINYINT NOT NULL DEFAULT 1 COMMENT '是否勾选 1-是 0-否',
    gmt_created DATETIME NOT NULL COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_detail (user_id, product_detail_id)
    -- P2：idx_user_id 冗余已删除（uk_user_detail 左前缀已覆盖 user_id 查询）
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 支付流水记录表
CREATE TABLE qinghe_payment_record (
    id VARCHAR(32) NOT NULL COMMENT '主键 id',
    user_id BIGINT NOT NULL COMMENT '用户 id',
    order_number VARCHAR(100) NOT NULL COMMENT '订单号',
    channel_payment_id VARCHAR(100) NOT NULL DEFAULT '' COMMENT '外部支付渠道主键 id',
    channel_type VARCHAR(32) NOT NULL DEFAULT '' COMMENT '渠道类型',
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    pay_type VARCHAR(32) NOT NULL COMMENT '支付类型',
    pay_status VARCHAR(32) NOT NULL COMMENT '支付状态',
    extend_str VARCHAR(255) DEFAULT '' COMMENT '订单额外信息',
    pay_end_time DATETIME DEFAULT NULL COMMENT '支付完成时间',
    gmt_created DATETIME NOT NULL COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_order_number (order_number),
    KEY idx_pay_status_created (pay_status, gmt_created) COMMENT 'P1-9：支付对账/补单'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水记录表';

-- 收货地址表
CREATE TABLE `address` (
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

-- 商品评价表
CREATE TABLE `comment` (
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
CREATE TABLE `stock_log` (
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

-- ==================== 商品示例数据 ====================

INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p001', 'Nike Air Force 1 经典白鞋', 'Nike', 799.0000000000, 15234, '经典纯白低帮板鞋，头层牛皮鞋面，Air 气垫缓震，百搭耐穿。', '/uploads/products/p001-1.svg;/uploads/products/p001-2.svg;/uploads/products/p001-3.svg', '2024-01-01 10:00:00', '2024-01-01 10:00:00');
INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p002', 'Adidas Yeezy Boost 350 V2 黑白', 'Adidas', 1899.0000000000, 8921, 'Primeknit 针织鞋面搭配 Boost 中底，黑白配色，轻盈回弹。', '/uploads/products/p002-1.svg;/uploads/products/p002-2.svg;/uploads/products/p002-3.svg', '2024-01-02 10:00:00', '2024-01-02 10:00:00');
INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p003', 'New Balance 990v5 灰色', 'New Balance', 1599.0000000000, 6432, '美产复古跑鞋，ENCAP 缓震中底，灰色麂皮，舒适支撑。', '/uploads/products/p003-1.svg;/uploads/products/p003-2.svg;/uploads/products/p003-3.svg', '2024-01-03 10:00:00', '2024-01-03 10:00:00');
INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p004', 'Converse Chuck 70 黑色高帮', 'Converse', 599.0000000000, 23156, '经典高帮帆布鞋，加厚鞋头与鞋底，黑色简约百搭。', '/uploads/products/p004-1.svg;/uploads/products/p004-2.svg;/uploads/products/p004-3.svg', '2024-01-04 10:00:00', '2024-01-04 10:00:00');
INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p005', 'Vans Old Skool 经典黑白', 'Vans', 469.0000000000, 31245, '经典黑白滑板鞋，麂皮拼接鞋面，华夫格大底，街头风格。', '/uploads/products/p005-1.svg;/uploads/products/p005-2.svg;/uploads/products/p005-3.svg', '2024-01-05 10:00:00', '2024-01-05 10:00:00');
INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p006', 'Puma Suede Classic 蓝色', 'Puma', 529.0000000000, 8765, '经典蓝色麂皮板鞋，橡胶大底，复古运动风范。', '/uploads/products/p006-1.svg;/uploads/products/p006-2.svg;/uploads/products/p006-3.svg', '2024-01-06 10:00:00', '2024-01-06 10:00:00');
INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p007', 'Reebok Club C 85 白绿', 'Reebok', 549.0000000000, 5432, '复古网球鞋，白绿配色，软皮鞋面，日常通勤舒适之选。', '/uploads/products/p007-1.svg;/uploads/products/p007-2.svg;/uploads/products/p007-3.svg', '2024-01-07 10:00:00', '2024-01-07 10:00:00');
INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p008', 'Jordan 1 Retro High 黑红', 'Jordan', 1399.0000000000, 12890, '高帮篮球鞋，黑红经典配色，真皮鞋面，潮流标志性单品。', '/uploads/products/p008-1.svg;/uploads/products/p008-2.svg;/uploads/products/p008-3.svg', '2024-01-08 10:00:00', '2024-01-08 10:00:00');

-- ==================== 商品规格示例数据 ====================

-- Nike Air Force 1 规格
INSERT INTO `product_detail` VALUES ('pd001', 'p001', 799.0000000000, 38.00, 50, '2024-01-01 10:00:00', '2024-01-01 10:00:00');
INSERT INTO `product_detail` VALUES ('pd002', 'p001', 799.0000000000, 39.00, 45, '2024-01-01 10:00:00', '2024-01-01 10:00:00');
INSERT INTO `product_detail` VALUES ('pd003', 'p001', 799.0000000000, 40.00, 30, '2024-01-01 10:00:00', '2024-01-01 10:00:00');
INSERT INTO `product_detail` VALUES ('pd004', 'p001', 799.0000000000, 41.00, 25, '2024-01-01 10:00:00', '2024-01-01 10:00:00');
INSERT INTO `product_detail` VALUES ('pd005', 'p001', 799.0000000000, 42.00, 20, '2024-01-01 10:00:00', '2024-01-01 10:00:00');
INSERT INTO `product_detail` VALUES ('pd006', 'p001', 799.0000000000, 43.00, 15, '2024-01-01 10:00:00', '2024-01-01 10:00:00');

-- Adidas Yeezy Boost 350 V2 规格
INSERT INTO `product_detail` VALUES ('pd007', 'p002', 1899.0000000000, 39.00, 30, '2024-01-02 10:00:00', '2024-01-02 10:00:00');
INSERT INTO `product_detail` VALUES ('pd008', 'p002', 1899.0000000000, 40.00, 25, '2024-01-02 10:00:00', '2024-01-02 10:00:00');
INSERT INTO `product_detail` VALUES ('pd009', 'p002', 1899.0000000000, 41.00, 20, '2024-01-02 10:00:00', '2024-01-02 10:00:00');
INSERT INTO `product_detail` VALUES ('pd010', 'p002', 1899.0000000000, 42.00, 15, '2024-01-02 10:00:00', '2024-01-02 10:00:00');
INSERT INTO `product_detail` VALUES ('pd011', 'p002', 1899.0000000000, 43.00, 10, '2024-01-02 10:00:00', '2024-01-02 10:00:00');

-- New Balance 990v5 规格
INSERT INTO `product_detail` VALUES ('pd012', 'p003', 1599.0000000000, 39.00, 40, '2024-01-03 10:00:00', '2024-01-03 10:00:00');
INSERT INTO `product_detail` VALUES ('pd013', 'p003', 1599.0000000000, 40.00, 35, '2024-01-03 10:00:00', '2024-01-03 10:00:00');
INSERT INTO `product_detail` VALUES ('pd014', 'p003', 1599.0000000000, 41.00, 30, '2024-01-03 10:00:00', '2024-01-03 10:00:00');
INSERT INTO `product_detail` VALUES ('pd015', 'p003', 1599.0000000000, 42.00, 25, '2024-01-03 10:00:00', '2024-01-03 10:00:00');

-- Converse Chuck 70 规格
INSERT INTO `product_detail` VALUES ('pd016', 'p004', 599.0000000000, 37.00, 60, '2024-01-04 10:00:00', '2024-01-04 10:00:00');
INSERT INTO `product_detail` VALUES ('pd017', 'p004', 599.0000000000, 38.00, 55, '2024-01-04 10:00:00', '2024-01-04 10:00:00');
INSERT INTO `product_detail` VALUES ('pd018', 'p004', 599.0000000000, 39.00, 50, '2024-01-04 10:00:00', '2024-01-04 10:00:00');
INSERT INTO `product_detail` VALUES ('pd019', 'p004', 599.0000000000, 40.00, 45, '2024-01-04 10:00:00', '2024-01-04 10:00:00');
INSERT INTO `product_detail` VALUES ('pd020', 'p004', 599.0000000000, 41.00, 40, '2024-01-04 10:00:00', '2024-01-04 10:00:00');

-- Vans Old Skool 规格
INSERT INTO `product_detail` VALUES ('pd021', 'p005', 469.0000000000, 37.00, 70, '2024-01-05 10:00:00', '2024-01-05 10:00:00');
INSERT INTO `product_detail` VALUES ('pd022', 'p005', 469.0000000000, 38.00, 65, '2024-01-05 10:00:00', '2024-01-05 10:00:00');
INSERT INTO `product_detail` VALUES ('pd023', 'p005', 469.0000000000, 39.00, 60, '2024-01-05 10:00:00', '2024-01-05 10:00:00');
INSERT INTO `product_detail` VALUES ('pd024', 'p005', 469.0000000000, 40.00, 55, '2024-01-05 10:00:00', '2024-01-05 10:00:00');

-- Puma Suede Classic 规格
INSERT INTO `product_detail` VALUES ('pd025', 'p006', 529.0000000000, 38.00, 45, '2024-01-06 10:00:00', '2024-01-06 10:00:00');
INSERT INTO `product_detail` VALUES ('pd026', 'p006', 529.0000000000, 39.00, 40, '2024-01-06 10:00:00', '2024-01-06 10:00:00');
INSERT INTO `product_detail` VALUES ('pd027', 'p006', 529.0000000000, 40.00, 35, '2024-01-06 10:00:00', '2024-01-06 10:00:00');
INSERT INTO `product_detail` VALUES ('pd028', 'p006', 529.0000000000, 41.00, 30, '2024-01-06 10:00:00', '2024-01-06 10:00:00');

-- Reebok Club C 85 规格
INSERT INTO `product_detail` VALUES ('pd029', 'p007', 549.0000000000, 38.00, 40, '2024-01-07 10:00:00', '2024-01-07 10:00:00');
INSERT INTO `product_detail` VALUES ('pd030', 'p007', 549.0000000000, 39.00, 35, '2024-01-07 10:00:00', '2024-01-07 10:00:00');
INSERT INTO `product_detail` VALUES ('pd031', 'p007', 549.0000000000, 40.00, 30, '2024-01-07 10:00:00', '2024-01-07 10:00:00');
INSERT INTO `product_detail` VALUES ('pd032', 'p007', 549.0000000000, 41.00, 25, '2024-01-07 10:00:00', '2024-01-07 10:00:00');

-- Jordan 1 Retro High 规格
INSERT INTO `product_detail` VALUES ('pd033', 'p008', 1399.0000000000, 39.00, 25, '2024-01-08 10:00:00', '2024-01-08 10:00:00');
INSERT INTO `product_detail` VALUES ('pd034', 'p008', 1399.0000000000, 40.00, 20, '2024-01-08 10:00:00', '2024-01-08 10:00:00');
INSERT INTO `product_detail` VALUES ('pd035', 'p008', 1399.0000000000, 41.00, 15, '2024-01-08 10:00:00', '2024-01-08 10:00:00');
INSERT INTO `product_detail` VALUES ('pd036', 'p008', 1399.0000000000, 42.00, 10, '2024-01-08 10:00:00', '2024-01-08 10:00:00');
INSERT INTO `product_detail` VALUES ('pd037', 'p008', 1399.0000000000, 43.00, 8, '2024-01-08 10:00:00', '2024-01-08 10:00:00');
INSERT INTO `product_detail` VALUES ('pd038', 'p008', 1399.0000000000, 44.00, 5, '2024-01-08 10:00:00', '2024-01-08 10:00:00');

-- ==================== 管理员角色字段 ====================
ALTER TABLE `user` ADD COLUMN `role` VARCHAR(20) DEFAULT 'USER' AFTER `avatar`;

-- ==================== 种子管理员账号 ====================
-- 默认账号 admin / 123456（BCrypt 哈希，M0 安全规范）
INSERT INTO `user` (user_name, pwd, nick_name, avatar, role, gmt_created, gmt_modified)
VALUES ('admin', '$2b$10$fSkzsMiltPP4I.YavxaRY.lqLdgi9wpE6jCG6JlDEEziAc9YcY44K', '管理员', '', 'ADMIN', NOW(), NOW());

-- 历史存量用户兜底置为普通用户（不覆盖已明确设置的角色）
UPDATE `user` SET role = 'USER' WHERE role IS NULL OR role = '';

-- ==================== 优惠券（A2，结构与 v1.8 迁移一致） ====================
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
  `merchant_id`   BIGINT        DEFAULT NULL COMMENT '归属商家ID(NULL=平台券)',
  `gmt_created`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`) COMMENT '商家券索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板';

CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id`            VARCHAR(32)   NOT NULL,
  `user_id`       BIGINT        NOT NULL,
  `coupon_id`     VARCHAR(32)   NOT NULL,
  `status`        VARCHAR(16)   NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED 未使用 / USED 已使用 / EXPIRED 已过期 / RELEASED 已释放',
  `order_number`  VARCHAR(32)   NULL     COMMENT '核销绑定的订单号',
  `used_time`     DATETIME      NULL,
  `gmt_created`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `status`),
  UNIQUE KEY `uk_user_coupon` (`user_id`, `coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户持有券';

-- ==================== 秒杀（A3，结构与 v1.9 迁移一致） ====================
CREATE TABLE IF NOT EXISTS `seckill_activity` (
  `id`                VARCHAR(32)   NOT NULL,
  `product_detail_id` VARCHAR(32)   NOT NULL COMMENT '绑定的 SKU',
  `seckill_price`     DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
  `total_stock`       INT           NOT NULL COMMENT '活动总库存',
  `remain_stock`      INT           NOT NULL COMMENT '剩余库存（CAS 扣减，防超卖主防线）',
  `start_time`        DATETIME      NOT NULL COMMENT '活动开始时间',
  `end_time`          DATETIME      NOT NULL COMMENT '活动结束时间',
  `status`            VARCHAR(16)   NOT NULL DEFAULT 'NOT_START' COMMENT 'NOT_START / ONGOING / ENDED / CLOSED',
  `merchant_id`       BIGINT        DEFAULT NULL COMMENT '归属商家ID(NULL=平台秒杀)',
  `gmt_created`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status_time` (`status`, `start_time`),
  KEY `idx_merchant_id` (`merchant_id`) COMMENT '商家秒杀索引'
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

-- ==================== 入驻商家（M6，结构与 v2.0 迁移一致） ====================
CREATE TABLE IF NOT EXISTS `merchant` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `user_id` bigint NOT NULL COMMENT '店主用户ID（1:1）',
    `shop_name` varchar(64) NOT NULL COMMENT '店铺名称',
    `shop_logo` varchar(200) DEFAULT NULL COMMENT '店铺Logo',
    `shop_desc` varchar(255) DEFAULT NULL COMMENT '店铺简介',
    `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待审/ACTIVE正常/REJECTED驳回/DISABLED禁用',
    `reject_reason` varchar(255) DEFAULT NULL COMMENT '驳回原因',
    `gmt_created` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入驻商家表';

-- ==================== 退货退款 + 物流跟踪（P2-18，结构与 v2.6 迁移一致） ====================
CREATE TABLE IF NOT EXISTS `logistics` (
    `id` varchar(64) NOT NULL COMMENT '主键ID',
    `order_number` varchar(64) NOT NULL COMMENT '订单编号',
    `company` varchar(64) NOT NULL COMMENT '承运商',
    `tracking_number` varchar(64) NOT NULL COMMENT '运单号',
    `status` varchar(32) NOT NULL COMMENT '物流状态 SHIPPED/IN_TRANSIT/DELIVERING/SIGNED',
    `gmt_created` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_number` (`order_number`),
    KEY `idx_tracking_number` (`tracking_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流表';

CREATE TABLE IF NOT EXISTS `logistics_trace` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_number` varchar(64) NOT NULL COMMENT '订单编号',
    `description` varchar(255) NOT NULL COMMENT '轨迹描述',
    `trace_time` datetime NOT NULL COMMENT '轨迹时间',
    `gmt_created` datetime NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_number` (`order_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流轨迹表';

CREATE TABLE IF NOT EXISTS `refund_request` (
    `id` varchar(64) NOT NULL COMMENT '主键ID',
    `order_number` varchar(64) NOT NULL COMMENT '订单编号',
    `user_id` bigint NOT NULL COMMENT '申请用户ID',
    `type` varchar(32) NOT NULL COMMENT '申请类型 REFUND_ONLY=仅退款 / RETURN_REFUND=退货退款',
    `reason` varchar(255) NOT NULL COMMENT '申请原因',
    `status` varchar(32) NOT NULL COMMENT '审核状态 PENDING/APPROVED/REJECTED',
    `previous_order_status` varchar(32) DEFAULT NULL COMMENT '申请前订单状态（驳回时回退用）',
    `review_comment` varchar(255) DEFAULT NULL COMMENT '审核意见',
    `gmt_created` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_number` (`order_number`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款/退货申请表';

-- ==================== 多品类种子商品（综合性超级商场：覆盖数码/个护/服饰/家居/食品/美妆） ====================
-- 图复用内置 24 张占位图（p001~p008 各 3 张，SeedImageInitializer 启动时拷贝）；正式运营请替换为真实商品图
INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p009', '真无线蓝牙耳机 Pro', '青禾数码', 299.00, 12680, '主动降噪，蓝牙5.3，单次续航8小时+充电仓28小时，半入耳舒适佩戴。', '/uploads/products/p001-1.svg;/uploads/products/p001-2.svg;/uploads/products/p001-3.svg', '2024-02-01 10:00:00', '2024-02-01 10:00:00');
INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p010', '智能手环 6 代', '青禾数码', 199.00, 8930, '心率/血氧/睡眠监测，50+运动模式，14天超长续航，5ATM 防水。', '/uploads/products/p002-1.svg;/uploads/products/p002-2.svg;/uploads/products/p002-3.svg', '2024-02-02 10:00:00', '2024-02-02 10:00:00');
INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p011', '声波电动牙刷', '青禾个护', 159.00, 5670, '五档清洁模式，2分钟智能计时，IPX7 全身防水，配 4 支刷头。', '/uploads/products/p003-1.svg;/uploads/products/p003-2.svg;/uploads/products/p003-3.svg', '2024-02-03 10:00:00', '2024-02-03 10:00:00');
INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p012', '重磅纯棉印花T恤', '青禾服饰', 79.00, 21030, '260g 精梳棉，oversize 落肩版型，数码直喷印花不褪色，S/M/L 三码。', '/uploads/products/p004-1.svg;/uploads/products/p004-2.svg;/uploads/products/p004-3.svg', '2024-02-04 10:00:00', '2024-02-04 10:00:00');
INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p013', '316 不锈钢保温杯', '青禾家居', 89.00, 15760, '316 医用级不锈钢，12小时保温/24小时保冷，500ml 大容量，杯盖防漏。', '/uploads/products/p005-1.svg;/uploads/products/p005-2.svg;/uploads/products/p005-3.svg', '2024-02-05 10:00:00', '2024-02-05 10:00:00');
INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p014', '每日坚果混合装 30 包', '青禾食品', 129.00, 34210, '六种坚果科学配比，独立小包装锁鲜，办公零食/早餐伴侣首选。', '/uploads/products/p006-1.svg;/uploads/products/p006-2.svg;/uploads/products/p006-3.svg', '2024-02-06 10:00:00', '2024-02-06 10:00:00');
INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p015', '玻尿酸保湿面霜 50g', '青禾美妆', 219.00, 9870, '三重玻尿酸+神经酰胺，长效锁水修护，敏感肌可用，早晚均可。', '/uploads/products/p007-1.svg;/uploads/products/p007-2.svg;/uploads/products/p007-3.svg', '2024-02-07 10:00:00', '2024-02-07 10:00:00');
INSERT INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p016', '精品挂耳咖啡 10 包礼盒', '青禾食品', 99.00, 15890, '精选云南/巴西/埃塞三产区，中度烘焙，手冲风味，送人自饮皆宜。', '/uploads/products/p008-1.svg;/uploads/products/p008-2.svg;/uploads/products/p008-3.svg', '2024-02-08 10:00:00', '2024-02-08 10:00:00');

-- 多品类商品规格（非鞋类商品 size 1.00 表示标准款；T 恤 0.5/1.0/1.5 对应 S/M/L）
INSERT INTO `product_detail` VALUES ('pd039', 'p009', 299.00, 1.00, 100, '2024-02-01 10:00:00', '2024-02-01 10:00:00');
INSERT INTO `product_detail` VALUES ('pd040', 'p010', 199.00, 1.00, 120, '2024-02-02 10:00:00', '2024-02-02 10:00:00');
INSERT INTO `product_detail` VALUES ('pd041', 'p011', 159.00, 1.00, 80, '2024-02-03 10:00:00', '2024-02-03 10:00:00');
INSERT INTO `product_detail` VALUES ('pd042', 'p012', 79.00, 0.50, 200, '2024-02-04 10:00:00', '2024-02-04 10:00:00');
INSERT INTO `product_detail` VALUES ('pd043', 'p012', 79.00, 1.00, 200, '2024-02-04 10:00:00', '2024-02-04 10:00:00');
INSERT INTO `product_detail` VALUES ('pd044', 'p012', 79.00, 1.50, 200, '2024-02-04 10:00:00', '2024-02-04 10:00:00');
INSERT INTO `product_detail` VALUES ('pd045', 'p013', 89.00, 1.00, 150, '2024-02-05 10:00:00', '2024-02-05 10:00:00');
INSERT INTO `product_detail` VALUES ('pd046', 'p014', 129.00, 1.00, 100, '2024-02-06 10:00:00', '2024-02-06 10:00:00');
INSERT INTO `product_detail` VALUES ('pd047', 'p015', 219.00, 1.00, 90, '2024-02-07 10:00:00', '2024-02-07 10:00:00');
INSERT INTO `product_detail` VALUES ('pd048', 'p016', 99.00, 1.00, 110, '2024-02-08 10:00:00', '2024-02-08 10:00:00');

-- ==================== 演示秒杀活动种子（D1/v1.9：全新库即有进行中秒杀，保障移动端 e2e 可跑） ====================
INSERT INTO `seckill_activity` (id, product_detail_id, seckill_price, total_stock, remain_stock, start_time, end_time, status, merchant_id)
VALUES
('skseed001', 'pd001', 599.00, 20, 20, '2025-01-01 00:00:00', '2035-01-01 00:00:00', 'ONGOING', NULL),
('skseed002', 'pd012', 59.00, 50, 50, '2025-01-01 00:00:00', '2035-01-01 00:00:00', 'ONGOING', NULL);

-- ==================== 商家结算分账 + 评价回复（A2/A4，v1.5，结构与 v2.9 迁移一致） ====================

-- 分账流水表（订单确认收货分账 EARN / 退款审核通过冲销 REVERSAL；net 带符号，gross/commission 恒正）
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

-- 结算单表（管理端按商家生成/审核放款；仅汇总未结算的 EARN 流水，冲销负流水直接抵减商家余额）
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

-- 评价回复表（A4：商家对本店商品评价的一对一回复，uk 防重复回复）
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
