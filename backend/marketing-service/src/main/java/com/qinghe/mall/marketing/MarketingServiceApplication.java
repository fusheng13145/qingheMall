package com.qinghe.mall.marketing;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 营销服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.qinghe.mall.marketing.mapper")
public class MarketingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketingServiceApplication.class, args);
    }
}
