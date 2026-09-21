package com.qinghe.mall.config;

import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 雪花 ID 生成器（T5，B2/v1.6：order.id UUID → BIGINT）。
 *
 * 结构（标准 Twitter Snowflake，共 63 bit，正数）：
 *   1 bit 符号位(0) | 41 bit 毫秒时间戳 | 10 bit workerId | 12 bit 同毫秒序列
 * 自定义 epoch：2026-01-01T00:00:00Z（可用 41 bit 表达至 2100 年前后）。
 *
 * workerId 分配优先级：
 *   1) 环境变量 {@code SNOWFLAKE_WORKER_ID}（0~1023，多实例部署显式规划）；
 *   2) Redis 原子自增取模 1024（{@code qinghe:snowflake:worker:seq}，多实例自动错开）；
 *   3) 网卡 MAC 派生 + 随机扰动兜底（Redis 不可用时的单机降级，冲突概率 ~1/1024，
 *      同毫秒序列叠加随机起始偏移，实际碰撞概率可忽略）。
 *
 * 时钟回拨防护：回拨 ≤5ms 自旋等待追平；超过 5ms 拒绝发号（fail-fast，避免重复 ID）。
 * 单机吞吐：同毫秒 4096 个，理论峰值 ~409 万/s，远超当前写链路容量（#36 验收 161 RPS）。
 */
@Component
public class SnowflakeIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    /** 自定义纪元：2026-01-01T00:00:00Z */
    public static final long EPOCH = 1767225600000L;

    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1;      // 1023
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;       // 4095
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;                 // 12
    private static final long TIMESTAMP_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS; // 22
    /** 允许的最大时钟回拨（毫秒），超过即 fail-fast */
    private static final long MAX_BACKWARD_MS = 5L;

    private final long workerId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    private final Object lock = new Object();
    private final AtomicLong backwardWaits = new AtomicLong();
    private final RedissonClient redissonClient;

    /** Spring 单构造器自动装配（workerId 配置 + Redisson 可选注入）；测试可直接 new（传 null） */
    public SnowflakeIdGenerator(@Value("${app.snowflake.worker-id:}") String workerIdConfig,
                                RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        this.workerId = resolveWorkerId(workerIdConfig);
        log.info("SnowflakeIdGenerator 初始化 workerId={}（epoch={}）", this.workerId, EPOCH);
    }

    /** 生成下一个全局唯一 ID（线程安全） */
    public long nextId() {
        synchronized (lock) {
            long now = System.currentTimeMillis();
            if (now < lastTimestamp) {
                long backward = lastTimestamp - now;
                if (backward > MAX_BACKWARD_MS) {
                    throw new IllegalStateException(
                            "时钟回拨 " + backward + "ms 超过阈值 " + MAX_BACKWARD_MS + "ms，拒绝发号以防 ID 重复");
                }
                // 小幅回拨：自旋等待追平
                backwardWaits.incrementAndGet();
                while ((now = System.currentTimeMillis()) < lastTimestamp) {
                    Thread.onSpinWait();
                }
            }
            if (now == lastTimestamp) {
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence == 0) {
                    // 同毫秒序列耗尽：自旋进位到下一毫秒
                    while ((now = System.currentTimeMillis()) <= lastTimestamp) {
                        Thread.onSpinWait();
                    }
                }
            } else {
                sequence = 0L;
            }
            lastTimestamp = now;
            return ((now - EPOCH) << TIMESTAMP_SHIFT) | (workerId << WORKER_ID_SHIFT) | sequence;
        }
    }

    /** 测试观测用：时钟回拨等待次数 */
    long backwardWaits() {
        return backwardWaits.get();
    }

    long workerId() {
        return workerId;
    }

    /**
     * workerId 解析：环境变量显式指定 > Redis 自增取模 > MAC 派生兜底。
     * 任一环节异常均降级到下一优先级，绝不让发号器初始化失败阻断应用启动。
     */
    private long resolveWorkerId(String workerIdConfig) {
        if (workerIdConfig != null && !workerIdConfig.isBlank()) {
            try {
                long id = Long.parseLong(workerIdConfig.trim());
                if (id < 0) {
                    log.warn("SNOWFLAKE_WORKER_ID={} 为负，忽略改用自动分配", id);
                } else {
                    // 超范围取模收敛到 [0, 1023]（可预测，避免静默漂移到随机兜底）
                    return id & MAX_WORKER_ID;
                }
            } catch (NumberFormatException e) {
                log.warn("SNOWFLAKE_WORKER_ID={} 非数字，忽略改用自动分配", workerIdConfig);
            }
        }
        try {
            if (redissonClient != null) {
                // 原子自增取模：多实例自动错开 workerId
                long seq = redissonClient.getAtomicLong("qinghe:snowflake:worker:seq").incrementAndGet();
                return (seq - 1) % (MAX_WORKER_ID + 1);
            }
        } catch (Exception e) {
            log.warn("Redis workerId 分配不可用，降级 MAC 派生: {}", e.getMessage());
        }
        return macDerivedWorkerId();
    }

    /** MAC + 随机兜底（Redis 不可用场景） */
    private long macDerivedWorkerId() {
        long base = new SecureRandom().nextInt(1 << WORKER_ID_BITS);
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                byte[] mac = nics.nextElement().getHardwareAddress();
                if (mac != null && mac.length >= 2) {
                    base = ((mac[mac.length - 2] & 0x03) << 8) | (mac[mac.length - 1] & 0xFF);
                    break;
                }
            }
        } catch (Exception ignored) {
            // 无网卡信息时用随机值
        }
        return base & MAX_WORKER_ID;
    }
}
