package com.qinghe.mall.service.impl;

import com.qinghe.mall.dao.StockLogDAO;
import com.qinghe.mall.dataobject.StockLogDO;
import com.qinghe.mall.service.StockLogService;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockLogServiceImpl implements StockLogService {

    @Autowired
    private StockLogDAO stockLogDAO;

    @Override
    public void record(String productDetailId, String productId, String orderNumber,
                       String changeType, int changeQuantity, int beforeStock, int afterStock) {
        if (StringUtils.isBlank(productDetailId) || StringUtils.isBlank(changeType)) {
            return;
        }
        StockLogDO log = new StockLogDO();
        log.setProductDetailId(productDetailId);
        log.setProductId(productId);
        log.setOrderNumber(orderNumber);
        log.setChangeType(changeType);
        log.setChangeQuantity(changeQuantity);
        log.setBeforeStock(beforeStock);
        log.setAfterStock(afterStock);
        log.setGmtCreated(new Date());
        stockLogDAO.insert(log);
    }
}
