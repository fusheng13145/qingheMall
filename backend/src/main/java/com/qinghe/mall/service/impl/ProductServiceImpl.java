package com.qinghe.mall.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghe.mall.dao.ProductDAO;
import com.qinghe.mall.dataobject.ProductDO;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.util.UUIDUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductDAO productDAO;

    @Autowired
    private ProductDetailService productDetailService;

    @Override
    public Paging<Product> queryPage(Integer pagination, Integer pageSize, String keyword, String brand, String sort) {
        Page<ProductDO> page = PageHelper.startPage(pagination, pageSize)
                .doSelectPage(() -> productDAO.queryAll(keyword, brand, sort, null));

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
    public Paging<Product> queryOnSalePage(Integer pagination, Integer pageSize, String keyword, String brand, String sort) {
        Page<ProductDO> page = PageHelper.startPage(pagination, pageSize)
                .doSelectPage(() -> productDAO.queryAll(keyword, brand, sort, "ON"));

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
    public Paging<Product> queryMerchantPage(Long merchantId, String keyword, String status, int pageNum, int pageSize) {
        Page<ProductDO> page = PageHelper.startPage(pageNum, pageSize)
                .doSelectPage(() -> productDAO.queryByMerchantId(merchantId, keyword, status));
        Paging<Product> paging = new Paging<>();
        paging.setPageNum(pageNum);
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
    public List<String> listBrands() {
        return productDAO.listBrands();
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
    public List<Product> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<ProductDO> productDOs = productDAO.findByIds(ids);
        List<Product> products = new ArrayList<>();
        for (ProductDO productDO : productDOs) {
            products.add(productDO.convertToModel());
        }
        return products;
    }

    @Override
    public Product add(Product product) {
        ProductDO productDO = new ProductDO();
        productDO.setId(UUIDUtils.uuid());
        productDO.setName(product.getName());
        productDO.setBrand(product.getBrand());
        productDO.setMerchantId(product.getMerchantId());
        productDO.setStatus(product.getStatus() != null ? product.getStatus() : "ON");
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
        productDO.setBrand(product.getBrand());
        productDO.setMerchantId(product.getMerchantId());
        productDO.setStatus(product.getStatus());
        productDO.setPrice(product.getPrice());
        productDO.setPurchaseNum(product.getPurchaseNum());
        productDO.setProductIntro(product.getProductIntro());
        productDO.setProductImgs(product.getProductImgs());
        productDO.setGmtModified(new Date());
        productDAO.update(productDO);
        return productDAO.findById(product.getId()).convertToModel();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product saveWithDetails(Product product, List<ProductDetail> details) {
        if (StringUtils.isBlank(product.getId())) {
            // 新增：add 内部生成 id，须以返回值为准
            Product created = add(product);
            product.setId(created.getId());
        } else {
            update(product);
        }
        // SKU 整体替换与商品保存同事务：失败整体回滚，防孤儿商品
        productDetailService.replaceByProductId(product.getId(), details);
        Product saved = findById(product.getId());
        saved.setDetails(productDetailService.findByProductId(saved.getId()));
        return saved;
    }

    @Override
    public boolean delete(String id) {
        return productDAO.deleteById(id) > 0;
    }
}
