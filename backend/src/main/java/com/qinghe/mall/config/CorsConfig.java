package com.qinghe.mall.config;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /** 允许携带凭证的跨域来源白名单（与 CSRF Referer 白名单保持一致） */
    @Value("${app.security.csrf.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:8080,http://127.0.0.1:8080}")
    private String allowedOriginsConfig;

    /** 本地上传目录（M3-4 图片本地化），通过 /uploads/** 对外访问 */
    @Value("${app.upload.dir:./upload}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 安全收紧：不再允许任意来源携带凭证；仅放行配置中的白名单来源
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOriginsArray())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 本地上传文件静态映射：/uploads/xxx.jpg -> <uploadDir>/xxx.jpg
        String location = "file:" + new File(uploadDir).getAbsolutePath() + File.separator;
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    private String[] allowedOriginsArray() {
        List<String> list = Arrays.stream(allowedOriginsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return list.toArray(new String[0]);
    }
}
