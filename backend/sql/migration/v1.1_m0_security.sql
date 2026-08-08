-- ============================================================
-- 青禾商城 M0 安全加固 数据库迁移脚本 v1.1
-- 说明：对已存在的库执行本脚本；全新部署直接使用根目录 index.sql（已含变更）
-- 变更内容：user.pwd 由 varchar(32)（MD5）扩展为 varchar(60)（BCrypt）
-- ============================================================

USE qinghedb;

-- 1. 扩展密码字段长度以容纳 BCrypt 哈希（60 字符）
ALTER TABLE `user`
    MODIFY COLUMN `pwd` VARCHAR(60) NOT NULL COMMENT '密码(BCrypt哈希，兼容旧MD5)';

-- 2. 存量 MD5 密码保持原样（登录时自动升级为 BCrypt），无需数据回填
--    如需强制下线旧密码，可执行（谨慎）：
--    UPDATE `user` SET pwd = '' WHERE LENGTH(pwd) != 60;
