package com.qinghe.mall.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 过滤器注册（P2-2）。
 *
 * TraceIdFilter 放在最前（order=1），保证所有请求（含 CSRF 过滤器之前）都有 traceId，
 * 后续过滤器/业务异常日志均可通过 %X{traceId} 关联。
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}
