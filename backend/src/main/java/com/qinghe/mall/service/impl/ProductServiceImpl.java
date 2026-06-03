package com.qinghe.mall.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghe.mall.dao.ProductDAO;
import com.qinghe.mall.dataobject.ProductDO;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.util.UUIDUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductDAO productDAO;

    @Override
    public Paging<Product> queryPage(Integer pagination, Integer pageSize) {
        Page<ProductDO> page = PageHelper.startPage(pagination, pageSize)
                .doSelectPage(() -> productDAO.queryAll());

        Paging<Product> paging = new Paging<>();
        paging.setPageNum(pagination);
        paging.setPageSize(pageSize);
        paging.setTotalPage(page.getPages());
        paging.setTotalCount(page.getTotal());

        List<Product> products = new ArrayList<>();
        for (ProductDO productDO : page.getResult()) {
            products.add(productDO.convertToModel());
        }
        paging.setData(products);
        return paging;
    }

    @Override
    public Product findById(String id) {
        ProductDO productDO = productDAO.findById(id);
        if (productDO == null) {
            return null;
        }
        return productDO.convertToModel();
    }

    @Override
    public Product add(Product product) {
        ProductDO productDO = new ProductDO();
        productDO.setId(UUIDUtils.uuid());
        productDO.setName(product.getName());
        productDO.setPrice(product.getPrice());
        productDO.setPurchaseNum(product.getPurchaseNum() != null ? product.getPurchaseNum() : 0);
        productDO.setProductIntro(product.getProductIntro());
        productDO.setProductImgs(product.getProductImgs());
        productDO.setGmtCreated(new Date());
        productDO.setGmtModified(new Date());
        productDAO.insert(productDO);
        return productDO.convertToModel();
    }

    @Override
    public Product update(Product product) {
        ProductDO existing = productDAO.findById(product.getId());
        if (existing == null) {
            throw new RuntimeException("商品不存在");
        }
        ProductDO productDO = new ProductDO();
        productDO.setId(product.getId());
        productDO.setName(product.getName());
        productDO.setPrice(product.getPrice());
        productDO.setPurchaseNum(product.getPurchaseNum());
        productDO.setProductIntro(product.getProductIntro());
        productDO.setProductImgs(product.getProductImgs());
        productDO.setGmtModified(new Date());
        productDAO.update(productDO);
        return productDAO.findById(product.getId()).convertToModel();
    }

    @Override
    public boolean delete(String id) {
        return productDAO.deleteById(id) > 0;
    }
}
