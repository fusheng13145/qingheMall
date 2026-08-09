package com.qinghe.mall.config;

import java.time.Duration;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 显式配置。
 *
 * 背景：redisson-spring-boot-starter 的自动配置在 redis 密码为空字符串时
 * 仍会执行 AUTH 命令，导致「本地 Redis 未设置密码」的场景连接失败（ERR Client sent AUTH...）。
 * 本配置自行读取 spring.data.redis.*（Boot 3 命名空间）构建 RedissonClient，密码为空时跳过 AUTH。
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.timeout:5000ms}")
    private Duration timeout;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer();
        singleServerConfig.setAddress("redis://" + host + ":" + port);
        // 密码为空（本地无密码 Redis）时不发送 AUTH
        if (StringUtils.isNotBlank(password)) {
            singleServerConfig.setPassword(password);
        }
        if (timeout != null) {
            singleServerConfig.setTimeout(Math.toIntExact(timeout.toMillis()));
        }
        return Redisson.create(config);
    }
}
