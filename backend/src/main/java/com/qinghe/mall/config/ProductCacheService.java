package com.qinghe.mall.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 商品热点数据缓存（P1-10 修复）。
 *
 * 前台最高频读路径（商品详情 / 规格列表）此前每次请求直达 MySQL，
 * 本组件基于 Redisson RMapCache + fastjson 提供两级缓存：
 * - 商品基础信息：TTL 10 分钟
 * - 规格列表（含库存）：TTL 10 分钟（库存变更走写路径主动失效，避免脏读）
 *
 * 一致性策略：所有写路径（商品增改删、SKU 替换、库存增减）显式调用
 * {@link #evict(String)} 清缓存；Redis 异常时缓存读降级为直查 DB（不阻塞主流程）。
 */
@Component
public class ProductCacheService {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheService.class);

    private static final String CACHE_NAME = "cache:product";
    private static final String KEY_PRODUCT = "product:";
    private static final String KEY_DETAILS = "details:";
    private static final long TTL_MINUTES = 10;

    @Autowired
    private RedissonClient redissonClient;

    private RMapCache<String, String> cache() {
        return redissonClient.getMapCache(CACHE_NAME);
    }

    public Product getProduct(String id) {
        try {
            String json = cache().get(KEY_PRODUCT + id);
            return json == null ? null : JSON.parseObject(json, Product.class);
        } catch (Exception e) {
            log.warn("商品缓存读取失败，降级直查 DB id={}, reason={}", id, e.getMessage());
            return null;
        }
    }

    public void putProduct(Product product) {
        if (product == null || product.getId() == null) {
            return;
        }
        try {
            cache().put(KEY_PRODUCT + product.getId(), JSON.toJSONString(product), TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("商品缓存写入失败 id={}, reason={}", product.getId(), e.getMessage());
        }
    }

    public List<ProductDetail> getDetails(String productId) {
        try {
            String json = cache().get(KEY_DETAILS + productId);
            return json == null ? null : JSON.parseObject(json, new TypeReference<List<ProductDetail>>() {
            });
        } catch (Exception e) {
            log.warn("商品规格缓存读取失败，降级直查 DB productId={}, reason={}", productId, e.getMessage());
            return null;
        }
    }

    public void putDetails(String productId, List<ProductDetail> details) {
        try {
            cache().put(KEY_DETAILS + productId, JSON.toJSONString(details), TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("商品规格缓存写入失败 productId={}, reason={}", productId, e.getMessage());
        }
    }

    /** 商品任何写操作后调用：清商品与规格缓存，保证下次读为最新 */
    public void evict(String productId) {
        try {
            cache().fastRemove(KEY_PRODUCT + productId, KEY_DETAILS + productId);
        } catch (Exception e) {
            log.warn("商品缓存清理失败 productId={}, reason={}", productId, e.getMessage());
        }
    }
}
