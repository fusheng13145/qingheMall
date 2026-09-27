package com.qinghe.mall.service.impl;

import com.qinghe.mall.exception.BusinessException;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghe.mall.config.ProductCacheService;
import com.qinghe.mall.config.elasticsearch.ProductIndexService;
import com.qinghe.mall.dao.ProductDAO;
import com.qinghe.mall.dataobject.ProductDO;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.util.UUIDUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductDAO productDAO;

    @Autowired
    private ProductDetailService productDetailService;

    @Autowired
    private ProductCacheService productCacheService;

    @Autowired
    private ProductIndexService productIndexService;

    @Autowired
    private com.qinghe.mall.service.UserAffinityService userAffinityService;

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Override
    public Paging<Product> queryPage(Integer pagination, Integer pageSize, String keyword, String brand, String sort) {
        try {
            return productIndexService.search(keyword, brand, null, null, sort, pagination, pageSize);
        } catch (Exception e) {
            log.warn("[ES] 商品分页搜索降级 MySQL: {}", e.getMessage());
            return queryPageMysql(pagination, pageSize, keyword, brand, sort, null);
        }
    }

    @Override
    public Paging<Product> queryOnSalePage(Integer pagination, Integer pageSize, String keyword, String brand, String sort) {
        try {
            return productIndexService.search(keyword, brand, "ON", null, sort, pagination, pageSize);
        } catch (Exception e) {
            log.warn("[ES] 在售商品搜索降级 MySQL: {}", e.getMessage());
            return queryPageMysql(pagination, pageSize, keyword, brand, sort, "ON");
        }
    }

    @Override
    public Paging<Product> queryMerchantPage(Long merchantId, String keyword, String status, int pageNum, int pageSize) {
        try {
            return productIndexService.search(keyword, null, status, merchantId, null, pageNum, pageSize);
        } catch (Exception e) {
            log.warn("[ES] 商家商品搜索降级 MySQL: {}", e.getMessage());
            return queryMerchantPageMysql(merchantId, keyword, status, pageNum, pageSize);
        }
    }

    private Paging<Product> queryPageMysql(Integer pagination, Integer pageSize, String keyword, String brand, String sort, String status) {
        Page<ProductDO> page = PageHelper.startPage(pagination, pageSize)
                .doSelectPage(() -> productDAO.queryAll(keyword, brand, sort, status));
        return toPaging(page, pagination, pageSize);
    }

    private Paging<Product> queryMerchantPageMysql(Long merchantId, String keyword, String status, int pageNum, int pageSize) {
        Page<ProductDO> page = PageHelper.startPage(pageNum, pageSize)
                .doSelectPage(() -> productDAO.queryByMerchantId(merchantId, keyword, status));
        return toPaging(page, pageNum, pageSize);
    }

    private Paging<Product> toPaging(Page<ProductDO> page, int pageNum, int pageSize) {
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
        // P1-10：热点读优先走缓存，未命中回源 DB 并回填
        Product cached = productCacheService.getProduct(id);
        if (cached != null) {
            return cached;
        }
        ProductDO productDO = productDAO.findById(id);
        if (productDO == null) {
            return null;
        }
        Product product = productDO.convertToModel();
        productCacheService.putProduct(product);
        return product;
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
        productCacheService.evict(productDO.getId());
        Product created = productDO.convertToModel();
        productIndexService.index(created);
        return created;
    }

    @Override
    public Product update(Product product) {
        ProductDO existing = productDAO.findById(product.getId());
        if (existing == null) {
            throw new BusinessException("商品不存在");
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
        productCacheService.evict(product.getId());
        productIndexService.index(product);
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
        productCacheService.evict(product.getId());
        Product saved = findById(product.getId());
        saved.setDetails(productDetailService.findByProductId(saved.getId()));
        return saved;
    }

    @Override
    public boolean delete(String id) {
        boolean deleted = productDAO.deleteById(id) > 0;
        if (deleted) {
            productCacheService.evict(id);
            productIndexService.delete(id);
        }
        return deleted;
    }


    /**
     * 个性化推荐（v1.10；v1.13 重构）：按用户偏好对在售商品重排。
     * 候选池取热销前 3×limit（ES 优先/MySQL 降级双路同语义）；
     * ① 排除已购商品（不足 limit 时按热销次序回补，保证返回密度）；
     * ② 品牌命中优先且组内保持热销次序；
     * 未登录或无偏好且无已购降级热销。跨域订单读已收敛至 UserAffinityService。
     */
    @Override
    public List<Product> recommendForUser(Long userId, int limit) {
        int size = Math.min(Math.max(limit, 1), 20);
        List<String> preferred = userAffinityService.preferredBrands(userId, 5);
        Set<String> purchased = userAffinityService.purchasedProductIds(userId);
        if (preferred.isEmpty() && purchased.isEmpty()) {
            return hotTop(userId, size);
        }
        Paging<Product> pool = queryOnSalePage(1, size * 3, null, null, "sales_desc");
        List<Product> candidates = pool.getData() == null ? Collections.emptyList() : pool.getData();

        // ① 品牌命中优先（稳定分区，组内保持热销次序）
        List<Product> matched = new ArrayList<>();
        List<Product> rest = new ArrayList<>();
        for (Product p : candidates) {
            if (p.getBrand() != null && preferred.contains(p.getBrand())) {
                matched.add(p);
            } else {
                rest.add(p);
            }
        }
        List<Product> ordered = new ArrayList<>(matched);
        ordered.addAll(rest);

        // ② 已购后移（新鲜度优先）：未购的按偏好序在前，不足 limit 时按偏好序回补已购
        List<Product> freshOrdered = new ArrayList<>();
        List<Product> boughtOrdered = new ArrayList<>();
        for (Product p : ordered) {
            (p.getId() != null && purchased.contains(p.getId()) ? boughtOrdered : freshOrdered).add(p);
        }
        List<Product> result = new ArrayList<>(freshOrdered);
        if (result.size() < size) {
            result.addAll(boughtOrdered);
        }
        return result.size() > size ? new ArrayList<>(result.subList(0, size)) : result;
    }

    /** 热销降级：未登录/无画像时取热销前 size（ES/MySQL 双路） */
    private List<Product> hotTop(Long userId, int size) {
        Paging<Product> pool = queryOnSalePage(1, size, null, null, "sales_desc");
        List<Product> data = pool.getData() == null ? Collections.emptyList() : pool.getData();
        return data.size() > size ? new ArrayList<>(data.subList(0, size)) : data;
    }
}