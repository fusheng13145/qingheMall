-- ============================================================
-- v1.7 M5 存量数据本地化（RR-5）
-- 将 product 种子数据中的 product_intro（原为外部图片生成 API 的 URL）
-- 改为文字描述；将 product_imgs（原为外部 text_to_image API URL）改为
-- 本地约定路径 /uploads/products/...（由后端启动时从 classpath 拷贝占位图）。
-- 目的：去除对外部图像服务的强依赖，使演示/生产数据自包含。
-- 幂等：重复执行安全（UPDATE 相同值无副作用）。
-- ============================================================
UPDATE `product` SET
    product_intro = '经典纯白低帮板鞋，头层牛皮鞋面，Air 气垫缓震，百搭耐穿。',
    product_imgs  = '/uploads/products/p001-1.svg;/uploads/products/p001-2.svg;/uploads/products/p001-3.svg'
WHERE id = 'p001';

UPDATE `product` SET
    product_intro = 'Primeknit 针织鞋面搭配 Boost 中底，黑白配色，轻盈回弹。',
    product_imgs  = '/uploads/products/p002-1.svg;/uploads/products/p002-2.svg;/uploads/products/p002-3.svg'
WHERE id = 'p002';

UPDATE `product` SET
    product_intro = '美产复古跑鞋，ENCAP 缓震中底，灰色麂皮，舒适支撑。',
    product_imgs  = '/uploads/products/p003-1.svg;/uploads/products/p003-2.svg;/uploads/products/p003-3.svg'
WHERE id = 'p003';

UPDATE `product` SET
    product_intro = '经典高帮帆布鞋，加厚鞋头与鞋底，黑色简约百搭。',
    product_imgs  = '/uploads/products/p004-1.svg;/uploads/products/p004-2.svg;/uploads/products/p004-3.svg'
WHERE id = 'p004';

UPDATE `product` SET
    product_intro = '经典黑白滑板鞋，麂皮拼接鞋面，华夫格大底，街头风格。',
    product_imgs  = '/uploads/products/p005-1.svg;/uploads/products/p005-2.svg;/uploads/products/p005-3.svg'
WHERE id = 'p005';

UPDATE `product` SET
    product_intro = '经典蓝色麂皮板鞋，橡胶大底，复古运动风范。',
    product_imgs  = '/uploads/products/p006-1.svg;/uploads/products/p006-2.svg;/uploads/products/p006-3.svg'
WHERE id = 'p006';

UPDATE `product` SET
    product_intro = '复古网球鞋，白绿配色，软皮鞋面，日常通勤舒适之选。',
    product_imgs  = '/uploads/products/p007-1.svg;/uploads/products/p007-2.svg;/uploads/products/p007-3.svg'
WHERE id = 'p007';

UPDATE `product` SET
    product_intro = '高帮篮球鞋，黑红经典配色，真皮鞋面，潮流标志性单品。',
    product_imgs  = '/uploads/products/p008-1.svg;/uploads/products/p008-2.svg;/uploads/products/p008-3.svg'
WHERE id = 'p008';
