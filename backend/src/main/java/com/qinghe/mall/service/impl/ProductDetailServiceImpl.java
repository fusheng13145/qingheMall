package com.qinghe.mall.service.impl;

import com.qinghe.mall.dao.ProductDetailDAO;
import com.qinghe.mall.dataobject.ProductDetailDO;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.util.UUIDUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductDetailServiceImpl implements ProductDetailService {

    @Autowired
    private ProductDetailDAO productDetailDAO;

    @Override
    public List<ProductDetail> findByProductId(String productId) {
        List<ProductDetailDO> detailDOs = productDetailDAO.findByProductId(productId);
        List<ProductDetail> details = new ArrayList<>();
        for (ProductDetailDO detailDO : detailDOs) {
            details.add(detailDO.convertToModel());
        }
        return details;
    }

    @Override
    public ProductDetail findById(String id) {
        ProductDetailDO detailDO = productDetailDAO.findById(id);
        if (detailDO == null) {
            return null;
        }
        return detailDO.convertToModel();
    }

    @Override
    public List<ProductDetail> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<ProductDetailDO> detailDOs = productDetailDAO.findByIds(ids);
        List<ProductDetail> details = new ArrayList<>();
        for (ProductDetailDO detailDO : detailDOs) {
            details.add(detailDO.convertToModel());
        }
        return details;
    }

    @Override
    public boolean updateStock(String id, Integer stock) {
        return productDetailDAO.updateStock(id, stock) > 0;
    }

    @Override
    public boolean decreaseStock(String id, Integer quantity) {
        int qty = quantity != null && quantity > 0 ? quantity : 1;
        return productDetailDAO.decreaseStock(id, qty) > 0;
    }

    @Override
    public boolean increaseStock(String id, Integer quantity) {
        int qty = quantity != null && quantity > 0 ? quantity : 1;
        return productDetailDAO.increaseStock(id, qty) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceByProductId(String productId, List<ProductDetail> details) {
        productDetailDAO.deleteByProductId(productId);
        if (details == null || details.isEmpty()) {
            return;
        }
        for (ProductDetail detail : details) {
            if (detail == null || detail.getPrice() == null || detail.getStock() == null) {
                throw new RuntimeException("规格信息不完整（价格/库存必填）");
            }
            ProductDetailDO detailDO = new ProductDetailDO();
            detailDO.setId(UUIDUtils.uuid());
            detailDO.setProductId(productId);
            detailDO.setPrice(detail.getPrice());
            detailDO.setSize(detail.getSize() != null ? detail.getSize() : 0.0);
            detailDO.setStock(detail.getStock());
            detailDO.setGmtCreated(new Date());
            detailDO.setGmtModified(new Date());
            productDetailDAO.insert(detailDO);
        }
    }
}
