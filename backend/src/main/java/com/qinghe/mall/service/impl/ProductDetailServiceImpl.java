package com.qinghe.mall.service.impl;

import com.qinghe.mall.dao.ProductDetailDAO;
import com.qinghe.mall.dataobject.ProductDetailDO;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.ProductDetailService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public boolean updateStock(String id, Integer stock) {
        return productDetailDAO.updateStock(id, stock) > 0;
    }
}
