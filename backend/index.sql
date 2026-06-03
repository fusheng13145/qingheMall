-- 青禾商城数据库初始化脚本
CREATE DATABASE IF NOT EXISTS qinghedb DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE qinghedb;

-- 用户表
CREATE TABLE `user` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `user_name` varchar(20) NOT NULL COMMENT '用户名',
    `pwd` varchar(32) NOT NULL COMMENT '密码',
    `nick_name` varchar(20) DEFAULT NULL COMMENT '昵称',
    `avatar` varchar(200) DEFAULT NULL COMMENT '头像url',
    `gmt_created` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品表
CREATE TABLE product (
    id VARCHAR(32) NOT NULL COMMENT '主键',
    name VARCHAR(64) NOT NULL COMMENT '商品名称',
    price DOUBLE(32,8) NOT NULL COMMENT '商品参考价格',
    purchase_num INT(11) NOT NULL DEFAULT 0 COMMENT '商品销量',
    product_intro VARCHAR(256) NOT NULL COMMENT '商品介绍',
    product_imgs VARCHAR(1024) NOT NULL COMMENT '商品轮播图片，多个URL用空格隔开，第一张是缩略图',
    gmt_created DATETIME NOT NULL COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL COMMENT '修改日期',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 商品详情表（规格）
CREATE TABLE product_detail (
    id VARCHAR(32) NOT NULL COMMENT '主键',
    product_id VARCHAR(32) NOT NULL COMMENT '关联商品',
    price DOUBLE(32,8) NOT NULL COMMENT '价格',
    size DOUBLE(8,2) NOT NULL COMMENT '尺码',
    stock INT(11) NOT NULL COMMENT '库存',
    gmt_created DATETIME NOT NULL COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL COMMENT '修改日期',
    PRIMARY KEY (id),
    KEY idx_product_id (product_id) COMMENT '关联商品的索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品详情表';

-- 订单表
CREATE TABLE `order` (
    `id` varchar(64) NOT NULL COMMENT '主键ID',
    `order_number` varchar(64) NOT NULL COMMENT '订单编号',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `product_detail_id` varchar(64) NOT NULL COMMENT '商品详情ID',
    `total_price` double(32,8) NOT NULL COMMENT '订单总价格',
    `status` varchar(32) NOT NULL COMMENT '订单状态',
    `gmt_created` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY idx_order_number (`order_number`),
    KEY idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 支付流水记录表
CREATE TABLE qinghe_payment_record (
    id VARCHAR(32) NOT NULL COMMENT '主键 id',
    user_id BIGINT NOT NULL COMMENT '用户 id',
    order_number VARCHAR(100) NOT NULL COMMENT '订单号',
    channel_payment_id VARCHAR(100) NOT NULL DEFAULT '' COMMENT '外部支付渠道主键 id',
    channel_type VARCHAR(32) NOT NULL DEFAULT '' COMMENT '渠道类型',
    amount DOUBLE(16,2) NOT NULL COMMENT '支付金额',
    pay_type VARCHAR(32) NOT NULL COMMENT '支付类型',
    pay_status VARCHAR(32) NOT NULL COMMENT '支付状态',
    extend_str VARCHAR(255) DEFAULT '' COMMENT '订单额外信息',
    pay_end_time DATETIME DEFAULT NULL COMMENT '支付完成时间',
    gmt_created DATETIME NOT NULL COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_order_number (order_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水记录表';

-- ==================== 商品示例数据 ====================

INSERT INTO `product` VALUES ('p001', 'Nike Air Force 1 经典白鞋', 799.0000000000, 15234, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=white%20nike%20air%20force%201%20sneaker%20on%20white%20background%20product%20photo&image_size=square_hd', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=white%20nike%20air%20force%201%20side%20view%20product%20photo&image_size=square_hd;https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=white%20nike%20air%20force%201%20top%20view%20product%20photo&image_size=square_hd;https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=white%20nike%20air%20force%201%20detail%20photo&image_size=square_hd', '2024-01-01 10:00:00', '2024-01-01 10:00:00');
INSERT INTO `product` VALUES ('p002', 'Adidas Yeezy Boost 350 V2 黑白', 1899.0000000000, 8921, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=adidas%20yeezy%20boost%20350%20v2%20black%20white%20product%20photo&image_size=square_hd', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=adidas%20yeezy%20boost%20350%20v2%20side%20view&image_size=square_hd;https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=adidas%20yeezy%20boost%20350%20v2%20top%20view&image_size=square_hd', '2024-01-02 10:00:00', '2024-01-02 10:00:00');
INSERT INTO `product` VALUES ('p003', 'New Balance 990v5 灰色', 1599.0000000000, 6432, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=new%20balance%20990v5%20grey%20sneaker%20product%20photo&image_size=square_hd', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=new%20balance%20990v5%20side%20view&image_size=square_hd;https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=new%20balance%20990v5%20detail&image_size=square_hd', '2024-01-03 10:00:00', '2024-01-03 10:00:00');
INSERT INTO `product` VALUES ('p004', 'Converse Chuck 70 黑色高帮', 599.0000000000, 23156, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=converse%20chuck%2070%20black%20high%20top%20product%20photo&image_size=square_hd', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=converse%20chuck%2070%20side%20view&image_size=square_hd;https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=converse%20chuck%2070%20detail&image_size=square_hd', '2024-01-04 10:00:00', '2024-01-04 10:00:00');
INSERT INTO `product` VALUES ('p005', 'Vans Old Skool 经典黑白', 469.0000000000, 31245, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=vans%20old%20skool%20black%20white%20product%20photo&image_size=square_hd', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=vans%20old%20skool%20side%20view&image_size=square_hd;https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=vans%20old%20skool%20detail&image_size=square_hd', '2024-01-05 10:00:00', '2024-01-05 10:00:00');
INSERT INTO `product` VALUES ('p006', 'Puma Suede Classic 蓝色', 529.0000000000, 8765, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=puma%20suede%20classic%20blue%20sneaker%20product%20photo&image_size=square_hd', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=puma%20suede%20classic%20side%20view&image_size=square_hd;https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=puma%20suede%20classic%20detail&image_size=square_hd', '2024-01-06 10:00:00', '2024-01-06 10:00:00');
INSERT INTO `product` VALUES ('p007', 'Reebok Club C 85 白绿', 549.0000000000, 5432, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=reebok%20club%20c%2085%20white%20green%20product%20photo&image_size=square_hd', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=reebok%20club%20c%2085%20side%20view&image_size=square_hd;https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=reebok%20club%20c%2085%20detail&image_size=square_hd', '2024-01-07 10:00:00', '2024-01-07 10:00:00');
INSERT INTO `product` VALUES ('p008', 'Jordan 1 Retro High 黑红', 1399.0000000000, 12890, 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=jordan%201%20retro%20high%20black%20red%20product%20photo&image_size=square_hd', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=jordan%201%20retro%20high%20side%20view&image_size=square_hd;https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=jordan%201%20retro%20high%20detail&image_size=square_hd', '2024-01-08 10:00:00', '2024-01-08 10:00:00');

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
UPDATE `user` SET role = 'USER';
-- 设置第一个用户为管理员（如果存在）
UPDATE `user` SET role = 'ADMIN' WHERE id = 1;
