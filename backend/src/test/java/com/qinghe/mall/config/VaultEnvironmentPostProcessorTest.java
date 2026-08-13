package com.qinghe.mall.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * VaultEnvironmentPostProcessor 离线单测：验证 vault.enabled 开关下的条件注入行为，
 * 以及注入属性源的最高优先级（addFirst）与不重复替换。
 */
class VaultEnvironmentPostProcessorTest {

    private StandardEnvironment envWith(String key, String value) {
        StandardEnvironment env = new StandardEnvironment();
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(key, value);
        env.getPropertySources().addFirst(new MapPropertySource("test-props", props));
        return env;
    }

    @Test
    void disabled_isNoOp() {
        StandardEnvironment env = envWith("vault.enabled", "false");
        new VaultEnvironmentPostProcessor().postProcessEnvironment(env, null);

        assertNull(env.getPropertySources().get("vault-secrets"));
        assertNull(env.getProperty("alipay.private-key"));
    }

    @Test
    void enabled_injectsVaultSecretsSourceFirst() {
        StandardEnvironment env = envWith("vault.enabled", "true");

        Map<String, Object> fake = new LinkedHashMap<>();
        fake.put("alipay.private-key", "PK-FROM-VAULT");
        fake.put("wechat.pay.api-v3-key", "K-FROM-VAULT");
        VaultSecretFetcher fetcher = new VaultSecretFetcher(null, "", "", "", null) {
            @Override
            public Map<String, Object> fetchSecrets() {
                return fake;
            }
        };
        new VaultEnvironmentPostProcessor(fetcher).postProcessEnvironment(env, null);

        PropertySource<?> source = env.getPropertySources().get("vault-secrets");
        assertNotNull(source, "vault-secrets 属性源应已注入");
        assertEquals("PK-FROM-VAULT", env.getProperty("alipay.private-key"));
        assertEquals("K-FROM-VAULT", env.getProperty("wechat.pay.api-v3-key"));
        // 最高优先级：位于属性源列表首位
        assertEquals("vault-secrets", env.getPropertySources().stream().findFirst().get().getName());
    }

    @Test
    void enabled_replaceExisting_doesNotDuplicate() {
        StandardEnvironment env = envWith("vault.enabled", "true");
        env.getPropertySources().addFirst(new MapPropertySource("vault-secrets", new LinkedHashMap<>()));

        Map<String, Object> fake = new LinkedHashMap<>();
        fake.put("x", "y");
        VaultSecretFetcher fetcher = new VaultSecretFetcher(null, "", "", "", null) {
            @Override
            public Map<String, Object> fetchSecrets() {
                return fake;
            }
        };
        new VaultEnvironmentPostProcessor(fetcher).postProcessEnvironment(env, null);

        long count = env.getPropertySources().stream()
                .filter(s -> "vault-secrets".equals(s.getName())).count();
        assertEquals(1, count, "vault-secrets 不应重复");
        assertEquals("y", env.getProperty("x"));
    }

    @Test
    void enabled_fetchViaEnv_realPath_withMockServer() {
        // 覆盖生产路径：未注入 fetcher → 走 fetchViaEnv，使用注入的 RestTemplate（由 Mock 拦截）
        RestTemplate rt = VaultSecretFetcher.createRestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rt).build();
        server.expect(requestTo("http://localhost:8200/v1/secret/qinghe-mall"))
                .andRespond(withSuccess(
                        "{\"data\":{\"data\":{\"spring.datasource.password\":\"DBPASS\"}}}",
                        MediaType.APPLICATION_JSON));

        StandardEnvironment env = new StandardEnvironment();
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("vault.enabled", "true");
        props.put("vault.uri", "http://localhost:8200");
        props.put("vault.token", "tok");
        props.put("vault.secret-path", "secret/qinghe-mall");
        env.getPropertySources().addFirst(new MapPropertySource("test-props", props));

        new VaultEnvironmentPostProcessor(rt).postProcessEnvironment(env, null);

        assertEquals("DBPASS", env.getProperty("spring.datasource.password"));
        server.verify();
    }
}
