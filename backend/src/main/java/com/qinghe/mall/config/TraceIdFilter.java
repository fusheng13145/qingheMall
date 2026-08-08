package com.qinghe.mall.config;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

/**
 * 链路追踪过滤器（P2-2 监控）。
 *
 * 为每个请求生成/透传 traceId：
 * - 优先透传请求头 X-Trace-Id（网关/上游传入，跨服务串联）
 * - 缺失时生成 UUID 前 8 位
 * - 写入 MDC（日志 pattern %X{traceId} 输出）与响应头 X-Trace-Id（前端/排查可回溯）
 *
 * 注意：MDC 是线程局部变量，必须用 finally 清理，防止线程池复用导致 traceId 串线。
 */
public class TraceIdFilter implements Filter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (StringUtils.isBlank(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put("traceId", traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            chain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.remove("traceId");
        }
    }
}
