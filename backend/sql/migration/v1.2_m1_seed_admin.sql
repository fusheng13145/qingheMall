-- ============================================================
-- 青禾商城 M1 联调收尾 数据库迁移脚本 v1.2
-- 说明：对存量库执行；全新部署直接使用根目录 index.sql（已含变更）
-- 变更内容：1) user.role 列缺失时补齐；2) 创建种子管理员账号 admin/123456
-- 幂等：可重复执行
-- ============================================================

USE qinghedb;

-- 1. user.role 列缺失时补齐（幂等）
SET @has_role = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'qinghedb' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'role'
);
SET @sql = IF(@has_role = 0,
    'ALTER TABLE `user` ADD COLUMN `role` VARCHAR(20) DEFAULT ''USER'' AFTER `avatar`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 种子管理员账号（BCrypt 哈希，密码 123456）；不存在时才创建（幂等）
INSERT INTO `user` (user_name, pwd, nick_name, avatar, role, gmt_created, gmt_modified)
SELECT 'admin', '$2b$10$fSkzsMiltPP4I.YavxaRY.lqLdgi9wpE6jCG6JlDEEziAc9YcY44K', '管理员', '', 'ADMIN', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE user_name = 'admin');

-- 3. 存量用户兜底置为普通用户（不覆盖已明确设置的角色）
UPDATE `user` SET role = 'USER' WHERE role IS NULL OR role = '';
