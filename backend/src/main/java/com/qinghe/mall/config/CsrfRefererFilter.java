package com.qinghe.mall.config;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 基础 CSRF 防护：对状态变更请求（POST/PUT/DELETE/PATCH）校验 Referer/Origin 来源。
 *
 * <p>策略：请求必须携带 Referer 或 Origin，且解析出的 Origin（scheme://host[:port]）
 * 属于配置白名单（app.security.csrf.allowed-origins），否则返回 403。
 * 可通过 app.security.csrf.enabled=false 关闭（仅建议纯 API/内部调用场景关闭）。</p>
 */
@Component
public class CsrfRefererFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CsrfRefererFilter.class);

    private static final Set<String> SAFE_METHODS = new HashSet<>(Arrays.asList("GET", "HEAD", "OPTIONS", "TRACE"));

    @Value("${app.security.csrf.enabled:true}")
    private boolean enabled;

    @Value("${app.security.csrf.allowed-origins:}")
    private String allowedOriginsConfig;

    /** 无需校验来源的路径前缀（逗号分隔）：如微信支付异步回调（服务器到服务器，无 Referer） */
    @Value("${app.security.csrf.exclude-paths:/api/pay/wechatNotify}")
    private String excludePaths;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!enabled || SAFE_METHODS.contains(request.getMethod().toUpperCase()) || isExcluded(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String referer = request.getHeader("Referer");
        String origin = request.getHeader("Origin");
        boolean allowed = isAllowed(origin) || isAllowed(referer);

        if (!allowed) {
            log.warn("[CSRF] 拒绝非法来源请求: method={}, uri={}, referer={}, origin={}",
                    request.getMethod(), request.getRequestURI(), referer, origin);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"code\":403,\"message\":\"非法请求来源\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 判断 url 解析出的 Origin（scheme://host[:port]）是否在白名单内。
     * 对 Referer 和 Origin 头均适用；null/非法 URL 一律视为不允许。
     */
    private boolean isAllowed(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String origin = extractOrigin(url);
        if (origin == null) {
            return false;
        }
        for (String item : allowedOrigins().split(",")) {
            if (item.trim().equalsIgnoreCase(origin)) {
                return true;
            }
        }
        return false;
    }

    /** 从 URL/Referer 中提取 origin（scheme://host[:port]），解析失败返回 null。 */
    private String extractOrigin(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            String origin = uri.getScheme().toLowerCase() + "://" + uri.getHost().toLowerCase();
            int port = uri.getPort();
            if (port > 0) {
                origin += ":" + port;
            }
            return origin;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private String allowedOrigins() {
        return allowedOriginsConfig == null ? "" : allowedOriginsConfig;
    }

    /** 判断请求 URI 是否命中排除路径前缀（微信回调等服务器间调用） */
    private boolean isExcluded(String uri) {
        if (uri == null || excludePaths == null || excludePaths.isEmpty()) {
            return false;
        }
        for (String prefix : excludePaths.split(",")) {
            if (!prefix.trim().isEmpty() && uri.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }
}
