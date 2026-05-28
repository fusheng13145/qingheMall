package com.qinghe.mall.order.client;

import com.qinghe.mall.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

/**
 * 商品服务Feign客户端
 */
@FeignClient(name = "goods-service", path = "/api/goods")
public interface GoodsFeignClient {

    /**
     * 扣减库存
     */
    @PostMapping("/stock/deduct")
    R<Void> deductStock(@RequestBody Map<Long, Integer> goodsQuantityMap);
}
