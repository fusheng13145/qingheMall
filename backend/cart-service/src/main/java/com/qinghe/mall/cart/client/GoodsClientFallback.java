package com.qinghe.mall.cart.client;

import com.qinghe.mall.common.R;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品服务降级处理
 */
@Component
public class GoodsClientFallback implements GoodsClient {

    @Override
    public R<Map<String, Object>> getGoods(Long id) {
        Map<String, Object> fallbackData = new HashMap<>();
        fallbackData.put("id", id);
        fallbackData.put("name", "商品信息获取失败");
        fallbackData.put("price", BigDecimal.ZERO);
        fallbackData.put("stock", 0);
        return R.success(fallbackData);
    }

    @Override
    public R<Map<Long, Map<String, Object>>> getGoodsBatch(String ids) {
        return R.success(new HashMap<>());
    }

    @Override
    public R<Integer> getStock(Long id) {
        return R.success(0);
    }
}
