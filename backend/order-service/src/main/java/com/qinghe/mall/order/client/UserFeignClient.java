package com.qinghe.mall.order.client;

import com.qinghe.mall.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 用户服务Feign客户端
 */
@FeignClient(name = "user-service", path = "/api/user")
public interface UserFeignClient {

    /**
     * 获取用户地址详情
     */
    @GetMapping("/address/{id}")
    R<Map<String, Object>> getAddress(@PathVariable("id") Long addressId);
}
