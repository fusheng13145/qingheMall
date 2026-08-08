package com.qinghe.mall.service;

import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import java.util.List;

public interface ProductService {

    Paging<Product> queryPage(Integer pagination, Integer pageSize, String keyword, String brand, String sort);

    /** 顾客端在售商品分页（仅 status=ON，M6） */
    Paging<Product> queryOnSalePage(Integer pagination, Integer pageSize, String keyword, String brand, String sort);

    /** 商家店铺商品分页（按 merchant_id 归属过滤，M6） */
    Paging<Product> queryMerchantPage(Long merchantId, String keyword, String status, int pageNum, int pageSize);

    List<String> listBrands();

    Product findById(String id);

    /** 批量查询商品（订单列表消除 N+1） */
    List<Product> findByIds(List<String> ids);

    Product add(Product product);

    Product update(Product product);

    /**
     * 保存商品并整体替换规格（M6 商家用，单事务）：商品与 SKU 同生共死，
     * 任一失败整体回滚，杜绝孤儿商品/新旧不一致。
     */
    Product saveWithDetails(Product product, List<com.qinghe.mall.model.ProductDetail> details);

    boolean delete(String id);
}
