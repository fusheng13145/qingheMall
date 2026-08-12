package com.qinghe.mall.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * CsrfRefererFilter 分支覆盖（T3 续补）：覆盖 enabled 开关、安全方法短路、
 * 排除路径、Origin/Referer 白名单命中/未命中、非法 URL、带端口 Origin 等分支。
 * 通过直接调用 doFilterInternal 并注入 @Value 字段（standalone 无 Spring 注入）。
 */
class CsrfRefererFilterTest {

    private CsrfRefererFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CsrfRefererFilter();
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "allowedOriginsConfig", "http://good.com,https://good.com:8443");
        ReflectionTestUtils.setField(filter, "excludePaths", "/api/pay/wechatNotify,/api/pay/alipayNotify");
    }

    private static final class RecordingChain implements FilterChain {
        boolean called = false;
        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) { called = true; }
    }

    @Test
    void post_noOriginReferer_rejected_403() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/order/add");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();
        filter.doFilter(req, resp, chain);
        assertEquals(403, resp.getStatus());
        org.junit.jupiter.api.Assertions.assertFalse(chain.called);
    }

    @Test
    void post_allowedOrigin_passes() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/order/add");
        req.addHeader("Origin", "http://good.com");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();
        filter.doFilter(req, resp, chain);
        org.junit.jupiter.api.Assertions.assertTrue(chain.called);
    }

    @Test
    void post_allowedReferer_passes() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/order/add");
        req.addHeader("Referer", "http://good.com/x");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();
        filter.doFilter(req, resp, chain);
        org.junit.jupiter.api.Assertions.assertTrue(chain.called);
    }

    @Test
    void get_safeMethod_passes() throws Exception {
        // 覆盖 SAFE_METHODS 短路分支
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/order/list");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();
        filter.doFilter(req, resp, chain);
        org.junit.jupiter.api.Assertions.assertTrue(chain.called);
    }

    @Test
    void post_excludedPath_passes() throws Exception {
        // 覆盖 isExcluded 命中分支（微信回调等服务器间调用）
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/pay/wechatNotify");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();
        filter.doFilter(req, resp, chain);
        org.junit.jupiter.api.Assertions.assertTrue(chain.called);
    }

    @Test
    void disabled_bypassesAll() throws Exception {
        // 覆盖 enabled=false 短路分支
        ReflectionTestUtils.setField(filter, "enabled", false);
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/order/add");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();
        filter.doFilter(req, resp, chain);
        org.junit.jupiter.api.Assertions.assertTrue(chain.called);
    }

    @Test
    void post_originWithPort_passes() throws Exception {
        // 覆盖 extractOrigin 中 port>0 分支
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/order/add");
        req.addHeader("Origin", "https://good.com:8443");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();
        filter.doFilter(req, resp, chain);
        org.junit.jupiter.api.Assertions.assertTrue(chain.called);
    }

    @Test
    void post_invalidOriginUrl_rejected() throws Exception {
        // 覆盖 extractOrigin 解析失败返回 null → isAllowed false 分支
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/order/add");
        req.addHeader("Origin", "not-a-valid-url");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();
        filter.doFilter(req, resp, chain);
        assertEquals(403, resp.getStatus());
    }

    @Test
    void post_nonWhitelistedOrigin_rejected() throws Exception {
        // 覆盖白名单遍历无匹配分支
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/order/add");
        req.addHeader("Origin", "http://evil.com");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();
        filter.doFilter(req, resp, chain);
        assertEquals(403, resp.getStatus());
        org.junit.jupiter.api.Assertions.assertFalse(chain.called);
    }
}
