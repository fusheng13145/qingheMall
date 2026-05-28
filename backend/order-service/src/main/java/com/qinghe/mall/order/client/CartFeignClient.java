package com.qinghe.mall.order.client;

import com.qinghe.mall.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

/**
 * 购物车服务Feign客户端
 */
@FeignClient(name = "cart-service", path = "/api/cart")
public interface CartFeignClient {

    /**
     * 获取选中的购物车商品
     */
    @PostMapping("/items/selected")
    R<List<Map<String, Object>>> getSelectedItems(@RequestBody List<Long> cartIds);

    /**
     * 删除购物车商品
     */
    @PostMapping("/delete")
    R<Void> deleteByCartIds(@RequestBody List<Long> cartIds);
}
