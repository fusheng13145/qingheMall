package com.qinghe.mall.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 客户端配置（阶段二 B-② 全文检索）。
 *
 * 使用官方 Java API Client（elasticsearch-java 8.15，与 docker-compose 中
 * elasticsearch:8.15.0 对齐）。RestClient 懒连接：Bean 创建阶段不连 ES，
 * 应用启动不依赖 ES 可用性；搜索请求失败由调用方降级 MySQL。
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.hosts:http://localhost:9200}")
    private String hosts;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(HttpHost.create(hosts)).build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
