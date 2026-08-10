package com.qinghe.mall.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * 链路追踪过滤器测试（原零覆盖）：
 * 透传 X-Trace-Id / 缺失时生成 / 链路期间写入 MDC 与响应头 / 返回后 finally 清理防串线。
 */
class TraceIdFilterTest {

    private TraceIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TraceIdFilter();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    /** 在 chain 执行期间捕获 MDC 值（过滤器返回后 MDC 已被 finally 清理，需在链路内断言） */
    private AtomicReference<String> captureMdcInChain() throws Exception {
        AtomicReference<String> mdcInChain = new AtomicReference<>();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            mdcInChain.set(MDC.get("traceId"));
            return null;
        }).when(chain).doFilter(any(), any());
        return mdcInChain;
    }

    @Test
    @DisplayName("请求带 X-Trace-Id 时透传并写入响应头，链路内 MDC 有值")
    void passesThroughIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-1234567890");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcInChain = captureMdcInChain();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            mdcInChain.set(MDC.get("traceId"));
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo("trace-1234567890");
        assertThat(mdcInChain.get()).isEqualTo("trace-1234567890");
    }

    @Test
    @DisplayName("请求缺失 X-Trace-Id 时自动生成 16 位 traceId")
    void generatesTraceIdWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcInChain = captureMdcInChain();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            mdcInChain.set(MDC.get("traceId"));
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        String traceId = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertThat(traceId).isNotBlank().hasSize(16);
        assertThat(mdcInChain.get()).isEqualTo(traceId);
    }

    @Test
    @DisplayName("filter 返回后 MDC 被清理（防线程池复用串线）")
    void cleansMdcAfterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain());

        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    @DisplayName("chain 异常时 MDC 仍被清理（finally 语义）")
    void cleansMdcEvenWhenChainThrows() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-xyz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain throwingChain = mock(FilterChain.class);
        org.mockito.Mockito.doThrow(new RuntimeException("downstream failed"))
                .when(throwingChain).doFilter(any(), any());

        try {
            filter.doFilter(request, response, throwingChain);
        } catch (RuntimeException expected) {
            // 预期下游异常向上传播
        }

        assertThat(MDC.get("traceId")).isNull();
    }

    private FilterChain chain() {
        return mock(FilterChain.class);
    }
}
