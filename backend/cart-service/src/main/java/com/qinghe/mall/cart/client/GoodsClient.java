package com.qinghe.mall.cart.client;

import com.qinghe.mall.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 商品服务Feign客户端
 */
@FeignClient(name = "goods-service", fallback = GoodsClientFallback.class)
public interface GoodsClient {

    /**
     * 获取商品信息
     */
    @GetMapping("/api/goods/{id}")
    R<Map<String, Object>> getGoods(@PathVariable("id") Long id);

    /**
     * 批量获取商品信息
     */
    @GetMapping("/api/goods/batch")
    R<Map<Long, Map<String, Object>>> getGoodsBatch(@RequestParam("ids") String ids);

    /**
     * 校验商品库存
     */
    @GetMapping("/api/goods/{id}/stock")
    R<Integer> getStock(@PathVariable("id") Long id);
}
