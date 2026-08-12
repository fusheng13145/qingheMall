package com.qinghe.mall.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qinghe.mall.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 限流切面 单元测试（P2-3，T3 扩展）。
 * 覆盖：令牌充足放行、令牌耗尽抛 RateLimitException、
 * 以及 resolveKey 的 {userId} 维度分支（无 RequestContext / 有请求但无 userId / 有 userId）。
 */
class RateLimitAspectTest {

    /** 不带 key（默认全局限流：类名.方法名） */
    static class DemoService {
        @RateLimit(rate = 2, message = "too fast")
        public String hello() {
            return "ok";
        }
    }

    /** 带 key={userId}（按登录用户维度限流） */
    static class DemoUserService {
        @RateLimit(key = "{userId}", rate = 2, message = "too fast")
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

    private RateLimit userAnnotation() throws Exception {
        Method method = DemoUserService.class.getMethod("hello");
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

    // ============ {userId} 维度分支 ============

    @Test
    void userKey_withoutRequestContext_fallsBackToClassName() throws Throwable {
        stubUserKey();
        RequestContextHolder.resetRequestAttributes();

        Object r = aspect.around(joinPoint, userAnnotation());
        assertEquals("ok", r);
    }

    @Test
    void userKey_requestButNoUserId_fallsBackToClassName() throws Throwable {
        stubUserKey();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpSession sess = mock(HttpSession.class);
        when(req.getSession()).thenReturn(sess);
        when(sess.getAttribute("userId")).thenReturn(null);
        ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
        when(attrs.getRequest()).thenReturn(req);
        RequestContextHolder.setRequestAttributes(attrs);

        Object r = aspect.around(joinPoint, userAnnotation());
        assertEquals("ok", r);
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void userKey_withUserId_resolvesUserDimension() throws Throwable {
        stubUserKey();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpSession sess = mock(HttpSession.class);
        when(req.getSession()).thenReturn(sess);
        when(sess.getAttribute("userId")).thenReturn("42");
        ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
        when(attrs.getRequest()).thenReturn(req);
        RequestContextHolder.setRequestAttributes(attrs);

        Object r = aspect.around(joinPoint, userAnnotation());
        assertEquals("ok", r);
        RequestContextHolder.resetRequestAttributes();
    }

    private void stubUserKey() throws Throwable {
        when(limiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(false);
        when(limiter.tryAcquire()).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");
        when(signature.getDeclaringType()).thenReturn(DemoUserService.class);
        when(signature.getMethod()).thenReturn(DemoUserService.class.getMethod("hello"));
    }
}
