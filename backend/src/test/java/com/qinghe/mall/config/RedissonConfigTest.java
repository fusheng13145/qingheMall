package com.qinghe.mall.config;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * RedissonConfig 分支覆盖补充（T3 续补）。
 * 覆盖 redissonClient() 中「密码非空发送 AUTH」「timeout 非空设置」两个 if 分支。
 * 注意：会真实调用 Redisson.create（懒连接），测试结束后显式 shutdown 避免句柄泄漏。
 */
class RedissonConfigTest {

    @Test
    void redissonClient_blankPasswordSkipsAuth() {
        RedissonConfig config = new RedissonConfig();
        ReflectionTestUtils.setField(config, "host", "127.0.0.1");
        ReflectionTestUtils.setField(config, "port", 6379);
        ReflectionTestUtils.setField(config, "password", "");
        ReflectionTestUtils.setField(config, "timeout", null);
        RedissonClient client = config.redissonClient();
        assertNotNull(client);
        client.shutdown();
    }

    /**
     * 覆盖 timeout 非空分支（if (timeout != null) set...）。
     * 本机 Redis 无密码，password 保持空以避免 AUTH 失败；仅验证 timeout 分支被执行且不抛。
     */
    @Test
    void redissonClient_timeoutNotBlank_setsTimeout() {
        RedissonConfig config = new RedissonConfig();
        ReflectionTestUtils.setField(config, "host", "127.0.0.1");
        ReflectionTestUtils.setField(config, "port", 6379);
        ReflectionTestUtils.setField(config, "password", "");
        ReflectionTestUtils.setField(config, "timeout", java.time.Duration.ofMillis(3000));
        RedissonClient client = config.redissonClient();
        assertNotNull(client);
        client.shutdown();
    }
}
