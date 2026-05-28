-- 青禾商城数据库初始化脚本
-- 创建所有需要的数据库

CREATE DATABASE IF NOT EXISTS qinghe_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS qinghe_goods DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS qinghe_cart DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS qinghe_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS qinghe_pay DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS qinghe_marketing DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE qinghe_user;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `gender` tinyint DEFAULT 0 COMMENT '0未知 1男 2女',
  `status` tinyint DEFAULT 1 COMMENT '0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 收货地址表
CREATE TABLE IF NOT EXISTS `user_address` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `receiver` varchar(50) NOT NULL COMMENT '收货人',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `province` varchar(50) DEFAULT NULL COMMENT '省份',
  `city` varchar(50) DEFAULT NULL COMMENT '城市',
  `district` varchar(50) DEFAULT NULL COMMENT '区县',
  `detail` varchar(255) NOT NULL COMMENT '详细地址',
  `is_default` tinyint DEFAULT 0 COMMENT '是否默认',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收货地址表';

-- 初始化测试用户 (密码: 123456 BCrypt加密)
INSERT INTO `user` (`username`, `password`, `phone`, `nickname`, `status`) VALUES
('test', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13800138000', '测试用户', 1);

USE qinghe_goods;

-- 商品分类表
CREATE TABLE IF NOT EXISTS `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `parent_id` bigint DEFAULT 0 COMMENT '父分类ID',
  `level` int NOT NULL COMMENT '层级 1/2/3',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标',
  `sort` int DEFAULT 0 COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- 商品表
CREATE TABLE IF NOT EXISTS `goods` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `name` varchar(200) NOT NULL COMMENT '商品名称',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `price` decimal(10,2) NOT NULL COMMENT '售价',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `stock` int DEFAULT 0 COMMENT '库存',
  `image` varchar(255) DEFAULT NULL COMMENT '主图',
  `images` text COMMENT '轮播图JSON',
  `description` text COMMENT '商品详情',
  `sales` int DEFAULT 0 COMMENT '销量',
  `status` tinyint DEFAULT 1 COMMENT '0下架 1上架',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 初始化分类数据
INSERT INTO `category` (`id`, `name`, `parent_id`, `level`, `sort`) VALUES
(1, '手机数码', 0, 1, 1),
(2, '电脑办公', 0, 1, 2),
(3, '服装鞋帽', 0, 1, 3),
(4, '家用电器', 0, 1, 4),
(5, '智能手机', 1, 2, 1),
(6, '功能手机', 1, 2, 2),
(7, '游戏手机', 1, 2, 3),
(8, '笔记本电脑', 2, 2, 1),
(9, '台式电脑', 2, 2, 2),
(10, '平板', 2, 2, 3);

-- 初始化商品数据
INSERT INTO `goods` (`name`, `category_id`, `price`, `original_price`, `stock`, `image`, `sales`, `status`) VALUES
('iPhone 15 Pro Max 256GB', 5, 9999.00, 10999.00, 100, 'https://img14.360buyimg.com/n1/s450x450_jfs/t1/123456.jpg', 520, 1),
('华为 Mate 60 Pro', 5, 6999.00, 7999.00, 200, 'https://img14.360buyimg.com/n1/s450x450_jfs/t1/234567.jpg', 320, 1),
('小米 14 Ultra', 5, 5999.00, 6999.00, 150, 'https://img14.360buyimg.com/n1/s450x450_jfs/t1/345678.jpg', 280, 1),
('MacBook Pro 14寸 M3', 8, 15999.00, 16999.00, 50, 'https://img14.360buyimg.com/n1/s450x450_jfs/t1/456789.jpg', 120, 1),
('ThinkPad X1 Carbon', 8, 12999.00, 13999.00, 80, 'https://img14.360buyimg.com/n1/s450x450_jfs/t1/567890.jpg', 95, 1);

USE qinghe_cart;

-- 购物车表
CREATE TABLE IF NOT EXISTS `cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `goods_id` bigint NOT NULL COMMENT '商品ID',
  `goods_name` varchar(200) DEFAULT NULL COMMENT '商品名称',
  `goods_image` varchar(255) DEFAULT NULL COMMENT '商品图片',
  `price` decimal(10,2) DEFAULT NULL COMMENT '单价',
  `quantity` int DEFAULT 1 COMMENT '数量',
  `checked` tinyint DEFAULT 1 COMMENT '是否选中',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

USE qinghe_order;

-- 订单表
CREATE TABLE IF NOT EXISTS `order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_sn` varchar(32) NOT NULL COMMENT '订单编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `total_amount` decimal(10,2) DEFAULT NULL COMMENT '订单总金额',
  `pay_amount` decimal(10,2) DEFAULT NULL COMMENT '实付金额',
  `freight_amount` decimal(10,2) DEFAULT 0.00 COMMENT '运费',
  `coupon_amount` decimal(10,2) DEFAULT 0.00 COMMENT '优惠金额',
  `status` tinyint DEFAULT 0 COMMENT '0待付款 1已付款 2已发货 3已完成 4取消',
  `receiver` varchar(50) DEFAULT NULL COMMENT '收货人',
  `receiver_phone` varchar(20) DEFAULT NULL COMMENT '收货电话',
  `receiver_address` varchar(255) DEFAULT NULL COMMENT '收货地址',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `delivery_time` datetime DEFAULT NULL COMMENT '发货时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_sn` (`order_sn`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS `order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `goods_id` bigint DEFAULT NULL COMMENT '商品ID',
  `goods_name` varchar(200) DEFAULT NULL COMMENT '商品名称',
  `goods_image` varchar(255) DEFAULT NULL COMMENT '商品图片',
  `price` decimal(10,2) DEFAULT NULL COMMENT '单价',
  `quantity` int DEFAULT NULL COMMENT '数量',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

USE qinghe_pay;

-- 支付记录表
CREATE TABLE IF NOT EXISTS `payment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '支付ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `order_sn` varchar(32) DEFAULT NULL COMMENT '订单编号',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '支付金额',
  `pay_type` tinyint DEFAULT NULL COMMENT '1微信 2支付宝 3银联',
  `pay_status` tinyint DEFAULT 0 COMMENT '0待付 1已付 2已退款',
  `trade_no` varchar(100) DEFAULT NULL COMMENT '第三方交易号',
  `callback_content` text COMMENT '回调内容',
  `callback_time` datetime DEFAULT NULL COMMENT '回调时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_order_sn` (`order_sn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

USE qinghe_marketing;

-- 优惠券表
CREATE TABLE IF NOT EXISTS `coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
  `name` varchar(100) NOT NULL COMMENT '优惠券名称',
  `type` tinyint DEFAULT 1 COMMENT '1满减 2折扣',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '优惠金额/折扣',
  `min_amount` decimal(10,2) DEFAULT 0.00 COMMENT '使用门槛',
  `stock` int DEFAULT 0 COMMENT '库存',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `status` tinyint DEFAULT 1 COMMENT '0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券表';

-- 用户优惠券表
CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户优惠券ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `coupon_id` bigint NOT NULL COMMENT '优惠券ID',
  `status` tinyint DEFAULT 0 COMMENT '0未使用 1已使用 2已过期',
  `receive_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `use_time` datetime DEFAULT NULL COMMENT '使用时间',
  `order_id` bigint DEFAULT NULL COMMENT '使用的订单ID',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

-- 促销活动表
CREATE TABLE IF NOT EXISTS `seckill` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `name` varchar(200) NOT NULL COMMENT '活动名称',
  `goods_id` bigint NOT NULL COMMENT '商品ID',
  `price` decimal(10,2) DEFAULT NULL COMMENT '秒杀价',
  `stock` int DEFAULT 0 COMMENT '库存',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `status` tinyint DEFAULT 1 COMMENT '0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀活动表';
