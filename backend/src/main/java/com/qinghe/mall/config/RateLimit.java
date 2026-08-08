package com.qinghe.mall.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解（P2-3 限流防刷）。
 *
 * 基于 Redisson RRateLimiter 令牌桶实现：
 * - rate：令牌补充速率（每秒放行数量；桶容量隐含等于速率）
 * - key：限流维度。支持两种写法：
 *   - 留空：按「类名.方法名」全局限流（所有请求共享同一桶）
 *   - 含 {userId}：按登录用户维度限流（从 Session 取 userId）
 *
 * 超出速率时切面抛出 RateLimitException → HTTP 429。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 限流维度 key；留空按接口全局限流；含 {userId} 则按登录用户维度 */
    String key() default "";

    /** 令牌补充速率：每秒放行数量 */
    double rate() default 10;

    /** 被限流时的提示消息 */
    String message() default "操作过于频繁，请稍后再试";
}
