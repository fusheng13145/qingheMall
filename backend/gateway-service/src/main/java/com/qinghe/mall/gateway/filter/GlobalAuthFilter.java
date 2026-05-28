package com.qinghe.mall.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT认证全局过滤器
 */
@Component
public class GlobalAuthFilter implements GlobalFilter, Ordered {

    /**
     * 白名单路径
     */
    private static final List<String> WHITE_LIST = List.of(
            "/user/auth/login",
            "/user/auth/register",
            "/goods/",
            "/category/",
            "/health"
    );

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 获取Token
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        // 验证Token
        Long userId = validateToken(token);
        if (userId == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 将用户ID设置到请求头，转发给下游服务
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", String.valueOf(userId))
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhiteListed(String path) {
        for (String whitePath : WHITE_LIST) {
            if (path.startsWith(whitePath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证Token并获取用户ID
     */
    private Long validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}