package com.qinghe.mall.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Vault 密钥托管接入点（#24 / #38）。
 *
 * <p>以 {@link EnvironmentPostProcessor} 身份在「环境准备阶段」运行——早于任何
 * {@code @Configuration} bean 的属性解析。当 {@code vault.enabled=true} 时从 Vault KV v2 拉取密钥，
 * 并以<b>最高优先级</b>注入 Spring Environment（{@code addFirst}），使现有
 * {@code @Value("${alipay.*}")} / {@code @Value("${wechat.pay.*}")} / {@code @Value("${spring.datasource.password}")}
 * 等零改动即可从 Vault 取数。</p>
 *
 * <p>关闭（默认 dev/test，{@code vault.enabled=false}）：本处理器为空操作，密钥仍走既有的
 * 环境变量 {@code ${ENV:default}} 兜底，运行行为完全不变。</p>
 *
 * <p>排序：{@code LOWEST_PRECEDENCE}——必须晚于 {@code ConfigDataEnvironmentPostProcessor}（负责加载
 * application-*.properties）之后运行，方能读到 {@code vault.enabled}/{@code vault.uri} 等自身配置；
 * 同时仍远早于上下文刷新与 bean 实例化，故注入的属性源对全部 {@code @Value} 生效。</p>
 *
 * <p>注册：src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports</p>
 */
public class VaultEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private VaultSecretFetcher fetcher;
    private RestTemplate vaultRestTemplate = VaultSecretFetcher.createRestTemplate();

    /** 生产无参构造（由 Spring Boot 经 imports 文件实例化） */
    public VaultEnvironmentPostProcessor() {
    }

    /** 测试用：注入自定义 fetcher，避免真实网络 */
    VaultEnvironmentPostProcessor(VaultSecretFetcher fetcher) {
        this.fetcher = fetcher;
    }

    /** 测试用：注入自定义 RestTemplate（由 MockRestServiceServer 拦截），覆盖 fetchViaEnv 生产路径 */
    VaultEnvironmentPostProcessor(RestTemplate vaultRestTemplate) {
        this.vaultRestTemplate = vaultRestTemplate;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        boolean enabled = Boolean.parseBoolean(env.getProperty("vault.enabled", "false"));
        if (!enabled) {
            return;
        }

        Map<String, Object> secrets = (this.fetcher != null)
                ? this.fetcher.fetchSecrets()
                : fetchViaEnv(env);

        PropertySource<Map<String, Object>> source = new MapPropertySource("vault-secrets", secrets);
        if (env.getPropertySources().contains("vault-secrets")) {
            env.getPropertySources().replace("vault-secrets", source);
        } else {
            env.getPropertySources().addFirst(source);
        }
    }

    private Map<String, Object> fetchViaEnv(ConfigurableEnvironment env) {
        String uri = env.getRequiredProperty("vault.uri");
        String token = env.getRequiredProperty("vault.token");
        String secretPath = env.getRequiredProperty("vault.secret-path");
        String namespace = env.getProperty("vault.namespace");
        this.fetcher = new VaultSecretFetcher(this.vaultRestTemplate, uri, token, secretPath, namespace);
        return this.fetcher.fetchSecrets();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
