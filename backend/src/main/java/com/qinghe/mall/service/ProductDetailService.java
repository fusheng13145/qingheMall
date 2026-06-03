package com.qinghe.mall.service;

import com.qinghe.mall.model.ProductDetail;
import java.util.List;

public interface ProductDetailService {

    List<ProductDetail> findByProductId(String productId);

    ProductDetail findById(String id);

    boolean updateStock(String id, Integer stock);
}
