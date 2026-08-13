package com.qinghe.mall.config;

import com.qinghe.mall.exception.RateLimitException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 限流切面（P2-3 限流防刷）。
 *
 * 拦截标注 {@code @RateLimit} 的方法，通过 Redisson 分布式令牌桶限流：
 * - 同一 key 在所有实例间共享（RRateLimiter 基于 Redis，天然支持多实例）
 * - 取令牌失败抛 RateLimitException → GlobalExceptionHandler 转 429
 *
 * key 解析规则：
 * - 注解 key 含 {@code {userId}} → 从 Session 取 userId 拼入（按用户维度）
 * - 否则用 {@code 类名.方法名}（按接口全局限流）
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String limiterKey = resolveKey(joinPoint, rateLimit);

        RRateLimiter limiter = redissonClient.getRateLimiter("ratelimit:" + limiterKey);
        // 初始化令牌桶（幂等）：trySetRate 仅首次生效，桶容量隐含等于速率；
        // 后续请求只从桶中取令牌，不会重置配置。
        limiter.trySetRate(RateType.OVERALL, (long) rateLimit.rate(), 1, RateIntervalUnit.SECONDS);

        if (!limiter.tryAcquire()) {
            log.warn("rate limit hit: key={}", limiterKey);
            throw new RateLimitException(rateLimit.message());
        }
        return joinPoint.proceed();
    }

    /**
     * 解析限流维度 key：
     * 注解 key 含 {userId} 时按登录用户维度（支持前缀，如 "order.add.{userId}" → "order.add.user:42"），
     * 未登录（无 userId）回退到接口全局限流（类名.方法名），避免所有匿名请求共享同一 user:null 桶；
     * 注解 key 不含 {userId} 时按接口全局限流（类名.方法名）。
     */
    private String resolveKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        if (rateLimit.key() != null && rateLimit.key().contains("{userId}")) {
            HttpServletRequest request = currentRequest();
            Object userId = request == null ? null : request.getSession().getAttribute("userId");
            if (userId != null) {
                return rateLimit.key().replace("{userId}", "user:" + userId);
            }
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getMethod().getName();
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
