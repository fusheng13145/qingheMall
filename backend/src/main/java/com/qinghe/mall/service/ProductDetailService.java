package com.qinghe.mall.service;

import com.qinghe.mall.model.ProductDetail;
import java.util.List;

public interface ProductDetailService {

    List<ProductDetail> findByProductId(String productId);

    ProductDetail findById(String id);

    /** 批量查询规格（订单列表消除 N+1） */
    List<ProductDetail> findByIds(List<String> ids);

    boolean updateStock(String id, Integer stock);

    /**
     * 原子扣减库存（stock = stock - quantity，仅当 stock >= quantity 时生效）。
     *
     * @return true 表示扣减成功（库存充足），false 表示库存不足
     */
    boolean decreaseStock(String id, Integer quantity);

    /**
     * 回滚库存（stock = stock + quantity），用于取消订单/超时关单。
     */
    boolean increaseStock(String id, Integer quantity);

    /**
     * 整体替换商品规格（M6 商家商品保存）：先删后插（事务内）。
     * details 为空则仅清空该商品规格。
     */
    void replaceByProductId(String productId, List<ProductDetail> details);
}
