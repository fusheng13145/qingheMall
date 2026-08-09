-- ============================================================
-- 青禾商城 P1-20 商品全文搜索 迁移脚本 v2.3
-- 说明：对存量库执行；全新部署直接使用根目录 index.sql（已同步变更）
-- 变更内容：product 表新增 FULLTEXT 索引（name + product_intro，ngram 中文分词），
--           供 ProductDAO 搜索走 MATCH...AGAINST，消除 LIKE '%kw%' 全表扫描
-- 幂等：通过 information_schema 判断，可重复执行
-- 注意：ngram 默认 token_size=2，单字关键词由 DAO 降级走 LIKE（见 ProductDAO.xml）
-- ============================================================

USE qinghedb;

SET @c = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='qinghedb' AND TABLE_NAME='product' AND INDEX_NAME='ft_name_intro');
SET @s = IF(@c = 0, 'ALTER TABLE product ADD FULLTEXT INDEX ft_name_intro (name, product_intro) WITH PARSER ngram', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
