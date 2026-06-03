package com.qinghe.mall.service;

import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;

public interface ProductService {

    Paging<Product> queryPage(Integer pagination, Integer pageSize);

    Product findById(String id);

    Product add(Product product);

    Product update(Product product);

    boolean delete(String id);
}
