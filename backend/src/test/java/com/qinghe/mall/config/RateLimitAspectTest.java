package com.qinghe.mall.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.qinghe.mall.exception.RateLimitException;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 限流切面 单元测试（P2-3）。
 * 覆盖：令牌充足放行、令牌耗尽抛 RateLimitException。
 */
class RateLimitAspectTest {

    /** 带 @RateLimit 的测试目标方法 */
    static class DemoService {
        @RateLimit(rate = 2, message = "too fast")
        public String hello() {
            return "ok";
        }
    }

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RRateLimiter limiter;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    private RateLimitAspect aspect;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        aspect = new RateLimitAspect();
        ReflectionTestUtils.setField(aspect, "redissonClient", redissonClient);

        when(redissonClient.getRateLimiter(anyString())).thenReturn(limiter);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(DemoService.class);
        when(signature.getMethod()).thenReturn(DemoService.class.getMethod("hello"));
    }

    private RateLimit annotation() throws Exception {
        Method method = DemoService.class.getMethod("hello");
        return method.getAnnotation(RateLimit.class);
    }

    @Test
    void shouldPassThrough_whenTokenAvailable() throws Throwable {
        when(limiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(false);
        when(limiter.tryAcquire()).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.around(joinPoint, annotation());
        assertEquals("ok", result);
    }

    @Test
    void shouldThrowRateLimitException_whenTokenExhausted() throws Exception {
        when(limiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(false);
        when(limiter.tryAcquire()).thenReturn(false);

        RateLimitException exception = assertThrows(RateLimitException.class,
                () -> aspect.around(joinPoint, annotation()));
        assertEquals("too fast", exception.getMessage());
    }
}
