package com.qinghe.mall.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * VaultSecretFetcher 离线单测：用 MockRestServiceServer 模拟 Vault KV v2 端点，
 * 验证 URL 拼接、X-Vault-Token / X-Vault-Namespace 头、data.data 解析与错误路径。
 */
class VaultSecretFetcherTest {

    @Test
    void fetchSecrets_success_parsesDataData() {
        RestTemplate rt = VaultSecretFetcher.createRestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();
        VaultSecretFetcher fetcher = new VaultSecretFetcher(rt, "http://localhost:8200/", "tok-123", "secret/qinghe-mall", null);

        server.expect(requestTo("http://localhost:8200/v1/secret/qinghe-mall"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Vault-Token", "tok-123"))
                .andRespond(withSuccess(
                        "{\"data\":{\"data\":{\"alipay.private-key\":\"PK\",\"wechat.pay.api-v3-key\":\"K\"},\"metadata\":{}}}",
                        MediaType.APPLICATION_JSON));

        Map<String, Object> secrets = fetcher.fetchSecrets();
        assertEquals("PK", secrets.get("alipay.private-key"));
        assertEquals("K", secrets.get("wechat.pay.api-v3-key"));
        assertEquals("http://localhost:8200/v1/secret/qinghe-mall", fetcher.getRequestUrl());
        server.verify();
    }

    @Test
    void fetchSecrets_namespace_header_sent() {
        RestTemplate rt = VaultSecretFetcher.createRestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();
        VaultSecretFetcher fetcher = new VaultSecretFetcher(rt, "https://vault:8200", "t", "secret/qinghe-mall", "ns1");

        server.expect(requestTo("https://vault:8200/v1/secret/qinghe-mall"))
                .andExpect(header("X-Vault-Namespace", "ns1"))
                .andRespond(withSuccess("{\"data\":{\"data\":{\"a\":\"b\"}}}", MediaType.APPLICATION_JSON));

        Map<String, Object> secrets = fetcher.fetchSecrets();
        assertEquals("b", secrets.get("a"));
        server.verify();
    }

    @Test
    void fetchSecrets_forbidden_throws() {
        RestTemplate rt = VaultSecretFetcher.createRestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();
        VaultSecretFetcher fetcher = new VaultSecretFetcher(rt, "http://localhost:8200", "bad", "secret/qinghe-mall", null);

        server.expect(requestTo("http://localhost:8200/v1/secret/qinghe-mall"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        IllegalStateException ex = assertThrows(IllegalStateException.class, fetcher::fetchSecrets);
        assertTrue(ex.getMessage().contains("HTTP 403"), ex.getMessage());
        server.verify();
    }

    @Test
    void fetchSecrets_missingDataData_throws() {
        RestTemplate rt = VaultSecretFetcher.createRestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();
        VaultSecretFetcher fetcher = new VaultSecretFetcher(rt, "http://localhost:8200", "t", "secret/qinghe-mall", null);

        server.expect(requestTo("http://localhost:8200/v1/secret/qinghe-mall"))
                .andRespond(withSuccess("{\"data\":{\"metadata\":{}}}", MediaType.APPLICATION_JSON));

        IllegalStateException ex = assertThrows(IllegalStateException.class, fetcher::fetchSecrets);
        assertTrue(ex.getMessage().contains("data.data"), ex.getMessage());
        server.verify();
    }

    @Test
    void fetchSecrets_dataIsArray_throws() {
        RestTemplate rt = VaultSecretFetcher.createRestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();
        VaultSecretFetcher fetcher = new VaultSecretFetcher(rt, "http://localhost:8200", "t", "secret/qinghe-mall", null);

        server.expect(requestTo("http://localhost:8200/v1/secret/qinghe-mall"))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        IllegalStateException ex = assertThrows(IllegalStateException.class, fetcher::fetchSecrets);
        assertTrue(ex.getMessage().contains("data.data"), ex.getMessage());
        server.verify();
    }

    @Test
    void fetchSecrets_nullBody_throws() {
        RestTemplate rt = VaultSecretFetcher.createRestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();
        VaultSecretFetcher fetcher = new VaultSecretFetcher(rt, "http://localhost:8200", "t", "secret/qinghe-mall", null);

        server.expect(requestTo("http://localhost:8200/v1/secret/qinghe-mall"))
                .andRespond(withStatus(HttpStatus.OK)); // 200 但无响应体 → getBody() 为 null

        IllegalStateException ex = assertThrows(IllegalStateException.class, fetcher::fetchSecrets);
        assertTrue(ex.getMessage().contains("HTTP 200"), ex.getMessage());
        server.verify();
    }

    @Test
    void fetchSecrets_invalidJson_throws() {
        RestTemplate rt = VaultSecretFetcher.createRestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();
        VaultSecretFetcher fetcher = new VaultSecretFetcher(rt, "http://localhost:8200", "t", "secret/qinghe-mall", null);

        server.expect(requestTo("http://localhost:8200/v1/secret/qinghe-mall"))
                .andRespond(withSuccess("not-json", MediaType.TEXT_PLAIN));

        IllegalStateException ex = assertThrows(IllegalStateException.class, fetcher::fetchSecrets);
        assertTrue(ex.getMessage().contains("解析失败"), ex.getMessage());
        server.verify();
    }

    @Test
    void fetchSecrets_networkFailure_throws() {
        RestTemplate rt = VaultSecretFetcher.createRestTemplate();
        // 指向不可达地址，触发连接异常（外層 catch 转 IllegalStateException）
        VaultSecretFetcher fetcher = new VaultSecretFetcher(rt, "http://127.0.0.1:1", "t", "secret/qinghe-mall", null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, fetcher::fetchSecrets);
        assertTrue(ex.getMessage().contains("无法连接"), ex.getMessage());
    }

    @Test
    void buildUrl_noTrailingSlash_uri() {
        VaultSecretFetcher fetcher = new VaultSecretFetcher(null, "http://localhost:8200", "t", "/secret/qinghe-mall", null);
        assertEquals("http://localhost:8200/v1/secret/qinghe-mall", fetcher.getRequestUrl());
    }
}
