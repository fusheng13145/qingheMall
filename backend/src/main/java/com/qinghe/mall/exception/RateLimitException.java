package com.qinghe.mall.exception;

/**
 * 限流异常（P2-3 限流防刷）。
 *
 * 由 {@code @RateLimit} 切面在令牌桶取令牌失败时抛出，
 * GlobalExceptionHandler 将其转换为 Result.fail(429, message)。
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
