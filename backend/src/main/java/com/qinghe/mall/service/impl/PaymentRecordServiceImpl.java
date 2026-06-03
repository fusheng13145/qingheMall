package com.qinghe.mall.service.impl;

import com.qinghe.mall.dao.PaymentRecordDAO;
import com.qinghe.mall.dataobject.PaymentRecordDO;
import com.qinghe.mall.model.PaymentRecord;
import com.qinghe.mall.service.PaymentRecordService;
import com.qinghe.mall.util.UUIDUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentRecordServiceImpl implements PaymentRecordService {

    @Autowired
    private PaymentRecordDAO paymentRecordDAO;

    @Override
    public int insert(PaymentRecord paymentRecord) {
        PaymentRecordDO recordDO = new PaymentRecordDO();
        recordDO.setId(paymentRecord.getId() != null ? paymentRecord.getId() : UUIDUtils.uuid());
        recordDO.setUserId(paymentRecord.getUserId());
        recordDO.setOrderNumber(paymentRecord.getOrderNumber());
        recordDO.setChannelPaymentId(paymentRecord.getChannelPaymentId());
        recordDO.setChannelType(paymentRecord.getChannelType());
        recordDO.setAmount(paymentRecord.getAmount());
        recordDO.setPayType(paymentRecord.getPayType() != null ? paymentRecord.getPayType().name() : null);
        recordDO.setPayStatus(paymentRecord.getPayStatus() != null ? paymentRecord.getPayStatus().name() : null);
        recordDO.setExtendStr(paymentRecord.getExtendStr());
        recordDO.setPayEndTime(paymentRecord.getPayEndTime());
        recordDO.setGmtCreated(paymentRecord.getGmtCreated() != null ? paymentRecord.getGmtCreated() : new Date());
        recordDO.setGmtModified(paymentRecord.getGmtModified() != null ? paymentRecord.getGmtModified() : new Date());
        return paymentRecordDAO.insert(recordDO);
    }

    @Override
    public PaymentRecord findByOrderNumber(String orderNumber) {
        PaymentRecordDO recordDO = paymentRecordDAO.findByOrderNumber(orderNumber);
        if (recordDO == null) {
            return null;
        }
        return recordDO.convertToModel();
    }

    @Override
    public List<PaymentRecord> findByPayStatus(String payStatus) {
        List<PaymentRecordDO> recordDOs = paymentRecordDAO.findByPayStatus(payStatus);
        List<PaymentRecord> records = new ArrayList<>();
        for (PaymentRecordDO recordDO : recordDOs) {
            records.add(recordDO.convertToModel());
        }
        return records;
    }

    @Override
    public boolean updatePayStatus(String orderNumber, String payStatus, String channelPaymentId) {
        return paymentRecordDAO.updatePayStatus(orderNumber, payStatus, channelPaymentId) > 0;
    }
}
