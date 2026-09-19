package com.qinghe.mall.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.exception.RateLimitException;
import org.mockito.ArgumentCaptor;
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

    /** 带前缀 + {userId}（按接口 + 登录用户维度限流，前缀应被保留） */
    static class DemoPrefixedService {
        @RateLimit(key = "order.add.{userId}", rate = 2, message = "too fast")
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

    private RateLimit prefixedAnnotation() throws Exception {
        Method method = DemoPrefixedService.class.getMethod("hello");
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

    @Test
    void rateLimitRejection_incrementsCounter_withNormalizedKey() throws Throwable {
        // C2：限流命中打点（meterRegistry 就绪时），per-user 桶 key 归一化为接口维度
        io.micrometer.core.instrument.simple.SimpleMeterRegistry registry =
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        ReflectionTestUtils.setField(aspect, "meterRegistry", registry);
        stubPrefixedKey();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpSession sess = mock(HttpSession.class);
        when(req.getSession()).thenReturn(sess);
        when(sess.getAttribute("userId")).thenReturn("42");
        ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
        when(attrs.getRequest()).thenReturn(req);
        RequestContextHolder.setRequestAttributes(attrs);

        when(limiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(false);
        when(limiter.tryAcquire()).thenReturn(false);
        assertThrows(RateLimitException.class, () -> aspect.around(joinPoint, prefixedAnnotation()));

        // limiterKey=order.add.user:42 → tag 归一化 order.add.user:*（防 tag 基数膨胀）
        assertEquals(1.0, registry.get("qinghe.ratelimit.rejected")
                .tag("key", "order.add.user:*").counter().count());
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void rateLimitRejection_withoutRegistry_skipsMetric() throws Exception {
        // meterRegistry 未注入（纯 mock 单测环境）→ 不打点、仍正常拒绝
        when(limiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(false);
        when(limiter.tryAcquire()).thenReturn(false);
        assertThrows(RateLimitException.class, () -> aspect.around(joinPoint, annotation()));
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

    @Test
    void userKey_withPrefix_resolvesPrefixedUserDimension_andCapturesKey() throws Throwable {
        stubPrefixedKey();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpSession sess = mock(HttpSession.class);
        when(req.getSession()).thenReturn(sess);
        when(sess.getAttribute("userId")).thenReturn("42");
        ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
        when(attrs.getRequest()).thenReturn(req);
        RequestContextHolder.setRequestAttributes(attrs);

        Object r = aspect.around(joinPoint, prefixedAnnotation());
        assertEquals("ok", r);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redissonClient).getRateLimiter(captor.capture());
        assertEquals("ratelimit:order.add.user:42", captor.getValue());
        RequestContextHolder.resetRequestAttributes();
    }

    private void stubUserKey() throws Throwable {
        when(limiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(false);
        when(limiter.tryAcquire()).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");
        when(signature.getDeclaringType()).thenReturn(DemoUserService.class);
        when(signature.getMethod()).thenReturn(DemoUserService.class.getMethod("hello"));
    }

    private void stubPrefixedKey() throws Throwable {
        when(limiter.trySetRate(any(), anyLong(), anyLong(), any())).thenReturn(false);
        when(limiter.tryAcquire()).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");
        when(signature.getDeclaringType()).thenReturn(DemoPrefixedService.class);
        when(signature.getMethod()).thenReturn(DemoPrefixedService.class.getMethod("hello"));
    }
}
