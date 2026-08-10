package com.qinghe.mall.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * CSRF Referer 校验过滤器测试（原零覆盖）：
 * 白名单放行 / 非法来源 403 / 安全方法放行 / 排除路径放行 / 缺失来源拒绝 / 开关关闭放行。
 */
class CsrfRefererFilterTest {

    private CsrfRefererFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new CsrfRefererFilter();
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "allowedOriginsConfig", "http://localhost:5173,https://mall.example.com");
        ReflectionTestUtils.setField(filter, "excludePaths", "/api/pay/wechatNotify");
        chain = mock(FilterChain.class);
    }

    private MockHttpServletRequest post(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        return request;
    }

    @Test
    @DisplayName("白名单 Origin 放行")
    void allowedOriginPasses() throws Exception {
        MockHttpServletRequest request = post("/api/order/create");
        request.addHeader("Origin", "http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("白名单 Referer 放行（含端口）")
    void allowedRefererPasses() throws Exception {
        MockHttpServletRequest request = post("/api/cart/add");
        request.addHeader("Referer", "https://mall.example.com/cart");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("非法来源返回 403 且不进入链路")
    void evilOriginRejected() throws Exception {
        MockHttpServletRequest request = post("/api/order/create");
        request.addHeader("Origin", "https://evil.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("非法请求来源");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("GET 等安全方法跳过校验")
    void safeMethodBypasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/product/list");
        request.setRequestURI("/api/product/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("排除路径（微信支付回调）免校验")
    void excludedPathBypasses() throws Exception {
        MockHttpServletRequest request = post("/api/pay/wechatNotify");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("状态变更请求无 Referer/Origin 拒绝 403")
    void missingOriginRejected() throws Exception {
        MockHttpServletRequest request = post("/api/order/create");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("校验开关关闭时全部放行")
    void disabledBypasses() throws Exception {
        ReflectionTestUtils.setField(filter, "enabled", false);
        MockHttpServletRequest request = post("/api/order/create");
        request.addHeader("Origin", "https://evil.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("Origin 端口与白名单不一致时拒绝")
    void wrongPortRejected() throws Exception {
        MockHttpServletRequest request = post("/api/order/create");
        request.addHeader("Origin", "http://localhost:9999");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
    }
}
