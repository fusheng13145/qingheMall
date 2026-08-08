-- 青禾商城 迁移脚本 v2.1（M7：多品类种子商品 —— 综合性超级商场定位）
-- 新增 8 个多品类商品（数码/个护/服饰/家居/食品/美妆）与对应规格，覆盖原"球鞋单一品类"演示数据。
-- 幂等：INSERT IGNORE（主键冲突即跳过），可重复执行；全新库的 index.sql 已内置同样数据。
USE qinghedb;

INSERT IGNORE INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p009', '真无线蓝牙耳机 Pro', '青禾数码', 299.00, 12680, '主动降噪，蓝牙5.3，单次续航8小时+充电仓28小时，半入耳舒适佩戴。', '/uploads/products/p001-1.svg;/uploads/products/p001-2.svg;/uploads/products/p001-3.svg', '2024-02-01 10:00:00', '2024-02-01 10:00:00');
INSERT IGNORE INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p010', '智能手环 6 代', '青禾数码', 199.00, 8930, '心率/血氧/睡眠监测，50+运动模式，14天超长续航，5ATM 防水。', '/uploads/products/p002-1.svg;/uploads/products/p002-2.svg;/uploads/products/p002-3.svg', '2024-02-02 10:00:00', '2024-02-02 10:00:00');
INSERT IGNORE INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p011', '声波电动牙刷', '青禾个护', 159.00, 5670, '五档清洁模式，2分钟智能计时，IPX7 全身防水，配 4 支刷头。', '/uploads/products/p003-1.svg;/uploads/products/p003-2.svg;/uploads/products/p003-3.svg', '2024-02-03 10:00:00', '2024-02-03 10:00:00');
INSERT IGNORE INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p012', '重磅纯棉印花T恤', '青禾服饰', 79.00, 21030, '260g 精梳棉，oversize 落肩版型，数码直喷印花不褪色，S/M/L 三码。', '/uploads/products/p004-1.svg;/uploads/products/p004-2.svg;/uploads/products/p004-3.svg', '2024-02-04 10:00:00', '2024-02-04 10:00:00');
INSERT IGNORE INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p013', '316 不锈钢保温杯', '青禾家居', 89.00, 15760, '316 医用级不锈钢，12小时保温/24小时保冷，500ml 大容量，杯盖防漏。', '/uploads/products/p005-1.svg;/uploads/products/p005-2.svg;/uploads/products/p005-3.svg', '2024-02-05 10:00:00', '2024-02-05 10:00:00');
INSERT IGNORE INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p014', '每日坚果混合装 30 包', '青禾食品', 129.00, 34210, '六种坚果科学配比，独立小包装锁鲜，办公零食/早餐伴侣首选。', '/uploads/products/p006-1.svg;/uploads/products/p006-2.svg;/uploads/products/p006-3.svg', '2024-02-06 10:00:00', '2024-02-06 10:00:00');
INSERT IGNORE INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p015', '玻尿酸保湿面霜 50g', '青禾美妆', 219.00, 9870, '三重玻尿酸+神经酰胺，长效锁水修护，敏感肌可用，早晚均可。', '/uploads/products/p007-1.svg;/uploads/products/p007-2.svg;/uploads/products/p007-3.svg', '2024-02-07 10:00:00', '2024-02-07 10:00:00');
INSERT IGNORE INTO `product` (id, name, brand, price, purchase_num, product_intro, product_imgs, gmt_created, gmt_modified) VALUES ('p016', '精品挂耳咖啡 10 包礼盒', '青禾食品', 99.00, 15890, '精选云南/巴西/埃塞三产区，中度烘焙，手冲风味，送人自饮皆宜。', '/uploads/products/p008-1.svg;/uploads/products/p008-2.svg;/uploads/products/p008-3.svg', '2024-02-08 10:00:00', '2024-02-08 10:00:00');

INSERT IGNORE INTO `product_detail` VALUES ('pd039', 'p009', 299.00, 1.00, 100, '2024-02-01 10:00:00', '2024-02-01 10:00:00');
INSERT IGNORE INTO `product_detail` VALUES ('pd040', 'p010', 199.00, 1.00, 120, '2024-02-02 10:00:00', '2024-02-02 10:00:00');
INSERT IGNORE INTO `product_detail` VALUES ('pd041', 'p011', 159.00, 1.00, 80, '2024-02-03 10:00:00', '2024-02-03 10:00:00');
INSERT IGNORE INTO `product_detail` VALUES ('pd042', 'p012', 79.00, 0.50, 200, '2024-02-04 10:00:00', '2024-02-04 10:00:00');
INSERT IGNORE INTO `product_detail` VALUES ('pd043', 'p012', 79.00, 1.00, 200, '2024-02-04 10:00:00', '2024-02-04 10:00:00');
INSERT IGNORE INTO `product_detail` VALUES ('pd044', 'p012', 79.00, 1.50, 200, '2024-02-04 10:00:00', '2024-02-04 10:00:00');
INSERT IGNORE INTO `product_detail` VALUES ('pd045', 'p013', 89.00, 1.00, 150, '2024-02-05 10:00:00', '2024-02-05 10:00:00');
INSERT IGNORE INTO `product_detail` VALUES ('pd046', 'p014', 129.00, 1.00, 100, '2024-02-06 10:00:00', '2024-02-06 10:00:00');
INSERT IGNORE INTO `product_detail` VALUES ('pd047', 'p015', 219.00, 1.00, 90, '2024-02-07 10:00:00', '2024-02-07 10:00:00');
INSERT IGNORE INTO `product_detail` VALUES ('pd048', 'p016', 99.00, 1.00, 110, '2024-02-08 10:00:00', '2024-02-08 10:00:00');
