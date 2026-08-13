package com.qinghe.mall.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vault KV v2 HTTP 客户端（#24 / #38 密钥托管）。
 *
 * <p>设计取舍：本机无 Vault、Docker 不可用，且本地 Maven 仓库无 spring-vault-core（离线编译会失败）。
 * 故不引入额外依赖，直接使用项目既有的 spring-web(RestTemplate) + jackson 对接 Vault 原生 HTTP API，
 * 仅承担"读取一段 KV v2 密钥"这一最小职责，便于离线单测（MockRestServiceServer 模拟 Vault 端点）。</p>
 *
 * <p>取数路径：<code>GET {uri}/v1/{secretPath}</code>  Header: <code>X-Vault-Token</code>
 * 响应（KV v2）：<code>{ "data": { "data": { &lt;secrets&gt; }, "metadata": { ... } } }</code></p>
 *
 * <p>这是真实 Vault 集成（非桩），prod 启用后由 {@link VaultEnvironmentPostProcessor} 注入 Spring Environment，
 * 使现有 {@code @Value("${alipay.*}")} / {@code @Value("${wechat.pay.*}")} / {@code @Value("${spring.datasource.password}")}
 * 等零改动即可从 Vault 取数；dev/test 关闭时本类不被调用，密钥走既有环境变量兜底。</p>
 */
public class VaultSecretFetcher {

    private final RestTemplate restTemplate;
    private final String uri;
    private final String token;
    private final String secretPath;
    private final String namespace;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VaultSecretFetcher(RestTemplate restTemplate, String uri, String token, String secretPath, String namespace) {
        this.restTemplate = restTemplate;
        this.uri = uri;
        this.token = token;
        this.secretPath = secretPath;
        this.namespace = namespace;
    }

    /**
     * 从 Vault KV v2 拉取密钥，返回 扁平 key→value 映射（与 Spring 属性键对齐，如 alipay.private-key）。
     *
     * @throws IllegalStateException 网络不可达 / 非 2xx / 响应缺少 data.data（路径不存在或非 KV v2）
     */
    public Map<String, Object> fetchSecrets() {
        String url = buildUrl();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set("X-Vault-Token", token);
        if (namespace != null && !namespace.isBlank()) {
            headers.set("X-Vault-Namespace", namespace);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (Exception e) {
            throw new IllegalStateException("Vault 密钥拉取失败（无法连接 " + url + "）：" + e.getMessage(), e);
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Vault 密钥拉取失败：HTTP " + response.getStatusCode() + " from " + url);
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.path("data").path("data");
            if (data.isMissingNode() || !data.isObject()) {
                throw new IllegalStateException(
                        "Vault 响应缺少 data.data 字段（路径 " + secretPath + " 不存在或为非 KV v2 格式）");
            }
            // 转成普通 Java 类型（TextNode→String 等），避免下游 @Value 取到 JsonNode 而非标量值
            @SuppressWarnings("unchecked")
            Map<String, Object> secrets = objectMapper.convertValue(data, Map.class);
            return secrets;
        } catch (Exception e) {
            throw new IllegalStateException("Vault 响应解析失败：" + e.getMessage(), e);
        }
    }

    private String buildUrl() {
        String base = uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
        String path = secretPath.startsWith("/") ? secretPath.substring(1) : secretPath;
        return base + "/v1/" + path;
    }

    /**
     * 构造关闭默认错误处理的 RestTemplate：Vault 返回 4xx/5xx 时不抛异常，
     * 交由 {@link #fetchSecrets()} 显式检查状态码并给出清晰错误信息（如令牌失效 403）。
     */
    public static RestTemplate createRestTemplate() {
        RestTemplate rt = new RestTemplate();
        rt.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return rt;
    }

    /** 仅供单测断言请求 URL 拼接正确性（包级可见） */
    String getRequestUrl() {
        return buildUrl();
    }
}
