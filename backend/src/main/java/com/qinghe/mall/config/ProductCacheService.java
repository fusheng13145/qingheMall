package com.qinghe.mall.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import jakarta.annotation.PostConstruct;
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
 *
 * 可观测性（feature/stock-redis-cache）：接入 Micrometer 缓存命中/未命中计数器与命中率
 * Gauge，经 Actuator 暴露至 /actuator/prometheus，便于在 Grafana 观察缓存收益与命中率趋势：
 * - qinghe.product.cache.hits   缓存命中次数
 * - qinghe.product.cache.misses 缓存未命中次数（含 DB 回填前的首次读）
 * - qinghe.product.cache.hitratio  命中率 = hits / (hits + misses)，PromQL 亦可自行计算
 * 注：Redis 异常降级路径不计入命中/未命中（属异常而非正常缓存行为），以免污染命中率。
 */
@Component
public class ProductCacheService {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheService.class);

    private static final String CACHE_NAME = "cache:product";
    private static final String KEY_PRODUCT = "product:";
    private static final String KEY_DETAILS = "details:";
    private static final long TTL_MINUTES = 10;

    private static final String METRIC_HITS = "qinghe.product.cache.hits";
    private static final String METRIC_MISSES = "qinghe.product.cache.misses";
    private static final String METRIC_HIT_RATIO = "qinghe.product.cache.hitratio";

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private MeterRegistry meterRegistry;

    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private Counter hitCounter;
    private Counter missCounter;

    @PostConstruct
    public void init() {
        hitCounter = meterRegistry.counter(METRIC_HITS);
        missCounter = meterRegistry.counter(METRIC_MISSES);
        // 命中率 Gauge：采样时已无读取则记为 0，避免首读前的除零
        Gauge.builder(METRIC_HIT_RATIO, this, s -> {
            long h = s.hits.get();
            long m = s.misses.get();
            long total = h + m;
            return total == 0 ? 0.0 : (double) h / total;
        }).register(meterRegistry);
    }

    private RMapCache<String, String> cache() {
        return redissonClient.getMapCache(CACHE_NAME);
    }

    public Product getProduct(String id) {
        try {
            String json = cache().get(KEY_PRODUCT + id);
            if (json == null) {
                misses.incrementAndGet();
                missCounter.increment();
                return null;
            }
            hits.incrementAndGet();
            hitCounter.increment();
            return JSON.parseObject(json, Product.class);
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
            if (json == null) {
                misses.incrementAndGet();
                missCounter.increment();
                return null;
            }
            hits.incrementAndGet();
            hitCounter.increment();
            return JSON.parseObject(json, new TypeReference<List<ProductDetail>>() {
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

    /** 供测试与运维读取累计命中/未命中次数（非 Metrics 替代） */
    public long getHits() {
        return hits.get();
    }

    public long getMisses() {
        return misses.get();
    }
}
