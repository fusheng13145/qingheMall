package com.qinghe.mall.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 雪花 ID 生成器单元测试（B2/v1.6，T5）。
 *
 * 覆盖：结构解析（时间戳/workerId/序列提取）、单线程唯一性、
 * 多线程并发唯一性（无重复）、workerId 兜底取模、自定义纪元偏移。
 */
class SnowflakeIdGeneratorTest {

    @Test
    @DisplayName("ID 结构可逆解析：时间戳/workerId/序列")
    void structureDecodes() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator("777", null);
        long before = System.currentTimeMillis();
        long id = gen.nextId();
        long after = System.currentTimeMillis();

        assertEquals(777L, (id >> 12) & 1023, "workerId 段");
        assertTrue(id > 0, "符号位为 0");
        long ts = (id >> 22) + SnowflakeIdGenerator.EPOCH;
        assertTrue(ts >= before && ts <= after, "时间戳段落在生成窗口内");
        assertTrue(((id >> 12) & 1023) == 777L);
        assertTrue((id & 4095) >= 0);
    }

    @Test
    @DisplayName("单线程连续发号严格递增且唯一")
    void singleThreadMonotonicUnique() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator("1", null);
        long prev = -1;
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            long id = gen.nextId();
            assertTrue(id > prev, "同线程单调递增");
            assertTrue(seen.add(id), "无重复");
            prev = id;
        }
    }

    @Test
    @DisplayName("多线程并发发号 8 线程 × 2000 无重复")
    void multiThreadUnique() throws InterruptedException {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator("3", null);
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        int threads = 8, perThread = 2000;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    ready.await();
                    for (int i = 0; i < perThread; i++) {
                        ids.add(gen.nextId());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdownNow();
        assertEquals(threads * perThread, ids.size(), "并发无重复");
    }

    @Test
    @DisplayName("workerId 兜底：超过 1023 取模收敛到合法区间")
    void workerIdWraps() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator("2049", null); // 2049 % 1024 = 1
        assertEquals(1L, gen.workerId());
        long id = gen.nextId();
        assertEquals(1L, (id >> 12) & 1023);
    }

    @Test
    @DisplayName("小幅时钟回拨（≤5ms）：自旋等待追平后正常发号")
    void clockBackwardSmall_spinsAndRecovers() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator("7", null);
        // 预置 lastTimestamp 到未来 4ms 触发小幅回拨自旋；设置与发号之间存在调度延迟，
        // 时钟可能已追平（backwardWaits 不增），故重试直至命中回拨分支（Windows 时钟粒度 ~15ms，必命中）
        boolean hitBackward = false;
        for (int i = 0; i < 50 && !hitBackward; i++) {
            org.springframework.test.util.ReflectionTestUtils.setField(gen, "lastTimestamp",
                    System.currentTimeMillis() + 4L);
            long id = gen.nextId();
            assertTrue(id > 0);
            hitBackward = gen.backwardWaits() > 0;
        }
        assertTrue(hitBackward, "50 次尝试均未触发小幅回拨分支（时钟粒度异常）");
    }

    @Test
    @DisplayName("大幅时钟回拨（>5ms）：fail-fast 拒绝发号防重复")
    void clockBackwardLarge_throws() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator("7", null);
        org.springframework.test.util.ReflectionTestUtils.setField(gen, "lastTimestamp",
                System.currentTimeMillis() + 100L);
        assertThrows(IllegalStateException.class, gen::nextId);
    }

    @Test
    @DisplayName("同毫秒序列耗尽：进位到下一毫秒，持续发号保持唯一")
    void sequenceOverflow_carries() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator("9", null);
        Set<Long> seen = new HashSet<>();
        // 2×4096 次紧凑发号必然跨越序列回绕（seq 4095 → 0）
        for (int i = 0; i < 8192; i++) {
            assertTrue(seen.add(gen.nextId()), "第 " + i + " 个 ID 重复");
        }
    }

    @Test
    @DisplayName("workerId 配置为负数：忽略并走自动分配兜底")
    void workerIdNegative_fallsBack() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator("-5", null);
        assertTrue(gen.workerId() >= 0 && gen.workerId() <= 1023);
        assertTrue(gen.nextId() > 0);
    }

    @Test
    @DisplayName("workerId 配置非数字：忽略并走自动分配兜底")
    void workerIdNonNumeric_fallsBack() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator("abc", null);
        assertTrue(gen.workerId() >= 0 && gen.workerId() <= 1023);
        assertTrue(gen.nextId() > 0);
    }

    @Test
    @DisplayName("无显式配置且有 Redis：经自增序列取模分配 workerId")
    void workerIdRedisAssigned() {
        org.redisson.api.RedissonClient redisson = org.mockito.Mockito.mock(org.redisson.api.RedissonClient.class);
        org.redisson.api.RAtomicLong seq = org.mockito.Mockito.mock(org.redisson.api.RAtomicLong.class);
        when(redisson.getAtomicLong("qinghe:snowflake:worker:seq")).thenReturn(seq);
        when(seq.incrementAndGet()).thenReturn(3000L); // (3000-1) % 1024 = 2999 % 1024 = 951

        SnowflakeIdGenerator gen = new SnowflakeIdGenerator("", redisson);
        assertEquals(951L, gen.workerId());
        long id = gen.nextId();
        assertEquals(951L, (id >> 12) & 1023);
    }

    @Test
    @DisplayName("Redis 分配异常：降级 MAC 兜底不阻断启动")
    void workerIdRedisFailure_fallsBackToMac() {
        org.redisson.api.RedissonClient redisson = org.mockito.Mockito.mock(org.redisson.api.RedissonClient.class);
        when(redisson.getAtomicLong("qinghe:snowflake:worker:seq"))
                .thenThrow(new RuntimeException("redis down"));

        SnowflakeIdGenerator gen = new SnowflakeIdGenerator("", redisson);
        assertTrue(gen.workerId() >= 0 && gen.workerId() <= 1023);
        assertTrue(gen.nextId() > 0);
    }

    @Test
    @DisplayName("不同 workerId 的生成器同毫秒发号不冲突")
    void distinctWorkersNoClash() {
        SnowflakeIdGenerator g1 = new SnowflakeIdGenerator("1", null);
        SnowflakeIdGenerator g2 = new SnowflakeIdGenerator("2", null);
        long a = g1.nextId();
        long b = g2.nextId();
        assertNotEquals(a, b);
        assertEquals(1L, (a >> 12) & 1023);
        assertEquals(2L, (b >> 12) & 1023);
    }
}
